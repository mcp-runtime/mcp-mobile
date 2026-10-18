package org.mcpruntime.mobile

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpHeaders
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import java.net.URI

/** The host owns sign-in, refresh and storage. Called for each HTTP request; never logged here. */
fun interface AccessTokenProvider { fun accessToken(): String? }

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, CLOSED }

class McpAuthenticationException : Exception("MCP sign-in or credential refresh required")

/**
 * A single foreground connection. MCP framing, negotiation, SSE and tool types belong to the
 * official SDK. Create a new instance after close. Calls are serialized; no offline write replay.
 * Requires Android API 26+ (first device verification: API 30) or Java 11+ at runtime.
 */
class McpMobileClient @JvmOverloads constructor(
    endpoint: String,
    private val tokenProvider: AccessTokenProvider = AccessTokenProvider { null },
    allowLoopbackHttp: Boolean = false,
    private val timeoutMillis: Long = 30_000,
    private val clientName: String = "mcp-mobile",
    private val clientVersion: String = "0.1.0",
) {
    private val endpoint = validateEndpoint(endpoint, allowLoopbackHttp)
    private val lock = Mutex()
    private var client: Client? = null
    private var http: HttpClient? = null
    @Volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    init { require(timeoutMillis in 1..300_000) { "Timeout must be between 1 and 300000 milliseconds" } }

    suspend fun connect() = lock.withLock {
        check(state == ConnectionState.DISCONNECTED) { "Create a new client after closing or connect only once" }
        state = ConnectionState.CONNECTING
        val transportHttp = HttpClient(OkHttp) {
            followRedirects = false
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status.value == 401 || response.status.value == 403) throw McpAuthenticationException()
                }
            }
            install(SSE)
            install(HttpTimeout) { connectTimeoutMillis = minOf(timeoutMillis, 10_000); socketTimeoutMillis = timeoutMillis }
            engine { config { retryOnConnectionFailure(false); followRedirects(false); followSslRedirects(false) } }
        }
        val session = Client(Implementation(name = clientName, version = clientVersion))
        http = transportHttp
        client = session
        try {
            withTimeout(timeoutMillis) {
                session.connect(StreamableHttpClientTransport(
                    client = transportHttp, url = endpoint, maxInlineSseEventSize = 1024 * 1024,
                    requestBuilder = {
                        tokenProvider.accessToken()?.takeIf { it.isNotBlank() }?.let {
                            headers.append(HttpHeaders.Authorization, "Bearer $it")
                        }
                    },
                ))
            }
            state = ConnectionState.CONNECTED
        } catch (error: Throwable) {
            withContext(NonCancellable) { release() }
            state = ConnectionState.DISCONNECTED
            throw error
        }
    }

    /** One page, preserving the official nextCursor for the host to request subsequent pages. */
    suspend fun listTools(cursor: String? = null): ListToolsResult = lock.withLock {
        withTimeout(timeoutMillis) { connected().listTools(ListToolsRequest(PaginatedRequestParams(cursor = cursor))) }
    }

    /** Cancellation or timeout does not prove a server-side action was undone. Never auto-replay it. */
    suspend fun callTool(name: String, arguments: JsonObject): CallToolResult = lock.withLock {
        require(name.isNotBlank()) { "Tool name is required" }
        withTimeout(timeoutMillis) { connected().callTool(CallToolRequest(CallToolRequestParams(name = name, arguments = arguments))) }
    }

    suspend fun close() = withContext(NonCancellable) {
        lock.withLock { release(); state = ConnectionState.CLOSED }
    }

    private fun connected(): Client {
        check(state == ConnectionState.CONNECTED) { "Connect before requesting tools" }
        return checkNotNull(client)
    }

    private suspend fun release() {
        try { client?.close() } catch (_: Exception) { /* HTTP is closed even if session DELETE fails. */ }
        finally { client = null; http?.close(); http = null }
    }

    companion object {
        @JvmStatic fun validateEndpoint(endpoint: String, allowLoopbackHttp: Boolean): String {
            val uri = try { URI(endpoint) } catch (_: Exception) { throw IllegalArgumentException("Invalid MCP URL") }
            require(uri.host != null && uri.rawUserInfo == null && uri.rawFragment == null) { "Use an absolute MCP URL without embedded credentials or a fragment" }
            val loopback = uri.host.lowercase() in setOf("localhost", "127.0.0.1", "[::1]")
            require(uri.scheme == "https" || (allowLoopbackHttp && uri.scheme == "http" && loopback)) { "MCP requires HTTPS; development HTTP is restricted to loopback" }
            return uri.toASCIIString()
        }
    }
}
