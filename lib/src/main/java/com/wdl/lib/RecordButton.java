package com.wdl.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;


/**
 * Create by: wdl at 2020/3/19 15:24
 * 仿微信录制表情按钮
 */
@SuppressWarnings("unused")
public class RecordButton extends View implements View.OnTouchListener
{
    // TODO 起始/结束动画时长控制
    // TODO 录制时长控制等
    /**
     * 同心圆圆心
     */
    private long mCircleX;
    private long mCircleY;

    /**
     * View的宽高，本例中为正方形
     */
    private int mViewWH;

    /**
     * 录制时长
     */
    private long mRecordTime = 3000;

    /**
     * 缩放系数
     */
    private static final float SCALE_FACTOR = 0.67f;
    /**
     * 内外圆缩放比例
     */
    private static final float INNER_OUTER_SCALING = 0.75f;

    /**
     * 触摸延时，最小录制时间
     */
    private static final long TOUCH_DELAY = 300L;
    private static final long MIN_RECORD_TIME = TOUCH_DELAY + 200L;


    /**
     * 进度条画笔，进度条宽度，进度条颜色，定义进度条绘制的区域以及角度
     */
    private Paint mProgressPaint;
    private int mProgressStroke;
    private int mProgressColor;
    private RectF mProgressRf;
    private float mProgressAngle;

    /**
     * 内部圆画笔，内圆半径，颜色
     */
    private Paint mInnerCirclePaint;
    private int mInnerRadius;
    private int mInnerColor;

    /**
     * 外部圆画笔，内圆半径，颜色
     */
    private Paint mOutCirclePaint;
    private int mOutRadius;
    private int mOutColor;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private RecordListener mListener;

    private volatile boolean isFinish = false;
    private long hisTime;


    public RecordButton(Context context)
    {
        this(context, null);
    }

    public RecordButton(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public RecordButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs)
    {
        final TypedArray mTy = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);
        mProgressStroke = mTy.getInt(R.styleable.RecordButton_progressWidth, 15);
        mInnerColor = mTy.getColor(R.styleable.RecordButton_innerColor, getResources().getColor(R.color.innerCircle));
        mOutColor = mTy.getColor(R.styleable.RecordButton_outColor, getResources().getColor(R.color.externalCircle));
        mProgressColor = mTy.getColor(R.styleable.RecordButton_progressColor, getResources().getColor(R.color.progress));
        mTy.recycle();

        initPaint();

        setBackgroundResource(R.color.background);
        setOnTouchListener(this);

    }

    private void initPaint()
    {
        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStrokeWidth(mProgressStroke);

        mInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerCirclePaint.setColor(mInnerColor);

        mOutCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutCirclePaint.setColor(mOutColor);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawCircle(mCircleX, mCircleY, mOutRadius, mOutCirclePaint);
        canvas.drawCircle(mCircleX, mCircleY, mInnerRadius, mInnerCirclePaint);

        //画弧形
        canvas.drawArc(mProgressRf, -90, mProgressAngle, false, mProgressPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        mViewWH = Math.min(width, height);
        mCircleX = mCircleY = mViewWH / 2;
        reset();
        setMeasuredDimension(mViewWH, mViewWH);
    }

    /**
     * 初始化参数,确定内外圆半径
     */
    private void reset()
    {
        mOutRadius = (int) (mViewWH / 2 * SCALE_FACTOR);
        mInnerRadius = (int) (mOutRadius * INNER_OUTER_SCALING);

        mProgressAngle = 0;

        final int tOrB = mProgressStroke / 2;
        final int lOrR = mViewWH - tOrB;
        mProgressRf = new RectF(tOrB, tOrB, lOrR, lOrR);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        //if (mListener == null) return false;
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                onActionDown();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onActionEndAction();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 按下执行的操作
     */
    private void onActionDown()
    {
        // 标记完成
        isFinish = false;
        // 移除上一个还在执行的任务
        if (mRunnable != null)
        {
            mHandler.removeCallbacks(mRunnable);
        }

        // 记录点击时间，判断是否小时最小录制时间等
        hisTime = System.currentTimeMillis();

        mRunnable = new RecordRunnable();
        mHandler.postDelayed(mRunnable, TOUCH_DELAY);
    }

    public interface RecordListener
    {
        void onClick();

        void onRecording();

        void onError();
    }

    public void setListener(RecordListener mListener)
    {
        this.mListener = mListener;
    }

    private Runnable mRunnable;

    private class RecordRunnable implements Runnable
    {
        @Override
        public void run()
        {
            // 延时后动作还在执行
            if (!isFinish)
            {
                if (mListener != null) mListener.onRecording();

                // 开启动画
                startBeginAnimator();
            }
        }
    }

    private void startBeginAnimator()
    {
        reset();

        // 内圆缩小
        ValueAnimator mInnerAnimator = ValueAnimator.ofInt(mInnerRadius, (int) (mInnerRadius * SCALE_FACTOR));
        mInnerAnimator.setInterpolator(new LinearInterpolator());
        mInnerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                mInnerRadius = (int) animation.getAnimatedValue();
                // 已经完成，结束动画
                if (isFinish)
                {
                    animation.cancel();
                }
                postInvalidate();
            }
        });

        // 外缩小
        ValueAnimator mOuterAnimator = ValueAnimator.ofInt(mOutRadius, (int) (mOutRadius / SCALE_FACTOR));
        mOuterAnimator.setInterpolator(new LinearInterpolator());
        mOuterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                mOutRadius = (int) animation.getAnimatedValue();
                // 已经完成，结束动画
                if (isFinish)
                {
                    animation.cancel();
                }
                postInvalidate();
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(mInnerAnimator, mOuterAnimator);
        set.setDuration(200);
        set.setInterpolator(new LinearInterpolator());
        set.start();

        set.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                if (isFinish) return;

                startProgressAnimator();
            }
        });

    }

    private void startProgressAnimator()
    {
        //这里是进度条进度获取和赋值
        ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(mRecordTime);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                mProgressAngle = isFinish ? 0 : (float) valueAnimator.getAnimatedValue();
                //更新ui
                postInvalidate();
                //如果动作结束了，结束动画
                if (isFinish)
                {
                    valueAnimator.cancel();
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                onActionEndAction();
            }
        });
        animator.start();
    }

    private void onActionEndAction()
    {
        isFinish = true;
        final long time = System.currentTimeMillis() - hisTime;
        // 录制时间过短
        if (time < MIN_RECORD_TIME)
        {
            if (mListener != null)
            {
                mListener.onError();
            }
        }

        //执行结束动画效果
        startEndCircleAnimation();
    }

    private void startEndCircleAnimation()
    {
        //每次缩放动画之前重置参数，防止出现ui错误
        reset();
        //这里是内圈圆半径获取和赋值
        ValueAnimator animator = ValueAnimator.ofInt((int) (mInnerRadius * SCALE_FACTOR), mInnerRadius);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                mInnerRadius = (int) valueAnimator.getAnimatedValue();
                if (!isFinish)
                {
                    //如果在结束动画播放过程中再次点击，及时停止动画
                    valueAnimator.cancel();
                }
                //更新ui
                postInvalidate();
            }
        });
        //这里是外圈圆半径获取和赋值
        ValueAnimator animator1 = ValueAnimator.ofInt((int) (mOutRadius / SCALE_FACTOR), mOutRadius);
        animator1.setInterpolator(new LinearInterpolator());
        animator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                mOutRadius = (int) valueAnimator.getAnimatedValue();
                if (!isFinish)
                {
                    //如果在结束动画播放过程中再次点击，及时停止动画
                    valueAnimator.cancel();
                }
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator, animator1);
        set.setDuration(200);
        set.setInterpolator(new LinearInterpolator());
        set.start();
        set.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                postInvalidate();
            }
        });
    }
}
