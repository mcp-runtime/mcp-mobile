#if canImport(Security)
import Foundation
import Security
import MCP

/// Device-only Keychain storage for the official SDK's OAuth token model. No iCloud synchronization.
public final class KeychainTokenStorage: TokenStorage, Sendable {
    private let service: String
    private let account: String

    /// Use a distinct account per server/account selection. Signing in is still owned by the host.
    public init(service: String, account: String) { self.service = service; self.account = account }

    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service, kSecAttrAccount as String: account,
         kSecAttrSynchronizable as String: false]
    }

    public func save(_ token: OAuthAccessToken) {
        guard let data = try? JSONEncoder().encode(token) else { return }
        let attributes: [String: Any] = [kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            _ = SecItemAdd(query.merging(attributes) { _, new in new } as CFDictionary, nil)
        }
    }

    public func load() -> OAuthAccessToken? {
        var query = query
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return try? JSONDecoder().decode(OAuthAccessToken.self, from: data)
    }

    public func clear() { _ = SecItemDelete(query as CFDictionary) }
}
#endif
