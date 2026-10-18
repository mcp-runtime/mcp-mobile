package org.mcpruntime.mobile

import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.time.Instant

/** Opt-in: reads a private credential file, performs read-only calls, records no server content. */
class CullyLiveTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "CULLY_MOBILE_SESSION_FILE", matches = ".+")
    fun authenticatedCullyCatalogAndReadTool() = runBlocking {
        val credential = McpJson.parseToJsonElement(File(System.getenv("CULLY_MOBILE_SESSION_FILE")).readText()).jsonObject
        val endpoint = credential.getValue("endpoint").jsonPrimitive.content
        val token = credential.getValue("access_token").jsonPrimitive.content
        val client = McpMobileClient(endpoint, AccessTokenProvider { token })
        try {
            client.connect()
            val tools = client.listTools().tools
            assertTrue(tools.any { it.name == "cully_context" })
            val result = client.callTool("cully_context", buildJsonObject {
                put("query", "mcp-mobile-release-smoke-never-match-6f4b0c2d")
                put("limit", 1); put("mode", "text")
                put("project_url", "https://github.com/mcp-runtime/mcp-mobile")
            })
            assertNotEquals(true, result.isError)
            File("target/cully-live-report.json").writeText(buildJsonObject {
                put("testedAt", Instant.now().toString()); put("sdk", "0.1.0")
                put("officialKotlinSdk", "0.15.0"); put("authenticated", true)
                put("toolCount", tools.size); put("readTool", "cully_context")
                put("writesPerformed", false); put("passed", true)
            }.toString() + "\n")
        } finally { client.close() }
    }
}
