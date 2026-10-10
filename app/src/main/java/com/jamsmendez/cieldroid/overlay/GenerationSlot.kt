package com.jamsmendez.cieldroid.overlay

/**
 * Holds the overlay's single live generation and makes its release re-entrancy safe. The slot is cleared
 * before [release] runs, so an error callback or a second release arriving during (or after) a release is a
 * no-op. A failure reported while generation [begin]..[install] is still being built is remembered and the
 * generation is released as soon as it is installed. Main-thread owned; [release] must not throw.
 */
class GenerationSlot<T : Any>(private val release: (T) -> Unit) {
    private var currentEntry: Pair<Long, T>? = null
    private var building: Long? = null
    private var buildFailed = false

    val current: T? get() = currentEntry?.second
    val currentId: Long? get() = currentEntry?.first

    /** Releases the current generation and marks [id] as being built. */
    fun begin(id: Long) {
        clear()
        building = id
        buildFailed = false
    }

    /** Installs the built generation; false (and released at once) if it failed meanwhile or is not being built. */
    fun install(id: Long, value: T): Boolean {
        if (building != id || buildFailed) {
            if (building == id) endBuild()
            release(value)
            return false
        }
        endBuild()
        currentEntry = id to value
        return true
    }

    /** The build of [id] threw; its own teardown already released what it registered. */
    fun abort(id: Long) {
        if (building == id) endBuild()
    }

    /** Reports that generation [id] failed; true if this call released it or flagged its build. */
    fun fail(id: Long): Boolean {
        if (currentEntry?.first == id) return clear()
        if (building == id && !buildFailed) {
            buildFailed = true
            return true
        }
        return false
    }

    /** Releases the current generation once; true if one was released. */
    fun clear(): Boolean {
        val entry = currentEntry ?: return false
        currentEntry = null
        release(entry.second)
        return true
    }

    private fun endBuild() {
        building = null
        buildFailed = false
    }
}
