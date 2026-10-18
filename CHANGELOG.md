# Changelog

## 0.1.0 — Native mobile preview

- Replace the Expo chat demo and custom TypeScript `@mcp/core` implementation with a small Kotlin/JVM SDK around the official MCP Kotlin client 0.15.0.
- Add suspend APIs, a Java callback facade, request cancellation, explicit connection state, bounded operation timeouts and host-supplied bearer credentials.
- Add a Swift actor over the official MCP Swift SDK 0.12.1, an AuthenticationServices browser presenter and device-only Keychain token storage.
- Require HTTPS and allow explicit loopback HTTP development. Disable Kotlin HTTP redirects/connection retries and avoid wrapper-level offline tool-call replay on both platforms.
- Preserve upstream tool results and pagination; keep app UI, inference, permissions, sign-in and token persistence in the host application.
- Add local HTTP integration tests, an opt-in authenticated Cully MCP test, CI, source jars and a portable Maven repository release bundle.

The former demo is available in Git history. Native Cully application sources belong in [mcp-runtime/cully](https://github.com/mcp-runtime/cully), not this SDK repository. Physical iPhone verification remains pending.
