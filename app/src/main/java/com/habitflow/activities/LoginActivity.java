package com.habitflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.habitflow.R;
import com.habitflow.data.HabitStore;
import com.habitflow.util.ThemeManager;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private boolean isLoginMode = true;
    private TextView tv_title, tv_subtitle, tv_error;
    private TextInputLayout til_name, til_email, til_password;
    private TextInputEditText et_name, et_email, et_password;
    private MaterialButton btn_primary, btn_toggle, btn_google;
    private TextView tv_skip;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        setupGoogleSignIn();
        
        bindViews();
        setListeners();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void bindViews() {
        tv_title    = findViewById(R.id.tv_title);
        tv_subtitle = findViewById(R.id.tv_subtitle);
        tv_error    = findViewById(R.id.tv_error);
        til_name    = findViewById(R.id.til_name);
        til_email   = findViewById(R.id.til_email);
        til_password= findViewById(R.id.til_password);
        et_name     = findViewById(R.id.et_name);
        et_email    = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        btn_primary = findViewById(R.id.btn_primary_action);
        btn_toggle  = findViewById(R.id.btn_toggle_mode);
        btn_google  = findViewById(R.id.btn_google);
        tv_skip     = findViewById(R.id.tv_skip);
        progress    = findViewById(R.id.progress);
    }

    private void setListeners() {
        btn_primary.setOnClickListener(v -> handlePrimaryAction());
        btn_toggle.setOnClickListener(v -> toggleMode());
        btn_google.setOnClickListener(v -> signInWithGoogle());
        tv_skip.setOnClickListener(v -> goToMain());
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        tv_error.setVisibility(View.GONE);
        clearErrors();

        if (isLoginMode) {
            tv_title.setText(R.string.login_title);
            tv_subtitle.setText(R.string.login_subtitle);
            btn_primary.setText(R.string.btn_login);
            btn_toggle.setText(R.string.link_register);
            til_name.setVisibility(View.GONE);
        } else {
            tv_title.setText(R.string.register_title);
            tv_subtitle.setText(R.string.register_subtitle);
            btn_primary.setText(R.string.btn_register);
            btn_toggle.setText(R.string.link_login);
            til_name.setVisibility(View.VISIBLE);
        }
    }

    private void clearErrors() {
        til_email.setError(null);
        til_password.setError(null);
        til_name.setError(null);
    }

    private void handlePrimaryAction() {
        String email    = et_email.getText() != null ? et_email.getText().toString().trim() : "";
        String password = et_password.getText() != null ? et_password.getText().toString() : "";
        String name     = et_name.getText() != null ? et_name.getText().toString().trim() : "";

        if (!validateInputs(email, password, name)) return;

        showLoading(true);
        if (isLoginMode) {
            loginUser(email, password);
        } else {
            registerUser(email, password, name);
        }
    }

    private boolean validateInputs(String email, String password, String name) {
        boolean isValid = true;
        if (TextUtils.isEmpty(email)) {
            til_email.setError(getString(R.string.error_email_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            til_email.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            til_email.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            til_password.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 6) {
            til_password.setError(getString(R.string.error_password_length));
            isValid = false;
        } else {
            til_password.setError(null);
        }

        if (!isLoginMode && TextUtils.isEmpty(name)) {
            til_name.setError(getString(R.string.error_name_required));
            isValid = false;
        } else {
            til_name.setError(null);
        }
        return isValid;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        onAuthSuccess();
                    } else {
                        showError(getString(R.string.error_login_failed));
                        showLoading(false);
                    }
                });
    }

    private void registerUser(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(t -> onAuthSuccess());
                        }
                    } else {
                        showError(getString(R.string.error_register_failed));
                        showLoading(false);
                    }
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        // Force account picker by signing out first
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                if (!isGoogleSignInCancelled(e)) {
                    showError(getString(R.string.error_google_sign_in_failed));
                }
                showLoading(false);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        onAuthSuccess();
                    } else {
                        showError(getString(R.string.error_google_sign_in_failed));
                        showLoading(false);
                    }
                });
    }

    private boolean isGoogleSignInCancelled(ApiException e) {
        return e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED;
    }

    private void onAuthSuccess() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (!isLoginMode) {
            // REGISTRATION: Move existing local guest data to this new account
            HabitStore.reset(); // Ensure fresh instance after login
            HabitStore.get(this).migrateGuestDataToUser(user.getUid());
            goToMain();
        } else {
            // LOGIN: Clear existing local data (it belongs to someone else or is old)
            // and download this user's data from cloud.
            HabitStore.reset(); // Clear guest cache
            HabitStore.get(this).prepareForNewUser();
            HabitStore.get(this).fetchFromCloud(this, this::goToMain);
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
        finish();
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btn_primary.setEnabled(!show);
        btn_google.setEnabled(!show);
    }

    private void showError(String message) {
        tv_error.setText(message);
        tv_error.setVisibility(View.VISIBLE);
    }
}
