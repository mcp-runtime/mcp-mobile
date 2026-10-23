# Release notes

## 0.1.0 preview

The first native MCP Mobile SDK preview wraps the official MCP Kotlin SDK 0.15.0 and Swift SDK 0.12.1.

- Kotlin suspend APIs and a Java callback facade, with cancellable requests, host-supplied credentials, explicit connection state and bounded operations.
- Swift async APIs, system-browser OAuth presentation and a device-only Keychain token adapter.
- Tool discovery, pagination and invocation preserve upstream protocol models. The SDK adds no offline action replay or model provider.
- The former Expo demo and custom TypeScript `@mcp/core` are retired. Cully's app sources live in `tavric-labs/cully/apps`.

**Install:** Swift Package Manager uses this release tag and the `MCPMobile` product. Android consumers can extract the Maven repository bundle and add its `repository/` directory as a local Maven repository. The Android artifact is not on Maven Central. Upstream runtime dependencies resolve through the POM. Verify downloaded artifacts using `SHA256SUMS`.

**Validation:** Kotlin and Swift checks, an iOS SDK cross-build, plus read-only interoperability tests against authenticated Cully MCP. The SDK was also consumed by Cully on an Android 11/API 30 device. See `docs/VERIFICATION.md` and CI for the evidence and limits. Physical iPhone deployment, browser callback and Keychain behavior still require device validation; this is a prerelease preview.

The SDK includes no paid inference API. Local model and speech engines belong to the consuming Cully app.
