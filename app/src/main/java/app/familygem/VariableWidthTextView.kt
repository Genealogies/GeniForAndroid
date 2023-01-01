package app.familygem

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.ceil

/**
 * TextView that adapts the width even to multiple lines
 */
class VariableWidthTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        val widthMode = MeasureSpec.getMode(widthSpec)
        if (widthMode == MeasureSpec.AT_MOST) {
            layout?.let {
                val maxWidth = ceil(getMaxLineWidth(it).toDouble()).toInt() +
                        compoundPaddingLeft + compoundPaddingRight
                widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
            }
        }
        super.onMeasure(widthSpec, heightMeasureSpec)
    }

    private fun getMaxLineWidth(layout: Layout): Float {
        var maxWidth = 0.0f
        val lines = layout.lineCount
        for (i in 0 until lines) {
            if (layout.getLineWidth(i) > maxWidth) {
                maxWidth = layout.getLineWidth(i)
            }
        }
        return maxWidth
    }
}