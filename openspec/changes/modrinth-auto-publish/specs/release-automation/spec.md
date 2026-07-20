# Delta Spec: Release Automation

## ADDED Requirements

### Requirement: New versions publish to Modrinth after validation

When `mod_version` metadata changes on `master`, DeadRecall MUST build and validate the matching release artifact before creating a Modrinth version.

#### Scenario: A new stable version reaches master

- **WHEN** `gradle.properties` changes on `master`
- **AND** `mod_version` has matching indexed release notes
- **AND** the Java 25 Gradle build and Server GameTests pass
- **THEN** the workflow uploads the matching DeadRecall JAR to the configured Modrinth project
- **AND** uses the release notes as its changelog
- **AND** declares Minecraft, Fabric, environment and dependency metadata

#### Scenario: A pull request builds

- **WHEN** unmerged changes run pull request validation
- **THEN** no Modrinth token is exposed
- **AND** no Modrinth version is created

### Requirement: Publication is idempotent and non-destructive

The publisher MUST inspect existing project versions before upload and MUST NOT replace an existing version artifact.

#### Scenario: The exact artifact was already published

- **WHEN** the configured project already has the same `version_number`
- **AND** its primary JAR has the same SHA-512 as the local artifact
- **THEN** the workflow succeeds without another upload

#### Scenario: The version number exists with different contents

- **WHEN** the configured project already has the same `version_number`
- **AND** its primary JAR has a different SHA-512
- **THEN** the workflow fails
- **AND** does not modify or delete the existing Modrinth version

### Requirement: Credentials remain external

The Modrinth token MUST remain in GitHub Actions secrets and MUST NOT be stored in repository content or emitted to logs.

#### Scenario: Publishing is not configured

- **WHEN** the project ID variable or token secret is absent
- **THEN** the workflow fails before the build and upload steps
- **AND** reports which configuration key is missing without revealing secret content
