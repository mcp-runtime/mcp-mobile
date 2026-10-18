// swift-tools-version: 6.1
import PackageDescription

let package = Package(
    name: "MCPMobile",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "MCPMobile", targets: ["MCPMobile"])],
    dependencies: [.package(url: "https://github.com/modelcontextprotocol/swift-sdk.git", exact: "0.12.1")],
    targets: [
        .target(name: "MCPMobile", dependencies: [.product(name: "MCP", package: "swift-sdk")]),
        .executableTarget(name: "MCPMobileCheck", dependencies: ["MCPMobile", .product(name: "MCP", package: "swift-sdk")]),
        .testTarget(name: "MCPMobileTests", dependencies: ["MCPMobile", .product(name: "MCP", package: "swift-sdk")]),
    ]
)
