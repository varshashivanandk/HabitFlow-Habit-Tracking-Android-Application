package com.habitflow.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.habitflow.data.db.HabitDao;
import com.habitflow.model.Habit;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Local Storage implementation using SQLite (via HabitDao) with migration from SharedPreferences.
 */
public class HabitStore {

    private static final String PREF_NAME = "habit_flow_prefs";
    private static final String KEY_HABITS = "habits_data";
    private static final String KEY_LAST_DATE = "last_reset_date";
    private static final String KEY_MIGRATED_TO_DB = "migrated_to_db";

    private static HabitStore instance;
    private final HabitDao dao;
    private final FirebaseSyncManager syncManager;
    private List<Habit> cachedHabits = new ArrayList<>();

    private HabitStore(Context context) {
        this.dao = new HabitDao(context);
        this.syncManager = new FirebaseSyncManager();
        migrateIfNeeded(context);
        refreshCache();
        checkNewDay(context);
    }

    public static HabitStore get(Context context) {
        if (instance == null) {
            instance = new HabitStore(context.getApplicationContext());
        }
        return instance;
    }

    /** Clears the singleton instance to force a fresh reload for a new user session */
    public static void reset() {
        instance = null;
    }

    private void refreshCache() {
        cachedHabits = dao.getAllHabits();
        syncTodayStatus();
    }

    // ── Migration ──────────────────────────────────────────────────────────

    private void migrateIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_MIGRATED_TO_DB, false)) return;

        String json = prefs.getString(KEY_HABITS, null);
        if (json != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Habit>>() {}.getType();
                List<Habit> oldHabits = gson.fromJson(json, type);
                if (oldHabits != null) {
                    for (Habit h : oldHabits) {
                        h.ensureInitialized();
                        dao.insert(h);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putBoolean(KEY_MIGRATED_TO_DB, true).apply();
    }

    // ── Day Management ───────────────────────────────────────────────────────

    private void checkNewDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastDateStr = prefs.getString(KEY_LAST_DATE, "");
        String todayStr = getTodayString();

        if (!todayStr.equals(lastDateStr)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            boolean changed = false;
            for (Habit h : cachedHabits) {
                if (Habit.TYPE_HABIT.equals(h.type)) {
                    // Check if the habit was supposed to run yesterday
                    Calendar yesterdayCal = Calendar.getInstance();
                    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
                    boolean shouldHaveRunYesterday = isHabitScheduledForDate(h, yesterdayCal);

                    if (shouldHaveRunYesterday) {
                        boolean finishedYesterday = h.completedDates.contains(yesterdayStr);
                        boolean wasRestDay = h.restDates.contains(yesterdayStr);

                        if (!finishedYesterday && !wasRestDay) {
                            h.currentStreak = 0;
                            changed = true;
                        }
                    }
                }
                h.completedToday = false;
                if (changed) dao.update(h);
            }
            
            if (changed) refreshCache();
            prefs.edit().putString(KEY_LAST_DATE, todayStr).apply();
        }
    }

    public void syncTodayStatus() {
        String todayStr = getTodayString();
        for (Habit h : cachedHabits) {
            h.completedToday = h.completedDates.contains(todayStr);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public List<Habit> getHabits() { 
        syncTodayStatus();
        return filterHabitsForToday(cachedHabits); 
    }

    /** Filters habits based on their frequency settings for the current day. */
    private List<Habit> filterHabitsForToday(List<Habit> source) {
        List<Habit> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek);

        for (Habit h : source) {
            // Tasks always show up until completed (or stay if they have no deadline)
            if (Habit.TYPE_TASK.equals(h.type)) {
                filtered.add(h);
                continue;
            }

            // Frequency filtering for Habits
            if (Habit.FREQ_DAILY.equalsIgnoreCase(h.frequency)) {
                filtered.add(h);
            } else if (h.frequency != null && h.frequency.startsWith("Custom:")) {
                String selectedDays = h.frequency.substring(7).toLowerCase();
                if (selectedDays.contains(dayName.toLowerCase())) {
                    filtered.add(h);
                }
            } else {
                // For Weekly/Monthly, we show them every day for now as per simple MVP,
                // or we could add specific logic here.
                filtered.add(h);
            }
        }
        return filtered;
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            case Calendar.SUNDAY: return "Sun";
            default: return "";
        }
    }

    public void add(Context context, Habit h) {
        h.ensureInitialized();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) h.userId = user.getUid();
        
        dao.insert(h);
        refreshCache();
        syncManager.uploadHabit(h);
    }

    public void update(Context context, Habit updated) {
        dao.update(updated);
        refreshCache();
        syncManager.uploadHabit(updated);
    }

    public void delete(Context context, String id) {
        dao.delete(id);
        refreshCache();
        syncManager.deleteHabit(id);
    }

    public void addOrUpdateLocalOnly(Habit h) {
        h.ensureInitialized();
        // Ensure the habit has the correct UID assigned
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            h.userId = user.getUid();
        }

        Habit existing = findById(h.id);
        if (existing == null) {
            dao.insert(h);
        } else {
            dao.update(h);
        }
        refreshCache();
    }

    public void prepareForNewUser() {
        dao.clearAll();
        refreshCache();
    }

    public void migrateGuestDataToUser(String userId) {
        // Find all habits that have no user_id and assign them to the new user
        List<Habit> guestHabits = dao.getGuestHabits();
        for (Habit h : guestHabits) {
            h.userId = userId;
            dao.update(h);
            syncManager.uploadHabit(h);
        }
        refreshCache();
    }

    public void syncToCloud() {
        syncManager.syncAllLocalHabits(cachedHabits);
    }

    public void fetchFromCloud(Context context, FirebaseSyncManager.SyncCallback callback) {
        syncManager.fetchRemoteHabits(context, callback);
    }

    public Habit findById(String id) {
        for (Habit h : cachedHabits) {
            if (Objects.equals(h.id, id)) return h;
        }
        return null;
    }

    public void toggleComplete(Context context, String id) {
        toggleCompleteForDate(context, id, getTodayString());
    }

    public void toggleCompleteForDate(Context context, String id, String dateStr) {
        Habit h = findById(id);
        if (h == null) return;
        
        String todayStr = getTodayString();
        if (h.completedDates.contains(dateStr)) {
            h.completedDates.remove(dateStr);
            if (dateStr.equals(todayStr)) h.completedToday = false;
            if (Habit.TYPE_HABIT.equals(h.type)) {
                h.currentStreak = Math.max(0, h.currentStreak - 1);
            }
            h.totalCompletions = Math.max(0, h.totalCompletions - 1);
        } else {
            h.completedDates.add(dateStr);
            h.restDates.remove(dateStr); 
            if (dateStr.equals(todayStr)) h.completedToday = true;
            if (Habit.TYPE_HABIT.equals(h.type)) {
                h.currentStreak++;
                if (h.currentStreak > h.bestStreak) h.bestStreak = h.currentStreak;
            }
            h.totalCompletions++;
        }
        dao.update(h);
        refreshCache();
        syncManager.uploadHabit(h);
    }

    public void markRestDay(Context context) {
        String today = getTodayString();
        for (Habit h : cachedHabits) {
            if (!h.completedDates.contains(today)) {
                if (!h.restDates.contains(today)) {
                    h.restDates.add(today);
                    dao.update(h);
                    syncManager.uploadHabit(h);
                }
            }
        }
        refreshCache();
    }

    public void unmarkRestDay(Context context) {
        String today = getTodayString();
        for (Habit h : cachedHabits) {
            if (h.restDates.contains(today)) {
                h.restDates.remove(today);
                dao.update(h);
                syncManager.uploadHabit(h);
            }
        }
        refreshCache();
    }

    public boolean isTodayRestDay() {
        String today = getTodayString();
        for (Habit h : cachedHabits) {
            if (h.restDates.contains(today)) {
                return true;
            }
        }
        return false;
    }

    public int completedTodayCount() {
        int c = 0;
        for (Habit h : cachedHabits) {
            if (h.completedToday) c++;
        }
        return c;
    }

    public int getCompletedCountForDate(String dateStr) {
        int c = 0;
        for (Habit h : cachedHabits) {
            if (h.completedDates.contains(dateStr)) c++;
        }
        return c;
    }

    private boolean isHabitScheduledForDate(Habit h, Calendar cal) {
        if (Habit.TYPE_TASK.equals(h.type)) return true;
        if (Habit.FREQ_DAILY.equalsIgnoreCase(h.frequency)) return true;
        
        if (h.frequency != null && h.frequency.startsWith("Custom:")) {
            String dayName = getDayName(cal.get(Calendar.DAY_OF_WEEK)).toLowerCase();
            return h.frequency.substring(7).toLowerCase().contains(dayName);
        }
        
        return true; // Default for Weekly/Monthly
    }

    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
