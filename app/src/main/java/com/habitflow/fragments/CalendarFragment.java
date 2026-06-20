package com.habitflow.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.habitflow.R;
import com.habitflow.activities.MainActivity;
import com.habitflow.adapters.HabitAdapter;
import com.habitflow.data.HabitStore;
import com.habitflow.model.ChecklistItem;
import com.habitflow.model.Habit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private GridLayout      gridCalendar;
    private TextView        tvMonth, tvSelectedDate, tvSelectedRate;
    private RecyclerView    rvDayHabits;
    private HabitAdapter    dayAdapter;
    private final List<Habit> dayHabits = new ArrayList<>();

    private Calendar currentCal;
    private int selectedDay = -1;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridCalendar    = view.findViewById(R.id.grid_calendar);
        tvMonth         = view.findViewById(R.id.tv_month);
        tvSelectedDate  = view.findViewById(R.id.tv_selected_date);
        tvSelectedRate  = view.findViewById(R.id.tv_selected_rate);
        rvDayHabits     = view.findViewById(R.id.rv_day_habits);

        currentCal = Calendar.getInstance();
        selectedDay = currentCal.get(Calendar.DAY_OF_MONTH);

        setupDayRecycler();
        setupNavButtons(view);
        
        // Use post to ensure view is measured before rendering
        gridCalendar.post(this::renderCalendar);
        showDayHabits(selectedDay);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    public void onHabitAdded() {
        if (isAdded()) {
            refreshData();
        }
    }

    private void refreshData() {
        if (selectedDay != -1) {
            showDayHabits(selectedDay);
        }
        renderCalendar();
    }

    private void setupDayRecycler() {
        dayAdapter = new HabitAdapter(dayHabits, new HabitAdapter.OnHabitClick() {
            @Override
            public void onCheck(Habit habit, int position) {
                if (selectedDay == -1) return;

                Calendar target = (Calendar) currentCal.clone();
                target.set(Calendar.DAY_OF_MONTH, selectedDay);
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(target.getTime());

                HabitStore.get(requireContext()).toggleCompleteForDate(requireContext(), habit.id, dateStr);
                
                refreshData();
                
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).notifyDataChanged();
                }
            }

            @Override
            public void onLongPress(Habit habit, int position) {
                // Optional: Open edit sheet
            }

            @Override
            public void onSubtaskToggle(Habit habit, ChecklistItem item, int position) {
                Habit realHabit = HabitStore.get(requireContext()).findById(habit.id);
                if (realHabit != null) {
                    HabitStore.get(requireContext()).update(requireContext(), realHabit);
                }
            }
        });
        rvDayHabits.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDayHabits.setAdapter(dayAdapter);
    }

    private void setupNavButtons(View v) {
        ImageButton btnPrev = v.findViewById(R.id.btn_prev);
        ImageButton btnNext = v.findViewById(R.id.btn_next);

        btnPrev.setOnClickListener(vv -> {
            currentCal.add(Calendar.MONTH, -1);
            selectedDay = -1;
            renderCalendar();
        });
        btnNext.setOnClickListener(vv -> {
            currentCal.add(Calendar.MONTH, 1);
            selectedDay = -1;
            renderCalendar();
        });
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void renderCalendar() {
        if (!isAdded()) return;
        gridCalendar.removeAllViews();

        int monthlyPct = Math.round(getMonthlyCompletion() * 100);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthTitle = sdf.format(currentCal.getTime());
        if (monthlyPct > 0) {
            tvMonth.setText(String.format(Locale.getDefault(), "%s • %d%%", monthTitle, monthlyPct));
        } else {
            tvMonth.setText(monthTitle);
        }

        Calendar cal = (Calendar) currentCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; 
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        clearTime(today);

        // Colors from theme
        int colorPrimary = getThemeColor(R.attr.colorPrimary);
        int colorTextPrimary = getThemeColor(R.attr.customTextPrimary);
        int colorTextSecondary = getThemeColor(R.attr.customTextSecondary);

        // Calculate cell width based on actual grid width minus horizontal padding
        int totalPadding = gridCalendar.getPaddingLeft() + gridCalendar.getPaddingRight();
        int availableWidth = gridCalendar.getWidth() > 0 ? gridCalendar.getWidth() : getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        int cellWidth  = (availableWidth - totalPadding) / 7;
        int cellHeight = (int)(cellWidth * 1.25f); // Slightly taller for dots

        for (int i = 0; i < firstDow; i++) addBlankCell(cellHeight);

        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            
            Calendar cellDate = (Calendar) currentCal.clone();
            cellDate.set(Calendar.DAY_OF_MONTH, day);
            clearTime(cellDate);

            boolean isToday = cellDate.equals(today);
            boolean isPast = cellDate.before(today);
            boolean isSelected = (d == selectedDay);

            float completionPct = getCompletionForDay(day);

            android.widget.FrameLayout cellContainer = new android.widget.FrameLayout(requireContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            );
            lp.width  = 0;
            lp.height = cellHeight;
            cellContainer.setLayoutParams(lp);

            TextView cell = new TextView(requireContext());
            cell.setText(String.valueOf(day));
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(13f);
            
            android.widget.FrameLayout.LayoutParams cellLp = new android.widget.FrameLayout.LayoutParams(dpToPx(32), dpToPx(32));
            cellLp.gravity = Gravity.CENTER;
            cell.setLayoutParams(cellLp);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);

            if (isSelected) {
                bg.setColor(colorPrimary);
                cell.setTextColor(Color.WHITE);
                cell.setTypeface(null, Typeface.BOLD);
            } else if (isToday) {
                bg.setStroke(dpToPx(2), colorPrimary);
                cell.setTextColor(colorPrimary);
                cell.setTypeface(null, Typeface.BOLD);
            } else if (isPast) {
                if (completionPct > 0.99f) {
                    cell.setTextColor(Color.WHITE);
                    cell.setTypeface(null, Typeface.BOLD);
                    bg.setColor(adjustAlpha(colorPrimary, 0.40f)); // 40% opacity highlight for full completion
                } else if (completionPct > 0) {
                    cell.setTextColor(colorTextPrimary);
                    cell.setTypeface(null, Typeface.NORMAL);
                    bg.setColor(adjustAlpha(colorPrimary, 0.20f)); // 20% opacity highlight for partial completion
                } else {
                    cell.setTextColor(colorTextSecondary);
                    cell.setTypeface(null, Typeface.NORMAL);
                    bg.setColor(adjustAlpha(colorTextPrimary, 0.08f)); // Soft light highlight for past days
                }
            } else {
                cell.setTextColor(colorTextSecondary);
                cell.setTypeface(null, Typeface.NORMAL);
                bg.setColor(Color.TRANSPARENT);
            }

            cell.setBackground(bg);
            
            // Dot indicator for progress
            if (completionPct > 0 && !isSelected) {
                View dot = new View(requireContext());
                int dotSize = dpToPx(4);
                android.widget.FrameLayout.LayoutParams dotLp = new android.widget.FrameLayout.LayoutParams(dotSize, dotSize);
                dotLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                dotLp.bottomMargin = dpToPx(4);
                dot.setLayoutParams(dotLp);
                
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(completionPct > 0.99f ? colorPrimary : adjustAlpha(colorPrimary, 0.5f));
                dot.setBackground(dotBg);
                cellContainer.addView(dot);
            }

            cellContainer.addView(cell);
            cellContainer.setOnClickListener(v -> {
                selectedDay = d;
                renderCalendar();
                showDayHabits(d);
            });

            gridCalendar.addView(cellContainer);
        }
    }

    private float getMonthlyCompletion() {
        int daysInMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        float totalCompletion = 0;
        int activeDays = 0;
        
        Calendar today = Calendar.getInstance();
        clearTime(today);

        for (int d = 1; d <= daysInMonth; d++) {
            Calendar cellDate = (Calendar) currentCal.clone();
            cellDate.set(Calendar.DAY_OF_MONTH, d);
            clearTime(cellDate);
            
            // Only count days up to today
            if (!cellDate.after(today)) {
                totalCompletion += getCompletionForDay(d);
                activeDays++;
            }
        }
        return activeDays > 0 ? totalCompletion / activeDays : 0;
    }

    private void clearTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void addBlankCell(int h) {
        View blank = new View(requireContext());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        );
        lp.width = 0;
        lp.height = h;
        blank.setLayoutParams(lp);
        gridCalendar.addView(blank);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showDayHabits(int day) {
        if (!isAdded()) return;
        Calendar target = (Calendar) currentCal.clone();
        target.set(Calendar.DAY_OF_MONTH, day);
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(target.getTime());

        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        boolean isToday = (today.get(Calendar.YEAR)  == target.get(Calendar.YEAR) &&
                           today.get(Calendar.MONTH) == target.get(Calendar.MONTH) &&
                           today.get(Calendar.DAY_OF_MONTH) == day);
        tvSelectedDate.setText(isToday ? "Today" : fmt.format(target.getTime()));

        dayHabits.clear();
        for (Habit h : HabitStore.get(requireContext()).getHabits()) {
            Habit display = new Habit();
            display.id = h.id;
            display.name = h.name;
            display.emoji = h.emoji;
            display.category = h.category;
            display.priority = h.priority;
            display.type = h.type;
            display.frequency = h.frequency;
            display.currentStreak = h.currentStreak;
            display.completedToday = h.completedDates.contains(dateStr);
            display.checklist = h.checklist;
            dayHabits.add(display);
        }
        dayAdapter.notifyDataSetChanged();

        float pct = getCompletionForDay(day);
        int pctInt = Math.round(pct * 100);
        tvSelectedRate.setText(getString(R.string.completion_percentage, pctInt));
    }

    private float getCompletionForDay(int day) {
        Calendar cal = (Calendar) currentCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, day);
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
        
        HabitStore store = HabitStore.get(requireContext());
        List<Habit> allHabits = store.getHabits();
        if (allHabits.isEmpty()) return 0f;
        
        float totalWeight = 0;
        float completedWeight = 0;

        for (Habit h : allHabits) {
            float weight = getWeight(h.priority);
            totalWeight += weight;
            if (h.completedDates.contains(dateStr)) {
                completedWeight += weight;
            }
        }
        
        return completedWeight / totalWeight;
    }

    private float getWeight(String priority) {
        if (Habit.PRIORITY_HIGH.equalsIgnoreCase(priority)) return 3.0f;
        if (Habit.PRIORITY_MEDIUM.equalsIgnoreCase(priority)) return 2.0f;
        return 1.0f; // Low priority
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        if (alpha == 0 && factor > 0) alpha = Math.round(255 * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private int getInterpolatedColor(float pct) {
        int red = Color.parseColor("#FF5252");
        int yellow = Color.parseColor("#FFB300");
        int green = Color.parseColor("#7AD326");
        if (pct < 0.5f) return interpolate(red, yellow, pct * 2);
        return interpolate(yellow, green, (pct - 0.5f) * 2);
    }

    private int interpolate(int a, int b, float f) {
        int ar = (a >> 16) & 0xff; int ag = (a >> 8) & 0xff; int ab = a & 0xff;
        int br = (b >> 16) & 0xff; int bg = (b >> 8) & 0xff; int bb = b & 0xff;
        return Color.rgb((int) (ar + (br - ar) * f), (int) (ag + (bg - ag) * f), (int) (ab + (bb - ab) * f));
    }

    private int heatmapColor(float pct) {
        // Use brand primary with varying transparency or cleaner shades
        if (pct < 0.35f) return Color.parseColor("#E8EAF6"); // Very light
        if (pct < 0.65f) return Color.parseColor("#9FA8DA"); // Medium light
        return Color.parseColor("#728AED"); // Full brand color
    }
}
