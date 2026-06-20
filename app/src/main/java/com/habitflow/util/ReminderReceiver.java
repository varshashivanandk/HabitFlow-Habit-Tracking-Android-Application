package com.habitflow.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.habitflow.R;
import com.habitflow.activities.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "habit_reminders";
    public static final String EXTRA_HABIT_NAME = "habit_name";
    public static final String EXTRA_HABIT_ID = "habit_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean globalEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!globalEnabled) return;

        String habitId = intent.getStringExtra(EXTRA_HABIT_ID);
        if (habitId != null) {
            com.habitflow.model.Habit habit = com.habitflow.data.HabitStore.get(context).findById(habitId);
            if (habit != null) {
                String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                if (habit.completedDates != null && habit.completedDates.contains(todayStr)) {
                    return; // Skip notification, already completed today
                }
            }
        }

        String habitName = intent.getStringExtra(EXTRA_HABIT_NAME);

        showNotification(context, habitName, habitId);
    }

    private void showNotification(Context context, String name, String id) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle("Time for your habit!")
                .setContentText("Don't forget to: " + name)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(id.hashCode(), builder.build());
    }
}
