# MCP Mobile direction

Make existing MCP SDKs straightforward to consume from native mobile applications. Keep the wrapper smaller than the protocol implementation it delegates to.

## First release

- Android/Kotlin adapter with a Java facade around the official Kotlin MCP client.
- Swift actor around the official Swift MCP client, system-browser OAuth presentation and a Keychain adapter.
- Discovery/call APIs preserve official result types, errors and pagination rather than inventing another protocol model.
- Explicit lifecycle, cancellation, bounded timeouts and host-owned credentials.
- Cully is the first application; its Android and iOS source, personal-task UI, model/voice runtime and skill live in the Cully repository.
- Local fixtures plus authenticated read-only Cully interoperability checks. Document physical-device limits honestly.
- Portable Maven repository zip and a tagged Swift package; no Maven Central publication claim.

## Next, after consuming-app evidence

1. Complete physical iPhone sign-in, Keychain and foreground/background verification.
2. Add Android lifecycle and standards-based browser-auth adapters where they reduce host code without hiding account policy.
3. Add prompts/resources through the upstream types when required by an application.
4. Automate publication to a public Maven registry, then add bridges for other app frameworks only when needed.

Do not bring back custom JSON-RPC, HTTP/SSE parsing, offline write replay, a bundled LLM provider, Cully-specific endpoints or an Expo application. Skills and agent orchestration are host application concerns.
