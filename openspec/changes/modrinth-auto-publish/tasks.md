# Tasks: Modrinth Auto Publish

## 1. Contract

- [x] 1.1 Define `mod_version` changes on `master` as the automatic publish trigger.
- [x] 1.2 Keep project ID and token outside the repository.
- [x] 1.3 Define idempotent same-hash behavior and refuse different-hash overwrites.

## 2. Implementation

- [x] 2.1 Add the GitHub Actions build-and-publish workflow.
- [x] 2.2 Add a dry-run-capable official Modrinth API publisher.
- [x] 2.3 Validate release notes, JAR metadata, game version and dependency metadata.
- [x] 2.4 Document repository variable, secret and manual retry setup.

## 3. Verification

- [x] 3.1 Shell syntax and workflow structure pass static validation.
- [x] 3.2 Publisher dry run validates the current release JAR.
- [x] 3.3 Java 25 `./gradlew build --stacktrace` passes.
- [x] 3.4 Pull request GitHub Actions pass.
- [x] 3.5 Configure the repository project ID and token for future Modrinth releases.
- [ ] 3.6 The first future version bump publishes or safely recognizes its Modrinth version.

## Evidence

- Shell syntax, workflow YAML parsing, `git diff --check`, secret-pattern scan and the 2.4.1 publisher dry run passed locally.
- [`Yunitrish006006/DeadRecall#63`](https://github.com/Yunitrish006006/DeadRecall/pull/63) commits `6cffe94` and `9540965` passed Build, Validate and GitGuardian Security Checks.
- Build included the publisher dry run, Server GameTests and both three-phase Dedicated Server restart probes.
- GitHub Actions variable `MODRINTH_PROJECT_ID` targets project `AM1TmETA`; repository secret `MODRINTH_TOKEN` is configured without storing its value in repository content.
- The existing remote 2.4.1 JAR and the current 2.4.1 build have different SHA-512 hashes, so a live dispatch is intentionally deferred until the next version bump instead of attempting to overwrite the published version.
