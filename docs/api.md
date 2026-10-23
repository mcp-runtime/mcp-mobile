# API guide
Native adapters over the official MCP SDKs. Package: `org.mcpruntime.mobile` (Kotlin/Java); module: `MCPMobile` (Swift).

## Kotlin

```kotlin
McpMobileClient(endpoint, tokenProvider, allowLoopbackHttp, timeoutMillis)
suspend connect()
suspend listTools(cursor: String? = null): ListToolsResult
suspend callTool(name: String, arguments: JsonObject): CallToolResult
suspend close()
state: ConnectionState
```

The result and argument types come from the official MCP Kotlin SDK. `AccessTokenProvider.accessToken()` supplies a current bearer token for each HTTP request. HTTPS is required; explicit development HTTP is limited to loopback. Cancel operation jobs before closing a suspend client.

## Java

```java
McpMobileJavaClient(endpoint, tokenProvider)
McpRequest connect(McpCallback callback)
McpRequest listTools(String cursor, McpCallback callback)
McpRequest callTool(String name, String argumentsJson, McpCallback callback)
void close()
```

`McpCallback.onSuccess(String json)` and `onError(String code, String message)` run on IO threads. Dispatch UI changes to the main thread. `McpRequest.cancel()` cancels one operation. Closing cancels the facade's jobs and asynchronously releases transport resources. `lastFailureTypes` exposes exception class names only.

## Swift

```swift
actor MCPMobileClient
init(endpoint: URL, allowLoopbackHTTP: Bool = false,
     timeout: Duration = .seconds(30),
     clientName: String = "mcp-mobile", clientVersion: String = "0.1.0",
     authorizer: (any HTTPClientAuthorizer)? = nil,
     accessTokenProvider: @Sendable () -> String? = { nil }) throws
func connect() async throws
func listTools(cursor: String? = nil) async throws -> ListTools.Result
func callTool(name: String, arguments: [String: Value]) async throws -> CallTool.Result
func close() async
```

`BrowserAuthorizationPresenter` implements the upstream OAuth authorization delegate with AuthenticationServices. The host supplies its callback scheme and presentation window. `KeychainTokenStorage` implements the upstream token storage protocol with device-only, when-unlocked Keychain items.

## Lifecycle and policy

Close is final: create a new instance for a new connection. The host owns sign-in, refresh, permissions, UI and account policy. The SDK adds no offline action queue or automatic action replay. Cancellation and timeout do not imply remote rollback. Preserve pagination cursors and inspect tool-result errors before reporting success.

Source, README, examples and verification are on [GitHub](https://github.com/tavric-labs/mcp-mobile). This guide is also distributed as the Maven documentation artifact.
