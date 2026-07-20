# Design: Modrinth Auto Publish

## Trigger

```text
version command / release PR
  -> gradle.properties changes on master
     -> Publish Modrinth workflow
        -> validate repository configuration and release metadata
        -> Java 25 Gradle build + Server GameTests
        -> validate JAR metadata
        -> check existing Modrinth version
        -> create version through the official Modrinth API
```

The workflow does not run on pull requests. `workflow_dispatch` provides an explicit retry path without changing repository files.

## Configuration boundary

- `MODRINTH_PROJECT_ID` is a GitHub Actions repository variable because the stable ID is not a credential.
- `MODRINTH_TOKEN` is a GitHub Actions repository secret and is passed only to the publish step.
- The repository never stores or prints the token.

The project ID is resolved through the Modrinth API before upload so a private or draft project can be used, while the canonical ID is placed in the create-version request.

## Artifact and metadata

The publisher reads version, Minecraft version and archive name from `gradle.properties`. It requires matching release notes and verifies the built JAR contains `fabric.mod.json` with the same mod and Minecraft versions.

Stable releases are featured `release` versions. Names containing `beta` or `rc` map to `beta`; names containing `alpha` map to `alpha`. A manual dispatch may override the channel.

Fabric API (`P7dR8mSH`) is declared required. Trinkets Updated (`XaT8sLP6`) is declared optional, matching the mod metadata.

## Idempotency and failure safety

Before upload, the publisher lists project versions and compares `version_number`:

- no matching version: upload the JAR;
- matching version with the same primary SHA-512: finish successfully without uploading;
- matching version with a different primary SHA-512: fail and require a new version number.

The publisher never deletes or mutates an existing version. API, metadata, build and configuration errors fail the workflow before publication.
