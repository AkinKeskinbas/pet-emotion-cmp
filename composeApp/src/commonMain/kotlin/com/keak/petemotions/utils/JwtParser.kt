package com.keak.petemotions.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JwtParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extracts userId from JWT token payload
     * JWT format: header.payload.signature
     */
    fun getUserId(token: String): String? {
        return try {
            // Split JWT into parts
            val parts = token.split(".")
            if (parts.size != 3) {
                println("JwtParser: Invalid JWT format - expected 3 parts, got ${parts.size}")
                return null
            }

            // Decode base64 payload (second part)
            val payload = parts[1]
            val decodedPayload = decodeBase64(payload)
            println("JwtParser: Decoded payload: $decodedPayload")

            // Parse JSON and extract userId
            val jsonObject = json.parseToJsonElement(decodedPayload).jsonObject
            val userId = jsonObject["userId"]?.jsonPrimitive?.content
            println("JwtParser: Extracted userId: $userId")

            userId
        } catch (e: Exception) {
            println("JwtParser: Failed to parse JWT: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase64(input: String): String {
        // Add padding if needed
        val paddedInput = when (input.length % 4) {
            2 -> input + "=="
            3 -> input + "="
            else -> input
        }

        // Base64 URL-safe decoding (JWT uses URL-safe base64)
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val bytes = mutableListOf<Byte>()

        var i = 0
        while (i < paddedInput.length) {
            val c1 = base64Chars.indexOf(paddedInput[i])
            val c2 = base64Chars.indexOf(paddedInput[i + 1])
            val c3 = if (i + 2 < paddedInput.length && paddedInput[i + 2] != '=')
                base64Chars.indexOf(paddedInput[i + 2]) else -1
            val c4 = if (i + 3 < paddedInput.length && paddedInput[i + 3] != '=')
                base64Chars.indexOf(paddedInput[i + 3]) else -1

            val bitmap = (c1 shl 18) or (c2 shl 12) or
                         (if (c3 >= 0) c3 shl 6 else 0) or
                         (if (c4 >= 0) c4 else 0)

            bytes.add(((bitmap shr 16) and 0xFF).toByte())
            if (c3 >= 0) bytes.add(((bitmap shr 8) and 0xFF).toByte())
            if (c4 >= 0) bytes.add((bitmap and 0xFF).toByte())

            i += 4
        }

        return bytes.toByteArray().decodeToString()
    }
}
