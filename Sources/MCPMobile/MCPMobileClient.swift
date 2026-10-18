import Foundation
import MCP

public enum MobileConnectionState: Sendable { case disconnected, connecting, connected, closed }
public enum MobileClientError: Error, Sendable { case invalidEndpoint, invalidState, timedOut }

/// Native integration over the official Swift MCP client. Own one per foreground session.
/// Sign-in UI and token persistence belong to the host; an upstream OAuth authorizer can be supplied.
public actor MCPMobileClient {
    private let endpoint: URL
    private let timeout: Duration
    private let tokenProvider: @Sendable () -> String?
    private let authorizer: (any HTTPClientAuthorizer)?
    private let client: Client
    public private(set) var state: MobileConnectionState = .disconnected

    public init(
        endpoint: URL,
        allowLoopbackHTTP: Bool = false,
        timeout: Duration = .seconds(30),
        clientName: String = "mcp-mobile",
        clientVersion: String = "0.1.0",
        authorizer: (any HTTPClientAuthorizer)? = nil,
        accessTokenProvider: @escaping @Sendable () -> String? = { nil }
    ) throws {
        try Self.validateEndpoint(endpoint, allowLoopbackHTTP: allowLoopbackHTTP)
        guard timeout > .zero && timeout <= .seconds(300) else { throw MobileClientError.invalidState }
        self.endpoint = endpoint
        self.timeout = timeout
        self.authorizer = authorizer
        self.tokenProvider = accessTokenProvider
        self.client = Client(name: clientName, version: clientVersion)
    }

    public nonisolated static func validateEndpoint(_ endpoint: URL, allowLoopbackHTTP: Bool = false) throws {
        guard let host = endpoint.host, !host.isEmpty, endpoint.user == nil,
              endpoint.password == nil, endpoint.fragment == nil else { throw MobileClientError.invalidEndpoint }
        let loopback = ["localhost", "127.0.0.1", "[::1]", "::1"].contains(host.lowercased())
        guard endpoint.scheme == "https" || (allowLoopbackHTTP && endpoint.scheme == "http" && loopback)
        else { throw MobileClientError.invalidEndpoint }
    }

    public func connect() async throws {
        guard state == .disconnected else { throw MobileClientError.invalidState }
        state = .connecting
        let configuration = URLSessionConfiguration.ephemeral
        configuration.urlCache = nil
        configuration.httpCookieStorage = nil
        configuration.urlCredentialStorage = nil
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 300
        let provider = tokenProvider
        let transport = HTTPClientTransport(
            endpoint: endpoint, configuration: configuration, streaming: true, authorizer: authorizer,
            requestModifier: { request in
                var request = request
                if let token = provider(), !token.isEmpty {
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                }
                return request
            }
        )
        let client = client
        do {
            _ = try await Self.deadline(timeout, operation: { try await client.connect(transport: transport) },
                                        cancel: { await client.disconnect() })
            try Task.checkCancellation()
            guard state == .connecting else { throw CancellationError() }
            state = .connected
        } catch {
            await client.disconnect()
            if state != .closed { state = .disconnected }
            throw error
        }
    }

    /// Returns the upstream result, including its pagination cursor.
    public func listTools(cursor: String? = nil) async throws -> ListTools.Result {
        guard state == .connected else { throw MobileClientError.invalidState }
        let request = cursor.map { ListTools.request(.init(cursor: $0)) } ?? ListTools.request(.init())
        let context = try await client.send(request)
        return try await response(context)
    }

    /// Preserves structuredContent and isError from the official result. No offline replay.
    public func callTool(name: String, arguments: [String: Value] = [:]) async throws -> CallTool.Result {
        guard state == .connected, !name.isEmpty else { throw MobileClientError.invalidState }
        let context = try await client.send(CallTool.request(.init(name: name, arguments: arguments)))
        return try await response(context)
    }

    public func close() async {
        state = .closed
        await client.disconnect()
    }

    private func response<T: Sendable & Decodable>(_ context: RequestContext<T>) async throws -> T {
        let client = client
        return try await Self.deadline(timeout, operation: { try await context.value }, cancel: {
            try? await client.cancelRequest(context.requestID, reason: "Host cancelled or request timed out")
        })
    }

    private nonisolated static func deadline<T: Sendable>(
        _ timeout: Duration,
        operation: @escaping @Sendable () async throws -> T,
        cancel: @escaping @Sendable () async -> Void
    ) async throws -> T {
        try await withThrowingTaskGroup(of: T.self) { group in
            group.addTask {
                try await withTaskCancellationHandler(operation: operation, onCancel: { Task { await cancel() } })
            }
            group.addTask { try await Task.sleep(for: timeout); throw MobileClientError.timedOut }
            defer { group.cancelAll() }
            return try await group.next()!
        }
    }
}
