#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly BASELINE_FILE="${REPOSITORY_ROOT}/openspec/changes/safe-multi-repo-modularization/compatibility-surface.txt"

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$1" >&2
        exit 1
    }
}

collect_identifiers() {
    find src/main/java src/client/java src/main/resources \
        -type f \
        \( -name '*.java' -o -name '*.json' -o -name '*.json5' -o -name '*.mcmeta' \
            -o -name '*.properties' -o -name '*.txt' \) \
        -print0 \
        | xargs -0 awk '
            {
                remaining = $0
                while (match(remaining, /deadrecall:[a-z0-9_\.\/-]+/)) {
                    print "identifier " substr(remaining, RSTART, RLENGTH)
                    remaining = substr(remaining, RSTART + RLENGTH)
                }

                remaining = $0
                factory = "Identifier\\.fromNamespaceAndPath\\(\"deadrecall\",[[:space:]]*\"[a-z0-9_./-]+\"\\)"
                while (match(remaining, factory)) {
                    identifier = substr(remaining, RSTART, RLENGTH)
                    sub(/^.*\"deadrecall\",[[:space:]]*\"/, "", identifier)
                    sub(/\".*$/, "", identifier)
                    print "identifier deadrecall:" identifier
                    remaining = substr(remaining, RSTART + RLENGTH)
                }
            }
        '
}

collect_surface() {
    cd "${REPOSITORY_ROOT}"

    {
        find src/main/resources/assets/deadrecall src/main/resources/data/deadrecall \
            -type f \
            | sed 's#^src/main/resources/#resource #'

        collect_identifiers
    } | LC_ALL=C sort -u
}

for required_command in awk diff find mktemp sed sort xargs; do
    require_command "${required_command}"
done

if [[ "${1:-}" == "--print" ]]; then
    collect_surface
    exit 0
fi

[[ -f "${BASELINE_FILE}" ]] || {
    printf 'Missing modularization compatibility baseline: %s\n' "${BASELINE_FILE}" >&2
    printf 'Generate candidate content with: %s --print\n' "$0" >&2
    exit 1
}

current_surface="$(mktemp)"
trap 'rm -f -- "${current_surface}"' EXIT
collect_surface > "${current_surface}"

if ! diff -u "${BASELINE_FILE}" "${current_surface}"; then
    printf '\nCompatibility surface changed.\n' >&2
    printf 'Do not update the baseline until the owning module, migration path, and assembled bundle coverage are documented.\n' >&2
    exit 1
fi

printf 'Modularization compatibility surface matches the committed baseline.\n'
