package com.ttawatchai.tapbarwithball.manager

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.PendingIntent.getActivity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import com.ttawatchai.tapbarwithball.ObjectClass
import com.ttawatchai.tapbarwithball.ObjectClass2
import com.ttawatchai.tapbarwithball.R
import kotlin.math.abs
import kotlin.math.floor


open class TabViewHelper : View {

    //region Properties

    open var tabsImages = mutableListOf<ObjectClass>()
        set(value) {
            field = value
            addTabsImages()
        }


    open var selectedTabIndex: Float = 0F
        set(value) {
            field = value
            moveToSelectedTab()
        }

    open var onTabChangeListener: ((Int) -> Unit)? = null

    //endregion

    //region Private properties

    private var bitmapsIcons = mutableListOf<ObjectClass2>()
    private val tabPaint: Paint by lazy {
        Paint().apply {
            color = tabColor
        }
    }

    private val ballPaint: Paint by lazy {
        Paint().apply {
            color = ballColor

        }
    }

    private val unSelectedTabTintPaint: Paint by lazy {
        Paint().apply {
            colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            color = unSelectedTabTintColor
        }
    }

    private val selectedTabTintPaint: Paint by lazy {
        Paint().apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            color = selectedTabTintColor
        }
    }

//  paint.color = Color.WHITE
//  paint.style = Paint.Style.FILL
//  canvas!!.drawPaint(paint)
//
//  paint.color = Color.BLACK
//  paint.textSize = 12F

    private val unSelectTabText: Paint by lazy {
        val spSize = 14
        val scaledSizeInPixels = spSize * resources.displayMetrics.scaledDensity
        Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = scaledSizeInPixels
        }
    }

    private val selectTabText: Paint by lazy {
        Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 0F
        }
    }

    private val itemWidth: Float
        get() {
            val width = this.width / numberOfTabs
            return width
        }

    private val sectionWidth: Float
        get() {
            return width / numberOfTabs
        }
    private val sectionHight: Float
        get() {
            val tv = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                return TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
                    .toFloat().also {
                        Log.d(
                            "height",
                            it.toString()
                        )
                    }
            }
            return 0F
        }

    private val numberOfTabs: Float
        get() {
            return tabsImages.size.toFloat()
        }

    private val itemHeight: Float
        get() {
            val height = sectionHight
            return if (height > ballSize) ballSize else height.toFloat()
        }

    private val ballSize: Float
        get() {
            return sectionWidth / 3F
        }

    private var tabAnimationPercentage = 1F
    private var ballX: Float = 0F
    private var ballY: Float = 0F

    private var previousTabIndex: Float = 0F

    private var tabColor: Int = 0
    private var ballColor: Int = 0
    private var selectedTabTintColor: Int = 0
    private var unSelectedTabTintColor: Int = 0

    //endregion

    //region Constructors
    constructor(context: Context) : super(context) {
        setupAttributes(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setupAttributes(attrs)
    }
    //endregion

    private fun setupAttributes(attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.AMTabView, 0, 0)
        tabColor = typedArray.getColor(R.styleable.AMTabView_tabColor, Color.parseColor("#07891F"))
        ballColor =
            typedArray.getColor(R.styleable.AMTabView_ballColor, Color.parseColor("#C07298"))
        selectedTabTintColor = typedArray.getColor(
            R.styleable.AMTabView_selectedTabTintColor, Color.parseColor(
                "#FFFFFF"
            )
        )
        unSelectedTabTintColor = typedArray.getColor(
            R.styleable.AMTabView_unSelectedTabTintColor, Color.parseColor(
                "#000000"
            )
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x ?: 0F

        selectedTabIndex = floor(x / sectionWidth)

        moveToSelectedTab()
        performClick()

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTabChangeListener?.let { it(selectedTabIndex.toInt()) }
        return true
    }

    private fun addTabsImages() {
        bitmapsIcons = tabsImages
            .map { ObjectClass2(it.title, BitmapFactory.decodeResource(resources, it.drawable)) }
            .toMutableList()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post {
            moveToSelectedTab()
        }
    }

    //region Animation

    private fun animateBall() {
        val fromValue = ballPath().first.toFloat()
        val toValue = ballPath().second.toFloat()
        val y = ballPath().third.toFloat()
        ValueAnimator.ofFloat(fromValue, toValue).apply {
            addUpdateListener {
                ballX = it.animatedValue as Float
                invalidate()
            }
            duration = 200L
            start()
        }

        ValueAnimator.ofFloat(0F, y).apply {
            addUpdateListener {
                ballY = it.animatedValue as Float
                invalidate()
            }
            interpolator = TimeInterpolator {
                if (it > 0.5) {
                    return@TimeInterpolator abs(it - 1)
                }
                return@TimeInterpolator it
            }
            duration = 200L
            start()
        }
    }

    private fun animateTab(completion: (() -> Unit)) {
        ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener {
                tabAnimationPercentage = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                completion()
            })
            duration = 200L
            start()
        }
    }


    //endregion

    //region onDraw

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val shape = ContextCompat.getDrawable(context, R.drawable.circle);
        val bitmap =
            Bitmap.createBitmap(ballSize.toInt()*2, ballSize.toInt()*2, Bitmap.Config.ARGB_8888)
        val canvasCircle = Canvas(bitmap)
        shape?.setBounds(0, 0, bitmap.width, bitmap.height)
        shape?.draw(canvasCircle)
        canvas?.drawBitmap(bitmap,ballX-ballSize,ballY-ballSize + sectionHight * 0.2F,ballPaint)
        canvas?.drawPath(holePathForSelectedIndex(), tabPaint)
//        canvas?.drawCircle(ballX, ballY + sectionHight * 0.2F, ballSize, ballPaint)

        bitmapsIcons
            .withIndex()
            .forEach {
                val bounds = Rect()
                val isSelectedIndex = it.index.toFloat() == selectedTabIndex
                val y =
                    if (isSelectedIndex) (-(it.value.drawable.height / 4F) * tabAnimationPercentage) else ((height / 3) - (it.value.drawable.width / 2)).toFloat()
                val x =
                    (it.index * sectionWidth) + (sectionWidth / 2) - (it.value.drawable.width / 2)
                val yText =
                    if (isSelectedIndex) (0F) else ((height / 1.15)).toFloat()
                val paint = if (isSelectedIndex) selectedTabTintPaint else unSelectedTabTintPaint
                val paintText = if (isSelectedIndex) selectTabText else unSelectTabText
                paintText.getTextBounds(it.value.title, 0, it.value.title.length, bounds);
                val xText =
                    if (bounds.width() < sectionWidth) ((it.index * sectionWidth) + ((sectionWidth - bounds.width()) / 2)) else (0F)

                canvas?.drawBitmap(it.value.drawable, x, y, paint)
                canvas?.drawText(it.value.title, xText, yText, paintText)

            }
    }

    //endregion

    //region Moving methods

    private fun moveToSelectedTab() {
        animateBall()
        animateTab {
            previousTabIndex = selectedTabIndex
        }
    }

    //endregion

    //region Paths

    private fun ballPath(): Triple<Double, Double, Double> {
        val fromX = ((previousTabIndex + 1) * sectionWidth) - (sectionWidth * 0.5)
        val toX = ((selectedTabIndex + 1) * sectionWidth) - (sectionWidth * 0.5)
        val controlPointY = -500.0
        return Triple(fromX, toX, controlPointY)
    }

    private fun holePathForSelectedIndex1(): Path {
        val sectionWidth = sectionWidth
        val sectionHeight = (sectionHight + (ballSize * 1.5F))
        return Path().apply {
            moveTo(0F, 0F)
            lineTo(((selectedTabIndex * sectionWidth) - (sectionWidth * 0.3)).toFloat(), 0F)
            quadTo(
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.1)).toFloat() - (ballSize * 0.5F),
                0F * tabAnimationPercentage,
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.2)).toFloat() - (ballSize * 0.5F),
                (itemHeight * 0.5).toFloat() * tabAnimationPercentage
            )
            quadTo(
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.5)).toFloat(),
                (sectionHeight * 0.75).toFloat() * tabAnimationPercentage,
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.9)).toFloat(),
                (itemHeight * 0.5).toFloat() * tabAnimationPercentage
            )
            quadTo(
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.75)).toFloat() + (ballSize),
                0F * tabAnimationPercentage,
                ((selectedTabIndex * sectionWidth) + sectionWidth + (sectionWidth * 0.2)).toFloat() + (ballSize * 2),
                0F * tabAnimationPercentage
            )
            lineTo(width.toFloat(), 0F)
            lineTo(width.toFloat(), sectionHeight)
            lineTo(0F, sectionHeight)
            lineTo(0F, 0F)
            close()
        }
    }

    private fun holePathForSelectedIndex(): Path {
        val sectionWidth = sectionWidth
        val sectionHeight = (sectionHight + (ballSize * 1.5F))
        return Path().apply {
            moveTo(0F, 0F)
            lineTo(((selectedTabIndex * sectionWidth) - (sectionWidth * 0.3)).toFloat(), 0F)
            quadTo(
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.1)).toFloat() - (itemWidth * 0.25F).also {
                    Log.d(
                        "ballx x1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                0F * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.2)).toFloat() - (itemWidth * 0.25F).also {
                    Log.d(
                        "ballx x2 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                (itemHeight * 0.5).toFloat() * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y2 : " + selectedTabIndex.toString(), it.toString()
                    )
                }
            )
            quadTo(
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.5)).toFloat().also {
                    Log.d(
                        "ballx x1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                (sectionHeight * 0.75).toFloat() * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                ((selectedTabIndex * sectionWidth) + (sectionWidth * 0.85)).toFloat() + (itemWidth * 0.2F).also {
                    Log.d(
                        "ballx x2 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                (itemHeight * 0.5).toFloat() * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y2 : " + selectedTabIndex.toString(), it.toString()
                    )
                }
            )
            quadTo(
                (((selectedTabIndex * sectionWidth) + (sectionWidth * 0.9)).toFloat()) + (itemWidth * 0.3F).also {
                    Log.d(
                        "ballx x1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                0F * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y1 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                ((selectedTabIndex * sectionWidth) + sectionWidth + (sectionWidth * 0.2)).toFloat() + (itemWidth * 0.1F).also {
                    Log.d(
                        "ballx x2 : " + selectedTabIndex.toString(), it.toString()
                    )
                },
                0F * tabAnimationPercentage.also {
                    Log.d(
                        "ballx y2 : $selectedTabIndex", it.toString()
                    )
                }
            )
            lineTo(width.toFloat(), 0F)
            lineTo(width.toFloat(), sectionHeight)
            lineTo(0F, sectionHeight)
            lineTo(0F, 0F)
            close()
        }
    }

    //endregion

}