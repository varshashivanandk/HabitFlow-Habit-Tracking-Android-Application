package com.habitflow.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HabitDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habitflow.db";
    private static final int DATABASE_VERSION = 3;

    // Table Habits
    public static final String TABLE_HABITS = "habits";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_FREQUENCY = "frequency";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_EMOJI = "emoji";
    public static final String COLUMN_COLOR = "color_hex";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_NOTIFY_ENABLED = "notify_enabled";
    public static final String COLUMN_NOTIFY_TIME = "notify_time";
    public static final String COLUMN_DEADLINE = "deadline";
    public static final String COLUMN_CURRENT_STREAK = "current_streak";
    public static final String COLUMN_BEST_STREAK = "best_streak";
    public static final String COLUMN_TOTAL_COMPLETIONS = "total_completions";
    public static final String COLUMN_COMPLETED_DATES = "completed_dates"; // JSON string
    public static final String COLUMN_REST_DATES = "rest_dates"; // JSON string
    public static final String COLUMN_USER_ID = "user_id";

    // Table Checklist Items
    public static final String TABLE_CHECKLIST = "checklist_items";
    public static final String COLUMN_CHECKLIST_ID = "id";
    public static final String COLUMN_HABIT_ID = "habit_id";
    public static final String COLUMN_CHECKLIST_TITLE = "title";
    public static final String COLUMN_CHECKLIST_COMPLETED = "is_completed";

    private static final String CREATE_TABLE_HABITS =
            "CREATE TABLE " + TABLE_HABITS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_FREQUENCY + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_EMOJI + " TEXT, " +
                    COLUMN_COLOR + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_PRIORITY + " TEXT, " +
                    COLUMN_NOTIFY_ENABLED + " INTEGER, " +
                    COLUMN_NOTIFY_TIME + " TEXT, " +
                    COLUMN_DEADLINE + " INTEGER, " +
                    COLUMN_CURRENT_STREAK + " INTEGER, " +
                    COLUMN_BEST_STREAK + " INTEGER, " +
                    COLUMN_TOTAL_COMPLETIONS + " INTEGER, " +
                    COLUMN_COMPLETED_DATES + " TEXT, " +
                    COLUMN_REST_DATES + " TEXT, " +
                    COLUMN_USER_ID + " TEXT" +
                    ")";

    private static final String CREATE_TABLE_CHECKLIST =
            "CREATE TABLE " + TABLE_CHECKLIST + " (" +
                    COLUMN_CHECKLIST_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_HABIT_ID + " TEXT, " +
                    COLUMN_CHECKLIST_TITLE + " TEXT, " +
                    COLUMN_CHECKLIST_COMPLETED + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_HABIT_ID + ") REFERENCES " + TABLE_HABITS + "(" + COLUMN_ID + ") ON DELETE CASCADE" +
                    ")";

    public HabitDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HABITS);
        db.execSQL(CREATE_TABLE_CHECKLIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN " + COLUMN_COLOR + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN " + COLUMN_USER_ID + " TEXT");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
