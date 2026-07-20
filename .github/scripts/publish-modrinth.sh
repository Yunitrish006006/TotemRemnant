#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly API_BASE="${MODRINTH_API_BASE:-https://api.modrinth.com/v2}"
readonly USER_AGENT="Yunitrish006006/DeadRecall-GitHubActions/${GITHUB_RUN_ID:-local}"

cd "${REPOSITORY_ROOT}"

die() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

read_property() {
    local key="$1"
    local count
    local value

    count="$(awk -F= -v key="${key}" '$1 == key { count++ } END { print count + 0 }' gradle.properties)"
    [[ "${count}" == "1" ]] || die "gradle.properties must contain exactly one ${key}= entry"

    value="$(awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print }' gradle.properties)"
    [[ -n "${value}" ]] || die "gradle.properties contains an empty ${key} value"
    printf '%s' "${value}"
}

sha512_file() {
    local file="$1"

    if command -v sha512sum >/dev/null 2>&1; then
        sha512sum "${file}" | awk '{ print $1 }'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 512 "${file}" | awk '{ print $1 }'
    else
        die "sha512sum or shasum is required"
    fi
}

show_api_error() {
    local response_file="$1"

    if [[ -s "${response_file}" ]]; then
        jq . "${response_file}" >&2 2>/dev/null || sed -n '1,80p' "${response_file}" >&2
    fi
}

write_output() {
    local key="$1"
    local value="$2"

    if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
        printf '%s=%s\n' "${key}" "${value}" >> "${GITHUB_OUTPUT}"
    fi
}

write_summary() {
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
        printf '%s\n' "$*" >> "${GITHUB_STEP_SUMMARY}"
    fi
}

command -v jq >/dev/null 2>&1 || die "jq is required"
command -v unzip >/dev/null 2>&1 || die "unzip is required"

readonly MOD_VERSION="$(read_property mod_version)"
readonly MINECRAFT_VERSION="$(read_property minecraft_version)"
readonly ARCHIVES_BASE_NAME="$(read_property archives_base_name)"
readonly CHANGELOG_FILE="docs/releases/${MOD_VERSION}.md"
readonly ARTIFACT="${MODRINTH_ARTIFACT:-build/libs/${ARCHIVES_BASE_NAME}-${MOD_VERSION}.jar}"
readonly DRY_RUN="${MODRINTH_DRY_RUN:-false}"

case "${DRY_RUN}" in
    true|false) ;;
    *) die "MODRINTH_DRY_RUN must be true or false" ;;
esac

[[ -f "${CHANGELOG_FILE}" ]] || die "missing release notes: ${CHANGELOG_FILE}"
grep -F -- "- [${MOD_VERSION}](${MOD_VERSION}.md)" docs/releases/README.md >/dev/null \
    || die "docs/releases/README.md does not index ${MOD_VERSION}"
[[ -f "${ARTIFACT}" ]] || die "missing release artifact: ${ARTIFACT}; run ./gradlew build first"

readonly EMBEDDED_MOD_JSON="$(unzip -p "${ARTIFACT}" fabric.mod.json)"
jq -e --arg version "${MOD_VERSION}" --arg minecraft "${MINECRAFT_VERSION}" '
    .id == "deadrecall"
    and .version == $version
    and .depends.minecraft == $minecraft
' <<< "${EMBEDDED_MOD_JSON}" >/dev/null \
    || die "${ARTIFACT} metadata does not match gradle.properties"

version_type="${MODRINTH_VERSION_TYPE:-auto}"
case "${version_type}" in
    ""|auto)
        normalized_version="$(printf '%s' "${MOD_VERSION}" | tr '[:upper:]' '[:lower:]')"
        case "${normalized_version}" in
            *alpha*) version_type="alpha" ;;
            *beta*|*rc*) version_type="beta" ;;
            *) version_type="release" ;;
        esac
        ;;
    release|beta|alpha) ;;
    *) die "MODRINTH_VERSION_TYPE must be auto, release, beta, or alpha" ;;
esac
readonly VERSION_TYPE="${version_type}"

featured=false
if [[ "${VERSION_TYPE}" == "release" ]]; then
    featured=true
fi
readonly FEATURED="${featured}"

project_id="${MODRINTH_PROJECT_ID:-}"
if [[ -z "${project_id}" ]]; then
    if [[ "${DRY_RUN}" == "true" ]]; then
        project_id="dry-run-project"
    else
        die "MODRINTH_PROJECT_ID is required"
    fi
fi
readonly CONFIGURED_PROJECT_ID="${project_id}"

metadata_file="$(mktemp)"
canonical_metadata_file="$(mktemp)"
project_response="$(mktemp)"
versions_response="$(mktemp)"
game_versions_response="$(mktemp)"
publish_response="$(mktemp)"
cleanup() {
    rm -f -- "${metadata_file}" "${canonical_metadata_file}" "${project_response}" \
        "${versions_response}" "${game_versions_response}" "${publish_response}"
}
trap cleanup EXIT

jq -n \
    --arg name "DeadRecall ${MOD_VERSION}" \
    --arg version "${MOD_VERSION}" \
    --rawfile changelog "${CHANGELOG_FILE}" \
    --arg minecraft "${MINECRAFT_VERSION}" \
    --arg version_type "${VERSION_TYPE}" \
    --arg project_id "${CONFIGURED_PROJECT_ID}" \
    --argjson featured "${FEATURED}" \
    '{
        name: $name,
        version_number: $version,
        changelog: $changelog,
        dependencies: [
            {project_id: "P7dR8mSH", dependency_type: "required"},
            {project_id: "XaT8sLP6", dependency_type: "optional"}
        ],
        game_versions: [$minecraft],
        version_type: $version_type,
        loaders: ["fabric"],
        featured: $featured,
        status: "listed",
        project_id: $project_id,
        file_parts: ["primary"],
        primary_file: "primary",
        environment: "client_and_server"
    }' > "${metadata_file}"

readonly LOCAL_SHA512="$(sha512_file "${ARTIFACT}")"

if [[ "${DRY_RUN}" == "true" ]]; then
    printf 'Validated Modrinth dry run for %s (%s, Minecraft %s).\n' \
        "${MOD_VERSION}" "${VERSION_TYPE}" "${MINECRAFT_VERSION}"
    printf 'Artifact: %s\nSHA-512: %s\nMetadata:\n' "${ARTIFACT}" "${LOCAL_SHA512}"
    jq '.changelog = "<validated release notes>"' "${metadata_file}"
    write_output published false
    write_output skipped false
    write_output version "${MOD_VERSION}"
    write_summary "### Modrinth dry run: DeadRecall ${MOD_VERSION}"
    write_summary "Validated \`${ARTIFACT}\` without contacting Modrinth."
    exit 0
fi

command -v curl >/dev/null 2>&1 || die "curl is required"
readonly MODRINTH_TOKEN_VALUE="${MODRINTH_TOKEN:-}"
[[ -n "${MODRINTH_TOKEN_VALUE}" ]] || die "MODRINTH_TOKEN is required"

if ! curl --silent --show-error --fail-with-body --retry 3 \
    --header "Authorization: ${MODRINTH_TOKEN_VALUE}" \
    --header "User-Agent: ${USER_AGENT}" \
    --output "${project_response}" \
    "${API_BASE}/project/${CONFIGURED_PROJECT_ID}"; then
    show_api_error "${project_response}"
    die "could not read Modrinth project ${CONFIGURED_PROJECT_ID}"
fi

readonly CANONICAL_PROJECT_ID="$(jq -er '.id' "${project_response}")"
readonly PROJECT_SLUG="$(jq -er '.slug' "${project_response}")"
readonly PROJECT_TITLE="$(jq -er '.title' "${project_response}")"
jq --arg project_id "${CANONICAL_PROJECT_ID}" '.project_id = $project_id' \
    "${metadata_file}" > "${canonical_metadata_file}"
mv "${canonical_metadata_file}" "${metadata_file}"

if ! curl --silent --show-error --fail-with-body --retry 3 \
    --header "User-Agent: ${USER_AGENT}" \
    --output "${game_versions_response}" \
    "${API_BASE}/tag/game_version"; then
    show_api_error "${game_versions_response}"
    die "could not read Modrinth game version tags"
fi
jq -e --arg minecraft "${MINECRAFT_VERSION}" \
    'any(.[]; .version == $minecraft)' "${game_versions_response}" >/dev/null \
    || die "Modrinth does not recognize Minecraft ${MINECRAFT_VERSION}"

if ! curl --silent --show-error --fail-with-body --retry 3 \
    --header "Authorization: ${MODRINTH_TOKEN_VALUE}" \
    --header "User-Agent: ${USER_AGENT}" \
    --output "${versions_response}" \
    "${API_BASE}/project/${CANONICAL_PROJECT_ID}/version?include_changelog=false"; then
    show_api_error "${versions_response}"
    die "could not list existing versions for ${PROJECT_TITLE}"
fi

existing_id="$(jq -r --arg version "${MOD_VERSION}" \
    'first(.[] | select(.version_number == $version) | .id) // empty' "${versions_response}")"
if [[ -n "${existing_id}" ]]; then
    existing_sha512="$(jq -r --arg version "${MOD_VERSION}" '
        first(
            .[]
            | select(.version_number == $version)
            | (([.files[] | select(.primary == true)][0] // .files[0]) | .hashes.sha512)
        ) // empty
    ' "${versions_response}")"
    [[ -n "${existing_sha512}" ]] || die "existing Modrinth version ${MOD_VERSION} has no primary SHA-512"

    if [[ "${existing_sha512}" == "${LOCAL_SHA512}" ]]; then
        printf 'Modrinth version %s already contains this artifact; nothing to publish.\n' "${MOD_VERSION}"
        write_output published false
        write_output skipped true
        write_output version "${MOD_VERSION}"
        write_output version_id "${existing_id}"
        write_summary "### Modrinth publish skipped: DeadRecall ${MOD_VERSION}"
        write_summary "The existing primary JAR has the same SHA-512 hash."
        exit 0
    fi

    die "Modrinth version ${MOD_VERSION} already exists with a different primary JAR; refusing to overwrite it"
fi

if ! curl --silent --show-error --fail-with-body \
    --request POST \
    --header "Authorization: ${MODRINTH_TOKEN_VALUE}" \
    --header "User-Agent: ${USER_AGENT}" \
    --form "data=@${metadata_file};type=application/json" \
    --form "primary=@${ARTIFACT};type=application/java-archive" \
    --output "${publish_response}" \
    "${API_BASE}/version"; then
    show_api_error "${publish_response}"
    die "Modrinth rejected DeadRecall ${MOD_VERSION}"
fi

readonly PUBLISHED_VERSION_ID="$(jq -er '.id' "${publish_response}")"
readonly PUBLISHED_VERSION_NUMBER="$(jq -er '.version_number' "${publish_response}")"
[[ "${PUBLISHED_VERSION_NUMBER}" == "${MOD_VERSION}" ]] \
    || die "Modrinth returned unexpected version ${PUBLISHED_VERSION_NUMBER}"

printf 'Published DeadRecall %s to https://modrinth.com/mod/%s/version/%s\n' \
    "${MOD_VERSION}" "${PROJECT_SLUG}" "${PUBLISHED_VERSION_ID}"
write_output published true
write_output skipped false
write_output version "${MOD_VERSION}"
write_output version_id "${PUBLISHED_VERSION_ID}"
write_summary "### Published DeadRecall ${MOD_VERSION} to Modrinth"
write_summary "https://modrinth.com/mod/${PROJECT_SLUG}/version/${PUBLISHED_VERSION_ID}"
