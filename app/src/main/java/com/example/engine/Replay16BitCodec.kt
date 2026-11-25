package com.example.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

enum class MoveDir(val code: Int) {
    NONE(0),
    UP(1),
    DOWN(2),
    LEFT(3),
    RIGHT(4);

    fun toDelta(): Pair<Int, Int> = when (this) {
        NONE -> 0 to 0
        UP -> 0 to -1
        DOWN -> 0 to 1
        LEFT -> -1 to 0
        RIGHT -> 1 to 0
    }

    companion object {
        fun fromCode(code: Int): MoveDir = entries.firstOrNull { it.code == code } ?: NONE

        fun fromDelta(dx: Int, dy: Int): MoveDir = when {
            dy < 0 -> UP
            dy > 0 -> DOWN
            dx < 0 -> LEFT
            dx > 0 -> RIGHT
            else -> NONE
        }
    }
}

data class ReplayTickInput(
    val direction: MoveDir = MoveDir.NONE,
    val abilitySlot: Int = 0,       // 0 = none, 1..6 = ability index
    val triggerSurge: Boolean = false,
    val triggerTnt: Boolean = false
)

/**
 * 16-bit bitfield + 8-bit Run-Length Encoding (RLE) deterministic replay codec.
 * Packs per-tick input state into 8 bits + 8-bit repeat count (1..255) in little-endian
 * 16-bit words, serialized to RFC 4648 standard Base64 (NO_WRAP) so it is 100% compatible
 * across Android API 24+, Firestore <= 32KB rules, and pure JVM unit tests.
 */
object Replay16BitCodec {

    private const val MAX_RLE_COUNT = 255
    private const val MAX_TICKS_ALLOWED = 36_000 // 10 min @ 60Hz cap
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    /**
     * Encodes a chronological list of per-tick inputs into a compact Base64 string
     * using 16-bit bitfields + 8-bit Run-Length Encoding (RLE).
     */
    fun encodeToBase64(ticks: List<ReplayTickInput>): String {
        if (ticks.isEmpty()) return ""

        val packedShorts = ArrayList<Short>((ticks.size / 4).coerceAtLeast(4))
        var currentRawByte = packLowerByte(ticks[0])
        var runCount = 1

        for (i in 1 until ticks.size) {
            val nextRawByte = packLowerByte(ticks[i])
            if (nextRawByte == currentRawByte && runCount < MAX_RLE_COUNT) {
                runCount++
            } else {
                packedShorts.add(assemble16BitWord(currentRawByte, runCount))
                currentRawByte = nextRawByte
                runCount = 1
            }
        }
        packedShorts.add(assemble16BitWord(currentRawByte, runCount))

        // Little-endian ByteBuffer serialization
        val buffer = ByteBuffer.allocate(packedShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (word in packedShorts) {
            buffer.putShort(word)
        }
        return encodeBase64NoWrap(buffer.array())
    }

    /**
     * Decodes a Base64 16-bit RLE stream back into exact per-tick inputs for
     * Ghost Racer playback or deterministic SHA-256 verification.
     */
    fun decodeFromBase64(base64Payload: String): List<ReplayTickInput> {
        if (base64Payload.isBlank()) return emptyList()

        val bytes = decodeBase64NoWrap(base64Payload.trim())
        require(bytes.size % 2 == 0) { "Corrupt 16-bit replay stream: odd byte length" }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val wordCount = bytes.size / 2
        val result = ArrayList<ReplayTickInput>(wordCount * 3)

        for (i in 0 until wordCount) {
            val word = buffer.short.toInt() and 0xFFFF
            val lowerByte = word and 0xFF
            val repeatCount = (word ushr 8) and 0xFF
            val input = unpackLowerByte(lowerByte)

            require(result.size + repeatCount <= MAX_TICKS_ALLOWED) {
                "Replay exceeds maximum allowed simulation ticks"
            }
            repeat(repeatCount) {
                result.add(input)
            }
        }
        return result
    }

    /**
     * Generates the canonical SHA-256 fingerprint verified by Firestore rules.
     * Binds seed + final score + clear time + 16-bit replay stream together.
     */
    fun computeSha256Fingerprint(
        seed: Long,
        score: Int,
        clearTimeMs: Long,
        base64Replay: String
    ): String {
        val canonicalString = "$seed|$score|$clearTimeMs|$base64Replay"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonicalString.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Reconstructs deterministic GhostFrames directly from a decoded 16-bit RLE input stream
     * for holographic Canvas rendering during Daily Ranked & Ghost Challenges.
     */
    fun toGhostFrames(
        ticks: List<ReplayTickInput>,
        boardSize: Int = MAZE_SIZE,
        startX: Int = START_X,
        startY: Int = START_Y
    ): List<GhostFrame> {
        if (ticks.isEmpty()) return emptyList()
        var gx = startX
        var gy = startY
        val frames = ArrayList<GhostFrame>(ticks.size + 1)
        frames.add(GhostFrame(tick = 0, gridX = gx.toFloat(), gridY = gy.toFloat()))
        ticks.forEachIndexed { index, input ->
            val (dx, dy) = input.direction.toDelta()
            val stepScale = if (input.triggerSurge) 2 else 1
            gx = (gx + dx * stepScale).coerceIn(1, (boardSize - 2).coerceAtLeast(1))
            gy = (gy + dy * stepScale).coerceIn(1, (boardSize - 2).coerceAtLeast(1))
            val ability = if (input.abilitySlot in 1..MinerAbility.entries.size) {
                MinerAbility.entries[input.abilitySlot - 1]
            } else {
                null
            }
            frames.add(
                GhostFrame(
                    tick = index + 1,
                    gridX = gx.toFloat(),
                    gridY = gy.toFloat(),
                    activeAbility = ability,
                    isSurgeActive = input.triggerSurge
                )
            )
        }
        return frames
    }

    /**
     * Converts recorded player movement/mining trail into compact 16-bit RLE ReplayTickInput sequence.
     */
    fun fromTrail(trail: List<GridPos>): List<ReplayTickInput> {
        if (trail.size < 2) return emptyList()
        val ticks = ArrayList<ReplayTickInput>(trail.size - 1)
        for (i in 1 until trail.size) {
            val prev = trail[i - 1]
            val cur = trail[i]
            val dx = cur.x - prev.x
            val dy = cur.y - prev.y
            val dir = MoveDir.fromDelta(dx, dy)
            val isSurgeJump = kotlin.math.abs(dx) + kotlin.math.abs(dy) >= 2
            ticks.add(ReplayTickInput(direction = dir, triggerSurge = isSurgeJump))
        }
        return ticks
    }

    private fun packLowerByte(input: ReplayTickInput): Int {
        val dirBits = input.direction.code and 0x07
        val abilityBits = (input.abilitySlot.coerceIn(0, 6) and 0x07) shl 3
        val surgeBit = if (input.triggerSurge) (1 shl 6) else 0
        val tntBit = if (input.triggerTnt) (1 shl 7) else 0
        return dirBits or abilityBits or surgeBit or tntBit
    }

    private fun unpackLowerByte(lowerByte: Int): ReplayTickInput {
        val dir = MoveDir.fromCode(lowerByte and 0x07)
        val ability = (lowerByte ushr 3) and 0x07
        val surge = ((lowerByte ushr 6) and 0x01) == 1
        val tnt = ((lowerByte ushr 7) and 0x01) == 1
        return ReplayTickInput(
            direction = dir,
            abilitySlot = ability,
            triggerSurge = surge,
            triggerTnt = tnt
        )
    }

    private fun assemble16BitWord(lowerByte: Int, repeatCount: Int): Short {
        return (((repeatCount and 0xFF) shl 8) or (lowerByte and 0xFF)).toShort()
    }

    private fun encodeBase64NoWrap(data: ByteArray): String {
        val out = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val hasB1 = i < data.size
            val b1 = if (hasB1) data[i++].toInt() and 0xFF else 0
            val hasB2 = i < data.size
            val b2 = if (hasB2) data[i++].toInt() and 0xFF else 0

            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.append(BASE64_ALPHABET[(triple ushr 18) and 0x3F])
            out.append(BASE64_ALPHABET[(triple ushr 12) and 0x3F])
            out.append(if (hasB1) BASE64_ALPHABET[(triple ushr 6) and 0x3F] else '=')
            out.append(if (hasB2) BASE64_ALPHABET[triple and 0x3F] else '=')
        }
        return out.toString()
    }

    private fun decodeBase64NoWrap(encoded: String): ByteArray {
        val clean = encoded.filter { !it.isWhitespace() }
        require(clean.length % 4 == 0) { "Invalid Base64 length" }
        val padding = when {
            clean.endsWith("==") -> 2
            clean.endsWith("=") -> 1
            else -> 0
        }
        val outLen = (clean.length / 4) * 3 - padding
        val out = ByteArray(outLen)
        var outIdx = 0
        var i = 0
        while (i < clean.length) {
            val c0 = decodeChar(clean[i++])
            val c1 = decodeChar(clean[i++])
            val c2Char = clean[i++]
            val c3Char = clean[i++]
            val c2 = if (c2Char == '=') 0 else decodeChar(c2Char)
            val c3 = if (c3Char == '=') 0 else decodeChar(c3Char)
            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            if (outIdx < outLen) out[outIdx++] = ((triple ushr 16) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = ((triple ushr 8) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = (triple and 0xFF).toByte()
        }
        return out
    }

    private fun decodeChar(c: Char): Int = when (c) {
        in 'A'..'Z' -> c.code - 'A'.code
        in 'a'..'z' -> c.code - 'a'.code + 26
        in '0'..'9' -> c.code - '0'.code + 52
        '+' -> 62
        '/' -> 63
        else -> throw IllegalArgumentException("Invalid Base64 character: $c")
    }
}
