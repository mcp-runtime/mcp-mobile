# Android installation

JDK 17 and Maven 3.9+ build the library:

```sh
mvn -B verify install
```

For a local Android project, add `mavenLocal()` and `mavenCentral()` to dependency repositories, then:

```kotlin
implementation("io.github.mcp-runtime:mcp-mobile-client:0.1.0")
```

Use Kotlin 2.4+ for Kotlin consumers. Java consumers use the callback facade.

::: warning Not on Maven Central
The artifact is **not published to Maven Central** in this preview. Download the release's `mcp-mobile-client-0.1.0-repository.zip`, verify it against `SHA256SUMS`, extract it and add its `repository/` directory as a local Maven repository alongside Maven Central. The POM resolves upstream dependencies normally.
:::

## Kotlin

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

## Java

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

## Lifecycle notes

- Callbacks run on IO threads. Dispatch view changes to the main thread.
- Java errors are sanitized; optional `lastFailureTypes` contains exception class names only. Kotlin callers receive upstream exceptions and should avoid logging full responses or credentials.
- `close()` is final: create a new client for a new connection. Cancel Kotlin operation jobs before awaiting close; the Java facade cancels and releases its own jobs asynchronously.

Next: the [API guide](/api/) and the [architecture](/architecture/) notes on cancellation and credentials.
