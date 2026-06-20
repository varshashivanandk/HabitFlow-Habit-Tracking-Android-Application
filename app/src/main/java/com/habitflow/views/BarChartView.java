package com.habitflow.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.Nullable;

import com.habitflow.model.Habit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {
    public enum Period { DAY, WEEK, MONTH }
    
    private Paint barPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private List<Habit> habits = new ArrayList<>();
    private Period currentPeriod = Period.WEEK;
    private int accentColor = Color.parseColor("#728AED");

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(spToPx(10));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Get theme colors
        int colorTextSecondary = Color.parseColor("#8A8880");
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(com.habitflow.R.attr.customTextSecondary, typedValue, true)) {
            colorTextSecondary = typedValue.data;
        }
        textPaint.setColor(colorTextSecondary);

        gridPaint = new Paint();
        gridPaint.setColor((colorTextSecondary & 0x00FFFFFF) | 0x1A000000);
        gridPaint.setStrokeWidth(dpToPx(1));
    }

    public void setData(List<Habit> habits) {
        this.habits = (habits != null) ? new ArrayList<>(habits) : new ArrayList<>();
        invalidate();
    }

    public void setPeriod(Period period) {
        this.currentPeriod = period;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (habits == null || habits.isEmpty()) {
            drawPlaceholder(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        float paddingBottom = dpToPx(30);
        float paddingTop = dpToPx(20);
        float paddingLeft = dpToPx(10);
        float paddingRight = dpToPx(10);
        float chartHeight = height - paddingBottom - paddingTop;
        float chartWidth = width - paddingLeft - paddingRight;

        int numBars;
        switch (currentPeriod) {
            case MONTH:
                numBars = 30;
                break;
            case DAY:
            case WEEK:
            default:
                numBars = 7;
                break;
        }

        float barWidth = chartWidth / (numBars + (numBars + 1) * 0.4f);
        float spacing = barWidth * 0.4f;
        float startX = paddingLeft + (chartWidth - (numBars * barWidth + (numBars - 1) * spacing)) / 2f;

        List<String> dates = getDatesForPeriod(numBars);
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());

        for (int i = 0; i < numBars; i++) {
            float completionPct = calculateCompletionForPeriod(dates.get(i), i);
            float displayRatio = Math.max(completionPct, 0.05f);

            float left = startX + i * (barWidth + spacing);
            float top = (height - paddingBottom) - (displayRatio * chartHeight);
            float right = left + barWidth;
            float bottom = height - paddingBottom;

            barPaint.setColor(accentColor);
            if (completionPct >= 0.99f) {
                barPaint.setColor(Color.parseColor("#7AD326")); // Full completion green
            } else if (completionPct < 0.1f) {
                barPaint.setAlpha(40);
            } else {
                barPaint.setAlpha(255);
            }
            
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, dpToPx(6), dpToPx(6), barPaint);
            barPaint.setAlpha(255);

            // X-Axis Labels
            if (currentPeriod == Period.WEEK) {
                String label = dates.get(i).substring(8); // dd
                canvas.drawText(label, left + barWidth / 2, height - dpToPx(18), textPaint);
                
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -(numBars - 1 - i));
                String dow = dayFormat.format(cal.getTime()).substring(0, 1);
                canvas.drawText(dow, left + barWidth / 2, height - dpToPx(6), textPaint);
            } else if (currentPeriod == Period.MONTH) {
                if (i % 5 == 0 || i == numBars - 1) {
                    String label = dates.get(i).substring(8);
                    canvas.drawText(label, left + barWidth / 2, height - dpToPx(10), textPaint);
                }
            }
        }
    }

    private List<String> getDatesForPeriod(int numDays) {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = numDays - 1; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            if (currentPeriod != Period.DAY) {
                cal.add(Calendar.DAY_OF_YEAR, -i);
            }
            dates.add(sdf.format(cal.getTime()));
        }
        return dates;
    }

    private float calculateCompletionForPeriod(String dateKey, int index) {
        if (habits.isEmpty()) return 0;
        
        int completedCount = 0;
        for (Habit h : habits) {
            if (h.completedDates.contains(dateKey)) {
                completedCount++;
            }
        }
        return (float) completedCount / habits.size();
    }

    private void drawPlaceholder(Canvas canvas) {
        textPaint.setTextSize(spToPx(14));
        canvas.drawText("No Habit Data Yet", getWidth() / 2f, getHeight() / 2f, textPaint);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
