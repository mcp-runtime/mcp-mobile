package org.mcpruntime.mobile

import com.sun.net.httpserver.HttpServer
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class McpMobileClientTest {
    private class Fixture : AutoCloseable {
        val methods = CopyOnWriteArrayList<String>()
        val tokens = CopyOnWriteArrayList<String>()
        var failCalls = false
        var unauthorized = false
        var delayMillis = 0L
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            this.executor = this@Fixture.executor
            createContext("/mcp") { exchange ->
                try {
                    if (unauthorized) { exchange.sendResponseHeaders(401, -1); return@createContext }
                    if (exchange.requestMethod != "POST") { exchange.sendResponseHeaders(if (exchange.requestMethod == "DELETE") 200 else 405, -1); return@createContext }
                    val request = McpJson.parseToJsonElement(exchange.requestBody.bufferedReader().readText()).jsonObject
                    val method = request["method"]!!.jsonPrimitive.content
                    methods.add(method); tokens.add(exchange.requestHeaders.getFirst("Authorization") ?: "")
                    if (method.startsWith("notifications/")) { exchange.sendResponseHeaders(202, -1); return@createContext }
                    if (method == "tools/call" && failCalls) { exchange.sendResponseHeaders(503, -1); return@createContext }
                    if (method == "tools/list" && delayMillis > 0) Thread.sleep(delayMillis)
                    val payload = when (method) {
                        "initialize" -> """{"protocolVersion":"2025-11-25","capabilities":{"tools":{}},"serverInfo":{"name":"fixture","version":"1"}}"""
                        "tools/list" -> """{"tools":[{"name":"echo","inputSchema":{"type":"object"}}],"nextCursor":"page-2"}"""
                        "tools/call" -> """{"content":[{"type":"text","text":"fixture result"}],"structuredContent":{"ok":true},"isError":false}"""
                        else -> "{}"
                    }
                    val response = """{"jsonrpc":"2.0","id":${request["id"]},"result":$payload}""".toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.responseHeaders.add("Mcp-Session-Id", "fixture-session")
                    exchange.sendResponseHeaders(200, response.size.toLong())
                    exchange.responseBody.write(response)
                } finally { exchange.close() }
            }
            start()
        }
        val endpoint = "http://127.0.0.1:${server.address.port}/mcp"
        override fun close() { server.stop(0); executor.shutdownNow() }
    }

    @Test fun rejectsInsecureAndCredentialBearingUrls() {
        listOf("http://example.com/mcp", "https://user:secret@example.com/mcp", "file:///mcp", "https://example.com/mcp#token").forEach {
            assertThrows(IllegalArgumentException::class.java) { McpMobileClient(it) }
        }
        assertThrows(IllegalArgumentException::class.java) { McpMobileClient("http://192.168.1.2/mcp", allowLoopbackHttp = true) }
    }

    @Test fun negotiatesListsAndCallsWithFreshCredentials() = runBlocking {
        Fixture().use { fixture ->
            var token = "first"
            val client = McpMobileClient(fixture.endpoint, AccessTokenProvider { token }, true)
            try {
                client.connect()
                assertEquals(ConnectionState.CONNECTED, client.state)
                token = "refreshed"
                val tools = client.listTools()
                assertEquals("echo", tools.tools.single().name)
                assertEquals("page-2", tools.nextCursor)
                val result = client.callTool("echo", buildJsonObject { put("value", "hello") })
                assertEquals(true, result.structuredContent?.get("ok")?.jsonPrimitive?.boolean)
                assertTrue(fixture.methods.indexOf("notifications/initialized") < fixture.methods.indexOf("tools/list"))
                assertEquals("Bearer refreshed", fixture.tokens.last())
            } finally { client.close() }
            assertEquals(ConnectionState.CLOSED, client.state)
            client.close()
            assertThrows(IllegalStateException::class.java) { runBlocking { client.connect() } }
            Unit
        }
    }

    @Test fun doesNotReplayFailedToolCalls() = runBlocking {
        Fixture().use { fixture ->
            val client = McpMobileClient(fixture.endpoint, allowLoopbackHttp = true)
            try {
                client.connect(); fixture.failCalls = true
                assertThrows(Exception::class.java) { runBlocking { client.callTool("echo", buildJsonObject {}) } }
                assertEquals(1, fixture.methods.count { it == "tools/call" })
            } finally { client.close() }
        }
    }

    @Test fun cancellationAndTimeoutReleaseTheConnection() = runBlocking {
        Fixture().use { fixture ->
            val client = McpMobileClient(fixture.endpoint, allowLoopbackHttp = true, timeoutMillis = 500)
            try {
                client.connect(); fixture.delayMillis = 1500
                assertThrows(TimeoutCancellationException::class.java) { runBlocking { client.listTools() } }
            } finally { client.close() }
            assertEquals(ConnectionState.CLOSED, client.state)
        }
    }

    @Test fun javaFacadeReportsAuthWithoutLeakingTheToken() {
        Fixture().use { fixture ->
            fixture.unauthorized = true
            val client = McpMobileJavaClient(fixture.endpoint, AccessTokenProvider { "never-print-this-token" }, true)
            val done = CountDownLatch(1)
            var code = ""
            client.connect(object : McpCallback {
                override fun onSuccess(json: String) { done.countDown() }
                override fun onError(errorCode: String, message: String) {
                    code = errorCode
                    assertFalse(message.contains("never-print-this-token")); done.countDown()
                }
            })
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals("AUTH_REQUIRED", code)
            client.close()
        }
    }
}
