package com.example.engine

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Deterministic cryptographic & PRNG utilities.
 * Matches JS FNV-1a 32-bit hash() and Mulberry32 mulberry() bit-for-bit.
 * ZERO Math.random() / kotlin.random.Random used anywhere in gameplay.
 */
object DeterministicRng {

    fun fnv1aHash(input: String): UInt {
        var h: UInt = 2166136261u
        for (i in input.indices) {
            h = h xor input[i].code.toUInt()
            h = (h.toInt() * 16777619).toUInt()
        }
        return h
    }

    fun mulberry(seed: UInt): () -> Double {
        var a = seed.toInt()
        return {
            a = a + 0x6D2B79F5
            var t = (a xor (a ushr 15)) * (1 or a)
            t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
            val unsigned = (t xor (t ushr 14)).toUInt()
            unsigned.toDouble() / 4294967296.0
        }
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val bytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Canonical compact JSON serialization of replay actions for deterministic SHA-256 fingerprinting.
     */
    fun canonicalActionsJson(actions: List<ReplayAction>): String {
        val sb = StringBuilder(actions.size * 36)
        sb.append('[')
        actions.forEachIndexed { index, action ->
            if (index > 0) sb.append(',')
            when (action) {
                is ReplayAction.Move -> {
                    sb.append("{\"t\":\"move\",\"dx\":")
                        .append(action.dx)
                        .append(",\"dy\":")
                        .append(action.dy)
                        .append(",\"tick\":")
                        .append(action.tick)
                        .append('}')
                }
                is ReplayAction.Mine -> {
                    sb.append("{\"t\":\"mine\",\"x\":")
                        .append(action.x)
                        .append(",\"y\":")
                        .append(action.y)
                        .append(",\"tick\":")
                        .append(action.tick)
                        .append('}')
                }
            }
        }
        sb.append(']')
        return sb.toString()
    }

    fun fingerprintActions(actions: List<ReplayAction>): String {
        return sha256Hex(canonicalActionsJson(actions))
    }
}
