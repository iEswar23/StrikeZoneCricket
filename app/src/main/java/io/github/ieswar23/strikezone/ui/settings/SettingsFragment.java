package io.github.ieswar23.strikezone.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.BuildConfig;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.repository.AchievementRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.data.settings.AppSettings;
import io.github.ieswar23.strikezone.databinding.FragmentSettingsBinding;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.util.InsetsUtil;

/** Default difficulty and overs, vibration, theme, and data reset. */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    @Inject
    AppSettings settings;
    @Inject
    MatchRepository matchRepository;
    @Inject
    AchievementRepository achievementRepository;

    private FragmentSettingsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.scrollContent);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        Difficulty d = settings.getDefaultDifficulty();
        binding.difficultyToggle.check(d == Difficulty.EASY ? R.id.settingsEasy : d == Difficulty.HARD ? R.id.settingsHard : R.id.settingsMedium);
        binding.difficultyToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            settings.setDefaultDifficulty(id == R.id.settingsEasy ? Difficulty.EASY
                    : id == R.id.settingsHard ? Difficulty.HARD : Difficulty.MEDIUM);
        });

        int overs = settings.getDefaultOvers();
        binding.oversToggle.check(overs == 1 ? R.id.settingsOvers1 : overs == 5 ? R.id.settingsOvers5 : R.id.settingsOvers2);
        binding.oversToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            settings.setDefaultOvers(id == R.id.settingsOvers1 ? 1 : id == R.id.settingsOvers5 ? 5 : 2);
        });

        binding.vibrationSwitch.setChecked(settings.isVibrationEnabled());
        binding.vibrationSwitch.setOnCheckedChangeListener((button, checked) -> settings.setVibrationEnabled(checked));

        int theme = settings.getThemeMode();
        binding.themeToggle.check(theme == AppSettings.THEME_LIGHT ? R.id.themeLight
                : theme == AppSettings.THEME_DARK ? R.id.themeDark : R.id.themeSystem);
        binding.themeToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            int mode = id == R.id.themeLight ? AppSettings.THEME_LIGHT
                    : id == R.id.themeDark ? AppSettings.THEME_DARK : AppSettings.THEME_SYSTEM;
            if (mode != settings.getThemeMode()) settings.setThemeMode(mode);
        });

        binding.resetButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_reset_confirm, (dialog, which) -> {
                    matchRepository.clearAll();
                    achievementRepository.clearAll();
                    Snackbar.make(binding.getRoot(), R.string.settings_reset_done, Snackbar.LENGTH_SHORT).show();
                })
                .show());

        binding.version.setText(getString(R.string.settings_version, BuildConfig.VERSION_NAME));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
