package org.mcpruntime.mobile

import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpError
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.coroutines.*
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.atomic.AtomicBoolean

/** Callbacks run on an IO dispatcher. Android hosts must dispatch UI updates onto the main thread. */
interface McpCallback { fun onSuccess(json: String); fun onError(code: String, message: String) }
fun interface McpRequest { fun cancel() }

/** Java facade with cancellable operations. close() cancels requests before releasing the session. */
class McpMobileJavaClient @JvmOverloads constructor(
    endpoint: String,
    tokenProvider: AccessTokenProvider = AccessTokenProvider { null },
    allowLoopbackHttp: Boolean = false,
    timeoutMillis: Long = 30_000,
) : AutoCloseable {
    private val client = McpMobileClient(endpoint, tokenProvider, allowLoopbackHttp, timeoutMillis)
    private val closed = AtomicBoolean(false)
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    val state: ConnectionState get() = client.state
    /** Exception class names only, for host diagnostics. Never includes messages, URLs or credentials. */
    @Volatile var lastFailureTypes: List<String> = emptyList()
        private set

    fun connect(callback: McpCallback): McpRequest = submit(callback) { client.connect(); "{}" }
    fun listTools(cursor: String?, callback: McpCallback): McpRequest = submit(callback) {
        McpJson.encodeToString(ListToolsResult.serializer(), client.listTools(cursor))
    }
    fun callTool(name: String, argumentsJson: String, callback: McpCallback): McpRequest = submit(callback) {
        require(argumentsJson.length <= 1024 * 1024) { "Arguments exceed limit" }
        val arguments = McpJson.parseToJsonElement(argumentsJson).jsonObject
        McpJson.encodeToString(CallToolResult.serializer(), client.callTool(name, arguments))
    }

    private fun submit(callback: McpCallback, action: suspend () -> String): McpRequest {
        check(!closed.get()) { "Client is closed" }
        val request = scope.launch {
            val result = try { Result.success(action()) } catch (error: Exception) { Result.failure(error) }
            // Do not catch a host callback exception and invoke its callback a second time.
            result.fold(callback::onSuccess) { error ->
                lastFailureTypes = generateSequence<Throwable>(error) { it.cause }.take(16).map { it.javaClass.name }.toList()
                val code = when (error) {
                    is TimeoutCancellationException -> "TIMEOUT"
                    is CancellationException -> "CANCELLED"
                    is IllegalArgumentException -> "INVALID_ARGUMENT"
                    is IllegalStateException -> "INVALID_STATE"
                    is McpAuthenticationException -> "AUTH_REQUIRED"
                    is StreamableHttpError -> if (error.code == 401 || error.code == 403) "AUTH_REQUIRED" else "CONNECTION_FAILED"
                    else -> if (generateSequence(error.cause) { it.cause }.take(16).any { it is McpAuthenticationException }) "AUTH_REQUIRED" else "CONNECTION_FAILED"
                }
                callback.onError(code, when (code) {
                    "AUTH_REQUIRED" -> "Sign in or refresh the credential in your app."
                    "TIMEOUT" -> "Request timed out. Check the server before repeating an action."
                    "CANCELLED" -> "Request cancelled. A server-side action may still have completed."
                    "INVALID_ARGUMENT" -> "Check the tool name and JSON object arguments."
                    "INVALID_STATE" -> "Connect a new client before requesting tools."
                    else -> "Could not complete the MCP request."
                })
            }
        }
        return McpRequest { request.cancel() }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            job.cancel()
            // A cancelled scope cannot run cleanup. Join cancelled operations before closing transport.
            CoroutineScope(Dispatchers.IO).launch { job.join(); client.close() }
        }
    }
}
