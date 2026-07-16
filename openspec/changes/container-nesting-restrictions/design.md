# Design: Container Nesting Restrictions

## Classification

Introduce one shared server-side policy service for portable-container classification and insertion checks.

Suggested categories:

- `DEADRECALL_BACKPACK`
- `VANILLA_BUNDLE`
- `SHULKER_BOX`
- `CONFIGURED_PORTABLE_CONTAINER`

The default denied matrix is bidirectional between `DEADRECALL_BACKPACK` and every other restricted portable-container category. Classification should prefer item tags or registered predicates rather than scattered item-ID comparisons.

## Validation rule

Every insertion path SHALL call an equivalent policy:

```text
canInsert(destinationContainer, candidateStack, context)
```

The Server must inspect the candidate ItemStack and destination container identity. Client-side checks may improve UX but are never authoritative.

Covered paths include:

- Normal click, drag and number-key swap.
- Shift-click / quick move.
- Pickup-all and double-click collection.
- Bundle-specific insertion.
- Shulker Box inventory insertion.
- Hopper and hopper minecart transfer.
- Dropper or dispenser insertion behavior.
- Container APIs used by DeadRecall menus.
- Death capture, recovery and rollback transactions.
- Commands or addon APIs that route through exposed DeadRecall insertion helpers.

## Existing invalid data

- Loading an already nested item is permitted.
- Taking an invalid child item out is permitted.
- Moving the parent container as an opaque ItemStack is permitted unless doing so inserts it into another restricted portable container.
- Reinserting the invalid child into the same restricted parent is rejected.
- No automatic deletion, flattening or world migration is performed in 2.4.1.
- Admin diagnostics should be able to report invalid nesting without mutating it.

## Death capture

Death capture must never recursively package a restricted portable container into a DeadRecall backpack. Restricted stacks remain in the original death transaction and follow the configured fallback path, normally vanilla world drop. Rollback must restore each stack exactly once.

## Extensibility

Provide tags or a small registry/predicate API so future Totem modules and addons can mark portable containers as restricted without depending on concrete DeadRecall implementation classes.

## User feedback

Rejected manual insertion returns the item to its original slot/cursor and displays a localized message. Automation rejects the transfer without consuming or duplicating the stack and may emit rate-limited diagnostics.