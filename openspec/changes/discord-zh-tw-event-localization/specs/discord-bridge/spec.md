# Discord Bridge Delta Specification

## ADDED Requirements

### Requirement: Discord-facing Minecraft text uses zh-TW

Discord Bridge SHALL render Minecraft-generated translatable text using `zh_tw` before creating the asynchronous HTTP payload. Rendering SHALL run entirely on the Server and SHALL NOT depend on Client-only language classes or a connected player locale.

#### Scenario: Advancement title is localized

- **GIVEN** a player completes an advancement whose display title is a Vanilla translatable Component
- **AND** the advancement is configured to announce in chat
- **WHEN** Discord Bridge creates the `advancement` event
- **THEN** the message SHALL contain the `zh_tw` advancement title
- **AND** the message SHALL NOT contain an unresolved translation key
- **AND** the event SHALL be sent exactly once

#### Scenario: Advancement frame type is localized

- **GIVEN** an advancement has semantic type `task`, `goal`, or `challenge`
- **WHEN** Discord Bridge formats the notification
- **THEN** the visible type SHALL be「進度」、「目標」or「挑戰」respectively
- **AND** the serialized English type SHALL NOT be shown as user-facing prose

### Requirement: Literal and custom text is preserved

Discord Bridge SHALL distinguish translatable Minecraft system text from literal or user-authored text. Player names, player chat, villager custom names, item custom names, and datapack literal Components SHALL remain unchanged except for existing safety normalization.

#### Scenario: Custom villager name is not translated

- **GIVEN** a villager has a custom literal name
- **WHEN** the villager levels up
- **THEN** the custom name SHALL appear unchanged in Discord
- **AND** profession and career level labels MAY still be localized around it

#### Scenario: Nested death message preserves names

- **GIVEN** a Vanilla death message contains a translatable template with player and custom item Components as arguments
- **WHEN** Discord Bridge renders the death message
- **THEN** the template SHALL be rendered in `zh_tw`
- **AND** the player name and custom item name SHALL remain unchanged

### Requirement: Villager level-up messages include localized semantic data

Villager level-up events SHALL include a localized villager display name, profession, previous career level, and current career level. Unnamed villagers SHALL use the localized generic name「村民」. Numeric-only level messages SHALL NOT be the sole user-facing representation.

#### Scenario: Unnamed librarian advances from level one to two

- **GIVEN** an unnamed librarian villager advances from career level 1 to level 2
- **WHEN** Discord Bridge creates the `villager_level_up` event
- **THEN** the message SHALL identify the entity as「村民」
- **AND** the profession SHALL be displayed in `zh_tw`
- **AND** both previous and current career levels SHALL have Chinese labels
- **AND** the message SHALL NOT contain `Villager`, `Librarian`, or English career level names while the `zh_tw` table is available

### Requirement: Other Minecraft system labels share one localization path

Death message templates, default entity or Boss names, raid result labels, difficulty display names, and future Minecraft translatable Components sent through Discord Bridge SHALL use the same Server-side localization service rather than event-specific `getString()` calls or duplicated hard-coded maps.

#### Scenario: Boss default name is localized

- **GIVEN** a defeated Boss uses a Vanilla translatable default name
- **WHEN** Discord Bridge creates the Boss event
- **THEN** the Boss name SHALL be rendered in `zh_tw`
- **AND** a custom literal Boss name SHALL remain unchanged

### Requirement: Localization failure is isolated and deterministic

Missing translation keys, incompatible format arguments, language resource failures, or resource reload races SHALL NOT interrupt the originating Minecraft event or crash the Server. The localization service SHALL use an immutable translation snapshot and a deterministic fallback chain.

#### Scenario: Translation key is missing

- **GIVEN** a Component references a key absent from the `zh_tw` table
- **WHEN** Discord Bridge renders the Component
- **THEN** it SHALL use a safe fallback text
- **AND** it SHALL NOT send the raw key as the final Discord message when another fallback is available
- **AND** it SHALL NOT generate a second Discord event

#### Scenario: Dedicated Server has no Client language runtime

- **GIVEN** DeadRecall starts on a pure Dedicated Server
- **WHEN** Discord localization initializes
- **THEN** initialization SHALL succeed without loading Client-only classes
- **AND** Discord Bridge failure isolation SHALL remain active if localization resources are unavailable

### Requirement: Transport and routing compatibility is preserved

The localization change SHALL NOT alter `/api/mc/chat`, existing payload field names, Discord event identifiers, configured channel routing, API key handling, Bot Token routing, or Webhook fallback behavior.

#### Scenario: Localized advancement uses existing route

- **GIVEN** an advancement notification is localized successfully
- **WHEN** the payload is sent to the Worker
- **THEN** it SHALL still use event `advancement`
- **AND** it SHALL use the same configured `channels` and fallback rules as before