package com.udacity

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import timber.log.Timber
import java.lang.Math.min
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import com.udacity.ButtonState.*
//import com.udacity.util.ext.disableViewDuringAnimation

//https://github.com/filipebezerra/loading-status-bar-app-android-kotlin/blob/main/app/src/main/res/values/strings.xml
class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0
    private var currentProgressCircleAnimationValue = 0f

    // It'll be initialized when first Loading state is trigger
    private lateinit var buttonTextBounds: Rect

    // region Progress Circle/Arc variables
    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f
    // region Styling attributes
    private var loadingDefaultBackgroundColor = 0
    private var loadingBackgroundColor = 0
    private var loadingDefaultText: CharSequence = ""
    private var loadingText: CharSequence = ""
    private var loadingTextColor = 0
    private var progressCircleBackgroundColor = 0

    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, FULL_ANGLE).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
    }

    // region General Button variables
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var buttonText = ""
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55f
        typeface = Typeface.DEFAULT
    }

    private var currentButtonBackgroundAnimationValue = 0f
    private var valueAnimator = ValueAnimator()

    // region Animation variables
    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = THREE_SECONDS
        disableViewDuringAnimation(this@LoadingButton)
    }

    private var buttonState: ButtonState by Delegates.observable<ButtonState>(Completed) { p, old, newState  ->

        Timber.d("Button state changed: $newState")
        when (newState) {
            Loading -> {
                // LoadingButton is now Loading and we need to set the correct text
                buttonText = loadingText.toString()

                // We only calculate ButtonText bounds and ProgressCircle rect once,
                // Only when buttonText is first initialized with loadingText
                if (!::buttonTextBounds.isInitialized) {
                    retrieveButtonTextBounds()
                    computeProgressCircleRect()
                }

                // ProgressCircle and Button background animations must start now
                animatorSet.start()
            }
            else -> {
                // LoadingButton is not doing any Loading so we need to reset to default text
                buttonText = loadingDefaultText.toString()

                // ProgressCircle animation must stop now
                newState.takeIf { it == Completed }?.run { animatorSet.cancel() }
            }
        }
    }

    private fun computeProgressCircleRect() {
        val horizontalCenter =
            (buttonTextBounds.right + buttonTextBounds.width() + PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET)
        val verticalCenter = (heightSize / BY_HALF)

        progressCircleRect.set(
            horizontalCenter - progressCircleSize,
            verticalCenter - progressCircleSize,
            horizontalCenter + progressCircleSize,
            verticalCenter + progressCircleSize
        )
    }


    /**
     * Initialize and retrieve the text boundary box of [buttonText] and store it into [buttonTextBounds].
     */
    private fun retrieveButtonTextBounds() {
        buttonTextBounds = Rect()
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, buttonTextBounds)
    }



    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            loadingDefaultBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingDefaultBackgroundColor, 0)
            loadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            loadingDefaultText = "download"//getText(R.styleable.LoadingButton_loadingDefaultText)
            loadingTextColor =
                getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            loadingText = "loading"//getText(R.styleable.LoadingButton_loadingText)
        }.also {
            buttonText = loadingDefaultText.toString()
            progressCircleBackgroundColor = ContextCompat.getColor(context, R.color.colorAccent)
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressCircleSize = (min(w, h) / BY_HALF) * PROGRESS_CIRCLE_SIZE_MULTIPLIER
        createButtonBackgroundAnimator()
    }
    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthSize.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
        }.also {
            valueAnimator = it
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }
    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, valueAnimator) }


    override fun performClick(): Boolean {
        super.performClick()
        // We only change button state to Clicked if the current state is Completed
        if (buttonState == Completed) {
            buttonState = Clicked
            invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { buttonCanvas ->
       //     Timber.d("LoadingButton onDraw()")
            buttonCanvas.apply {
                drawBackgroundColor()
                drawButtonText()
                drawProgressCircleIfLoading()
            }
        }
    }

    private fun Canvas.drawButtonText() {
        // Draw the Loading Text at the Center of the Canvas
        // ref.: https://blog.danlew.net/2013/10/03/centering_single_line_text_in_a_canvas/
        buttonTextPaint.color = loadingTextColor
        drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + buttonTextPaint.computeTextOffset(),
            buttonTextPaint
        )
    }

//
    private fun TextPaint.computeTextOffset() = ((descent() - ascent()) / 2) - descent()

    /**
     * Draws the default button background color using [loadingBackgroundColor].
     */
    private fun Canvas.drawBackgroundColor() {
        when (buttonState) {
            Loading -> {
                drawLoadingBackgroundColor()
                drawDefaultBackgroundColor()
            }
            else -> drawColor(loadingDefaultBackgroundColor)
        }
    }

    /**
     * Draws the [Rect] with [loadingBackgroundColor] representing the loading progress.
     */
    private fun Canvas.drawLoadingBackgroundColor() = buttonPaint.apply {
        color = loadingBackgroundColor
    }.run {
        drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws the [Rect] with [loadingDefaultBackgroundColor] representing the background of the button.
     */
    private fun Canvas.drawDefaultBackgroundColor() = buttonPaint.apply {
        color = loadingDefaultBackgroundColor
    }.run {
        drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthSize.toFloat(),
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws progress circle if [buttonState] is [ButtonState.Loading].
     */
    private fun Canvas.drawProgressCircleIfLoading() =
        buttonState.takeIf { it == Loading }?.let { drawProgressCircle(this) }

    /**
     * Draws the progress circle using an arc only when [buttonState] changes to [ButtonState.Loading].
     * The sweep angle uses [currentProgressCircleAnimationValue] which is changed according to when
     * [progressCircleAnimator] send updates after the values for the animation have been calculated.
     */
    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonPaint.color = progressCircleBackgroundColor
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            buttonPaint
        )
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }


    /**
     * Changes [buttonState] to the given [state] if they are not the same causing the view to be
     * redrawn.
     */
    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }

    // region LoadingButton constants
    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
        private const val FULL_ANGLE = 360f
        private val THREE_SECONDS = TimeUnit.SECONDS.toMillis(3)
    }
}