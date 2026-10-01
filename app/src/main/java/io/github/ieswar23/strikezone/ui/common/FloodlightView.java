package io.github.ieswar23.strikezone.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Decorative night-stadium backdrop: a navy gradient sky with four floodlight beams that sweep
 * slowly and a twinkling crowd. Used behind the home screen title.
 */
public class FloodlightView extends View {
    private static final int SPARKS = 46;

    private final Paint skyPaint = new Paint();
    private final Paint beamPaintHigh = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint beamPaintLow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lampPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path beam = new Path();
    private final float[] sparkX = new float[SPARKS];
    private final float[] sparkY = new float[SPARKS];
    private final float[] sparkPhase = new float[SPARKS];
    private final float dp;
    private ValueAnimator animator;
    private float time;

    public FloodlightView(Context context) {
        this(context, null);
    }

    public FloodlightView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        Random r = new Random(7);
        for (int i = 0; i < SPARKS; i++) {
            sparkX[i] = r.nextFloat();
            sparkY[i] = 0.55f + r.nextFloat() * 0.45f;
            sparkPhase[i] = r.nextFloat() * 6.28f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        skyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#050A1A"), Color.parseColor("#0E1B3D"), Color.parseColor("#14592A")},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        // Beams fade vertically; one shader per tower height, built once per size.
        beamPaintHigh.setShader(new LinearGradient(0, h * 0.02f, 0, h, Color.argb(70, 255, 236, 170),
                Color.argb(0, 255, 236, 170), Shader.TileMode.CLAMP));
        beamPaintLow.setShader(new LinearGradient(0, h * 0.10f, 0, h, Color.argb(70, 255, 236, 170),
                Color.argb(0, 255, 236, 170), Shader.TileMode.CLAMP));
        lampPaint.setShader(new RadialGradient(0, 0, 26 * dp, Color.argb(255, 255, 246, 204),
                Color.argb(0, 255, 210, 63), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(8000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        final long start = SystemClock.uptimeMillis();
        animator.addUpdateListener(a -> {
            // Continuous clock (the animator itself only ticks frames) so beams never jump on repeat.
            time = (SystemClock.uptimeMillis() - start) / 1000f;
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        animator = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        canvas.drawRect(0, 0, w, h, skyPaint);

        drawBeam(canvas, beamPaintLow, w * 0.08f, h * 0.10f, w * 0.55f + (float) Math.sin(time * 0.6) * w * 0.12f, h);
        drawBeam(canvas, beamPaintLow, w * 0.92f, h * 0.10f, w * 0.45f + (float) Math.cos(time * 0.5) * w * 0.12f, h);
        drawBeam(canvas, beamPaintHigh, w * 0.30f, h * 0.02f, w * 0.70f + (float) Math.sin(time * 0.4 + 1) * w * 0.10f, h);
        drawBeam(canvas, beamPaintHigh, w * 0.70f, h * 0.02f, w * 0.30f + (float) Math.cos(time * 0.45 + 2) * w * 0.10f, h);

        drawLamp(canvas, w * 0.08f, h * 0.10f);
        drawLamp(canvas, w * 0.92f, h * 0.10f);
        drawLamp(canvas, w * 0.30f, h * 0.02f);
        drawLamp(canvas, w * 0.70f, h * 0.02f);

        for (int i = 0; i < SPARKS; i++) {
            float a = (float) (0.5 + 0.5 * Math.sin(time * 3 + sparkPhase[i]));
            sparkPaint.setColor(Color.argb((int) (40 + 150 * a), 255, 236, 170));
            canvas.drawCircle(sparkX[i] * w, sparkY[i] * h, 1.2f * dp, sparkPaint);
        }
    }

    private void drawBeam(Canvas canvas, Paint paint, float sx, float sy, float tx, float h) {
        float spread = 70 * dp;
        beam.reset();
        beam.moveTo(sx, sy);
        beam.lineTo(tx - spread, h);
        beam.lineTo(tx + spread, h);
        beam.close();
        canvas.drawPath(beam, paint);
    }

    private void drawLamp(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.translate(x, y);
        canvas.drawCircle(0, 0, 26 * dp, lampPaint);
        canvas.restore();
    }
}
