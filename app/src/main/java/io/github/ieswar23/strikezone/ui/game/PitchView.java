package io.github.ieswar23.strikezone.ui.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Random;

import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.Delivery;
import io.github.ieswar23.strikezone.engine.FieldSetting;
import io.github.ieswar23.strikezone.engine.Fielder;
import io.github.ieswar23.strikezone.engine.OutcomeType;
import io.github.ieswar23.strikezone.engine.ShotDirection;
import io.github.ieswar23.strikezone.engine.ShotInput;
import io.github.ieswar23.strikezone.util.ColorUtils;

/**
 * Night-stadium cricket ground rendered with Canvas and driven by {@link Choreographer} at the
 * display refresh rate. The static ground is pre-rendered into a bitmap once per size change;
 * each frame only draws the moving pieces (bowler, ball, shadow, batter, fielders, stumps), with
 * no allocations in {@link #onDraw(Canvas)}.
 *
 * <p>Timing: the moment the ball reaches the batter's crease is {@code arrivalTime}. A tap's
 * offset from that moment (negative = early) is reported through {@link Listener#onShot}. An
 * "approach ring" closes on the hitting zone exactly at {@code arrivalTime} to help the player.
 */
public class PitchView extends View implements Choreographer.FrameCallback {

    public interface Listener {
        /** Exactly once per delivery: the swing (or leave) to resolve. */
        void onShot(ShotInput input);

        /** The player swung in this direction (for haptics / HUD feedback). */
        void onSwing(ShotDirection direction);

        /** The outcome animation finished; ready for the next ball. */
        void onBallFinished();
    }

    private enum Phase { IDLE, WAITING, RUN_UP, FLIGHT, OUTCOME }

    private static final int TRAIL = 10;
    private static final long SWING_MS = 220L;
    private static final long STUMPS_FLY_MS = 520L;

    // Palette used every frame (parsed once).
    private static final int NAVY = 0xFF0A1128;
    private static final int GRASS = 0xFF3FB950;
    private static final int STUMP = 0xFFF5E6C8;
    private static final int FLOODLIGHT = 0xFFFFD23F;
    private static final int WILLOW = 0xFFE9D3A1;
    private static final int GRIP = 0xFF1B1B1B;

    private final float dp;
    private final int touchSlop;
    private final Choreographer choreographer = Choreographer.getInstance();

    // Paints (allocated once)
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint seamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();
    private final RectF rect = new RectF();

    // Geometry
    private float width;
    private float height;
    private float cx;
    private float fieldCy;
    private float rx;
    private float ry;
    private float bowlerCreaseY;
    private float batterCreaseY;
    private float pitchTopHalfW;
    private float pitchBotHalfW;
    private float keeperY;
    private float ballRadius;
    private Bitmap background;

    // Teams
    private int battingColor = Color.parseColor("#1E6FD9");
    private int bowlingColor = Color.parseColor("#C8102E");
    private FieldSetting field = FieldSetting.of(FieldSetting.Plan.BALANCED);

    // Delivery state
    private Phase phase = Phase.IDLE;
    private Delivery delivery;
    private long phaseStart;
    private long startDelay;
    private long runUpMs;
    private long releaseTime;
    private long arrivalTime;
    private long perfectWindow;
    private long goodWindow;
    private boolean shotReported;
    private boolean touchActive;
    private long touchDownTime;
    private float touchDownX;
    private float touchDownY;
    private ShotInput pendingInput;
    private long swingStart = -1L;
    private ShotDirection swingDirection = ShotDirection.STRAIGHT;

    // Last ball position (ground x/y + height above ground in px)
    private float ballX;
    private float ballY;
    private float ballH;
    private boolean ballVisible;
    private final float[] trailX = new float[TRAIL];
    private final float[] trailY = new float[TRAIL];
    private int trailCount;
    private int trailHead;

    // Outcome state: up to two path segments (e.g. run-out = to fielder, then throw to stumps)
    private BallOutcome outcome;
    private long outcomeStart;
    private long outcomeTotal;
    private int segmentCount;
    private final float[] segSx = new float[2];
    private final float[] segSy = new float[2];
    private final float[] segSh = new float[2];
    private final float[] segEx = new float[2];
    private final float[] segEy = new float[2];
    private final float[] segPeak = new float[2];
    private final long[] segDur = new long[2];
    private int breakStumpsAfterSegment = -1;
    private long stumpsBrokenAt = -1L;
    private Fielder highlightFielder;
    private boolean highlightKeeper;
    private int flashColor;
    private long flashStart = -1L;

    private boolean running;
    private long pausedAt = -1L;

    @Nullable
    private Listener listener;

    public PitchView(Context context) {
        this(context, null);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        stroke.setStyle(Paint.Style.STROKE);
        ringPaint.setStyle(Paint.Style.STROKE);
        ballPaint.setColor(Color.WHITE);
        seamPaint.setStyle(Paint.Style.STROKE);
        seamPaint.setColor(Color.parseColor("#3A6EA5"));
        shadowPaint.setColor(Color.argb(90, 0, 0, 0));
        setContentDescription("Cricket pitch. Tap left, centre or right as the ball reaches the batter.");
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setTeamColors(int batting, int bowling) {
        battingColor = batting;
        bowlingColor = bowling;
        invalidate();
    }

    public void setFieldSetting(@NonNull FieldSetting fieldSetting) {
        field = fieldSetting;
        invalidate();
    }

    // ------------------------------------------------------------------------------------------
    // Public control API
    // ------------------------------------------------------------------------------------------

    /** Starts the bowler's run-up for a new delivery. */
    public void bowl(@NonNull DeliveryCommand command) {
        delivery = command.delivery;
        field = command.field;
        startDelay = command.startDelayMs;
        perfectWindow = command.perfectWindowMs;
        goodWindow = command.goodWindowMs;
        runUpMs = delivery.getBowlerType().getRunUpMs();
        phase = Phase.WAITING;
        phaseStart = now();
        shotReported = false;
        touchActive = false;
        pendingInput = null;
        swingStart = -1L;
        outcome = null;
        segmentCount = 0;
        breakStumpsAfterSegment = -1;
        stumpsBrokenAt = -1L;
        highlightFielder = null;
        highlightKeeper = false;
        ballVisible = false;
        trailCount = 0;
        startLoop();
    }

    /** Animates the resolved outcome of the ball currently in play. */
    public void playOutcome(@NonNull BallOutcome result) {
        outcome = result;
        outcomeStart = now();
        phase = Phase.OUTCOME;
        trailCount = 0;
        planOutcomePath(result);
        startLoop();
    }

    public void pause() {
        if (pausedAt < 0) pausedAt = now();
        stopLoop();
    }

    public void resume() {
        if (pausedAt >= 0) {
            long shift = now() - pausedAt;
            phaseStart += shift;
            releaseTime += shift;
            arrivalTime += shift;
            outcomeStart += shift;
            if (swingStart >= 0) swingStart += shift;
            if (stumpsBrokenAt >= 0) stumpsBrokenAt += shift;
            if (flashStart >= 0) flashStart += shift;
            if (touchActive) touchDownTime += shift;
            pausedAt = -1L;
        }
        if (phase != Phase.IDLE) startLoop();
    }

    private static long now() {
        return SystemClock.uptimeMillis();
    }

    private void startLoop() {
        if (running || pausedAt >= 0) return;
        running = true;
        choreographer.postFrameCallback(this);
    }

    private void stopLoop() {
        running = false;
        choreographer.removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        update(now());
        invalidate();
        if (phase != Phase.IDLE || isFlashing()) {
            choreographer.postFrameCallback(this);
        } else {
            running = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        super.onDetachedFromWindow();
    }

    // ------------------------------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------------------------------

    private void update(long t) {
        switch (phase) {
            case WAITING:
                if (t - phaseStart >= startDelay) {
                    phase = Phase.RUN_UP;
                    phaseStart += startDelay;
                }
                break;
            case RUN_UP:
                if (t - phaseStart >= runUpMs) {
                    phase = Phase.FLIGHT;
                    releaseTime = phaseStart + runUpMs;
                    arrivalTime = releaseTime + delivery.getTravelMs();
                    ballVisible = true;
                }
                break;
            case FLIGHT:
                updateFlight(t);
                break;
            case OUTCOME:
                updateOutcome(t);
                break;
            default:
                break;
        }
    }

    private void updateFlight(long t) {
        float p = (t - releaseTime) / (float) delivery.getTravelMs();
        computeDeliveryPosition(p);
        pushTrail();
        if (shotReported) return;
        if (pendingInput != null) {
            // Contact happens when the ball arrives (or immediately for late swings).
            if (t >= arrivalTime || pendingInput.getOffsetMs() >= 0) report(pendingInput);
        } else if (!touchActive && t > arrivalTime + goodWindow + 60) {
            report(ShotInput.leave());
        } else if (touchActive && t > arrivalTime + goodWindow + 400) {
            report(ShotInput.leave());
        }
    }

    private void report(ShotInput input) {
        shotReported = true;
        if (listener != null) listener.onShot(input);
    }

    /** Ball position along the delivery for progress p (0 = release, 1 = batter's crease). */
    private void computeDeliveryPosition(float p) {
        float bounce = delivery.getLength().getBounceFraction();
        float releaseX = cx + halfWidthAt(bowlerCreaseY) * 0.4f;
        float releaseY = bowlerCreaseY + 2 * dp;
        float bounceY = lerp(bowlerCreaseY, batterCreaseY, bounce);
        float bounceX = cx + delivery.getLine().getOffset() * halfWidthAt(bounceY) * 0.9f;
        float arrivalX = cx + (delivery.getLine().getOffset() + delivery.getDeviation()) * halfWidthAt(batterCreaseY);
        float releaseH = 30 * dp;
        float riseH = 24 * dp * delivery.getLength().getRiseAfterBounce();
        if (p <= bounce) {
            float q = Math.max(0f, p / bounce);
            ballX = lerp(releaseX, bounceX, q);
            ballY = lerp(releaseY, bounceY, q);
            ballH = releaseH * (1f - q) * (1f + 0.4f * q);
        } else if (p <= 1f) {
            float q = (p - bounce) / (1f - bounce);
            ballX = lerp(bounceX, arrivalX, q);
            ballY = lerp(bounceY, batterCreaseY, q);
            ballH = riseH * (float) Math.sin(q * Math.PI / 2);
        } else {
            float over = p - 1f;
            float slope = (arrivalX - bounceX) / Math.max(0.05f, 1f - bounce);
            ballX = arrivalX + slope * over;
            ballY = batterCreaseY + over * (batterCreaseY - bowlerCreaseY);
            if (ballY > keeperY - 8 * dp) ballY = keeperY - 8 * dp;
            ballH = riseH;
        }
    }

    private void planOutcomePath(BallOutcome o) {
        OutcomeType type = o.getType();
        float sx = ballX;
        float sy = ballY;
        float sh = ballH;
        highlightFielder = null;
        highlightKeeper = false;
        segmentCount = 0;
        breakStumpsAfterSegment = -1;
        stumpsBrokenAt = -1L;
        boolean contact = o.getTiming().madeContact();

        if (type == OutcomeType.BOWLED) {
            segment(0, sx, sy, sh, cx, batterCreaseY + 2 * dp, 0, 140);
            segment(1, cx, batterCreaseY + 2 * dp, 4 * dp, cx + 14 * dp, keeperY - 14 * dp, 6 * dp, 320);
            segmentCount = 2;
            breakStumpsAfterSegment = 0;
            flash(Color.argb(70, 255, 77, 94));
        } else if (!contact) {
            // Missed, left or wide: carries through to the keeper.
            float ex = lerp(sx, cx, 0.5f);
            segment(0, sx, sy, sh, ex, keeperY - 6 * dp, 0, 380);
            segmentCount = 1;
            highlightKeeper = type == OutcomeType.CAUGHT;
            if (type.isWicket()) flash(Color.argb(70, 255, 77, 94));
        } else {
            float bx = cx;
            float by = batterCreaseY;
            double angle = o.getShotAngleDeg();
            Fielder fielder = field.find(o.getFielderPosition());
            switch (type) {
                case SIX: {
                    float d = boundaryDistance(angle) * 1.18f;
                    segment(0, bx, by, sh, polarX(angle, d), polarY(angle, d), height * 0.42f, 1250);
                    flash(Color.argb(90, 255, 210, 63));
                    break;
                }
                case FOUR: {
                    float d = boundaryDistance(angle) * 0.985f;
                    segment(0, bx, by, sh, polarX(angle, d), polarY(angle, d), 5 * dp, 950);
                    flash(Color.argb(60, 63, 185, 80));
                    break;
                }
                case CAUGHT: {
                    float fx = fielder == null ? polarX(angle, boundaryDistance(angle) * 0.9f) : fielderX(fielder);
                    float fy = fielder == null ? polarY(angle, boundaryDistance(angle) * 0.9f) : fielderY(fielder);
                    boolean deep = fielder != null && fielder.isDeep();
                    segment(0, bx, by, sh, fx, fy - 6 * dp, height * (deep ? 0.30f : 0.12f), deep ? 1150 : 650);
                    highlightFielder = fielder;
                    flash(Color.argb(70, 255, 77, 94));
                    break;
                }
                case RUN_OUT: {
                    float fx = fielder == null ? polarX(angle, boundaryDistance(angle) * 0.45f) : fielderX(fielder);
                    float fy = fielder == null ? polarY(angle, boundaryDistance(angle) * 0.45f) : fielderY(fielder);
                    segment(0, bx, by, sh, fx, fy, 3 * dp, 650);
                    segment(1, fx, fy, 0, cx, batterCreaseY + 2 * dp, 8 * dp, 380);
                    segmentCount = 2;
                    breakStumpsAfterSegment = 1;
                    highlightFielder = fielder;
                    flash(Color.argb(70, 255, 77, 94));
                    break;
                }
                default: {
                    float depth = type == OutcomeType.DOT ? 0.24f : type == OutcomeType.ONE ? 0.46f
                            : type == OutcomeType.TWO ? 0.78f : 0.88f;
                    float ex;
                    float ey;
                    if (fielder != null) {
                        ex = fielderX(fielder);
                        ey = fielderY(fielder);
                        highlightFielder = fielder;
                    } else {
                        float d = boundaryDistance(angle) * depth;
                        ex = polarX(angle, d);
                        ey = polarY(angle, d);
                    }
                    segment(0, bx, by, sh, ex, ey, type == OutcomeType.DOT ? 1 * dp : 4 * dp,
                            type == OutcomeType.DOT ? 520 : 820);
                    break;
                }
            }
            if (segmentCount == 0) segmentCount = 1;
        }

        long total = 0;
        for (int i = 0; i < segmentCount; i++) total += segDur[i];
        long hold = type == OutcomeType.SIX ? 650 : type.isWicket() ? 800 : 380;
        outcomeTotal = total + hold;
    }

    private void segment(int i, float sx, float sy, float sh, float ex, float ey, float peak, long duration) {
        segSx[i] = sx;
        segSy[i] = sy;
        segSh[i] = sh;
        segEx[i] = ex;
        segEy[i] = ey;
        segPeak[i] = peak;
        segDur[i] = duration;
        if (i == 0) segmentCount = Math.max(segmentCount, 1);
    }

    private void flash(int color) {
        flashColor = color;
        flashStart = now();
    }

    private boolean isFlashing() {
        return flashStart >= 0 && now() - flashStart < 600;
    }

    private void updateOutcome(long t) {
        long elapsed = t - outcomeStart;
        long acc = 0;
        int active = segmentCount - 1;
        float local = 1f;
        for (int i = 0; i < segmentCount; i++) {
            if (elapsed < acc + segDur[i]) {
                active = i;
                local = (elapsed - acc) / (float) segDur[i];
                break;
            }
            acc += segDur[i];
            if (i == breakStumpsAfterSegment && stumpsBrokenAt < 0) stumpsBrokenAt = outcomeStart + acc;
        }
        float e = easeOut(Math.min(1f, Math.max(0f, local)));
        ballX = lerp(segSx[active], segEx[active], e);
        ballY = lerp(segSy[active], segEy[active], e);
        ballH = lerp(segSh[active], 0f, e) + segPeak[active] * 4f * e * (1f - e);
        pushTrail();
        if (elapsed >= outcomeTotal) {
            phase = Phase.IDLE;
            if (listener != null) listener.onBallFinished();
        }
    }

    private void pushTrail() {
        trailX[trailHead] = ballX;
        trailY[trailHead] = ballY - ballH * scaleAt(ballY);
        trailHead = (trailHead + 1) % TRAIL;
        if (trailCount < TRAIL) trailCount++;
    }

    // ------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean acceptingInput = phase == Phase.FLIGHT && !shotReported && pendingInput == null;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!acceptingInput) return true;
                touchActive = true;
                touchDownTime = event.getEventTime();
                touchDownX = event.getX();
                touchDownY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (touchActive && acceptingInput) {
                    touchActive = false;
                    ShotDirection dir = directionFor(event.getX() - touchDownX, event.getY() - touchDownY, touchDownX);
                    pendingInput = ShotInput.swing(touchDownTime - arrivalTime, dir);
                    swingDirection = dir;
                    swingStart = now();
                    if (listener != null) listener.onSwing(dir);
                    performClick();
                }
                touchActive = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                touchActive = false;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Swipe left/right/up picks a direction; a plain tap uses the third of the screen tapped. */
    private ShotDirection directionFor(float dx, float dy, float downX) {
        float threshold = Math.max(touchSlop * 2f, 24 * dp);
        if (Math.abs(dx) > threshold && Math.abs(dx) >= Math.abs(dy)) {
            return dx < 0 ? ShotDirection.LEG : ShotDirection.OFF;
        }
        if (dy < -threshold) return ShotDirection.STRAIGHT;
        if (downX < width / 3f) return ShotDirection.LEG;
        if (downX > width * 2f / 3f) return ShotDirection.OFF;
        return ShotDirection.STRAIGHT;
    }

    // ------------------------------------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------------------------------------

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) return;
        width = w;
        height = h;
        cx = w / 2f;
        fieldCy = h * 0.50f;
        rx = w * 0.485f;
        ry = h * 0.47f;
        bowlerCreaseY = h * 0.30f;
        batterCreaseY = h * 0.70f;
        pitchTopHalfW = Math.min(w * 0.042f, 20 * dp);
        pitchBotHalfW = Math.min(w * 0.066f, 32 * dp);
        keeperY = batterCreaseY + h * 0.085f;
        ballRadius = 4.4f * dp;
        seamPaint.setStrokeWidth(1.1f * dp);
        renderBackground(w, h);
    }

    private float halfWidthAt(float y) {
        return lerp(pitchTopHalfW, pitchBotHalfW, (y - bowlerCreaseY) / (batterCreaseY - bowlerCreaseY));
    }

    /** Perspective scale: things near the bowler look smaller. */
    private float scaleAt(float y) {
        float t = (y - bowlerCreaseY) / (batterCreaseY - bowlerCreaseY);
        return Math.max(0.55f, Math.min(1.15f, lerp(0.68f, 1.0f, t)));
    }

    /** Distance from the batter to the boundary rope along the given shot angle. */
    private float boundaryDistance(double angleDeg) {
        double a = Math.toRadians(angleDeg);
        double dx = Math.sin(a);
        double dy = -Math.cos(a);
        double oy = batterCreaseY - fieldCy;
        double qa = dx * dx / (rx * rx) + dy * dy / (ry * ry);
        double qb = 2 * (oy * dy / (ry * ry));
        double qc = oy * oy / (ry * ry) - 1;
        double t = (-qb + Math.sqrt(qb * qb - 4 * qa * qc)) / (2 * qa);
        return (float) t;
    }

    private float polarX(double angleDeg, float distance) {
        return cx + (float) Math.sin(Math.toRadians(angleDeg)) * distance;
    }

    private float polarY(double angleDeg, float distance) {
        return batterCreaseY - (float) Math.cos(Math.toRadians(angleDeg)) * distance;
    }

    private float fielderX(Fielder f) {
        return polarX(f.getAngleDeg(), boundaryDistance(f.getAngleDeg()) * (float) f.getDepth());
    }

    private float fielderY(Fielder f) {
        return polarY(f.getAngleDeg(), boundaryDistance(f.getAngleDeg()) * (float) f.getDepth());
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    // ------------------------------------------------------------------------------------------
    // Static background (rendered once per size)
    // ------------------------------------------------------------------------------------------

    private void renderBackground(int w, int h) {
        if (background != null) background.recycle();
        background = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(background);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Night sky and stands
        p.setShader(new LinearGradient(0, 0, 0, h, Color.parseColor("#050A1A"), Color.parseColor("#0E1B3D"), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        Random crowd = new Random(2026);
        int[] shirts = {0xFFFFD23F, 0xFF3FB950, 0xFF1E6FD9, 0xFFC8102E, 0xFFFFFFFF, 0xFF7B2CBF};
        for (int i = 0; i < 900; i++) {
            float x = crowd.nextFloat() * w;
            float y = crowd.nextFloat() * h;
            p.setColor(ColorUtils.withAlpha(shirts[crowd.nextInt(shirts.length)], 40 + crowd.nextInt(80)));
            c.drawCircle(x, y, (0.8f + crowd.nextFloat()) * dp, p);
        }

        // Outfield with radial floodlit gradient. The paint still carries the last crowd colour's
        // alpha, which would also fade the shader, so reset it before every gradient fill.
        RectF oval = new RectF(cx - rx, fieldCy - ry, cx + rx, fieldCy + ry);
        p.setColor(Color.BLACK);
        p.setShader(new RadialGradient(cx, fieldCy, Math.max(rx, ry), Color.parseColor("#37A852"),
                Color.parseColor("#14592A"), Shader.TileMode.CLAMP));
        c.drawOval(oval, p);
        p.setShader(null);

        // Mowing stripes clipped to the outfield
        Path clip = new Path();
        clip.addOval(oval, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        p.setColor(Color.argb(22, 255, 255, 255));
        float band = h / 14f;
        for (int i = 0; i < 14; i += 2) {
            c.drawRect(0, i * band, w, (i + 1) * band, p);
        }
        c.restore();

        // Boundary rope and advertising glow
        Paint rope = new Paint(Paint.ANTI_ALIAS_FLAG);
        rope.setStyle(Paint.Style.STROKE);
        rope.setStrokeWidth(2.2f * dp);
        rope.setColor(Color.argb(230, 255, 255, 255));
        c.drawOval(oval, rope);
        rope.setStrokeWidth(5 * dp);
        rope.setColor(Color.argb(40, 255, 210, 63));
        RectF outer = new RectF(oval);
        outer.inset(-4 * dp, -4 * dp);
        c.drawOval(outer, rope);

        // 30-yard circle (dashed)
        float midPitch = (bowlerCreaseY + batterCreaseY) / 2f;
        RectF inner = new RectF(cx - rx * 0.6f, midPitch - ry * 0.52f, cx + rx * 0.6f, midPitch + ry * 0.52f);
        Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
        dash.setStyle(Paint.Style.STROKE);
        dash.setStrokeWidth(1.4f * dp);
        dash.setColor(Color.argb(120, 255, 255, 255));
        dash.setPathEffect(new DashPathEffect(new float[]{6 * dp, 6 * dp}, 0));
        c.drawOval(inner, dash);

        // Pitch strip (trapezoid for perspective)
        float top = bowlerCreaseY - 18 * dp;
        float bottom = batterCreaseY + 20 * dp;
        Path strip = new Path();
        strip.moveTo(cx - halfWidthAt(top) * 1.25f, top);
        strip.lineTo(cx + halfWidthAt(top) * 1.25f, top);
        strip.lineTo(cx + halfWidthAt(bottom) * 1.25f, bottom);
        strip.lineTo(cx - halfWidthAt(bottom) * 1.25f, bottom);
        strip.close();
        p.setColor(Color.BLACK);
        p.setShader(new LinearGradient(0, top, 0, bottom, Color.parseColor("#B89F6A"), Color.parseColor("#D8C38F"), Shader.TileMode.CLAMP));
        c.drawPath(strip, p);
        p.setShader(null);
        // Worn patches on a good length
        p.setColor(Color.argb(40, 120, 90, 40));
        for (int i = 0; i < 40; i++) {
            float y = lerp(bowlerCreaseY, batterCreaseY, 0.55f + crowd.nextFloat() * 0.3f);
            float x = cx + (crowd.nextFloat() * 2 - 1) * halfWidthAt(y) * 0.8f;
            c.drawCircle(x, y, (1 + crowd.nextFloat() * 2) * dp, p);
        }

        // Creases
        Paint crease = new Paint(Paint.ANTI_ALIAS_FLAG);
        crease.setColor(Color.argb(235, 255, 255, 255));
        crease.setStrokeWidth(1.6f * dp);
        drawCreases(c, crease, bowlerCreaseY, -1);
        drawCreases(c, crease, batterCreaseY, 1);

        // Bowler-end stumps (static)
        Paint stump = new Paint(Paint.ANTI_ALIAS_FLAG);
        stump.setColor(Color.parseColor("#F5E6C8"));
        float sw = 1.6f * dp;
        float sh = 11 * dp;
        float sy = bowlerCreaseY - 6 * dp;
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * 3.4f * dp;
            c.drawRect(x - sw / 2, sy - sh, x + sw / 2, sy, stump);
        }

        // Floodlight pools
        float glow = Math.max(w, h) * 0.55f;
        drawGlow(c, 0, 0, glow);
        drawGlow(c, w, 0, glow);
        drawGlow(c, 0, h, glow * 0.8f);
        drawGlow(c, w, h, glow * 0.8f);

        // Floodlight towers
        Paint lamp = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawTower(c, lamp, 14 * dp, 12 * dp);
        drawTower(c, lamp, w - 14 * dp, 12 * dp);

        // Vignette
        p.setColor(Color.BLACK);
        p.setShader(new RadialGradient(cx, h / 2f, Math.max(w, h) * 0.75f, Color.TRANSPARENT,
                Color.argb(140, 2, 6, 18), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private void drawCreases(Canvas c, Paint crease, float y, int dir) {
        float hw = halfWidthAt(y) * 1.25f;
        // Popping crease
        c.drawLine(cx - hw * 1.3f, y, cx + hw * 1.3f, y, crease);
        // Bowling crease (behind the popping crease)
        float by = y + dir * 7 * dp;
        c.drawLine(cx - hw, by, cx + hw, by, crease);
        // Return creases
        c.drawLine(cx - hw, y - 4 * dp, cx - hw, by + dir * 4 * dp, crease);
        c.drawLine(cx + hw, y - 4 * dp, cx + hw, by + dir * 4 * dp, crease);
    }

    private void drawGlow(Canvas c, float x, float y, float radius) {
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
        g.setShader(new RadialGradient(x, y, radius, Color.argb(70, 255, 236, 170), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, radius, g);
    }

    private void drawTower(Canvas c, Paint p, float x, float y) {
        p.setColor(Color.parseColor("#2A3556"));
        c.drawRect(x - 7 * dp, y - 4 * dp, x + 7 * dp, y + 6 * dp, p);
        p.setColor(Color.parseColor("#FFF4C2"));
        for (int i = 0; i < 3; i++) {
            c.drawCircle(x - 4.5f * dp + i * 4.5f * dp, y + 1 * dp, 1.6f * dp, p);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Per-frame drawing
    // ------------------------------------------------------------------------------------------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (background == null) return;
        canvas.drawBitmap(background, 0, 0, bitmapPaint);
        long t = now();
        drawFielders(canvas, t);
        drawBowler(canvas, t);
        drawApproachRing(canvas, t);
        drawStumps(canvas, t);
        drawBatter(canvas, t);
        drawBall(canvas);
        drawFlash(canvas, t);
    }

    private void drawFielders(Canvas canvas, long t) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(t / 120.0));
        List<Fielder> fielders = field.getFielders();
        for (int i = 0, n = fielders.size(); i < n; i++) {
            Fielder f = fielders.get(i);
            float x = fielderX(f);
            float y = fielderY(f);
            drawPlayer(canvas, x, y, 5.6f * dp * scaleAt(y), bowlingColor);
            if (f == highlightFielder && phase == Phase.OUTCOME) {
                ringPaint.setStrokeWidth(2 * dp);
                ringPaint.setColor(Color.argb((int) (120 + 120 * pulse), 255, 210, 63));
                canvas.drawCircle(x, y, (10 + 4 * pulse) * dp, ringPaint);
            }
        }
        // Wicket-keeper
        drawPlayer(canvas, cx + 2 * dp, keeperY, 6.2f * dp, bowlingColor);
        if (highlightKeeper && phase == Phase.OUTCOME) {
            ringPaint.setStrokeWidth(2 * dp);
            ringPaint.setColor(Color.argb((int) (120 + 120 * pulse), 255, 210, 63));
            canvas.drawCircle(cx + 2 * dp, keeperY, (11 + 4 * pulse) * dp, ringPaint);
        }
    }

    private void drawPlayer(Canvas canvas, float x, float y, float r, int color) {
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        rect.set(x - r * 1.1f, y + r * 0.3f, x + r * 1.3f, y + r * 1.2f);
        canvas.drawOval(rect, shadowPaint);
        fill.setColor(color);
        canvas.drawCircle(x, y, r, fill);
        stroke.setStrokeWidth(1.4f * dp);
        stroke.setColor(Color.WHITE);
        canvas.drawCircle(x, y, r, stroke);
    }

    private void drawBowler(Canvas canvas, long t) {
        float startY = bowlerCreaseY - height * 0.16f;
        float y;
        float armAngle;
        if (phase == Phase.WAITING || phase == Phase.IDLE) {
            y = phase == Phase.IDLE ? bowlerCreaseY - 10 * dp : startY;
            armAngle = 200;
        } else if (phase == Phase.RUN_UP) {
            float q = Math.min(1f, (t - phaseStart) / (float) runUpMs);
            y = lerp(startY, bowlerCreaseY - 4 * dp, q * q * (3 - 2 * q));
            armAngle = q > 0.75f ? lerp(200, 340, (q - 0.75f) / 0.25f) : 200;
        } else {
            float q = Math.min(1f, (t - releaseTime) / 400f);
            y = lerp(bowlerCreaseY - 4 * dp, bowlerCreaseY + 10 * dp, easeOut(q));
            armAngle = lerp(340, 470, Math.min(1f, q * 2));
        }
        float x = cx + halfWidthAt(bowlerCreaseY) * 0.55f;
        float r = 6.6f * dp * scaleAt(y);
        // Bowling arm
        stroke.setColor(bowlingColor);
        stroke.setStrokeWidth(2.4f * dp);
        double a = Math.toRadians(armAngle);
        canvas.drawLine(x + r * 0.6f, y, x + r * 0.6f + (float) Math.cos(a) * r * 1.6f,
                y + (float) Math.sin(a) * r * 1.6f, stroke);
        drawPlayer(canvas, x, y, r, bowlingColor);
        fill.setColor(NAVY);
        canvas.drawCircle(x, y - r * 0.15f, r * 0.45f, fill);
    }

    private void drawApproachRing(Canvas canvas, long t) {
        if (phase != Phase.FLIGHT || delivery == null || shotReported) return;
        float travel = delivery.getTravelMs();
        float remaining = (arrivalTime - t) / travel;
        if (remaining < -0.2f) return;
        float zoneX = cx + (delivery.getLine().getOffset() + delivery.getDeviation()) * halfWidthAt(batterCreaseY);
        float base = 9 * dp;
        float closing = Math.max(0f, remaining);
        float radius = base + closing * 46 * dp;
        boolean inWindow = Math.abs(t - arrivalTime) <= perfectWindow;
        ringPaint.setStrokeWidth(2.4f * dp);
        ringPaint.setColor(inWindow ? GRASS
                : Color.argb((int) (90 + 140 * (1 - Math.min(1f, closing))), 255, 210, 63));
        canvas.drawCircle(zoneX, batterCreaseY, radius, ringPaint);
        ringPaint.setStrokeWidth(1.2f * dp);
        ringPaint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(zoneX, batterCreaseY, base, ringPaint);
    }

    private void drawStumps(Canvas canvas, long t) {
        float sw = 2.2f * dp;
        float sh = 17 * dp;
        float baseY = batterCreaseY + 5 * dp;
        float broken = stumpsBrokenAt >= 0 ? Math.min(1f, (t - stumpsBrokenAt) / (float) STUMPS_FLY_MS) : 0f;
        float e = easeOut(broken);
        fill.setColor(STUMP);
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * 5 * dp;
            canvas.save();
            canvas.translate(x + i * 10 * dp * e, baseY - 6 * dp * e);
            canvas.rotate(i * 38 * e + (i == 0 ? -18 * e : 0));
            canvas.drawRect(-sw / 2, -sh, sw / 2, 0, fill);
            canvas.restore();
        }
        // Bails
        fill.setColor(FLOODLIGHT);
        if (broken <= 0f) {
            canvas.drawRect(cx - 7 * dp, baseY - sh - 1.6f * dp, cx + 7 * dp, baseY - sh, fill);
        } else {
            for (int i = -1; i <= 1; i += 2) {
                float bx = cx + i * (4 * dp + 26 * dp * e);
                float by = baseY - sh - 1 * dp - 34 * dp * (float) Math.sin(e * Math.PI) - 8 * dp * e;
                canvas.save();
                canvas.translate(bx, by);
                canvas.rotate(i * 540 * e);
                canvas.drawRect(-3.5f * dp, -0.8f * dp, 3.5f * dp, 0.8f * dp, fill);
                canvas.restore();
            }
        }
    }

    private void drawBatter(Canvas canvas, long t) {
        float x = cx - 13 * dp;
        float y = batterCreaseY + 3 * dp;
        float r = 8.4f * dp;
        // Bat: pivot at the hands, swings from behind the batter towards the chosen side.
        float angle = 150f;
        if (swingStart >= 0) {
            float q = Math.min(1f, (t - swingStart) / (float) SWING_MS);
            float target = swingDirection == ShotDirection.LEG ? -60f : swingDirection == ShotDirection.OFF ? 20f : -15f;
            angle = q < 1f ? lerp(150f, target, easeOut(q)) : target;
        }
        canvas.save();
        canvas.translate(x + r * 0.7f, y - r * 0.2f);
        canvas.rotate(angle);
        fill.setColor(WILLOW);
        rect.set(-2.2f * dp, 0, 2.2f * dp, 20 * dp);
        canvas.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, fill);
        fill.setColor(GRIP);
        rect.set(-1.6f * dp, -1 * dp, 1.6f * dp, 6 * dp);
        canvas.drawRoundRect(rect, 1 * dp, 1 * dp, fill);
        canvas.restore();
        drawPlayer(canvas, x, y, r, battingColor);
        // Helmet
        fill.setColor(NAVY);
        canvas.drawCircle(x, y - r * 0.1f, r * 0.5f, fill);
        stroke.setColor(FLOODLIGHT);
        stroke.setStrokeWidth(1.2f * dp);
        canvas.drawLine(x - r * 0.35f, y - r * 0.1f, x + r * 0.35f, y - r * 0.1f, stroke);
    }

    private void drawBall(Canvas canvas) {
        if (!ballVisible || phase == Phase.WAITING || phase == Phase.RUN_UP) return;
        float s = scaleAt(ballY);
        float liftScale = 1f + Math.min(1.6f, ballH / (height * 0.25f));
        float r = ballRadius * s * liftScale;
        float screenY = ballY - ballH * s;

        // Trail
        for (int i = 0; i < trailCount; i++) {
            int idx = (trailHead - 1 - i + TRAIL * 2) % TRAIL;
            float alpha = (1f - i / (float) TRAIL) * 0.35f;
            fill.setColor(Color.argb((int) (alpha * 255), 255, 255, 255));
            canvas.drawCircle(trailX[idx], trailY[idx], r * (1f - i / (float) (TRAIL + 2)), fill);
        }
        // Shadow on the ground
        float shadowR = ballRadius * s * (1f + ballH / (60 * dp));
        shadowPaint.setColor(Color.argb((int) Math.max(30, 110 - ballH / dp * 1.5f), 0, 0, 0));
        rect.set(ballX - shadowR, ballY - shadowR * 0.45f, ballX + shadowR, ballY + shadowR * 0.45f);
        canvas.drawOval(rect, shadowPaint);
        // White ball with a seam (white ball for a floodlit game)
        canvas.drawCircle(ballX, screenY, r, ballPaint);
        rect.set(ballX - r * 0.7f, screenY - r * 0.7f, ballX + r * 0.7f, screenY + r * 0.7f);
        canvas.drawArc(rect, 200, 140, false, seamPaint);
    }

    private void drawFlash(Canvas canvas, long t) {
        if (flashStart < 0) return;
        float q = (t - flashStart) / 600f;
        if (q >= 1f) {
            flashStart = -1L;
            return;
        }
        int alpha = (int) (Color.alpha(flashColor) * (1f - q));
        flashPaint.setColor(ColorUtils.withAlpha(flashColor, alpha));
        canvas.drawRect(0, 0, width, height, flashPaint);
    }
}
