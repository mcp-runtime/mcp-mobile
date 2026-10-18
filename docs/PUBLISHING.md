# Publishing SDKs

The `Release SDKs` workflow runs on `v*` tags, checks that the tag matches the Maven version/changelog, tests Kotlin and Swift, cross-compiles the iOS library and publishes a GitHub prerelease with checksummed SDK assets. A published release is not silently overwritten on rerun. Swift Package Manager consumes the same Git tag.

## Maven Central, after the GitHub release

Publication uses Sonatype Central Portal, not the retired OSSRH staging service. The POM includes SCM/developer/license metadata, sources and a documentation jar containing a hand-authored API guide. The `central-publish` profile signs artifacts and waits for Central publication.

One-time owner setup:

1. Sign in at https://central.sonatype.com and verify the `io.github.mcp-runtime` namespace. Repository administrator access alone does not create a Central publishing account.
2. Generate a Central user token. In this repository's GitHub Actions secrets, save its username as `MAVEN_CENTRAL_USERNAME` and token/password as `MAVEN_CENTRAL_TOKEN`.
3. Create or select a release-signing PGP key, publish its public key as required by Central, and save its ASCII-armored private key as `MAVEN_GPG_PRIVATE_KEY` and passphrase as `MAVEN_GPG_PASSPHRASE`. Do not paste them into issues, source or a coding chat.
4. After the GitHub release succeeds, run `Publish Maven Central` from Actions with that release tag, or `gh workflow run publish-central.yml -f tag=v0.1.0`.
5. Confirm the published coordinate in Central before changing installation docs to claim it is available there. Central releases are immutable; never reuse a version to replace artifacts.

Missing account verification or secrets blocks registry publication, but does not block the checked GitHub release and portable Maven repository distribution.

References: [Central Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/), [artifact requirements](https://central.sonatype.org/publish/requirements/), [namespace verification](https://central.sonatype.org/register/namespace/).

## Swift distribution and discovery

The public Git repository with a valid root Package.swift and semantic version tags is already a Swift Package Manager distribution source. Submit its `.git` URL to [Swift Package Index](https://swiftpackageindex.com/add-a-package) after a tagged build passes. The index is a catalog, not an artifact registry; maintainer acceptance and indexing happen externally.

For a separate Swift package registry, an actual service URL, package scope and publishing identity are required. Swift supports service registries, but there is no implicit universal upload destination. Do not claim a registry upload or index listing until its service confirms it.
