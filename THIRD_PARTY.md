# Upstream dependencies

MCP Mobile is MIT licensed. It wraps the official [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) (MIT), pinned to 0.15.0 for this release. That SDK owns MCP negotiation, JSON-RPC, protocol models, HTTP/SSE and session handling.

The POM records Ktor 3.5.1 (Apache-2.0), Kotlin 2.4.0 (Apache-2.0), kotlinx.coroutines 1.11.0 (Apache-2.0), and their transitive dependencies, including OkHttp/Okio (Apache-2.0) and SLF4J (MIT). Resolve dependencies from Maven Central and retain the notices required by their distributions when bundling an application.

Release jars contain this project's code only. The repository zip includes its POM and sources; it does not redistribute third-party runtime jars. `mvn dependency:tree` shows the resolved graph. The Swift adapter pins the official [Swift SDK](https://github.com/modelcontextprotocol/swift-sdk) 0.12.1; retain its MIT/Apache-2.0 notices and the notices of its resolved dependencies when distributing an app. Package.resolved records the Swift dependency revisions.
