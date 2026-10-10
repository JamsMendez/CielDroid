package com.jamsmendez.cieldroid.wallpaper

data class DisplaySpec(val width: Int, val height: Int, val densityDpi: Int)

/** Pure decision for an engine surface change; the engine applies it to its VirtualDisplay. */
sealed interface DisplayPlan {
    data class Create(val spec: DisplaySpec) : DisplayPlan
    data class Resize(val spec: DisplaySpec, val swapSurface: Boolean = false) : DisplayPlan
    /** Same size, new Surface object: only `VirtualDisplay.setSurface`. */
    data object SwapSurface : DisplayPlan
    data object Unchanged : DisplayPlan
    data object Invalid : DisplayPlan

    companion object {
        fun of(
            current: DisplaySpec?, width: Int, height: Int, densityDpi: Int, surfaceChanged: Boolean = false,
        ): DisplayPlan {
            if (width <= 0 || height <= 0 || densityDpi <= 0) return Invalid
            val next = DisplaySpec(width, height, densityDpi)
            return when (current) {
                null -> Create(next)
                next -> if (surfaceChanged) SwapSurface else Unchanged
                else -> Resize(next, swapSurface = surfaceChanged)
            }
        }
    }
}
