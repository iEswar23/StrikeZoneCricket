package io.github.ieswar23.strikezone.screenshots;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import com.github.takahirom.roborazzi.RoborazziKt;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.github.takahirom.roborazzi.RoborazziOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowChoreographer;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import dagger.hilt.components.SingletonComponent;
import io.github.ieswar23.strikezone.MainActivity;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.dao.AchievementDao;
import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.di.RandomModule;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.MatchRules;
import io.github.ieswar23.strikezone.engine.OutcomeType;
import io.github.ieswar23.strikezone.engine.ShotDirection;
import io.github.ieswar23.strikezone.engine.ShotResolver;
import io.github.ieswar23.strikezone.engine.Timing;
import io.github.ieswar23.strikezone.ui.game.DeliveryCommand;
import io.github.ieswar23.strikezone.ui.game.GameFragment;
import io.github.ieswar23.strikezone.ui.game.GameUiState;
import io.github.ieswar23.strikezone.ui.game.GameViewModel;
import io.github.ieswar23.strikezone.ui.game.GameViewModelTestAccess;
import io.github.ieswar23.strikezone.ui.game.PitchView;

/**
 * Renders the real app (Hilt activity, Navigation, Room, mock API) with Robolectric native graphics
 * and records the README screenshots into {@code docs/screenshots/} with Roborazzi.
 *
 * <p>Excluded from the regular {@code testDebugUnitTest} run; regenerate the images with
 * {@code ./gradlew recordRoborazziDebug} (or {@code ./gradlew testDebugUnitTest -PrecordScreenshots=true}).
 * The game RNG is seeded and the innings is scripted, so the shots are deterministic.
 */
@HiltAndroidTest
@UninstallModules(RandomModule.class)
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication.class, sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class AppScreenshotTest {

    /** Same seed for every injection so deliveries, targets and field plans repeat run to run. */
    @Module
    @InstallIn(SingletonComponent.class)
    public static final class SeededRandomModule {
        @Provides
        static Random provideRandom() {
            return new Random(2026L);
        }
    }

    private static final long WAIT_TIMEOUT_MS = 20_000L;
    /** 1233 px (xxhdpi phone) scaled down to ~540 px wide README images. */
    private static final double RESIZE_SCALE = 0.44;

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    CricketRepository cricketRepository;
    @Inject
    MatchRepository matchRepository;
    @Inject
    AchievementDao achievementDao;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private ActivityScenario<MainActivity> scenario;
    private MainActivity activity;

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        background.shutdownNow();
    }

    // ------------------------------------------------------------------------------------------
    // Screens
    // ------------------------------------------------------------------------------------------

    @Test
    public void home() {
        launch();
        seedHistory();
        waitUntil(() -> activity.findViewById(R.id.challengeBody).getVisibility() == View.VISIBLE);
        waitUntil(() -> !"0".contentEquals(((TextView) activity.findViewById(R.id.quickPlayed)).getText()));
        settle(2_000);
        capture("01_home");
    }

    @Test
    public void matchSetup() {
        launch();
        Bundle args = new Bundle();
        args.putString("mode", GameMode.QUICK_CHASE.name());
        navigate(R.id.setupFragment, args);
        waitUntil(() -> {
            View list = activity.findViewById(R.id.teamList);
            return list != null && list.getVisibility() == View.VISIBLE
                    && ((RecyclerView) list).getChildCount() > 0;
        });
        settle(1_200);
        capture("02_match_setup");
    }

    /** A scripted 5-over chase of 58 against London Lions, frozen mid-delivery and then on a SIX. */
    @Test
    public void gameInProgressAndSix() {
        launch();
        MatchSetup setup = new MatchSetup("mum", "lon", 5, 58, Difficulty.MEDIUM, GameMode.QUICK_CHASE, null, null);
        navigate(R.id.gameFragment, setup.toBundle());
        GameViewModel viewModel = gameViewModel();
        waitUntil(() -> {
            GameUiState s = viewModel.getState().getValue();
            return s != null && !s.loading && viewModel.getDeliveries().getValue() != null;
        });

        int index = 0;
        for (BallOutcome ball : scriptedInnings()) {
            GameUiState s = viewModel.getState().getValue();
            assertWithMessage("ball %s, state %s/%s %s", index++, s.runs, s.wickets, s.oversText)
                    .that(GameViewModelTestAccess.playResolved(viewModel, ball)).isNotNull();
            viewModel.onBallAnimationDone();
        }
        // 45/2 after 4.1 overs: 13 needed from 5 balls. Hold the pitch while the last popup and
        // the end-of-over banner fade out, then release it and freeze the frame mid-flight.
        PitchView pitch = activity.findViewById(R.id.pitch);
        pitch.pause();
        settle(3_000);
        pitch.resume();
        DeliveryCommand next = viewModel.getDeliveries().getValue().peek();
        long releaseAt = next.startDelayMs + next.delivery.getBowlerType().getRunUpMs();
        settle(releaseAt + Math.round(next.delivery.getTravelMs() * 0.55));
        capture("03_game_chase");

        GameViewModelTestAccess.playResolved(viewModel, ball(OutcomeType.SIX, Timing.PERFECT,
                ShotDirection.LEG, -38, "Launched over mid-wicket into the stands!"));
        settle(450);
        capture("04_game_six");
    }

    @Test
    public void matchSummary() {
        launch();
        long matchId = seedHistory();
        Bundle args = new Bundle();
        args.putLong("matchId", matchId);
        navigate(R.id.summaryFragment, args);
        waitUntil(() -> {
            TextView score = activity.findViewById(R.id.finalScore);
            return score != null && score.getText().length() > 0;
        });
        settle(1_500);
        capture("05_match_summary");
    }

    @Test
    public void stats() {
        launch();
        seedHistory();
        navigate(R.id.statsFragment, null);
        waitUntil(() -> {
            View content = activity.findViewById(R.id.statsContent);
            View best = activity.findViewById(R.id.bestList);
            return content != null && content.getVisibility() == View.VISIBLE
                    && best instanceof ViewGroup && ((ViewGroup) best).getChildCount() > 0;
        });
        settle(2_000);
        // Robolectric does not tick the indicator's spring animation; snap it to its final value.
        CircularProgressIndicator ring = activity.findViewById(R.id.winRing);
        ring.setProgressCompat(ring.getProgress(), false);
        settle(100);
        capture("06_stats");
    }

    @Test
    @Config(qualifiers = "+night")
    public void historyDark() {
        launch();
        seedHistory();
        navigate(R.id.historyFragment, null);
        waitUntil(() -> {
            RecyclerView list = activity.findViewById(R.id.list);
            return list != null && list.getChildCount() > 0;
        });
        settle(1_500);
        capture("07_history_dark");
    }

    @Test
    public void achievements() {
        launch();
        seedBadges(seedHistory());
        navigate(R.id.achievementsFragment, null);
        waitUntil(() -> {
            RecyclerView grid = activity.findViewById(R.id.badgeGrid);
            return grid != null && grid.getChildCount() > 4;
        });
        settle(1_500);
        // Robolectric does not tick the indicator's spring animation; snap it to its final value.
        CircularProgressIndicator ring = activity.findViewById(R.id.unlockRing);
        ring.setProgressCompat(ring.getProgress(), false);
        settle(100);
        capture("08_achievements");
    }

    @Test
    public void matchSummaryWithNewBadges() {
        launch();
        long matchId = seedHistory();
        seedBadges(matchId);
        Bundle args = new Bundle();
        args.putLong("matchId", matchId);
        navigate(R.id.summaryFragment, args);
        waitUntil(() -> {
            View banner = activity.findViewById(R.id.badgeBanner);
            TextView score = activity.findViewById(R.id.finalScore);
            return banner != null && banner.getVisibility() == View.VISIBLE
                    && score != null && score.getText().length() > 0;
        });
        settle(2_000);
        capture("09_achievement_unlocked");
    }

    // ------------------------------------------------------------------------------------------
    // Scripted data
    // ------------------------------------------------------------------------------------------

    private static List<BallOutcome> scriptedInnings() {
        List<BallOutcome> balls = new ArrayList<>();
        // Over 1: 14
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.OFF, 35, "Worked through the off side for a single"));
        balls.add(ball(OutcomeType.FOUR, Timing.PERFECT, ShotDirection.OFF, 42, "Timed to perfection, driven through the covers"));
        balls.add(ball(OutcomeType.DOT, Timing.EARLY, ShotDirection.STRAIGHT, 4, "Defended, no run"));
        balls.add(ball(OutcomeType.TWO, Timing.EARLY, ShotDirection.LEG, -40, "Into the gap through mid-wicket, two runs"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.STRAIGHT, 8, "Worked down the ground for a single"));
        balls.add(ball(OutcomeType.SIX, Timing.PERFECT, ShotDirection.STRAIGHT, -4, "Straight back over the bowler's head!"));
        // Over 2: 8, one wicket
        balls.add(ball(OutcomeType.DOT, Timing.MISSED, ShotDirection.OFF, 30, "Swing and a miss, way too early"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.LEG, -30, "Worked through mid-wicket for a single"));
        balls.add(ball(OutcomeType.CAUGHT, Timing.LATE, ShotDirection.STRAIGHT, -6, "Caught at long-on"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.OFF, 50, "Pushed towards point for a quick single"));
        balls.add(ball(OutcomeType.FOUR, Timing.PERFECT, ShotDirection.LEG, -55, "Timed to perfection, whipped through mid-wicket"));
        balls.add(ball(OutcomeType.TWO, Timing.EARLY, ShotDirection.OFF, 38, "Into the gap through the off side, two runs"));
        // Over 3: 10 (including a wide)
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.STRAIGHT, 6, "Worked down the ground for a single"));
        balls.add(ball(OutcomeType.WIDE, Timing.LEFT_ALONE, ShotDirection.OFF, 180, "Wide ball, sensibly left alone"));
        balls.add(ball(OutcomeType.FOUR, Timing.PERFECT, ShotDirection.OFF, 30, "Timed to perfection, driven through the covers"));
        balls.add(ball(OutcomeType.DOT, Timing.EARLY, ShotDirection.STRAIGHT, 2, "Defended, no run"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.LEG, -25, "Worked through mid-wicket for a single"));
        balls.add(ball(OutcomeType.TWO, Timing.EARLY, ShotDirection.STRAIGHT, -10, "Into the gap down the ground, two runs"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.OFF, 45, "Worked through the off side for a single"));
        // Over 4: 9, one wicket
        balls.add(ball(OutcomeType.SIX, Timing.PERFECT, ShotDirection.LEG, -42, "Launched over mid-wicket into the stands!"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.OFF, 40, "Worked through the off side for a single"));
        balls.add(ball(OutcomeType.BOWLED, Timing.MISSED, ShotDirection.STRAIGHT, 0, "Bowled! Through the gate, too early"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.LEG, -30, "Worked through mid-wicket for a single"));
        balls.add(ball(OutcomeType.DOT, Timing.EARLY, ShotDirection.STRAIGHT, 3, "Defended, no run"));
        balls.add(ball(OutcomeType.ONE, Timing.EARLY, ShotDirection.STRAIGHT, 10, "Worked down the ground for a single"));
        // Over 5, ball 1
        balls.add(ball(OutcomeType.FOUR, Timing.PERFECT, ShotDirection.OFF, 48, "Timed to perfection, driven through the covers"));
        return balls;
    }

    private static BallOutcome ball(OutcomeType type, Timing timing, ShotDirection direction, double angle,
                                    String description) {
        return new BallOutcome(type, timing, direction, angle, null, ShotResolver.headlineFor(type), description);
    }

    /** Inserts a realistic match history and returns the id of the featured (won) chase. */
    private long seedHistory() {
        List<MatchEntity> matches = new ArrayList<>();
        matches.add(shots(match(day(29, 20, 41), "mum", "Mumbai Mavericks", "MUM", "#1E6FD9", "lon", "London Lions", "LON", "#C8102E",
                5, 58, "MEDIUM", 59, 3, 28, 5, 3, 7, 2, true, false, "9:0,14:1,11:0,13:1,12:1", null), 10, 2, 1));
        matches.add(shots(match(day(28, 21, 5), "mum", "Mumbai Mavericks", "MUM", "#1E6FD9", "syd", "Sydney Strikers", "SYD", "#00A6B4",
                2, 31, "HARD", 24, 3, 11, 2, 1, 3, 1, false, false, "13:1,11:2", null), 3, 1, 0));
        matches.add(shots(match(day(27, 19, 32), "mum", "Mumbai Mavericks", "MUM", "#1E6FD9", "col", "Colombo Cyclones", "COL", "#0F4C81",
                2, 24, "MEDIUM", 25, 1, 10, 2, 1, 2, 1, true, false, "14:0,11:1", "dc-002"), 5, 1, 0));
        matches.add(shots(match(day(26, 22, 14), "che", "Chennai Chargers", "CHE", "#F2B705", "kar", "Karachi Kestrels", "KAR", "#1F8A4C",
                1, 16, "EASY", 17, 0, 5, 1, 2, 1, 0, true, false, "17:0", null), 4, 2, 0));
        matches.add(shots(match(day(25, 20, 3), "lon", "London Lions", "LON", "#C8102E", "mum", "Mumbai Mavericks", "MUM", "#1E6FD9",
                2, 28, "MEDIUM", 27, 2, 12, 3, 1, 3, 1, false, true, "15:1,12:1", null), 5, 1, 0));
        matches.add(shots(match(day(24, 18, 47), "syd", "Sydney Strikers", "SYD", "#00A6B4", "che", "Chennai Chargers", "CHE", "#F2B705",
                5, 62, "MEDIUM", 66, 4, 29, 6, 3, 8, 2, true, false, "12:1,9:0,15:1,16:1,14:1", null), 8, 2, 0));
        matches.add(shots(match(day(23, 21, 22), "mum", "Mumbai Mavericks", "MUM", "#1E6FD9", "kar", "Karachi Kestrels", "KAR", "#1F8A4C",
                5, 70, "HARD", 61, 5, 27, 5, 2, 9, 3, false, false, "10:1,16:0,12:2,15:1,8:1", null), 7, 1, 0));

        long[] featured = {-1};
        AtomicInteger saved = new AtomicInteger();
        for (int i = matches.size() - 1; i >= 0; i--) {
            MatchEntity m = matches.get(i);
            boolean isFeatured = i == 0;
            matchRepository.save(m, id -> {
                if (isFeatured) featured[0] = id;
                saved.incrementAndGet();
            });
            // Insert one at a time so ids follow the play order.
            int expected = matches.size() - i;
            waitUntil(() -> saved.get() == expected);
        }
        return featured[0];
    }

    /** Shot facts the achievements are judged on: perfect shots, most sixes in an over, scoring overs. */
    private static MatchEntity shots(MatchEntity m, int perfectShots, int maxSixesInOver, int scoringOvers) {
        m.perfectShots = perfectShots;
        m.maxSixesInOver = maxSixesInOver;
        m.scoringOvers = scoringOvers;
        return m;
    }

    /**
     * Stores the badges the seeded history has earned, dated by the match that earned each one, so
     * the achievements screen shows a realistic 5 / 12. The featured chase (the last one played)
     * unlocked two of them. Ids follow the play order, so earlier matches are {@code featuredId - n}.
     */
    private void seedBadges(long featuredId) {
        List<AchievementEntity> rows = new ArrayList<>();
        rows.add(new AchievementEntity(Achievement.FIRST_FIFTY.name(), day(23, 21, 22), featuredId - 6));
        rows.add(new AchievementEntity(Achievement.FIRST_WIN.name(), day(24, 18, 47), featuredId - 5));
        rows.add(new AchievementEntity(Achievement.FLAWLESS_CHASE.name(), day(26, 22, 14), featuredId - 3));
        rows.add(new AchievementEntity(Achievement.PERFECT_TEN.name(), day(29, 20, 41), featuredId));
        rows.add(new AchievementEntity(Achievement.DOT_FREE_OVER.name(), day(29, 20, 41), featuredId));
        try {
            background.submit(() -> achievementDao.insertAll(rows)).get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static MatchEntity match(long playedAt, String batId, String batName, String batShort, String batColor,
                                     String bowlId, String bowlName, String bowlShort, String bowlColor,
                                     int overs, int target, String difficulty, int runs, int wickets, int balls,
                                     int fours, int sixes, int dots, int extras, boolean won, boolean tied,
                                     String overSummary, String challengeId) {
        MatchEntity e = new MatchEntity();
        e.playedAt = playedAt;
        e.mode = challengeId == null ? GameMode.QUICK_CHASE.name() : GameMode.DAILY_CHALLENGE.name();
        e.challengeId = challengeId;
        e.bowlerFocus = challengeId == null ? null : "SPIN";
        e.battingTeamId = batId;
        e.battingTeamName = batName;
        e.battingShort = batShort;
        e.battingColor = batColor;
        e.bowlingTeamId = bowlId;
        e.bowlingTeamName = bowlName;
        e.bowlingShort = bowlShort;
        e.bowlingColor = bowlColor;
        e.overs = overs;
        e.wicketsLimit = MatchRules.wicketsForOvers(overs);
        e.target = target;
        e.difficulty = difficulty;
        e.runs = runs;
        e.wickets = wickets;
        e.balls = balls;
        e.fours = fours;
        e.sixes = sixes;
        e.dots = dots;
        e.extras = extras;
        e.won = won;
        e.tied = tied;
        int ballsLeft = overs * 6 - balls;
        if (won) {
            int inHand = Math.max(1, e.wicketsLimit - wickets);
            e.resultText = batName + " won by " + inHand + (inHand == 1 ? " wicket" : " wickets")
                    + (ballsLeft > 0 ? " (" + ballsLeft + (ballsLeft == 1 ? " ball" : " balls") + " to spare)" : "");
        } else if (tied) {
            e.resultText = "Match tied! Scores level on " + runs;
        } else {
            int margin = Math.max(1, target - 1 - runs);
            e.resultText = bowlName + " won by " + margin + (margin == 1 ? " run" : " runs");
        }
        e.overSummary = overSummary;
        return e;
    }

    private static long day(int dayOfMonth, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, dayOfMonth, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ------------------------------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------------------------------

    private void launch() {
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(a -> activity = a);
        // From here on frames (PitchView's game loop, animators, layout passes) only advance when
        // the test moves the virtual clock, so a delivery can be frozen at an exact point of its flight.
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
        ShadowChoreographer.setPaused(true);
        // The repository syncs teams and challenges from the mock API (300–700 ms) into Room.
        waitUntil(() -> teamCached("mum") && teamCached("lon"));
        settle(500);
    }

    private boolean teamCached(String id) {
        try {
            return background.submit(() -> cricketRepository.getTeamSync(id) != null).get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private NavController navController() {
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.navHost);
        assertThat(host).isNotNull();
        return host.getNavController();
    }

    private void navigate(int destination, Bundle args) {
        navController().navigate(destination, args);
        settle(800);
    }

    private GameViewModel gameViewModel() {
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.navHost);
        Fragment current = host.getChildFragmentManager().getPrimaryNavigationFragment();
        assertThat(current).isInstanceOf(GameFragment.class);
        return new ViewModelProvider(current).get(GameViewModel.class);
    }

    /** Runs the main looper (and lets real background threads progress) until the condition holds. */
    private void waitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("Timed out waiting for UI state");
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** Advances the (virtual) UI clock so animations reach their resting state. */
    private static void settle(long millis) {
        long step = 16;
        for (long t = 0; t < millis; t += step) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(Math.min(step, millis - t)));
        }
    }

    private void capture(String name) {
        File dir = new File(projectRoot(), "docs/screenshots");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        RoborazziOptions defaults = new RoborazziOptions();
        RoborazziOptions.RecordOptions record = defaults.getRecordOptions();
        RoborazziOptions options = new RoborazziOptions(defaults.getTaskType(), defaults.getContextData(),
                defaults.getCaptureType(), defaults.getReportOptions(), defaults.getCompareOptions(),
                new RoborazziOptions.RecordOptions(RESIZE_SCALE, record.getApplyDeviceCrop(),
                        record.getPixelBitConfig(), record.getImageIoFormat()));
        // Whole screen (every window), same as the device would show it.
        RoborazziKt.captureScreenRoboImage(new File(dir, name + ".png"), options);
    }

    private static File projectRoot() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null && !new File(dir, "settings.gradle.kts").exists()) dir = dir.getParentFile();
        if (dir == null) throw new IllegalStateException("Project root not found");
        return dir;
    }
}
