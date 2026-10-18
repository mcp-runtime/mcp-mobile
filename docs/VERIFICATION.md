# Preview verification

SDK tests use local fixtures unless explicitly opted in. This document records the release gate; it is not a claim of production certification.

## Verified locally

- Kotlin/JVM on JDK 17: initialization, initialized notification ordering, tool discovery, pagination cursor preservation, structured tool results, credential refresh lookup, rejected insecure endpoints, auth error reporting, timeout cleanup, close/reconnect rules and no wrapper retry of a failed tool action.
- Official Kotlin SDK 0.15.0 against authenticated Cully MCP: discovery and a bounded `cully_context` read, using a fresh browser-issued read-only credential.
- Cully Android app on Android 11/API 30, arm64: packaged the SDK and its real runtime dependencies, connected to Cully MCP, discovered personal tools and completed a bounded context read. The private debug credential handoff was deleted after consumption.
- Swift 6.1.2/macOS 15.5: compiled the Swift adapter, system-browser presenter and Keychain integration; ran endpoint/lifecycle checks and an authenticated Cully catalog/context read. No server content is recorded in proof files.

## Remaining platform checks

- This Mac has Command Line Tools but no full Xcode/XCTest installation yet. XCTest and iOS build checks run in GitHub CI with Xcode; record their outcome before calling the release checked.
- Physical iPhone deployment, browser callback, microphone and Keychain round-trip still require pairing, signing and a compatible Xcode installation. A macOS build does not establish iPhone behavior.
- The Android app's browser sign-in UI is a consuming-app concern. SDK live tests use host-provided bearer tokens; they do not establish browser-flow coverage on every device.

Use the release notes and CI links for the final run evidence. No credentials, user memory, personal task titles or raw transcripts belong in this document or in release assets.
