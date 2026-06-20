package com.habitflow.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.habitflow.R;
import com.habitflow.activities.LoginActivity;
import com.habitflow.activities.MainActivity;
import com.habitflow.adapters.ReminderSettingAdapter;
import com.habitflow.data.HabitStore;
import com.habitflow.model.Habit;
import com.habitflow.util.ReminderManager;
import com.habitflow.util.ThemeManager;

import java.util.List;

public class SettingsFragment extends Fragment {

    private androidx.gridlayout.widget.GridLayout gridThemes;
    private SwitchMaterial switchNotifs;
    private MaterialButton btnConnectGoogle, rowLogout, btnDeleteAccount;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private ActivityResultLauncher<Intent> googleReauthLauncher;
    private static final String PREFS_SETTINGS = "app_settings";
    private static final String KEY_NOTIFS = "notifications_enabled";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        setupGoogleSignIn();
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleGoogleSignInResult(result.getData())
        );
        googleReauthLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleGoogleReauthResult(result.getData())
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridThemes = view.findViewById(R.id.grid_themes);
        switchNotifs = view.findViewById(R.id.switch_notifs);
        btnConnectGoogle = view.findViewById(R.id.btn_connect_google);
        rowLogout = view.findViewById(R.id.row_logout);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
        
        loadSettings();
        setupThemePicker();
        setupClickListeners(view);
        updateAccountActions();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
    }

    private void loadSettings() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        boolean notifsEnabled = prefs.getBoolean(KEY_NOTIFS, true);
        switchNotifs.setChecked(notifsEnabled);
        
        switchNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFS, isChecked).apply();
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(getContext(), "Notifications " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.row_reminders).setOnClickListener(v -> showRemindersDialog());

        view.findViewById(R.id.row_rate).setOnClickListener(v -> {
            String packageName = requireContext().getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }
        });

        view.findViewById(R.id.row_share).setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out HabitFlow! https://play.google.com/store/apps/details?id=" + requireContext().getPackageName());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share via"));
        });

        btnConnectGoogle.setOnClickListener(v -> connectGuestToGoogle());
        rowLogout.setOnClickListener(v -> showLogoutConfirmation());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void updateAccountActions() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        btnConnectGoogle.setVisibility(user == null ? View.VISIBLE : View.GONE);
        rowLogout.setVisibility(user != null ? View.VISIBLE : View.GONE);
        btnDeleteAccount.setVisibility(user != null ? View.VISIBLE : View.GONE);
    }

    private void connectGuestToGoogle() {
        setConnectGoogleLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
        );
    }

    private void handleGoogleSignInResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account == null || TextUtils.isEmpty(account.getIdToken())) {
                Toast.makeText(getContext(), R.string.error_google_connect_failed, Toast.LENGTH_SHORT).show();
                setConnectGoogleLoading(false);
                return;
            }
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            if (!isGoogleSignInCancelled(e)) {
                Toast.makeText(getContext(), R.string.error_google_connect_failed, Toast.LENGTH_SHORT).show();
            }
            setConnectGoogleLoading(false);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (!isAdded()) return;

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (!task.isSuccessful() || user == null) {
                        Toast.makeText(getContext(), R.string.error_google_connect_failed, Toast.LENGTH_SHORT).show();
                        setConnectGoogleLoading(false);
                        return;
                    }

                    HabitStore.reset();
                    HabitStore store = HabitStore.get(requireContext());
                    store.migrateGuestDataToUser(user.getUid());
                    store.syncToCloud();
                    store.fetchFromCloud(requireContext(), () -> {
                        if (!isAdded()) return;
                        setConnectGoogleLoading(false);
                        updateAccountActions();
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).notifyDataChanged();
                        }
                        Toast.makeText(getContext(), R.string.toast_google_connected, Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void setConnectGoogleLoading(boolean loading) {
        btnConnectGoogle.setEnabled(!loading);
        btnConnectGoogle.setText(loading ? R.string.btn_connecting_google : R.string.btn_connect_google);
    }

    private boolean isGoogleSignInCancelled(ApiException e) {
        return e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED;
    }

    private void showRemindersDialog() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reminders_list, null);
        RecyclerView rv = v.findViewById(R.id.rv_reminders_list);
        
        List<Habit> habits = HabitStore.get(requireContext()).getHabits();
        ReminderSettingAdapter adapter = new ReminderSettingAdapter(habits, h -> {
            HabitStore.get(requireContext()).update(requireContext(), h);
            ReminderManager.scheduleReminder(requireContext(), h);
        });
        
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setTitle("Habit Reminders")
                .setView(v)
                .setPositiveButton("Done", null)
                .show();
    }

    private void showLogoutConfirmation() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setView(dialogView)
                .create();

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnLogout = dialogView.findViewById(R.id.btn_logout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            
            // Clear local cache for the singleton to prevent data leak to next user
            com.habitflow.data.HabitStore.reset();
            
            // Also sign out from Google to allow account switching next time
            com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso).signOut();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        dialog.show();
    }

    private void showDeleteAccountConfirmation() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setTitle("⚠️ Delete Account")
                .setMessage("Are you absolutely sure you want to delete your account? This action is permanent and will delete all your habits, history, and cloud backups forever.")
                .setPositiveButton("Yes, delete forever", (dialog, which) -> executeDeleteAccountFlow())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteAccountFlow() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        new com.habitflow.data.FirebaseSyncManager().deleteUserData(() -> {
            HabitStore.get(requireContext()).prepareForNewUser();
            HabitStore.reset();

            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    completeDeletionAndExit();
                } else {
                    if (task.getException() != null && task.getException().getMessage() != null 
                            && task.getException().getMessage().contains("credential")) {
                        promptReauthentication();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void promptReauthentication() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        
        boolean isGoogle = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                isGoogle = true;
                break;
            }
        }

        if (isGoogle) {
            new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                    .setTitle("🔑 Reauthentication Required")
                    .setMessage("For security, please sign in with Google again to confirm your identity before deleting your account.")
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), t ->
                            googleReauthLauncher.launch(googleSignInClient.getSignInIntent())
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reauth_email, null);
            com.google.android.material.textfield.TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);

            new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                    .setTitle("🔑 Confirm Password")
                    .setMessage("Please enter your current password to confirm account deletion.")
                    .setView(dialogView)
                    .setPositiveButton("Confirm Deletion", (dialog, which) -> {
                        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
                        if (TextUtils.isEmpty(password)) {
                            Toast.makeText(getContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        reauthenticateWithEmailAndDelete(user.getEmail(), password);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void handleGoogleReauthResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account == null || TextUtils.isEmpty(account.getIdToken())) {
                Toast.makeText(getContext(), "Reauthentication failed", Toast.LENGTH_SHORT).show();
                return;
            }
            reauthenticateAndDelete(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(getContext(), "Reauthentication cancelled or failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void reauthenticateAndDelete(String idToken) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                executeDeleteAccountFlow();
            } else {
                Toast.makeText(getContext(), "Reauthentication failed: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reauthenticateWithEmailAndDelete(String email, String password) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || email == null) return;

        AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                executeDeleteAccountFlow();
            } else {
                Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeDeletionAndExit() {
        Toast.makeText(getContext(), "Account successfully deleted", Toast.LENGTH_SHORT).show();
        googleSignInClient.signOut();
        
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupThemePicker() {
        String[] themeKeys = {
                ThemeManager.THEME_DARK, ThemeManager.THEME_LIGHT, ThemeManager.THEME_OCEAN,
                ThemeManager.THEME_SUNSET, ThemeManager.THEME_FOREST, ThemeManager.THEME_AMOLED
        };

        gridThemes.removeAllViews();
        // Calculate item width based on grid column count
        gridThemes.post(() -> {
            int gridWidth = gridThemes.getWidth() - gridThemes.getPaddingLeft() - gridThemes.getPaddingRight();
            int itemWidth = gridWidth / 3;

            for (String key : themeKeys) {
                gridThemes.addView(createThemeItem(key, itemWidth));
            }
        });
    }

    private View createThemeItem(String key, int width) {
        boolean isSelected = key.equals(ThemeManager.getSavedTheme(requireContext()));
        
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dpToPx(6), dpToPx(12), dpToPx(6), dpToPx(12));
        container.setClickable(true);
        container.setFocusable(true);
        
        android.util.TypedValue rippleValue = new android.util.TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, rippleValue, true)) {
            container.setBackgroundResource(rippleValue.resourceId);
        }
        
        androidx.gridlayout.widget.GridLayout.LayoutParams lp = new androidx.gridlayout.widget.GridLayout.LayoutParams();
        lp.width = width;
        container.setLayoutParams(lp);

        // FrameLayout holds the mini-phone preview and check mark badge
        android.widget.FrameLayout frame = new android.widget.FrameLayout(getContext());
        int previewWidth = dpToPx(72);
        int previewHeight = dpToPx(96);
        LinearLayout.LayoutParams lpFrame = new LinearLayout.LayoutParams(previewWidth, previewHeight);
        lpFrame.bottomMargin = dpToPx(8);
        frame.setLayoutParams(lpFrame);

        // Mini device screen
        LinearLayout miniScreen = new LinearLayout(getContext());
        miniScreen.setOrientation(LinearLayout.VERTICAL);
        miniScreen.setPadding(dpToPx(6), dpToPx(8), dpToPx(6), dpToPx(8));
        
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dpToPx(14));
        gd.setColor(Color.parseColor(ThemeManager.previewColorFor(key)));
        
        if (isSelected) {
            gd.setStroke(dpToPx(2.5f), Color.parseColor(ThemeManager.accentColorFor(key)));
        } else {
            int borderColor = 0x228A8880;
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(R.attr.customBorderColor, typedValue, true)) {
                borderColor = typedValue.data;
            }
            gd.setStroke(dpToPx(1), borderColor);
        }
        miniScreen.setBackground(gd);

        // 1. Mini header (accent-colored short line)
        View miniHeader = new View(getContext());
        LinearLayout.LayoutParams lpHeader = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(4));
        lpHeader.bottomMargin = dpToPx(10);
        miniHeader.setLayoutParams(lpHeader);
        GradientDrawable headerGd = new GradientDrawable();
        headerGd.setCornerRadius(dpToPx(2));
        headerGd.setColor(Color.parseColor(ThemeManager.accentColorFor(key)));
        miniHeader.setBackground(headerGd);
        miniScreen.addView(miniHeader);

        // 2. Mini list cards
        for (int i = 0; i < 2; i++) {
            View miniCard = new View(getContext());
            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(10));
            lpCard.bottomMargin = dpToPx(6);
            miniCard.setLayoutParams(lpCard);
            
            GradientDrawable cardGd = new GradientDrawable();
            cardGd.setCornerRadius(dpToPx(3));
            cardGd.setColor(Color.parseColor(ThemeManager.accentColorFor(key)));
            cardGd.setAlpha(i == 0 ? 55 : 25); // Varying transparency
            miniCard.setBackground(cardGd);
            miniScreen.addView(miniCard);
        }

        // Space filler
        View filler = new View(getContext());
        LinearLayout.LayoutParams lpFiller = new LinearLayout.LayoutParams(1, 0, 1.0f);
        filler.setLayoutParams(lpFiller);
        miniScreen.addView(filler);

        // 3. Mini navigation bar preview
        LinearLayout miniNav = new LinearLayout(getContext());
        miniNav.setOrientation(LinearLayout.HORIZONTAL);
        miniNav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lpNav = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(10));
        miniNav.setLayoutParams(lpNav);

        View dotLeft = new View(getContext());
        LinearLayout.LayoutParams lpDotLeft = new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
        dotLeft.setLayoutParams(lpDotLeft);
        GradientDrawable dotGd1 = new GradientDrawable();
        dotGd1.setShape(GradientDrawable.OVAL);
        dotGd1.setColor(Color.parseColor(ThemeManager.accentColorFor(key)));
        dotGd1.setAlpha(80);
        dotLeft.setBackground(dotGd1);
        miniNav.addView(dotLeft);

        View spacer1 = new View(getContext());
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        miniNav.addView(spacer1);

        // Center plus indicator
        View dotCenter = new View(getContext());
        LinearLayout.LayoutParams lpDotCenter = new LinearLayout.LayoutParams(dpToPx(14), dpToPx(6));
        dotCenter.setLayoutParams(lpDotCenter);
        GradientDrawable centerGd = new GradientDrawable();
        centerGd.setCornerRadius(dpToPx(2));
        centerGd.setColor(Color.parseColor(ThemeManager.accentColorFor(key)));
        dotCenter.setBackground(centerGd);
        miniNav.addView(dotCenter);

        View spacer2 = new View(getContext());
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        miniNav.addView(spacer2);

        View dotRight = new View(getContext());
        LinearLayout.LayoutParams lpDotRight = new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
        dotRight.setLayoutParams(lpDotRight);
        GradientDrawable dotGd2 = new GradientDrawable();
        dotGd2.setShape(GradientDrawable.OVAL);
        dotGd2.setColor(Color.parseColor(ThemeManager.accentColorFor(key)));
        dotGd2.setAlpha(80);
        dotRight.setBackground(dotGd2);
        miniNav.addView(dotRight);

        miniScreen.addView(miniNav);
        frame.addView(miniScreen);



        TextView label = new TextView(getContext());
        label.setText(ThemeManager.labelFor(key));
        label.setTextSize(12);
        
        int textSecondary = 0xFF8A8880;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(R.attr.customTextSecondary, typedValue, true)) {
            textSecondary = typedValue.data;
        }

        label.setTextColor(isSelected ? Color.parseColor(ThemeManager.accentColorFor(key)) : textSecondary);
        if (isSelected) label.setTypeface(null, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);

        container.addView(frame);
        container.addView(label);

        container.setOnClickListener(v -> {
            if (!isSelected) {
                ThemeManager.saveTheme(requireContext(), key);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).restartForTheme();
                } else {
                    requireActivity().recreate();
                }
            }
        });

        return container;
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
