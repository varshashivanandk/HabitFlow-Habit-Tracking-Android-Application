package com.habitflow.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.habitflow.R;
import com.habitflow.fragments.AddHabitSheet;
import com.habitflow.fragments.CalendarFragment;
import com.habitflow.fragments.HomeFragment;
import com.habitflow.fragments.ProgressFragment;
import com.habitflow.fragments.SettingsFragment;
import com.habitflow.util.NotificationHelper;
import com.habitflow.util.ThemeManager;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private BottomNavigationView bottomNav;
    private ViewPager2 viewPager;
    private FloatingActionButton fab;

    private HomeFragment     homeFragment;
    private CalendarFragment calendarFragment;
    private ProgressFragment progressFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra("target_tab")) {
            overridePendingTransition(0, 0);
        }
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this);

        bottomNav = findViewById(R.id.bottom_nav);
        viewPager = findViewById(R.id.view_pager);
        fab       = findViewById(R.id.fab_add);

        initFragments();
        setupBottomNav();
        setupFab();
        checkNotificationPermission();

        int targetTab = getIntent().getIntExtra("target_tab", -1);
        if (targetTab != -1) {
            viewPager.setCurrentItem(targetTab, false);
        }

        triggerStartupSync();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, notifications should work now
            }
        }
    }

    // ── Fragment management ───────────────────────────────────────────────────

    private void initFragments() {
        homeFragment     = new HomeFragment();
        calendarFragment = new CalendarFragment();
        progressFragment = new ProgressFragment();
        settingsFragment = new SettingsFragment();

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return homeFragment;
                    case 1: return calendarFragment;
                    case 2: return progressFragment;
                    case 3: return settingsFragment;
                    default: return homeFragment;
                }
            }
            @Override public int getItemCount() { return 4; }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.nav_today); break;
                    case 1: bottomNav.setSelectedItemId(R.id.nav_calendar); break;
                    case 2: bottomNav.setSelectedItemId(R.id.nav_progress); break;
                    case 3: bottomNav.setSelectedItemId(R.id.nav_settings); break;
                }
            }
        });

        // Disable swiping for some fragments if needed, but the user requested left/right slide
        viewPager.setOffscreenPageLimit(3);
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) {
                viewPager.setCurrentItem(0);
            } else if (id == R.id.nav_calendar) {
                viewPager.setCurrentItem(1);
            } else if (id == R.id.nav_progress) {
                viewPager.setCurrentItem(2);
            } else if (id == R.id.nav_settings) {
                viewPager.setCurrentItem(3);
            }
            return true;
        });
    }

    // ── FAB ───────────────────────────────────────────────────────────────────

    private void setupFab() {
        fab.setOnClickListener(v -> {
            AddHabitSheet sheet = AddHabitSheet.newInstance(null);
            sheet.setOnSaveListener(this::notifyDataChanged);
            sheet.show(getSupportFragmentManager(), "add_habit");
        });
    }

    /** Called when data in HabitStore changes (added, edited, or toggled) */
    public void notifyDataChanged() {
        if (homeFragment != null) homeFragment.refreshData();
        if (calendarFragment != null) calendarFragment.onHabitAdded();
        if (progressFragment != null) progressFragment.onHabitAdded();
    }

    public void restartForTheme() {
        android.content.Intent intent = getIntent();
        intent.putExtra("target_tab", viewPager.getCurrentItem());
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void triggerStartupSync() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.habitflow.data.HabitStore.get(this).fetchFromCloud(this, () -> {
                runOnUiThread(this::notifyDataChanged);
            });
        }
    }
}
