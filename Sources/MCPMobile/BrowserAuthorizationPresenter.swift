#if canImport(AuthenticationServices)
import AuthenticationServices
import Foundation
import MCP

/// System-browser OAuth UI for the upstream Swift authorizer; never an embedded credential form.
@MainActor
public final class BrowserAuthorizationPresenter: NSObject, OAuthAuthorizationDelegate,
    ASWebAuthenticationPresentationContextProviding {
    private let callbackScheme: String
    private let anchor: @MainActor () -> ASPresentationAnchor
    private let ephemeral: Bool
    private var session: ASWebAuthenticationSession?
    private var continuation: CheckedContinuation<URL, Error>?

    public init(callbackScheme: String, ephemeral: Bool = false,
                presentationAnchor: @escaping @MainActor () -> ASPresentationAnchor) {
        self.callbackScheme = callbackScheme
        self.ephemeral = ephemeral
        self.anchor = presentationAnchor
    }

    public func presentAuthorizationURL(_ url: URL) async throws -> URL {
        guard continuation == nil else { throw MobileClientError.invalidState }
        return try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                self.continuation = continuation
                let session = ASWebAuthenticationSession(url: url, callbackURLScheme: callbackScheme) { [weak self] url, error in
                    Task { @MainActor in self?.finish(url: url, error: error) }
                }
                self.session = session
                session.presentationContextProvider = self
                session.prefersEphemeralWebBrowserSession = ephemeral
                if !session.start() { finish(url: nil, error: MobileClientError.invalidState) }
            }
        } onCancel: { Task { @MainActor [weak self] in self?.cancel() } }
    }

    public func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor { anchor() }

    public func cancel() {
        session?.cancel()
        finish(url: nil, error: CancellationError())
    }

    private func finish(url: URL?, error: Error?) {
        guard let continuation else { return }
        self.continuation = nil
        self.session = nil
        if let url { continuation.resume(returning: url) }
        else { continuation.resume(throwing: error ?? CancellationError()) }
    }
}
#endif
