package com.habitflow;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class HabitFlowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable disk persistence for Realtime Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
