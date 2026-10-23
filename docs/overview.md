# Overview

MCP Mobile is a set of small native adapters around the **official MCP SDKs**, for apps that need an MCP client.

Android uses the official [Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk); iOS uses the official [Swift SDK](https://github.com/modelcontextprotocol/swift-sdk). This repository owns mobile connection ergonomics, cancellation, credential integration and native authentication helpers. It does not implement another MCP protocol stack or ship a chat application.

The [Cully apps](https://github.com/tavric-labs/cully/tree/main/apps) are the first consumers. App UI, model inference, skills, task policy and service URLs stay in Cully. Other apps configure their own servers.

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

This is a preview. See [verification](/verification/) for the tested environments and remaining device checks. There is no paid model API or inference dependency in this SDK.

## Where to go next

- [Android](/android/) and [iOS](/ios/): installation and first client.
- [API guide](/api/): client signatures and lifecycle rules.
- [Architecture](/architecture/): who owns what between your app, MCP Mobile, the official SDK and the server.

## Checks and release packaging

```sh
mvn -B verify
swift test --jobs 4                # Xcode includes XCTest
swift run --jobs 4 MCPMobileCheck # Also works with Swift Command Line Tools
python3 scripts/package_release.py
```

The opt-in live test reads `CULLY_MOBILE_SESSION_FILE`, a private JSON file with `endpoint` and a fresh `access_token`. It discovers tools and makes one bounded read-only Cully context call. Never commit that file, credentials, server content or device transcripts. Public CI uses fixtures and does not require a Cully account.

## Migration

The old Expo app and custom TypeScript `@mcp/core` have been retired. They remain in Git history. There is no TypeScript package or React Native bridge in 0.1.0. Native Cully app sources live in `tavric-labs/cully/apps`; this repository is reusable SDK code.

See also the [plan](https://github.com/tavric-labs/mcp-mobile/blob/main/PLAN.md) and [third-party licenses](https://github.com/tavric-labs/mcp-mobile/blob/main/THIRD_PARTY.md). MIT license.
