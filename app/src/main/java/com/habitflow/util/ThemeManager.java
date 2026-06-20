package com.habitflow.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.habitflow.R;

public class ThemeManager {
    public static final String THEME_DARK = "DARK";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_OCEAN = "OCEAN";
    public static final String THEME_SUNSET = "SUNSET";
    public static final String THEME_FOREST = "FOREST";
    public static final String THEME_AMOLED = "AMOLED";

    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public static void applyTheme(Activity activity) {
        String theme = getSavedTheme(activity);
        switch (theme) {
            case THEME_LIGHT:
                activity.setTheme(R.style.Theme_HabitFlow_Light);
                break;
            case THEME_OCEAN:
                activity.setTheme(R.style.Theme_HabitFlow_Ocean);
                break;
            case THEME_SUNSET:
                activity.setTheme(R.style.Theme_HabitFlow_Sunset);
                break;
            case THEME_FOREST:
                activity.setTheme(R.style.Theme_HabitFlow_Forest);
                break;
            case THEME_AMOLED:
                activity.setTheme(R.style.Theme_HabitFlow_Amoled);
                break;
            default:
                activity.setTheme(R.style.Theme_HabitFlow_Dark);
                break;
        }
    }

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_DARK);
    }

    public static void saveTheme(Context context, String themeKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeKey).apply();
    }

    public static String labelFor(String key) {
        switch (key) {
            case THEME_LIGHT: return "Light";
            case THEME_OCEAN: return "Ocean";
            case THEME_SUNSET: return "Sunset";
            case THEME_FOREST: return "Forest";
            case THEME_AMOLED: return "AMOLED";
            default: return "Dark";
        }
    }

    public static String previewColorFor(String key) {
        switch (key) {
            case THEME_LIGHT: return "#F8F9FD";
            case THEME_OCEAN: return "#0E1B2E";
            case THEME_SUNSET: return "#1F1515";
            case THEME_FOREST: return "#121B15";
            case THEME_AMOLED: return "#000000";
            default: return "#12121A";
        }
    }

    public static String accentColorFor(String key) {
        switch (key) {
            case THEME_LIGHT: return "#1A1A24";
            case THEME_OCEAN: return "#00D4FF";
            case THEME_SUNSET: return "#FF6B35";
            case THEME_FOREST: return "#4CAF50";
            case THEME_AMOLED: return "#FFFFFF";
            default: return "#EEEAE0";
        }
    }
}