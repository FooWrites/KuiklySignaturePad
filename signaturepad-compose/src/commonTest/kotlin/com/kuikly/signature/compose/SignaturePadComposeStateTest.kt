package com.kuikly.signature.compose

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignaturePadComposeStateTest {
    private val state = SignaturePadComposeState()
    private val signed = mutableListOf<Boolean>()

    init {
        state.onSigned = { signed.add(it) }
    }

    @Test
    fun endingStrokeReportsInk() {
        state.begin(Offset(0f, 0f), Color.Black, 3f)
        assertFalse(state.isEmpty)
        assertTrue(signed.isEmpty())

        state.end(Offset(10f, 0f), MIN_DISTANCE)

        assertEquals(listOf(true), signed)
        assertFalse(state.showPlaceholder)
    }

    @Test
    fun secondBeginIsIgnoredWhileStrokeActive() {
        state.begin(Offset(0f, 0f), Color.Black, 3f)
        state.begin(Offset(50f, 50f), Color.Black, 3f)
        state.end(null, MIN_DISTANCE)

        assertEquals(listOf(true), signed)
    }

    @Test
    fun clearReportsOnlyWhenThereWasInk() {
        state.clear()
        assertTrue(signed.isEmpty())

        state.begin(Offset(0f, 0f), Color.Black, 3f)
        state.end(null, MIN_DISTANCE)
        state.clear()

        assertEquals(listOf(true, false), signed)
        assertTrue(state.isEmpty)
        assertTrue(state.showPlaceholder)
    }

    @Test
    fun clearDuringStrokeDropsActiveStroke() {
        state.begin(Offset(0f, 0f), Color.Black, 3f)
        state.clear()
        state.end(Offset(10f, 10f), MIN_DISTANCE)

        assertEquals(listOf(false), signed)
        assertTrue(state.isEmpty)
    }

    @Test
    fun exportRequiresInkAndLayout() {
        val codes = mutableListOf<Int>()

        state.export { codes.add(it.code) }
        state.begin(Offset(0f, 0f), Color.Black, 3f)
        state.end(null, MIN_DISTANCE)
        state.export { codes.add(it.code) }

        assertEquals(
            listOf(SignatureExportResult.EMPTY_CODE, SignatureExportResult.NOT_READY_CODE),
            codes
        )
    }

    private companion object {
        const val MIN_DISTANCE = 1.5f
    }
}
