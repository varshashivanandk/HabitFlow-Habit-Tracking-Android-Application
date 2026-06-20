package com.habitflow.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.habitflow.data.HabitStore;
import com.habitflow.model.Habit;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all active habit reminders
            List<Habit> habits = HabitStore.get(context).getHabits();
            for (Habit h : habits) {
                if (h.notifyEnabled && !h.notifyTime.isEmpty()) {
                    ReminderManager.scheduleReminder(context, h);
                }
            }
        }
    }
}
