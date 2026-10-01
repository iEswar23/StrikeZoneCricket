package io.github.ieswar23.strikezone;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.databinding.ActivityMainBinding;

/** Single activity hosting the navigation graph. */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    CricketRepository cricketRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        if (host != null) {
            NavController nav = host.getNavController();
            WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            nav.addOnDestinationChangedListener((controller, destination, arguments) -> {
                boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;
                int id = destination.getId();
                // Home and the game draw the dark stadium behind the status bar.
                boolean darkTop = id == R.id.homeFragment || id == R.id.gameFragment;
                bars.setAppearanceLightStatusBars(!night && !darkTop);
                bars.setAppearanceLightNavigationBars(!night && id != R.id.gameFragment);
            });
        }

        if (savedInstanceState == null) {
            // Warm the team/challenge cache from the tournament API on first launch.
            cricketRepository.refresh(false);
        }
    }
}
