# MCP Mobile

Small native adapters around the **official MCP SDKs**, for apps that need an MCP client.

Android uses the official [Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk); iOS uses the official [Swift SDK](https://github.com/modelcontextprotocol/swift-sdk). This repository owns mobile connection ergonomics, cancellation, credential integration and native authentication helpers. It does not implement another MCP protocol stack or ship a chat application.

The [Cully apps](https://github.com/mcp-runtime/cully/tree/main/apps) are the first consumers. App UI, model inference, skills, task policy and service URLs stay in Cully. Other apps configure their own servers.

## 0.1.0 preview

| | Android / JVM | iOS / Apple platforms |
| --- | --- | --- |
| Upstream | MCP Kotlin SDK 0.15.0 | MCP Swift SDK 0.12.1 |
| API | Kotlin suspend + Java callbacks | Swift actor + async/await |
| Connection | Streamable HTTP/SSE | Streamable HTTP/SSE |
| Tools | Discovery, pagination, invocation | Discovery, pagination, invocation |
| Credentials | Host-supplied current bearer token | Host token provider or upstream OAuth authorizer |
| Native helpers | Cancellable Java requests | System-browser OAuth presenter, device-only Keychain token storage |
| Minimum | Android API 26+; verified on API 30 | iOS 16+, macOS 13+, Swift 6.1 |

Both adapters enforce HTTPS, with explicit loopback HTTP for development. Operations have bounded timeouts. No offline tool-call queue or wrapper-level automatic replay is added. Kotlin disables redirects and OkHttp connection retries; Swift uses the upstream Foundation transport and its configured OAuth challenge recovery. A cancelled action may already have run on the server.

This is a preview. See [verification](docs/VERIFICATION.md) for actual tested environments and remaining device checks. There is no paid model API or inference dependency in this SDK.

## Android installation

JDK 17 and Maven 3.9+ build the library:

```sh
mvn -B verify install
```

For a local Android project, add `mavenLocal()` and `mavenCentral()` to dependency repositories, then:

```kotlin
implementation("io.github.mcp-runtime:mcp-mobile-client:0.1.0")
```

Use Kotlin 2.4+ for Kotlin consumers. Java consumers use the callback facade. The artifact is **not published to Maven Central** in this preview. Alternatively, download the release's `mcp-mobile-client-0.1.0-repository.zip`, verify it against `SHA256SUMS`, extract it and add its `repository/` directory as a local Maven repository alongside Maven Central. The POM resolves upstream dependencies normally.

```kotlin
val client = McpMobileClient(
    endpoint = serverUrl,
    tokenProvider = AccessTokenProvider { credentialStore.currentAccessToken() }
)
try {
    client.connect()
    val page = client.listTools()
    // Present tools under your app's permission policy. page.nextCursor preserves pagination.
} finally {
    client.close()
}
```

In Java:

```java
McpMobileJavaClient client = new McpMobileJavaClient(
    serverUrl, () -> credentialStore.currentAccessToken());
McpRequest request = client.connect(new McpCallback() {
    public void onSuccess(String json) {
        // Now use listTools(cursor, callback) or callTool(name, argumentsJson, callback).
    }
    public void onError(String code, String message) {
        activity.runOnUiThread(() -> showConnectionError(message));
    }
});
// Cancel an individual operation: request.cancel();
// Close in the owning screen/service lifecycle: client.close();
```

Callbacks run on IO threads. Java errors are sanitized; optional `lastFailureTypes` contains exception class names only. Kotlin callers receive upstream exceptions and should avoid logging full responses or credentials. `close()` is final: create a new client for a new connection. Cancel Kotlin operation jobs before awaiting close; the Java facade cancels and releases its own jobs asynchronously.

## iOS installation

Add this repository as a Swift package with version `0.1.0`, then select the `MCPMobile` library product. Or add it to `Package.swift`:

```swift
.package(url: "https://github.com/mcp-runtime/mcp-mobile.git", from: "0.1.0")
// Target dependency:
.product(name: "MCPMobile", package: "mcp-mobile")
```

```swift
import MCPMobile

let client = try MCPMobileClient(endpoint: serverURL, accessTokenProvider: {
    credentials.currentAccessToken()
})
try await client.connect()
let page = try await client.listTools()
await client.close()
```

For interactive sign-in, supply the official `OAuthAuthorizer` with `BrowserAuthorizationPresenter` as its authorization delegate, and optionally `KeychainTokenStorage`. The host provides its callback scheme, presentation window and OAuth scope policy. Register the callback URL scheme in the app's Info.plist. See Cully's `apps/ios/Sources/CullyModel.swift` for a consuming application. No client secret belongs in a public mobile app.

## Checks and release packaging

```sh
mvn -B verify
swift test --jobs 4                # Xcode includes XCTest
swift run --jobs 4 MCPMobileCheck # Also works with Swift Command Line Tools
python3 scripts/package_release.py
```

The opt-in live test reads `CULLY_MOBILE_SESSION_FILE`, a private JSON file with `endpoint` and a fresh `access_token`. It discovers tools and makes one bounded read-only Cully context call. Never commit that file, credentials, server content or device transcripts. Public CI uses fixtures and does not require a Cully account.

## Migration

The old Expo app and custom TypeScript `@mcp/core` have been retired. They remain in Git history. There is no TypeScript package or React Native bridge in 0.1.0. Native Cully app sources live in `mcp-runtime/cully/apps`; this repository is reusable SDK code.

See [architecture](docs/ARCHITECTURE.md), [plan](PLAN.md), and [third-party licenses](THIRD_PARTY.md). MIT license.
