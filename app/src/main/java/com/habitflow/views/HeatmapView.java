package com.habitflow.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.habitflow.data.HabitStore;
import com.habitflow.model.Habit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HeatmapView extends View {
    private Paint paint;
    private float cornerRadius;
    private List<Habit> habits;

    public HeatmapView(Context context) {
        super(context);
        init();
    }

    public HeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        cornerRadius = dpToPx(3);
    }

    public void setData(List<Habit> habits) {
        this.habits = habits;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int columns = 20; // Show approx 20 weeks of data
        int rows = 7;
        int size = dpToPx(12);
        int spacing = dpToPx(4);
        
        int width = (columns * (size + spacing)) + getPaddingLeft() + getPaddingRight();
        int height = (rows * (size + spacing)) + getPaddingTop() + getPaddingBottom();
        
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int columns = 20;
        int rows = 7;
        int size = dpToPx(12);
        int spacing = dpToPx(4);
        
        // Theme-aware colors
        int colorPrimary = 0x728AED;
        int colorEmpty = 0x1A1A24;
        
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(com.habitflow.R.attr.colorPrimary, typedValue, true)) {
            colorPrimary = typedValue.data;
        }
        if (getContext().getTheme().resolveAttribute(com.habitflow.R.attr.customElevatedBackground, typedValue, true)) {
            colorEmpty = typedValue.data;
        }

        // Generate lighter levels based on brand color
        int[] levels = {
            colorEmpty,                                      // Empty
            adjustAlpha(colorPrimary, 0.2f),                // Low
            adjustAlpha(colorPrimary, 0.4f),                // Medium-Low
            adjustAlpha(colorPrimary, 0.7f),                // Medium-High
            colorPrimary                                     // High (Completed all)
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        // Go back (columns * 7) days to start the grid
        cal.add(Calendar.DAY_OF_YEAR, -(columns * rows) + 1);
        
        // Snap to Sunday if we want consistent rows
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                String dateKey = sdf.format(cal.getTime());
                
                int level = 0;
                if (habits != null && !habits.isEmpty()) {
                    float totalWeight = 0;
                    float completedWeight = 0;
                    for (Habit h : habits) {
                        float weight = getWeight(h.priority);
                        totalWeight += weight;
                        if (h.completedDates.contains(dateKey)) {
                            completedWeight += weight;
                        }
                    }
                    
                    float pct = completedWeight / totalWeight;
                    if (pct > 0.99f) level = 4;
                    else if (pct > 0.66f) level = 3;
                    else if (pct > 0.33f) level = 2;
                    else if (pct > 0.01f) level = 1;
                }

                float left = i * (size + spacing);
                float top = j * (size + spacing);
                float right = left + size;
                float bottom = top + size;
                
                paint.setColor(levels[level]);
                
                RectF rect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
                
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        if (alpha == 0 && factor > 0) alpha = Math.round(255 * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private float getWeight(String priority) {
        if (Habit.PRIORITY_HIGH.equalsIgnoreCase(priority)) return 3.0f;
        if (Habit.PRIORITY_MEDIUM.equalsIgnoreCase(priority)) return 2.0f;
        return 1.0f;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}