package com.habitflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Habit implements Serializable {
    public static final String CAT_FITNESS = "Fitness";
    public static final String CAT_LEARNING = "Learning";
    public static final String CAT_WELLNESS = "Wellness";
    public static final String CAT_NUTRITION = "Nutrition";
    public static final String CAT_PRODUCTIVITY = "Productivity";
    public static final String CAT_SOCIAL = "Social";

    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";

    public static final String TYPE_HABIT = "Habit";
    public static final String TYPE_TASK = "Task";

    public static final String FREQ_DAILY = "Daily";
    public static final String FREQ_WEEKLY = "Weekly";
    public static final String FREQ_MONTHLY = "Monthly";

    public String id;
    public String name;
    public String type = TYPE_HABIT; // Default to habit
    public String frequency = FREQ_DAILY; // Default to daily
    public String description = "";
    public String emoji;
    public String category;
    public String priority;
    public boolean notifyEnabled;
    public String notifyTime = "";
    public long deadline = 0; // Timestamp for deadline, 0 if none
    
    public int currentStreak = 0;
    public int bestStreak = 0;
    public int totalCompletions = 0;
    public boolean completedToday = false;
    public String userId; // Owner of the habit
    
    // Checklist
    public List<ChecklistItem> checklist = new ArrayList<>();
 
    // Track completion history and rest days
    public List<String> completedDates = new ArrayList<>();
    public List<String> restDates = new ArrayList<>();
 
    public Habit() {
        this.id = UUID.randomUUID().toString();
    }
 
    public Habit(String id, String name, String emoji, String category, String priority) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.category = category;
        this.priority = priority;
    }

    /** 
     * Ensures sets are not null after deserialization 
     */
    public void ensureInitialized() {
        if (completedDates == null) completedDates = new ArrayList<>();
        if (restDates == null) restDates = new ArrayList<>();
        if (checklist == null) checklist = new ArrayList<>();
    }
}
