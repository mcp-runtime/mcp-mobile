# iOS installation

Add this repository as a Swift package with version `0.1.0`, then select the `MCPMobile` library product. Or add it to `Package.swift`:

```swift
.package(url: "https://github.com/tavric-labs/mcp-mobile.git", from: "0.1.0")
// Target dependency:
.product(name: "MCPMobile", package: "mcp-mobile")
```

Requirements: iOS 16+, macOS 13+, Swift 6.1.

## First client

```swift
import MCPMobile

let client = try MCPMobileClient(endpoint: serverURL, accessTokenProvider: {
    credentials.currentAccessToken()
})
try await client.connect()
let page = try await client.listTools()
await client.close()
```

## Interactive sign-in

For interactive sign-in, supply the official `OAuthAuthorizer` with `BrowserAuthorizationPresenter` as its authorization delegate, and optionally `KeychainTokenStorage`. The host provides its callback scheme, presentation window and OAuth scope policy. Register the callback URL scheme in the app's Info.plist.

- `BrowserAuthorizationPresenter` implements the upstream OAuth authorization delegate with AuthenticationServices.
- `KeychainTokenStorage` implements the upstream token storage protocol with device-only, when-unlocked Keychain items.
- No client secret belongs in a public mobile app.

See Cully's [`apps/ios/Sources/CullyModel.swift`](https://github.com/tavric-labs/cully/blob/main/apps/ios/Sources/CullyModel.swift) for a consuming application.

::: tip Verification status
The package compiles and has passed authenticated Cully read checks on macOS. Physical iPhone verification is separate; see [verification](/verification/).
:::
