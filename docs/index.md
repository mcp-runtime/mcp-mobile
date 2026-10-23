---
layout: home
title: Documentation
hero:
  name: MCP Mobile
  text: Native MCP clients for Android and iOS
  tagline: Small adapters around the official MCP Kotlin and Swift SDKs, for apps that need an MCP client.
  image:
    src: /logo.svg
    alt: MCP Mobile
  actions:
    - theme: brand
      text: Overview
      link: /overview/
    - theme: alt
      text: Android
      link: /android/
    - theme: alt
      text: iOS
      link: /ios/
    - theme: alt
      text: View on GitHub
      link: https://github.com/tavric-labs/mcp-mobile
features:
  - icon: 🧩
    title: Official SDKs underneath
    details: Protocol work is delegated to the official Kotlin and Swift SDKs. MCP Mobile does not implement another MCP protocol stack.
    link: /architecture/
    linkText: Architecture
  - icon: 🤖
    title: Android and JVM
    details: Kotlin suspend APIs plus a Java callback facade, with cancellable requests. Android API 26+, verified on API 30.
    link: /android/
    linkText: Android installation
  - icon: 🍎
    title: iOS and Apple platforms
    details: A Swift actor with async/await, a system-browser OAuth presenter and device-only Keychain token storage.
    link: /ios/
    linkText: iOS installation
  - icon: 🔑
    title: Host-owned credentials
    details: Your app supplies a current bearer token per request. Sign-in, refresh, secret storage and account policy stay in the host.
    link: /overview/
    linkText: Overview
  - icon: 🛡️
    title: Bounded and explicit
    details: HTTPS is enforced, with loopback HTTP only for development. Operations have timeouts, and there is no offline queue or automatic replay.
    link: /api/
    linkText: API guide
  - icon: 🧪
    title: Honest about verification
    details: This is a preview. The verification page lists the tested environments and the device checks that remain.
    link: /verification/
    linkText: Preview verification
---

<div class="home-section">

## How it fits

```mermaid
flowchart TB
    app["Your app: sign-in, token storage and UI"]
    mobile["MCP Mobile: connection, cancellation and credential lookup"]
    sdk["Official MCP SDK for Kotlin or Swift"]
    server["Your MCP server: identity, permissions and tools"]

    app -->|"current bearer token"| mobile
    mobile --> sdk
    sdk -->|"Streamable HTTP / SSE"| server
```

## Install

<div class="home-paths">
  <a class="home-path" href="./android/">
    <strong>Android</strong>
    <span>Add the release repository bundle as a local Maven repository, then use the Kotlin or Java client.</span>
  </a>
  <a class="home-path" href="./ios/">
    <strong>iOS</strong>
    <span>Add the Swift package at version 0.1.0 and select the <code>MCPMobile</code> product.</span>
  </a>
  <a class="home-path" href="./api/">
    <strong>API guide</strong>
    <span>Client signatures for Kotlin, Java and Swift, and the lifecycle rules.</span>
  </a>
  <a class="home-path" href="./publishing/">
    <strong>Publishing</strong>
    <span>How releases, Maven distribution and Swift Package Manager work.</span>
  </a>
</div>

</div>
