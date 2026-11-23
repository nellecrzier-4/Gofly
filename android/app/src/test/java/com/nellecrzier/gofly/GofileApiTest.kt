package com.nellecrzier.gofly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Token: ekstraksi dari link/token mentah dan token website SHA-256.
 * Keduanya fungsi murni — tidak ada jaringan.
 */
class GofileApiTest {

    private val raw = "a1b2c3d4e5f60718293a4b5c6d7e8f90"

    /* --------------------------- extractToken --------------------------- */

    @Test
    fun extractTokenReadsFullLoginUrl() {
        assertEquals(raw, GofileApi.extractToken("https://gofile.io/login/$raw"))
        assertEquals(
            raw,
            GofileApi.extractToken("https://gofile.io/login/$raw?src=email&utm=1"),
        )
        assertEquals(raw, GofileApi.extractToken("https://gofile.io/login/$raw#selesai"))
    }

    @Test
    fun extractTokenReadsTokenFromQueryParameters() {
        assertEquals(raw, GofileApi.extractToken("https://gofile.io/?token=$raw"))
        assertEquals(raw, GofileApi.extractToken("https://example.com/cb?apiToken=$raw&state=x"))
        // Parameter lain di depan tidak mengganggu.
        assertEquals(raw, GofileApi.extractToken("https://example.com/cb?x=1&token=$raw"))
    }

    @Test
    fun extractTokenAcceptsRawToken() {
        assertEquals(raw, GofileApi.extractToken(raw))
    }

    @Test
    fun extractTokenStripsWhitespaceAndQuotes() {
        assertEquals(raw, GofileApi.extractToken("  \"$raw\"  "))
        assertEquals(raw, GofileApi.extractToken("'$raw'\n"))
        // Spasi di tengah token dibuang sebelum pencocokan.
        assertEquals("abcd1234abcd1234", GofileApi.extractToken("abcd 1234 abcd 1234"))
    }

    @Test
    fun extractTokenRejectsBlankInput() {
        assertNull(GofileApi.extractToken(""))
        assertNull(GofileApi.extractToken("    "))
        assertNull(GofileApi.extractToken("\"\"\n"))
    }

    @Test
    fun extractTokenRejectsGarbageAndFolderLinks() {
        assertNull(GofileApi.extractToken("bukan tautan"))
        assertNull(GofileApi.extractToken("https://example.com/hello"))
        // Tautan folder publik bukan login link dan tidak boleh jadi token.
        assertNull(GofileApi.extractToken("https://gofile.io/d/$raw"))
    }

    @Test
    fun extractTokenRejectsTokensOutsideLengthBounds() {
        assertNull(GofileApi.extractToken("a".repeat(15)))
        assertNull(GofileApi.extractToken("a".repeat(65)))
        assertEquals(16, GofileApi.extractToken("a".repeat(16))!!.length)
        assertEquals(64, GofileApi.extractToken("b".repeat(64))!!.length)
    }

    @Test
    fun extractTokenMatchesLoginSegmentIgnoringCase() {
        assertEquals(raw, GofileApi.extractToken("HTTPS://GOFILE.IO/LOGIN/$raw"))
    }

    /* --------------------------- websiteToken --------------------------- */

    @Test
    fun websiteTokenIsLowercaseSha256Hex() {
        val token = GofileApi.websiteToken(raw)

        assertEquals(64, token.length)
        assertTrue("harus hex kecil: $token", Regex("^[0-9a-f]{64}$").matches(token))
        assertTrue(token.none { it.isUpperCase() })
    }

    @Test
    fun websiteTokenIsDeterministicForTheSameToken() {
        // Rumus gofile.io memakai periode 4 jam; dua panggilan berurutan sama.
        assertEquals(GofileApi.websiteToken(raw), GofileApi.websiteToken(raw))
    }

    @Test
    fun websiteTokenDiffersBetweenTokens() {
        assertNotEquals(
            GofileApi.websiteToken(raw),
            GofileApi.websiteToken("ffffffffffffffffffffffffffffffff"),
        )
        assertNotEquals(GofileApi.websiteToken(""), GofileApi.websiteToken(raw))
    }
}
