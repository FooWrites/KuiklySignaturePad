package com.kuikly.signature

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignatureStrokesTest {
    @Test
    fun startsEmpty() {
        val ink = SignatureStrokes()
        assertTrue(ink.isEmpty)
        assertEquals(0, ink.strokeCount)
        assertFalse(ink.extend(1f, 1f))
        assertFalse(ink.end())
    }

    @Test
    fun dropsPointsCloserThanMinDistance() {
        val ink = SignatureStrokes(minDistance = 2f)
        ink.begin(0f, 0f)
        assertFalse(ink.extend(1f, 1f))
        assertTrue(ink.extend(2f, 0f))
        assertEquals(2, ink.activeStroke?.size)
    }

    @Test
    fun endCommitsActiveStroke() {
        val ink = SignatureStrokes()
        ink.begin(0f, 0f)
        ink.extend(10f, 0f)
        assertTrue(ink.end())
        assertNull(ink.activeStroke)
        assertEquals(1, ink.finishedStrokes.size)
        assertFalse(ink.isEmpty)
    }

    @Test
    fun beginCommitsUnfinishedStroke() {
        val ink = SignatureStrokes()
        ink.begin(0f, 0f)
        ink.begin(5f, 5f)
        assertEquals(1, ink.finishedStrokes.size)
        assertEquals(2, ink.strokeCount)
    }

    @Test
    fun clearRemovesEverything() {
        val ink = SignatureStrokes()
        ink.begin(0f, 0f)
        ink.end()
        ink.begin(1f, 1f)
        ink.clear()
        assertTrue(ink.isEmpty)
        assertNull(ink.activeStroke)
    }

    @Test
    fun jsonIncludesFinishedAndActiveStrokes() {
        val ink = SignatureStrokes(minDistance = 0f)
        ink.begin(1f, 2f)
        ink.extend(3.25f, 4f)
        ink.end()
        ink.begin(-0.04f, 10.06f)
        assertEquals(
            "{\"width\":320,\"height\":240.5,\"strokes\":[[1,2,3.3,4],[0,10.1]]}",
            ink.toJson(320f, 240.5f),
        )
    }

    @Test
    fun emptyJsonHasNoStrokes() {
        assertEquals("{\"width\":10,\"height\":20,\"strokes\":[]}", SignatureStrokes().toJson(10f, 20f))
    }

    @Test
    fun svgDrawsOnePathWithAllStrokes() {
        val ink = SignatureStrokes(minDistance = 0f)
        ink.begin(0f, 0f)
        ink.extend(10f, 5f)
        ink.end()
        ink.begin(20f, 20f)
        ink.end()
        assertEquals(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"50\" viewBox=\"0 0 100 50\">" +
                "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>" +
                "<path fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke=\"#1f6feb\"" +
                " stroke-opacity=\"0.502\" stroke-width=\"2.5\" d=\"M0 0L10 5 M20 20l0.1 0\"/>" +
                "</svg>",
            ink.toSvg(100f, 50f, penColor = 0x801F6FEBL, penWidth = 2.5f),
        )
    }

    @Test
    fun svgWithoutPaperOrStrokesIsBareDocument() {
        assertEquals(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\" viewBox=\"0 0 10 10\"></svg>",
            SignatureStrokes().toSvg(10f, 10f, paperColor = null),
        )
    }

    @Test
    fun formatsNumbersCompactly() {
        assertEquals("0", SignatureStrokes.formatNumber(-0.01f))
        assertEquals("-1.5", SignatureStrokes.formatNumber(-1.5f))
        assertEquals("12", SignatureStrokes.formatNumber(11.96f))
        assertEquals("0.05", SignatureStrokes.formatNumber(0.05f, 2))
    }
}
