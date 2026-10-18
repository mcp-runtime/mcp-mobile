import Foundation
import MCP
import MCPMobile

/// Framework-free checks for Macs with Command Line Tools but no XCTest/Xcode installation.
@main struct MobileCheck {
    static func main() async {
        do {
            for invalid in ["http://example.com/mcp", "https://user:secret@example.com/mcp", "https://example.com/mcp#token"] {
                do { _ = try MCPMobileClient(endpoint: URL(string: invalid)!); throw CheckFailure.failed }
                catch MobileClientError.invalidEndpoint { }
            }
            let closed = try MCPMobileClient(endpoint: URL(string: "https://example.com/mcp")!)
            await closed.close(); await closed.close()
            do { try await closed.connect(); throw CheckFailure.failed }
            catch MobileClientError.invalidState { }
            var report: [String: Any] = ["sdk": "0.1.0", "officialSwiftSdk": "0.12.1", "endpointValidation": true, "closedLifecycle": true]
            if let path = ProcessInfo.processInfo.environment["CULLY_MOBILE_SESSION_FILE"] {
                let credential = try JSONSerialization.jsonObject(with: Data(contentsOf: URL(fileURLWithPath: path))) as! [String: Any]
                let token = credential["access_token"] as! String
                let client = try MCPMobileClient(endpoint: URL(string: credential["endpoint"] as! String)!, accessTokenProvider: { token })
                do {
                    try await client.connect()
                    let catalog = try await client.listTools()
                    guard catalog.tools.contains(where: { $0.name == "cully_context" }) else { throw CheckFailure.failed }
                    let result = try await client.callTool(name: "cully_context", arguments: [
                        "query": .string("mcp-mobile-release-smoke-never-match-6f4b0c2d"), "limit": .int(1),
                        "mode": .string("text"), "project_url": .string("https://github.com/tavric-labs/mcp-mobile"),
                    ])
                    guard result.isError != true else { throw CheckFailure.failed }
                    report["authenticatedCully"] = true; report["toolCount"] = catalog.tools.count
                    report["readTool"] = "cully_context"; report["writesPerformed"] = false
                    await client.close()
                } catch { await client.close(); throw error }
            }
            report["testedAt"] = ISO8601DateFormatter().string(from: Date())
            let data = try JSONSerialization.data(withJSONObject: report, options: [.sortedKeys])
            try FileManager.default.createDirectory(atPath: ".build", withIntermediateDirectories: true)
            try data.write(to: URL(fileURLWithPath: ".build/mobile-check-report.json"))
            print(String(decoding: data, as: UTF8.self))
        } catch {
            print("Mobile SDK check failed (\(type(of: error))). No credentials or server content logged.")
            exit(1)
        }
    }
    enum CheckFailure: Error { case failed }
}
