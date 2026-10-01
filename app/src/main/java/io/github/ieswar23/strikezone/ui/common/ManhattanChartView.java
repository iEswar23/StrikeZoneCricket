package io.github.ieswar23.strikezone.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.ieswar23.strikezone.R;

/**
 * Over-by-over "Manhattan" bar chart. Bars grow in on first display; wickets appear as red dots
 * stacked on top of the bar for the over in which they fell. Overs that were not needed (chase
 * completed early) are drawn as dashed placeholders.
 */
public class ManhattanChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wicketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final float dp;

    private int[] runs = new int[0];
    private int[] wickets = new int[0];
    private int totalOvers = 1;
    private float progress = 1f;
    private final int barColorTop;
    private final int barColorBottom;
    private ValueAnimator animator;

    public ManhattanChartView(Context context) {
        this(context, null);
    }

    public ManhattanChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        int onSurface;
        TypedArray a = context.obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurfaceVariant});
        try {
            onSurface = a.getColor(0, Color.GRAY);
        } finally {
            a.recycle();
        }
        barColorTop = context.getColor(R.color.floodlight_yellow);
        barColorBottom = context.getColor(R.color.grass_green);
        gridPaint.setColor(Color.argb(60, Color.red(onSurface), Color.green(onSurface), Color.blue(onSurface)));
        gridPaint.setStrokeWidth(1 * dp);
        labelPaint.setColor(onSurface);
        labelPaint.setTextSize(11 * dp);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(onSurface);
        valuePaint.setTextSize(12 * dp);
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        wicketPaint.setColor(context.getColor(R.color.wicket_red));
        placeholderPaint.setStyle(Paint.Style.STROKE);
        placeholderPaint.setStrokeWidth(1.2f * dp);
        placeholderPaint.setColor(gridPaint.getColor());
        placeholderPaint.setPathEffect(new DashPathEffect(new float[]{4 * dp, 4 * dp}, 0));
        setContentDescription(context.getString(R.string.summary_chart_title));
    }

    /** Sets the data and animates the bars in. */
    public void setData(int[] runsPerOver, int[] wicketsPerOver, int overs) {
        runs = runsPerOver == null ? new int[0] : runsPerOver;
        wickets = wicketsPerOver == null ? new int[0] : wicketsPerOver;
        totalOvers = Math.max(1, Math.max(overs, runs.length));
        StringBuilder description = new StringBuilder(getContext().getString(R.string.summary_chart_title));
        for (int i = 0; i < runs.length; i++) {
            description.append(". Over ").append(i + 1).append(": ").append(runs[i]).append(" runs");
        }
        setContentDescription(description);
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(va -> {
            progress = (float) va.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // One vertical gradient for all bars: taller overs glow floodlight-yellow at the top.
        barPaint.setShader(new LinearGradient(0, 22 * dp, 0, h - 22 * dp, barColorTop, barColorBottom, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float left = 28 * dp;
        float right = getWidth() - 8 * dp;
        float top = 22 * dp;
        float bottom = getHeight() - 22 * dp;
        float chartH = bottom - top;

        int max = 6;
        for (int r : runs) max = Math.max(max, r);
        max = ((max + 5) / 6) * 6; // round up to a multiple of six for tidy gridlines

        // Gridlines with y labels
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 3; i++) {
            float y = bottom - chartH * i / 3f;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(String.valueOf(max * i / 3), left - 6 * dp, y + 4 * dp, labelPaint);
        }
        labelPaint.setTextAlign(Paint.Align.CENTER);

        float slot = (right - left) / totalOvers;
        float barW = Math.min(slot * 0.6f, 44 * dp);
        for (int i = 0; i < totalOvers; i++) {
            float centre = left + slot * i + slot / 2f;
            canvas.drawText(String.valueOf(i + 1), centre, bottom + 16 * dp, labelPaint);
            if (i >= runs.length) {
                bar.set(centre - barW / 2, bottom - chartH * 0.15f, centre + barW / 2, bottom);
                canvas.drawRoundRect(bar, 6 * dp, 6 * dp, placeholderPaint);
                continue;
            }
            float barTop = bottom - chartH * (runs[i] / (float) max) * progress;
            bar.set(centre - barW / 2, Math.min(barTop, bottom - 2 * dp), centre + barW / 2, bottom);
            canvas.drawRoundRect(bar, 6 * dp, 6 * dp, barPaint);
            if (progress > 0.95f) {
                canvas.drawText(String.valueOf(runs[i]), centre, bar.top - 6 * dp, valuePaint);
            }
            int w = i < wickets.length ? wickets[i] : 0;
            for (int k = 0; k < w; k++) {
                float cy = bar.top + 7 * dp + k * 11 * dp;
                if (cy > bottom - 5 * dp) break;
                canvas.drawCircle(centre, cy, 4.2f * dp * progress, wicketPaint);
            }
        }
    }
}
