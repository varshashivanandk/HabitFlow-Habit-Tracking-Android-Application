package com.habitflow.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.habitflow.model.ChecklistItem;
import com.habitflow.model.Habit;

import java.util.ArrayList;
import java.util.List;

public class HabitDao {
    private final HabitDbHelper dbHelper;
    private final Gson gson = new Gson();

    public HabitDao(Context context) {
        this.dbHelper = new HabitDbHelper(context);
    }

    public void insert(Habit habit) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = habitToContentValues(habit);
            db.insert(HabitDbHelper.TABLE_HABITS, null, values);

            for (ChecklistItem item : habit.checklist) {
                insertChecklistItem(db, habit.id, item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void update(Habit habit) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = habitToContentValues(habit);
            db.update(HabitDbHelper.TABLE_HABITS, values, HabitDbHelper.COLUMN_ID + "=?", new String[]{habit.id});

            // Refresh checklist: delete and re-insert
            db.delete(HabitDbHelper.TABLE_CHECKLIST, HabitDbHelper.COLUMN_HABIT_ID + "=?", new String[]{habit.id});
            for (ChecklistItem item : habit.checklist) {
                insertChecklistItem(db, habit.id, item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void delete(String habitId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(HabitDbHelper.TABLE_HABITS, HabitDbHelper.COLUMN_ID + "=?", new String[]{habitId});
    }

    public List<Habit> getAllHabits() {
        List<Habit> habits = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = null;
        String[] selectionArgs = null;
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            selection = HabitDbHelper.COLUMN_USER_ID + "=?";
            selectionArgs = new String[]{user.getUid()};
        } else {
            selection = HabitDbHelper.COLUMN_USER_ID + " IS NULL";
        }

        Cursor cursor = db.query(HabitDbHelper.TABLE_HABITS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Habit habit = cursorToHabit(cursor);
                habit.checklist = getChecklistForHabit(db, habit.id);
                habits.add(habit);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return habits;
    }

    public List<Habit> getGuestHabits() {
        List<Habit> habits = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(HabitDbHelper.TABLE_HABITS, null, 
                HabitDbHelper.COLUMN_USER_ID + " IS NULL", null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Habit habit = cursorToHabit(cursor);
                habit.checklist = getChecklistForHabit(db, habit.id);
                habits.add(habit);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return habits;
    }

    public void clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(HabitDbHelper.TABLE_CHECKLIST, null, null);
        db.delete(HabitDbHelper.TABLE_HABITS, null, null);
    }

    private void insertChecklistItem(SQLiteDatabase db, String habitId, ChecklistItem item) {
        ContentValues values = new ContentValues();
        values.put(HabitDbHelper.COLUMN_CHECKLIST_ID, item.id);
        values.put(HabitDbHelper.COLUMN_HABIT_ID, habitId);
        values.put(HabitDbHelper.COLUMN_CHECKLIST_TITLE, item.title);
        values.put(HabitDbHelper.COLUMN_CHECKLIST_COMPLETED, item.isCompleted ? 1 : 0);
        db.insert(HabitDbHelper.TABLE_CHECKLIST, null, values);
    }

    private List<ChecklistItem> getChecklistForHabit(SQLiteDatabase db, String habitId) {
        List<ChecklistItem> items = new ArrayList<>();
        Cursor cursor = db.query(HabitDbHelper.TABLE_CHECKLIST, null,
                HabitDbHelper.COLUMN_HABIT_ID + "=?", new String[]{habitId}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ChecklistItem item = new ChecklistItem(cursor.getString(cursor.getColumnIndexOrThrow(HabitDbHelper.COLUMN_CHECKLIST_TITLE)));
                item.id = cursor.getString(cursor.getColumnIndexOrThrow(HabitDbHelper.COLUMN_CHECKLIST_ID));
                item.isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(HabitDbHelper.COLUMN_CHECKLIST_COMPLETED)) == 1;
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    private ContentValues habitToContentValues(Habit h) {
        ContentValues v = new ContentValues();
        v.put(HabitDbHelper.COLUMN_ID, h.id);
        v.put(HabitDbHelper.COLUMN_NAME, h.name);
        v.put(HabitDbHelper.COLUMN_TYPE, h.type);
        v.put(HabitDbHelper.COLUMN_FREQUENCY, h.frequency);
        v.put(HabitDbHelper.COLUMN_DESCRIPTION, h.description);
        v.put(HabitDbHelper.COLUMN_EMOJI, h.emoji);
        v.put(HabitDbHelper.COLUMN_CATEGORY, h.category);
        v.put(HabitDbHelper.COLUMN_PRIORITY, h.priority);
        v.put(HabitDbHelper.COLUMN_NOTIFY_ENABLED, h.notifyEnabled ? 1 : 0);
        v.put(HabitDbHelper.COLUMN_NOTIFY_TIME, h.notifyTime);
        v.put(HabitDbHelper.COLUMN_DEADLINE, h.deadline);
        v.put(HabitDbHelper.COLUMN_CURRENT_STREAK, h.currentStreak);
        v.put(HabitDbHelper.COLUMN_BEST_STREAK, h.bestStreak);
        v.put(HabitDbHelper.COLUMN_TOTAL_COMPLETIONS, h.totalCompletions);
        v.put(HabitDbHelper.COLUMN_COMPLETED_DATES, gson.toJson(h.completedDates));
        v.put(HabitDbHelper.COLUMN_REST_DATES, gson.toJson(h.restDates));
        v.put(HabitDbHelper.COLUMN_USER_ID, h.userId);
        return v;
    }

    private Habit cursorToHabit(Cursor c) {
        Habit h = new Habit();
        h.id = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_ID));
        h.name = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_NAME));
        h.type = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_TYPE));
        h.frequency = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_FREQUENCY));
        h.description = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_DESCRIPTION));
        h.emoji = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_EMOJI));
        h.category = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_CATEGORY));
        h.priority = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_PRIORITY));
        h.notifyEnabled = c.getInt(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_NOTIFY_ENABLED)) == 1;
        h.notifyTime = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_NOTIFY_TIME));
        h.deadline = c.getLong(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_DEADLINE));
        h.currentStreak = c.getInt(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_CURRENT_STREAK));
        h.bestStreak = c.getInt(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_BEST_STREAK));
        h.totalCompletions = c.getInt(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_TOTAL_COMPLETIONS));
        h.userId = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_USER_ID));

        String compDatesJson = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_COMPLETED_DATES));
        h.completedDates = gson.fromJson(compDatesJson, new TypeToken<ArrayList<String>>(){}.getType());
        
        String restDatesJson = c.getString(c.getColumnIndexOrThrow(HabitDbHelper.COLUMN_REST_DATES));
        h.restDates = gson.fromJson(restDatesJson, new TypeToken<ArrayList<String>>(){}.getType());

        h.ensureInitialized();
        return h;
    }
}
