package com.tangem.id.features.issuecredentials.ui.textwatchers

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ReplacementSpan

class DateFormattingTextWatcher: TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }


    override fun afterTextChanged(editable: Editable) {
        if (editable.length > 8 ) editable.delete(8, editable.length)

        val paddingSpans = editable.getSpans(0, editable.length, SlashSpan::class.java)
        for (span in paddingSpans) {
            editable.removeSpan(span)
        }
        addSpans(editable, intArrayOf(2, 4))
    }

    private fun addSpans(editable: Editable, spaceIndices: IntArray) {
        val length = editable.length
        spaceIndices
            .filter { it <= length }
            .forEach {
                editable.setSpan(SlashSpan(), it - 1, it,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }
}

class SlashSpan: ReplacementSpan() {

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val padding = paint.measureText("/", 0, 1)
        val textSize = paint.measureText(text, start, end)
        return (padding + textSize).toInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int,
                      bottom: Int, paint: Paint
    ) {
        canvas.drawText(text.subSequence(start, end).toString() + "/", x, y.toFloat(), paint)
    }
}