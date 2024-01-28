package com.example.otpview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter.LengthFilter
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.getDrawableOrThrow


private const val DEFAULT_INPUT_COUNT = 6
private const val SUPER_STATE = "superState"
private const val IS_INPUT_ERROR_ENABLED = "isInputErrorEnabled"

class OtpView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    fun interface SetOnInputFilledListener {
        fun onFilled(code: String)
    }

    private var itemPaddings: Float = 0f
    private var inputCount: Int = DEFAULT_INPUT_COUNT
    private lateinit var errorInputBackground: Drawable
    private lateinit var selectedInputBackground: Drawable
    private lateinit var unSelectedInputBackground: Drawable
    private var inputFilledListener: SetOnInputFilledListener? = null

    private val code: String
        get() = if (text == null) "" else text.toString()

    var isInputErrorEnabled: Boolean = false
        set(hasError) {
            field = hasError
            if (hasError) {
                text = null
            }
            isEnabled = !hasError
            invalidate()
        }

    init {
        setDefaultAttrs()
        readAttrs(attrs)
    }

    override fun setTextColor(colors: ColorStateList?) {
        colors?.let {
            paint.color = it.defaultColor
        }
        super.setTextColor(colors)
    }

    override fun setTypeface(tf: Typeface?) {
        if (paint.typeface == null) {
            paint.setTypeface(tf)
        }
    }

    private fun setDefaultAttrs() {
        isLongClickable = false
        isCursorVisible = false
        isFocusableInTouchMode = true
        isSingleLine = true
        filters = arrayOf(LengthFilter(inputCount))
    }

    private fun readAttrs(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.OtpView, 0, 0).apply {
            try {
                inputCount = getInteger(R.styleable.OtpView_inputCount, DEFAULT_INPUT_COUNT)
                selectedInputBackground =
                    getDrawableOrThrow(R.styleable.OtpView_selectedInputDrawable)
                unSelectedInputBackground =
                    getDrawableOrThrow(R.styleable.OtpView_unSelectedInputDrawable)
                errorInputBackground = getDrawableOrThrow(R.styleable.OtpView_errorInputDrawable)
                itemPaddings = getDimension(R.styleable.OtpView_paddingBetweenItems, 0f)
            } finally {
                recycle()
            }
        }
    }

    private fun getCurrentInputBackground(index: Int): Drawable {
        return when {

            isInputErrorEnabled -> {
                errorInputBackground
            }

            code.length == index || (code.length == inputCount) && index == inputCount - 1 -> {
                selectedInputBackground
            }

            else -> {
                unSelectedInputBackground
            }
        }
    }

    private fun Canvas.drawInputs() {
        (1..inputCount).forEachIndexed { index, _ ->
            getCurrentInputBackground(index).apply {
                setBounds(
                    (index * intrinsicWidth) + itemPaddings.toInt() * index,
                    0,
                    ((index + 1) * intrinsicWidth) + itemPaddings.toInt() * index,
                    intrinsicHeight
                )
            }.draw(this)
        }
    }

    private fun Canvas.drawCodes() {
        code.forEachIndexed { index, char ->
            val currentInputBackground = getCurrentInputBackground(index)
            val currentInputBackgroundWidth = currentInputBackground.intrinsicWidth
            val currentInputBackgroundHeight = currentInputBackground.intrinsicHeight
            val halfWidthOfText = paint.measureText(char.toString()) / 2f
            drawText(
                char.toString(),
                (index * (currentInputBackgroundWidth + itemPaddings)) + ((currentInputBackgroundWidth / 2f) - halfWidthOfText),
                (currentInputBackgroundHeight / 2f) + (getCharHeight(char) / 2f),
                paint
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            drawInputs()
            drawCodes()
        }
    }

    override fun onTextChanged(
        text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int
    ) {
        if (code.length == inputCount) {
            inputFilledListener?.onFilled(code)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putBoolean(IS_INPUT_ERROR_ENABLED, isInputErrorEnabled)
            putParcelable(SUPER_STATE, super.onSaveInstanceState())
        }
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle).apply {
            if (this != null) {
                isInputErrorEnabled = getBoolean(IS_INPUT_ERROR_ENABLED, false)
            }
            val parcelable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this?.getParcelable(SUPER_STATE, Parcelable::class.java)
            } else {
                this?.getParcelable(SUPER_STATE)
            }
            super.onRestoreInstanceState(parcelable ?: state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val maxContentWidth = maxOf(
            selectedInputBackground.intrinsicWidth,
            unSelectedInputBackground.intrinsicWidth,
            errorInputBackground.intrinsicWidth
        ) * inputCount

        val maxContentHeight = maxOf(
            selectedInputBackground.intrinsicHeight,
            unSelectedInputBackground.intrinsicHeight,
            errorInputBackground.intrinsicHeight
        )

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val width = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            maxContentWidth + (itemPaddings.toInt() * (inputCount - 1))
        }

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            maxContentHeight
        }

        setMeasuredDimension(width, height)
    }

    private fun getCharHeight(char: Char): Float {
        return Rect().apply {
            paint.getTextBounds(char.toString(), 0, 1, this)
        }.height().toFloat()
    }

    fun showSoftKeyboard() {
        if (requestFocus()) {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun setOnInputFilledListener(l: SetOnInputFilledListener) {
        inputFilledListener = l
    }
}