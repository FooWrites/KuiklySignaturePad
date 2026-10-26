package com.kuikly.signature

import kotlin.test.Test
import kotlin.test.assertEquals

class SignaturePadAttrTest {
    @Test
    fun placeholderFontSizeCanBeCustomized() {
        val attr = SignaturePadAttr()

        attr.placeholderFontSize = 22f

        assertEquals(22f, attr.placeholderFontSize)
    }
}
