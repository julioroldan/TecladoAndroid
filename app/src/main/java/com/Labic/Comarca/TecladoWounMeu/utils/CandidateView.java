package com.Labic.Comarca.TecladoWounMeu.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.Labic.Comarca.TecladoWounMeu.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ankit on 4/4/16.
 */
public class CandidateView extends View {
    private static final int OUT_OF_BOUNDS = 10;
    private SoftKeyboard mService;
    private List<String> mSuggestions;

    private ArrayList<String> wordSuggestions;
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    private boolean mTypedWordValid;

    private Rect mBgPadding;
    private static final int MAX_SUGGESTIONS = 32;
    private static final int SCROLL_PIXELS = 20;

    private int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private int[] mWordX = new int[MAX_SUGGESTIONS];
    private static final int X_GAP = 20;

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private int mColorNormal;
    private int mColorRecommended;
    private int mColorOther;
    private int mVerticalPadding;
    private Paint mPaint;
    private boolean mScrolled;
    private int mTargetScrollX;

    private int mTotalWidth;

    private GestureDetector mGestureDetector;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     *
     * @param context
     */
    public CandidateView(Context context) {

        super(context);

        mSelectionHighlight = context.getResources().getDrawable(android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(
                new int[]{
                        android.R.attr.state_enabled,
                        android.R.attr.state_focused,
                        android.R.attr.state_window_focused,
                        android.R.attr.state_pressed
                });
        Resources r = context.getResources();

        setBackgroundColor(r.getColor(R.color.candidate_background));

        mColorNormal = r.getColor(R.color.candidate_normal);
        mColorRecommended = r.getColor(R.color.candidate_recommended);
        mColorOther = r.getColor(R.color.candidate_other);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);

        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(10);

        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                mScrolled = true;
                int sx = getScrollX();
                sx += distanceX;
                if (sx < 0) {
                    sx = 0;
                }
                if (sx + getWidth() > mTotalWidth) {
                    sx -= distanceX;
                }
                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }

    /**
     * A connection back to the service to communicate with the text field
     *
     * @param listener
     */
    public void setService(SoftKeyboard listener) {
        mService = listener;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);

        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int) mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;

        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec));
    }
    ArrayList<String> lst = new ArrayList<String>();
    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        mTotalWidth = 0;
        if (mSuggestions == null) return;
        LoadWordSuggestions();
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        final int count = mSuggestions.size();
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;
        final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;
            mWordX[i] = x;
            mWordWidth[i] = wordWidth;
            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(10, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }
            if (canvas != null) {
                if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                    paint.setFakeBoldText(true);
                    paint.setColor(mColorRecommended);
                } else if (i != 0) {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(suggestion, x + X_GAP, y, paint);
                paint.setColor(mColorOther);
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top,
                        x + wordWidth + 0.5f, height + 1, paint);
                paint.setFakeBoldText(false);
            }
            x += wordWidth;
        }
        try {
            wordRects = new ArrayList<>();
            lst = new ArrayList<String>();
        String suggestion = mSuggestions.get(0);
        lst  =GetProximasPalabras(5,suggestion,wordSuggestions);
        int nextPosicion= x + X_GAP;
        for (String palabra: lst) {

            Rect rect = new Rect(nextPosicion, y, nextPosicion + (int) paint.getTextSize(), y + (int) paint.getTextSize());
            wordRects.add(rect);
            canvas.drawText(palabra, nextPosicion, y, paint);
            nextPosicion+=x +X_GAP+20;

        }
        }
        catch (Exception e)
        {}
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }
    ArrayList<Rect> wordRects = new ArrayList<>();
    private void LoadWordSuggestions()
    {
        wordSuggestions = palabrasClass.palabrasSugeridas();
    }
    private ArrayList<String> GetProximasPalabras(int numSugerencias, String palabraDada, ArrayList<String> palabras)
    {
        ArrayList<SugerenciaPalabra> sugerencias = new ArrayList<SugerenciaPalabra>();
        ArrayList<String> res = new ArrayList<String>();
        // Calcular la distancia de edición para cada palabra en 'palabras'
        for (String palabra: palabras) {
            int distancia = distanciaEdicion(palabra, palabraDada);
            sugerencias.add(new SugerenciaPalabra(palabra, distancia));
        }
        Collections.sort(sugerencias, new Comparator<SugerenciaPalabra>() {
            @Override
            public int compare(SugerenciaPalabra s1, SugerenciaPalabra s2) {
                return Integer.compare(s1.getDistancia(), s2.getDistancia());
            }
        });
        for (int i = 0; i < Math.min(numSugerencias, sugerencias.size()); i++) {
            res.add(sugerencias.get(i).getPalabra());
        }
        return res;
    }
    public static int distanciaEdicion(String palabra1, String palabra2) {
        int m = palabra1.length();
        int n = palabra2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (palabra1.charAt(i - 1) == palabra2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }

        return dp[m][n];
    }
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }

    public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid) {
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<String>(suggestions);
        }
        mTypedWordValid = typedWordValid;
        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
        //draw(null);
        invalidate();
        requestLayout();
    }

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }
        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrolled = false;

                String future="";

                boolean existe=false;
                for (int i = 0; i < wordRects.size(); i++) {
                    Rect rect = wordRects.get(i);
                    if (rect.left < x  && rect.right > x) {
                            existe=true;
                            future = lst.get(i);
                            mService.getCurrentInputConnection().setComposingText(future,1);
                            mService.getCurrentInputConnection().finishComposingText();
                            break;
                        }
                    }

                invalidate();
                return existe;

            case MotionEvent.ACTION_MOVE:
                if (y <= 0) {
                    // Fling up!?
                    if (mSelectedIndex >= 0) {
                        mService.pickSuggestionManually(mSelectedIndex);
                        mSelectedIndex = -1;
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (!mScrolled) {
                    if (mSelectedIndex >= 0) {
                        mService.pickSuggestionManually(mSelectedIndex);
                    }
                }

                mSelectedIndex = -1;
                removeHighlight();
                requestLayout();
                break;
        }
        return true;
    }

    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick
     * gesture.
     *
     * @param x
     */
    public void takeSuggestionAt(float x) {
        mTouchX = (int) x;
        // To detect candidate
        draw(null);
        if (mSelectedIndex >= 0) {
            mService.pickSuggestionManually(mSelectedIndex);
        }
        invalidate();
    }

    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }

}
class SugerenciaPalabra {
    private String palabra;
    private int distancia;

    public SugerenciaPalabra(String palabra, int distancia) {
        this.palabra = palabra;
        this.distancia = distancia;
    }

    public String getPalabra() {
        return palabra;
    }

    public int getDistancia() {
        return distancia;
    }
}