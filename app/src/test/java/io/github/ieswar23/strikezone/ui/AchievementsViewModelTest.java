package io.github.ieswar23.strikezone.ui;

import static com.google.common.truth.Truth.assertThat;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.AchievementEvaluator;
import io.github.ieswar23.strikezone.domain.achievements.Badge;
import io.github.ieswar23.strikezone.domain.achievements.MatchRecord;
import io.github.ieswar23.strikezone.ui.FakeRepositories.FakeAchievementRepository;
import io.github.ieswar23.strikezone.ui.FakeRepositories.FakeMatchRepository;
import io.github.ieswar23.strikezone.ui.achievements.AchievementsUiState;
import io.github.ieswar23.strikezone.ui.achievements.AchievementsViewModel;
import io.github.ieswar23.strikezone.ui.achievements.BadgeIcons;
import io.github.ieswar23.strikezone.ui.summary.SummaryViewModel;

public class AchievementsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

    private FakeAchievementRepository repository;
    private final AchievementEvaluator evaluator = new AchievementEvaluator(TimeZone.getTimeZone("UTC"));

    @Before
    public void setUp() {
        repository = new FakeAchievementRepository();
    }

    private AchievementsUiState observe(AchievementsViewModel vm) {
        List<AchievementsUiState> states = new ArrayList<>();
        vm.getState().observeForever(states::add);
        return states.isEmpty() ? null : states.get(states.size() - 1);
    }

    private List<Badge> badges(List<MatchRecord> history, Map<Achievement, Long> unlocks) {
        return AchievementEvaluator.badges(evaluator.evaluate(history), unlocks);
    }

    @Test
    public void openingTheScreenBackfillsUnlocksFromOlderHistory() {
        new AchievementsViewModel(repository);
        assertThat(repository.checkedMatchIds).containsExactly(0L);
    }

    @Test
    public void staysLoadingUntilBadgesArrive() {
        AchievementsViewModel vm = new AchievementsViewModel(repository);
        repository.badges.setValue(null);
        AchievementsUiState state = observe(vm);
        assertThat(state.loading).isTrue();
        assertThat(state.badges).isEmpty();
    }

    @Test
    public void headerCountsUnlockedBadgesAndPicksTheClosestLockedOne() {
        Map<Achievement, Long> unlocks = new EnumMap<>(Achievement.class);
        unlocks.put(Achievement.FIRST_WIN, 1L);
        unlocks.put(Achievement.FIRST_FIFTY, 2L);
        List<MatchRecord> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            history.add(MatchRecord.builder().won(true).overs(2).balls(10).runs(25).wickets(1).sixes(2).fours(3).build());
        }
        repository.badges.setValue(badges(history, unlocks));

        AchievementsUiState state = observe(new AchievementsViewModel(repository));

        assertThat(state.loading).isFalse();
        assertThat(state.total).isEqualTo(12);
        assertThat(state.unlocked).isEqualTo(2);
        assertThat(state.unlockedPercent()).isEqualTo(16);
        // Chase Master (4/5 = 80%) is ahead of Six Machine (0/3), Big Hitter (8/50) and the rest.
        assertThat(state.nextUp.getAchievement()).isEqualTo(Achievement.CHASE_MASTER);
        assertThat(state.nextUp.getProgress().getProgressText()).isEqualTo("4/5");
        assertThat(state.badges.get(0).getAchievement()).isEqualTo(Achievement.FIRST_WIN);
    }

    @Test
    public void everythingUnlockedHasNoNextUp() {
        Map<Achievement, Long> unlocks = new EnumMap<>(Achievement.class);
        for (Achievement a : Achievement.values()) unlocks.put(a, 10L);
        repository.badges.setValue(badges(Collections.emptyList(), unlocks));

        AchievementsUiState state = observe(new AchievementsViewModel(repository));

        assertThat(state.unlocked).isEqualTo(12);
        assertThat(state.unlockedPercent()).isEqualTo(100);
        assertThat(state.nextUp).isNull();
    }

    @Test
    public void summaryShowsTheBadgesItsMatchUnlocked() {
        FakeMatchRepository matches = new FakeMatchRepository();
        MatchEntity m = new MatchEntity();
        matches.save(m, id -> { });
        repository.unlockedByMatch.setValue(Arrays.asList(Achievement.FIRST_WIN, Achievement.SIX_MACHINE));
        Map<String, Object> args = new HashMap<>();
        args.put(SummaryViewModel.ARG_MATCH_ID, m.id);

        SummaryViewModel vm = new SummaryViewModel(new SavedStateHandle(args), matches, repository);

        assertThat(repository.observedMatchId).isEqualTo(m.id);
        assertThat(vm.getNewBadges().getValue()).containsExactly(Achievement.FIRST_WIN, Achievement.SIX_MACHINE).inOrder();
    }

    @Test
    public void everyBadgeHasItsOwnIcon() {
        Set<Integer> icons = new HashSet<>();
        for (Achievement a : Achievement.values()) {
            int icon = BadgeIcons.iconFor(a);
            assertThat(icon).isNotEqualTo(0);
            icons.add(icon);
        }
        assertThat(icons).hasSize(Achievement.values().length);
    }
}
