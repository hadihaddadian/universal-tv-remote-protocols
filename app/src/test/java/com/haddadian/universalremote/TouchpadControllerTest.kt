package com.haddadian.universalremote

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchpadControllerTest {
    @Test fun horizontalSwipeMapsToDirection() {
        var result: TouchpadController.GestureKey? = null
        val t = TouchpadController({ result = it }, {})
        t.swipe(80f, 5f)
        assertEquals(TouchpadController.GestureKey.RIGHT, result)
    }

    @Test fun verticalSwipeMapsToDirection() {
        var result: TouchpadController.GestureKey? = null
        val t = TouchpadController({ result = it }, {})
        t.swipe(0f, -80f)
        assertEquals(TouchpadController.GestureKey.UP, result)
    }
}
