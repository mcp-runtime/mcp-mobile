# Mobile integration without a second protocol implementation

Applications consume MCP Mobile. MCP Mobile delegates protocol work to the official Kotlin SDK, using Ktor's OkHttp engine on Android. The server remains the authority for identity, permissions and tool behavior.

For Cully, the path is `Cully Android app → MCP Mobile → official MCP Kotlin SDK → Cully MCP → Cully services`. Self mode is the first application scope. Neither the generic SDK nor a client-supplied owner parameter grants access to another user's records. Private data API, database and Mem0 credentials stay server-side.

The wrapper owns endpoint validation, bounded operations, explicit connection state, host credential lookup and a Java callback facade. The official SDK owns initialization, protocol versions, messages, tool/resource models, HTTP/SSE and session IDs. A reconnect creates a new wrapper instance. SSE stream recovery in the upstream transport is separate from replaying a tool invocation; MCP Mobile never adds an offline write queue.

The host owns sign-in/refresh, secret storage, account selection, Android lifecycle and UI. Credentials are retrieved at request time. Closing the Java facade cancels its jobs and asynchronously releases the transport; Kotlin hosts cancel their operation jobs before awaiting `close()`. A cancelled/timed-out action may already have executed remotely. The host must check its result before deciding whether to repeat it.

Callbacks run on IO threads. Android applications dispatch view changes to the main thread and close the session when its owning screen leaves the foreground. No phone data, prompt, tool output or token is logged by the wrapper. Generic connection errors are sanitized for the Java facade; Kotlin callers receive upstream exceptions and must avoid logging credentials or full responses.

The preview exposes tool discovery and invocation. Resources, prompts, server-driven requests and richer lifecycle bindings should be added only with a consuming-app requirement and interoperability coverage. The Swift adapter uses the official Swift SDK with async request cancellation, an AuthenticationServices browser presenter and device-only Keychain token storage. The host supplies an OAuth scope policy and presentation anchor. The package compiles and has passed authenticated Cully read checks on macOS; physical iPhone verification is separate.
