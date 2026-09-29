package com.haddadian.universalremote

import kotlin.math.abs

class TouchpadController(
    private val onKey: (GestureKey) -> Unit,
    private val onSelect: () -> Unit
) {
    enum class GestureKey { UP, DOWN, LEFT, RIGHT }

    fun tap() = onSelect()

    fun swipe(dx: Float, dy: Float) {
        val threshold = 32f
        if (abs(dx) < threshold && abs(dy) < threshold) return
        if (abs(dx) > abs(dy)) onKey(if (dx > 0) GestureKey.RIGHT else GestureKey.LEFT)
        else onKey(if (dy > 0) GestureKey.DOWN else GestureKey.UP)
    }
}
