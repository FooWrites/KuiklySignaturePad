package com.kuikly.signature

import com.tencent.kuikly.core.base.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SignatureInkTest {
    private val ink = SignatureInk(minPointDistance = 2f)

    @Test
    fun strokeMovesToStrokesWhenEnded() {
        assertTrue(ink.begin(pointerId = 1, x = 0f, y = 0f, color = Color.BLACK, width = 3f))
        val active = ink.activeStroke
        assertFalse(ink.isEmpty)
        assertTrue(ink.strokes.isEmpty())

        assertTrue(ink.end(pointerId = 1, x = 10f, y = 0f))

        assertNull(ink.activeStroke)
        assertEquals(1, ink.strokes.size)
        assertSame(active, ink.strokes[0])
        assertEquals(2, ink.strokes[0].points.size)
    }

    @Test
    fun tapLeavesSinglePointStroke() {
        ink.begin(1, 5f, 5f, Color.BLACK, 3f)
        ink.end(1, 5f, 5f)

        assertEquals(1, ink.strokes[0].points.size)
        assertFalse(ink.isEmpty)
    }

    @Test
    fun ignoresPointsCloserThanMinDistance() {
        ink.begin(1, 0f, 0f, Color.BLACK, 3f)

        assertFalse(ink.extend(1, 1f, 1f))
        assertTrue(ink.extend(1, 2f, 0f))
        assertEquals(2, ink.activeStroke?.points?.size)
    }

    @Test
    fun ignoresOtherPointers() {
        ink.begin(1, 0f, 0f, Color.BLACK, 3f)

        assertFalse(ink.begin(2, 50f, 50f, Color.BLACK, 3f))
        assertFalse(ink.extend(2, 60f, 60f))
        assertFalse(ink.end(2, 60f, 60f))
        assertEquals(1, ink.activeStroke?.points?.size)

        assertTrue(ink.end(1, 0f, 0f))
        assertTrue(ink.begin(2, 50f, 50f, Color.BLACK, 3f))
    }

    @Test
    fun clearReportsWhetherThereWasInk() {
        assertFalse(ink.clear())

        ink.begin(1, 0f, 0f, Color.BLACK, 3f)
        ink.end(1, 0f, 0f)

        assertTrue(ink.clear())
        assertTrue(ink.isEmpty)
    }

    @Test
    fun clearDuringStrokeDropsActiveStroke() {
        ink.begin(1, 0f, 0f, Color.BLACK, 3f)

        assertTrue(ink.clear())
        assertFalse(ink.extend(1, 10f, 10f))
        assertFalse(ink.end(1, 10f, 10f))
        assertTrue(ink.isEmpty)
    }

    @Test
    fun strokeKeepsPenAtBegin() {
        val red = Color(0xFFFF0000)
        ink.begin(1, 0f, 0f, red, 6f)
        ink.end(1, 0f, 0f)

        assertSame(red, ink.strokes[0].color)
        assertEquals(6f, ink.strokes[0].width)
    }
}
