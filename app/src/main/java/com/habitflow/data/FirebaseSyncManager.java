package com.habitflow.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.habitflow.model.Habit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private final DatabaseReference mDatabase;
    private final FirebaseAuth auth;

    public FirebaseSyncManager() {
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    private String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public void uploadHabit(Habit habit) {
        String uid = getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid)
                .child("habits").child(habit.id)
                .setValue(habit)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Habit uploaded to RTDB: " + habit.id))
                .addOnFailureListener(e -> Log.e(TAG, "Error uploading habit to RTDB", e));
    }

    public void deleteHabit(String habitId) {
        String uid = getUid();
        if (uid == null) return;

        mDatabase.child("users").child(uid)
                .child("habits").child(habitId)
                .removeValue()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Habit deleted from RTDB: " + habitId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting habit from RTDB", e));
    }

    public void syncAllLocalHabits(List<Habit> habits) {
        String uid = getUid();
        if (uid == null || habits.isEmpty()) return;

        Map<String, Object> childUpdates = new HashMap<>();
        for (Habit habit : habits) {
            childUpdates.put("/users/" + uid + "/habits/" + habit.id, habit);
        }

        mDatabase.updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "All local habits synced to RTDB"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing local habits to RTDB", e));
    }

    public void fetchRemoteHabits(Context context, SyncCallback callback) {
        String uid = getUid();
        if (uid == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        mDatabase.child("users").child(uid).child("habits")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HabitStore store = HabitStore.get(context);
                        for (DataSnapshot habitSnap : snapshot.getChildren()) {
                            Habit h = habitSnap.getValue(Habit.class);
                            if (h != null) {
                                h.userId = uid;
                                store.addOrUpdateLocalOnly(h);
                            }
                        }
                        if (callback != null) callback.onComplete();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching remote habits from RTDB", error.toException());
                        if (callback != null) callback.onComplete();
                    }
                });
    }

    public void deleteUserData(Runnable onComplete) {
        String uid = getUid();
        if (uid == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        mDatabase.child("users").child(uid)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (onComplete != null) onComplete.run();
                });
    }

    public interface SyncCallback {
        void onComplete();
    }
}

