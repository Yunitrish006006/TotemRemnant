# Copper Golem LLM asynchronous regression

Copper Golem LLM requests use a shared atomic request gate. Each query key can have at most one in-flight request, and failed requests enter a 60-second retry cooldown.

## Query generations

- Gathering block classification keys include the gathering Prompt revision.
- Sorting item classification keys include the normalized binding Prompt.
- Tag lists are canonicalized before key construction, so order and duplicate tag entries do not create duplicate requests.

## Callback authority

LLM responses are treated as delayed, untrusted callbacks:

- Gathering responses are written only when their Prompt revision still matches the golem state.
- Sorting responses are written only when the binding still exists, its classifier is enabled, and its current normalized Prompt matches the request Prompt.
- Changing a Prompt may start a new request even while the previous Prompt request is still pending.
- A stale allow or deny response cannot replace the cache belonging to the current Prompt.
- Pending and retry state is cleared when the Minecraft server stops, so an interrupted callback cannot block a later integrated-server session.

## Automated coverage

`LlmRequestGateTest` verifies:

- atomic concurrent pending de-duplication;
- immediate release after success;
- failure cooldown and retry at the exact deadline;
- cancellation without cooldown;
- server-shutdown clearing of pending and retry state;
- Prompt/revision separation and tag canonicalization in query keys.

`CopperGolemLlmAsyncGameTest` verifies delayed callback behavior against a real Copper Golem entity and its persisted custom data.
