package com.habitflow.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.habitflow.R;
import com.habitflow.util.ThemeManager;

@SuppressLint("CustomSplashScreen")
public class
SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout llLogo = findViewById(R.id.ll_logo);
        LinearLayout llDots = findViewById(R.id.ll_dots);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // Animation logic
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(llLogo, "alpha", 0f, 1f);
        ObjectAnimator logoScale = ObjectAnimator.ofFloat(llLogo, "scaleX", 0.7f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(llLogo, "scaleY", 0.7f, 1f);
        logoAlpha.setDuration(700);
        logoScale.setDuration(700);
        logoScaleY.setDuration(700);
        logoScale.setInterpolator(new OvershootInterpolator(1.2f));
        logoScaleY.setInterpolator(new OvershootInterpolator(1.2f));

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(logoAlpha, logoScale, logoScaleY);
        logoAnim.setStartDelay(200);
        logoAnim.start();

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DURATION);
    }

    private void navigateNext() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        boolean isLoggedIn = user != null;

        Intent intent = new Intent(this, isLoggedIn ? MainActivity.class : LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}