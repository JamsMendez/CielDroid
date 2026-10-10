package com.jamsmendez.cieldroid.wallpaper

/**
 * Release stack for one engine generation: parts are released in reverse creation order, exactly once.
 * A failing release is reported and does not stop the remaining releases. Main-thread owned.
 */
class Teardown(private val onFailure: (String, Throwable) -> Unit) {
    private val parts = ArrayDeque<Pair<String, () -> Unit>>()
    private var released = false

    /** Registers [value]; after [releaseAll] the late part is released at once and rejected. */
    fun <T> add(name: String, value: T, release: (T) -> Unit): T {
        if (released) {
            runCatching { release(value) }.onFailure { onFailure(name, it) }
            throw IllegalStateException("Teardown already released; $name was released immediately")
        }
        parts.addLast(name to { release(value) })
        return value
    }

    fun releaseAll() {
        if (released) return
        released = true
        while (parts.isNotEmpty()) {
            val (name, release) = parts.removeLast()
            try { release() } catch (e: Exception) { onFailure(name, e) }
        }
    }

    companion object {
        /** Runs [block]; if it throws, everything it registered is released before the failure propagates. */
        fun <T> build(onFailure: (String, Throwable) -> Unit, block: (Teardown) -> T): T {
            val teardown = Teardown(onFailure)
            try {
                return block(teardown)
            } catch (e: Throwable) {
                teardown.releaseAll()
                throw e
            }
        }
    }
}
