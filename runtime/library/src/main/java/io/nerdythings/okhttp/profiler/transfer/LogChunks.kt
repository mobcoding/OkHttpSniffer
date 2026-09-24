package io.nerdythings.okhttp.profiler.transfer

/** Android's log payload is byte-limited, and JNI encodes strings as modified UTF-8. */
internal object LogChunks {
    // Leave space for the priority, tag and terminators within Android's log payload.
    const val MAX_BYTES = 3000

    fun split(text: String): Sequence<String> = sequence {
        var start = 0
        var index = 0
        var bytes = 0
        while (index < text.length) {
            val paired = Character.isHighSurrogate(text[index]) && index + 1 < text.length &&
                Character.isLowSurrogate(text[index + 1])
            val count = if (paired) 2 else 1
            val size = if (paired) 6 else when (text[index].code) {
                in 1..0x7f -> 1
                in 0..0x7ff -> 2
                else -> 3
            }
            if (bytes + size > MAX_BYTES) {
                yield(text.substring(start, index))
                start = index
                bytes = 0
            }
            bytes += size
            index += count
        }
        if (start < text.length) yield(text.substring(start))
    }
}
