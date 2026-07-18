# Copper Golem Delta Specification

## ADDED Requirements

### Requirement: Focused text fields own printable keyboard input

When an `EditBox` inside the Copper Golem management screen is focused, container-level keyboard shortcuts SHALL NOT close the screen or execute DeadRecall container actions before the text input path receives the character.

#### Scenario: Typing the inventory key in a Prompt

- **GIVEN** the Copper Golem Prompt field is visible and focused
- **AND** the Inventory key is bound to `E`
- **WHEN** the user types lowercase or uppercase `e`
- **THEN** the character SHALL be inserted into the Prompt
- **AND** the screen SHALL remain open
- **AND** no container operation SHALL be executed

#### Scenario: Typing while the sort shortcut is focused

- **GIVEN** any Copper Golem `EditBox` is focused
- **WHEN** the user presses the configured DeadRecall container-sort key
- **THEN** the key SHALL be handled as text input when applicable
- **AND** no sort request SHALL be sent

### Requirement: Normal container shortcuts remain available outside text input

The fix SHALL be conditional on an active focused `EditBox`. When no text field is focused, Vanilla Inventory key handling and DeadRecall container-sort handling SHALL remain unchanged.

#### Scenario: Close after leaving the text field

- **GIVEN** the Copper Golem screen is open
- **AND** no `EditBox` is focused
- **WHEN** the user presses the Inventory key
- **THEN** the screen SHALL close using the normal Vanilla path

#### Scenario: Escape remains available

- **GIVEN** a Copper Golem text field is focused
- **WHEN** the user presses Escape
- **THEN** the screen SHALL close using the normal Screen behavior

### Requirement: All Copper Golem text fields share the same policy

Sorting Prompt, Gathering Prompt, API URL, API Key, and Model fields SHALL use the same focused-text shortcut protection. The fix SHALL NOT alter their maximum lengths, visibility rules, payloads, permission checks, or Server-side revision validation.
