package com.kmab.singalong.backup

import java.io.InputStream

/** Null removes the user-selected byte cap; available storage and memory still apply. */
data class BackupLimits(val maxBytes: Long? = 50L * 1024 * 1024) {
    init { require(maxBytes == null || maxBytes > 0) { "Backup size limit must be positive or unlimited." } }

    internal fun check(bytes: Long) {
        require(bytes >= 0 && (maxBytes == null || bytes <= maxBytes)) {
            "Backup exceeds the selected size limit. Increase the limit or choose Unlimited."
        }
    }

    /** Allow compressed data plus ZIP headers, without retaining an unbounded input on disk. */
    internal fun compressedLimit(): Long? = maxBytes?.let {
        val overhead = 4L * 1024 * 1024
        if (it > (Long.MAX_VALUE - overhead) / 2) Long.MAX_VALUE else it * 2 + overhead
    }
}

internal class BackupByteBudget(private val limits: BackupLimits) {
    var bytes: Long = 0
        private set

    fun add(count: Int) {
        require(count >= 0 && bytes <= Long.MAX_VALUE - count) { "Backup byte count overflow." }
        bytes += count
        limits.check(bytes)
    }
}

internal class BudgetInputStream(private val source: InputStream, private val budget: BackupByteBudget) : InputStream() {
    override fun read(): Int = source.read().also { if (it >= 0) budget.add(1) }
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int = source.read(bytes, offset, length)
        .also { if (it > 0) budget.add(it) }
    override fun close() = source.close()
}

internal fun String.utf8Size(): Long {
    var size = 0L
    var index = 0
    while (index < length) {
        val character = this[index]
        size += when {
            character.code < 0x80 -> 1
            character.code < 0x800 -> 2
            character.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate() -> { index++; 4 }
            character.isSurrogate() -> 1
            else -> 3
        }
        index++
    }
    return size
}
