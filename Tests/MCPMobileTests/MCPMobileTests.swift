import Foundation
import XCTest
import MCP
@testable import MCPMobile

final class MCPMobileTests: XCTestCase {
    func testRejectsInsecureAndCredentialBearingEndpoints() throws {
        for endpoint in ["http://example.com/mcp", "https://user:secret@example.com/mcp", "file:///mcp", "https://example.com/mcp#token"] {
            XCTAssertThrowsError(try MCPMobileClient(endpoint: URL(string: endpoint)!))
        }
        XCTAssertThrowsError(try MCPMobileClient(endpoint: URL(string: "http://192.168.1.2/mcp")!, allowLoopbackHTTP: true))
        _ = try MCPMobileClient(endpoint: URL(string: "http://127.0.0.1:9000/mcp")!, allowLoopbackHTTP: true)
    }

    func testCloseIsFinalAndIdempotent() async throws {
        let client = try MCPMobileClient(endpoint: URL(string: "https://example.com/mcp")!)
        await client.close()
        await client.close()
        let state = await client.state
        XCTAssertEqual(state, .closed)
        do { try await client.connect(); XCTFail("Closed client reconnected") }
        catch MobileClientError.invalidState { }
    }

    func testLiveCullyReadOnly() async throws {
        guard let path = ProcessInfo.processInfo.environment["CULLY_MOBILE_SESSION_FILE"] else {
            throw XCTSkip("Set CULLY_MOBILE_SESSION_FILE to a private, fresh read-only credential file")
        }
        let values = try JSONSerialization.jsonObject(with: Data(contentsOf: URL(fileURLWithPath: path))) as! [String: Any]
        let token = values["access_token"] as! String
        let client = try MCPMobileClient(endpoint: URL(string: values["endpoint"] as! String)!, accessTokenProvider: { token })
        do {
            try await client.connect()
            let tools = try await client.listTools()
            XCTAssertTrue(tools.tools.contains { $0.name == "cully_context" })
            let result = try await client.callTool(name: "cully_context", arguments: [
                "query": .string("mcp-mobile-release-smoke-never-match-6f4b0c2d"),
                "limit": .int(1), "mode": .string("text"),
                "project_url": .string("https://github.com/mcp-runtime/mcp-mobile"),
            ])
            XCTAssertNotEqual(result.isError, true)
            await client.close()
        } catch { await client.close(); throw error }
    }
}
