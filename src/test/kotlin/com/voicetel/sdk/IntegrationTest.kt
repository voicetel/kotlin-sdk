package com.voicetel.sdk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live, read-only integration tests against the real VoiceTel API.
 *
 * Gated by the `VOICETEL_USERNAME` and `VOICETEL_PASSWORD` env vars — skipped
 * otherwise. Use only to verify that the wire shape still matches the SDK on
 * a real account. None of these mutate server state.
 */
class IntegrationTest {

    private fun creds(): Triple<Int, String, String>? {
        val u = System.getenv("VOICETEL_USERNAME")?.toIntOrNull() ?: return null
        val p = System.getenv("VOICETEL_PASSWORD") ?: return null
        val b = System.getenv("VOICETEL_BASE_URL") ?: Version.DEFAULT_BASE_URL
        return Triple(u, p, b)
    }

    @Test
    fun loginAndReadProfile() = runBlocking {
        val c = creds()
        assumeTrue(c != null, "VOICETEL_USERNAME / VOICETEL_PASSWORD not set; skipping integration test")
        val (user, pass, base) = c!!
        val client = VoiceTelClient {
            baseUrl = base
        }
        try {
            client.login(user, pass)
            assertTrue(client.apiKey.isNotEmpty())
            val me = client.account.get()
            assertTrue((me.username ?: "").isNotEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun listNumbersReadOnly() = runBlocking {
        val c = creds()
        assumeTrue(c != null, "credentials not set; skipping")
        val (user, pass, base) = c!!
        val client = VoiceTelClient { baseUrl = base }
        try {
            client.login(user, pass)
            // Just ensure the call decodes successfully.
            client.numbers.list()
        } finally {
            client.close()
        }
    }
}
