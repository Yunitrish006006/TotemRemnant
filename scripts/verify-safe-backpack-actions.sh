#!/usr/bin/env bash
# Local/CI verification only; never uploads or publishes artifacts.
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"
core_jar="${1:-../TotemCore/build/libs/totem-core-0.7.23.jar}"
vanilla_jar="${2:-../TotemVanillaTweaks/build/libs/totem-vanilla-tweaks-0.1.31.jar}"
test -f "$core_jar"
test -f "$vanilla_jar"
artifacts="${TOTEM_ACTIONS_ARTIFACTS:-$repo_root/test-artifacts/safe-actions-$(date +%Y%m%d%H%M%S)}"
mkdir -p "$(dirname "$artifacts")"
# Reject reused evidence directories: old phase markers must never prove a new run.
mkdir "$artifacts"
artifacts="$(cd "$artifacts" && pwd)"
backend="${TOTEM_GRAPHICS_BACKEND:-VULKAN}"
case "$backend" in VULKAN|OPENGL) ;; *) echo 'Unsupported graphics backend'; exit 2 ;; esac
if [[ -z "${VK_DRIVER_FILES:-}" && "$backend" == VULKAN ]]; then
    for driver in /usr/share/vulkan/icd.d/lvp_icd*.json; do
        if [[ -f "$driver" ]]; then export VK_DRIVER_FILES="$driver"; break; fi
    done
fi
init_script="$artifacts/runtime.gradle"
cat > "$init_script" <<GROOVY
allprojects {
    afterEvaluate {
        tasks.matching { it.name == 'runProductionClientGameTest' }.configureEach {
            programArgs.addAll(['--graphicsBackend', '$backend'])
        }
        tasks.matching { it.name == 'runRemnantRestartProbe' }.configureEach {
            args('--port', '0')
        }
    }
}
GROOVY
sha512sum "$core_jar" "$vanilla_jar" > "$artifacts/dependencies.sha512"
git rev-parse HEAD > "$artifacts/source-head.txt"
git ls-files --cached --others --exclude-standard src build.gradle gradle.properties scripts \
    | sort -u | while IFS= read -r path; do sha256sum "$path"; done > "$artifacts/source.sha256"
run_check() {
    local label="$1"
    shift
    echo "START $label"
    if ./gradlew "-PtotemCoreJar=$core_jar" --init-script "$init_script" --console=plain "$@" > "$artifacts/$label.log" 2>&1; then
        echo "PASS $label"
    else
        tail -50 "$artifacts/$label.log"
        echo "FAIL $label; artifacts: $artifacts"
        return 1
    fi
}
run_check server test assemble compileGametestJava runGameTest
if [[ -d run/screenshots ]]; then mv run/screenshots "$artifacts/screenshots-before"; fi
run_check client-present -PtotemBackpackActionsOnly=true -PtotemExpectedVanillaTweaks=true "-PtotemVisualModuleJars=$vanilla_jar" runProductionClientGameTest
if [[ -d run/screenshots ]]; then mv run/screenshots "$artifacts/client-present-screenshots"; fi
run_check client-absent -PtotemBackpackActionsOnly=true -PtotemExpectedVanillaTweaks=false runProductionClientGameTest
if [[ -d run/screenshots ]]; then mv run/screenshots "$artifacts/client-absent-screenshots"; fi
run_check restart-seed -PtotemRemnantRestartProbePhase=seed "-PtotemRemnantRestartProbeMarkerDir=$artifacts/restart" runRemnantRestartProbe
test -f "$artifacts/restart/seed.ok"
test ! -e "$artifacts/restart/seed.failure"
run_check restart-verify -PtotemRemnantRestartProbePhase=verify "-PtotemRemnantRestartProbeMarkerDir=$artifacts/restart" runRemnantRestartProbe
test -f "$artifacts/restart/verify.ok"
test ! -e "$artifacts/restart/verify.failure"
sha512sum build/libs/totem-remnant-*.jar > "$artifacts/remnant-artifacts.sha512"
echo "Validation completed: $artifacts"
