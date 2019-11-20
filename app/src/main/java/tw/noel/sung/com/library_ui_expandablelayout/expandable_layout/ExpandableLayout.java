package tw.noel.sung.com.library_ui_expandablelayout.expandable_layout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import tw.noel.sung.com.library_ui_expandablelayout.R;


public class ExpandableLayout extends LinearLayout implements ValueAnimator.AnimatorUpdateListener, View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {

    private int maxHeight;
    //當為水平 minHeight 鏈結view之當前寬度 ,為垂直則表示當前高度
    private int minHeight;
    //可被拖拉
    private boolean dragEnable;
    //延展方向
    private int expandWay;


    @IntDef({TOP, DOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExpandWay {
    }

    public static final int TOP = 0;
    public static final int DOWN = 1;

    private Context context;

    //寬高變化值漸變
    private ValueAnimator valueAnimator;

    private int oldHeight;
    private int currentHeight;
    private OnExpandStateChangeListener onExpandStateChangeListener;
    private ViewGroup.LayoutParams params;
    private int phoneMaxY;

    public ExpandableLayout(Context context) {
        this(context, null);
    }

    public ExpandableLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        initAttr(attrs, defStyleAttr);
        init();
    }
    //----------------

    private void initAttr(AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, 0);
        maxHeight = typedArray.getDimensionPixelSize(R.styleable.ExpandableLayout_ExpandableLayout_MaxHeight, 0);
        minHeight = typedArray.getDimensionPixelSize(R.styleable.ExpandableLayout_ExpandableLayout_MinHeight, 0);
        dragEnable = typedArray.getBoolean(R.styleable.ExpandableLayout_ExpandableLayout_Drag_Enable, true);
        expandWay = typedArray.getInt(R.styleable.ExpandableLayout_ExpandableLayout_ExpandWay, TOP);
        typedArray.recycle();
    }

    //-------------

    private void init() {
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        valueAnimator = new ValueAnimator();
        valueAnimator.addUpdateListener(this);
        valueAnimator.setInterpolator(new AccelerateInterpolator());

        setClickable(true);
        setFocusable(true);
        setDragEnable(dragEnable);
    }

    //------------------

    public void setDragEnable(boolean dragEnable){
        if(dragEnable){
            setOnTouchListener(this);
        }else {
            setOnTouchListener(null);
        }
    }

    //------------------

    /***
     *  手機高度
     * @return
     */
    private int getPhoneHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }


    //------------------

    @Override
    public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        minHeight = minHeight > 0 ? minHeight : getHeight();
        maxHeight = maxHeight > 0 ? maxHeight : context.getResources().getDisplayMetrics().heightPixels;
        currentHeight = minHeight;
        phoneMaxY = getPhoneHeight() + minHeight;
        params = getLayoutParams();

    }


    //------------------

    /***
     *   當為水平 maxHeight 表示最大寬度 ,為垂直則表示最大高度
     * @param maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    //--------------------

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (minHeight > 0 && maxHeight > minHeight) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (valueAnimator.isRunning()) {
                        valueAnimator.cancel();
                    }
                    oldHeight = params.height;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int currentSize;
                    if(expandWay == TOP){
                        currentSize = phoneMaxY - (int) (event.getRawY());
                    }else {
                        currentSize = (int) (event.getRawY());
                    }
                    currentSize = currentSize > maxHeight ? maxHeight : currentSize < minHeight ? minHeight : currentSize;
                    params.height = currentSize;
                    setLayoutParams(params);
                    break;
                case MotionEvent.ACTION_UP:
                    currentHeight = params.height;

                    //由下往上滑動
                    if (currentHeight > oldHeight) {
                        expand(true);
                    }
                    //由上往下滑動
                    else {
                        collapse(true);
                    }
                    break;
            }
        }

        return false;
    }


    //-----------------

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        currentHeight = (int) ((float) valueAnimator.getAnimatedValue());
        updateHeight();

        if (onExpandStateChangeListener != null) {
            if (currentHeight == minHeight) {
                onExpandStateChangeListener.onExpandStateChanged(false);
            } else if (currentHeight == maxHeight) {
                onExpandStateChangeListener.onExpandStateChanged(true);
            }
        }
    }
    //---------------

    /***
     *  更新高度
     */
    private void updateHeight(){
        params.height = currentHeight;
        setLayoutParams(params);
    }
    //-----------------

    /***
     *  展開
     */
    public void expand(boolean isAnimate) {
        if(isAnimate){
            valueAnimator.setFloatValues(currentHeight, maxHeight);
            valueAnimator.start();
        }else {
            currentHeight = maxHeight;
            updateHeight();
        }
    }

    //---------------

    /***
     * 收合
     */
    public void collapse(boolean isAnimate) {
        if(isAnimate){
            valueAnimator.setFloatValues(currentHeight, minHeight);
            valueAnimator.start();
        }else {
            currentHeight = minHeight;
            updateHeight();
        }
    }

    //-----------------

    /***
     *  已經完全展開
     */
    public boolean isExpanded() {
        return currentHeight == maxHeight;
    }

    //-----------------

    /***
     *  已經完全收合
     */
    public boolean isCollapsed() {
        return currentHeight == minHeight;
    }

    //---------------

    public interface OnExpandStateChangeListener {
        void onExpandStateChanged(boolean isExpanded);
    }
    //---------------

    public void setOnExpandStateChangeListener(OnExpandStateChangeListener onExpandStateChangeListener) {
        this.onExpandStateChangeListener = onExpandStateChangeListener;
    }
}
