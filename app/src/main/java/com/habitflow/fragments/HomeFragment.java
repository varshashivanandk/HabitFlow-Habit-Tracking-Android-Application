package com.habitflow.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.chip.ChipGroup;
import com.habitflow.R;
import com.habitflow.activities.MainActivity;
import com.habitflow.adapters.HabitAdapter;
import com.habitflow.data.HabitStore;
import com.habitflow.model.ChecklistItem;
import com.habitflow.model.Habit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import android.graphics.Color;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HomeFragment extends Fragment {

    private RecyclerView    rvHabits;
    private HabitAdapter    adapter;
    private final List<Habit> displayList = new ArrayList<>();
    private ProgressBar     pbToday;
    private TextView        tvProgressCount, tvProgressLabel;
    private TextView        tvGreeting, tvUsername, tvStreak, btnRestDay;
    private TextView        tvQuote, tvQuoteAuthor;
    private LinearLayout    llEmpty;
    private String          cachedQuoteText = null;
    private String          cachedQuoteAuthor = null;
    private boolean         isPrefetching = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerView();
        setupSwipeAction();
        setGreeting();
        setDailyQuote();
        setupQuoteSwipe(view);
        refreshData();
    }

    @Override public void onResume() {
        super.onResume();
        refreshData();
    }

    public void refreshData() {
        if (isAdded()) {
            loadHabits();
            updateStreakBadge();
            updateRestDayButtonState();
        }
    }

    private void bindViews(View v) {
        rvHabits         = v.findViewById(R.id.rv_habits);
        pbToday          = v.findViewById(R.id.pb_today);
        tvProgressCount  = v.findViewById(R.id.tv_progress_count);
        tvProgressLabel  = v.findViewById(R.id.tv_progress_label);
        tvGreeting       = v.findViewById(R.id.tv_greeting);
        tvUsername       = v.findViewById(R.id.tv_username);
        tvStreak         = v.findViewById(R.id.tv_streak);
        tvQuote          = v.findViewById(R.id.tv_quote);
        tvQuoteAuthor    = v.findViewById(R.id.tv_quote_author);
        btnRestDay       = v.findViewById(R.id.btn_rest_day);
        btnRestDay.setOnClickListener(vv -> showRestDayDialog());
        llEmpty          = v.findViewById(R.id.ll_empty);
    }

    private void setupRecyclerView() {
        adapter = new HabitAdapter(displayList, new HabitAdapter.OnHabitClick() {
            @Override public void onCheck(Habit habit, int position) {
                HabitStore.get(requireContext()).toggleComplete(requireContext(), habit.id);
                loadHabits(); // Immediately re-sort and refresh
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).notifyDataChanged();
                }
            }
            @Override public void onLongPress(Habit habit, int position) {
                openEditSheet(habit);
            }
            @Override public void onSubtaskToggle(Habit habit, ChecklistItem item, int position) {
                HabitStore.get(requireContext()).update(requireContext(), habit);
            }
        });
        rvHabits.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHabits.setAdapter(adapter);
        rvHabits.setItemAnimator(null);
    }

    private void loadHabits() {
        displayList.clear();
        List<Habit> source = HabitStore.get(requireContext()).getHabits();
        
        // Sorting Logic: Completed at bottom, then Priority, then Deadline
        source.sort((h1, h2) -> {
            // 1. Completion Status (Completed at bottom)
            if (h1.completedToday != h2.completedToday) {
                return h1.completedToday ? 1 : -1;
            }
            
            // 2. Priority (High > Medium > Low)
            int p1 = getPriorityValue(h1.priority);
            int p2 = getPriorityValue(h2.priority);
            if (p1 != p2) return p2 - p1; // Descending (3, 2, 1)

            // 3. Deadline (Earliest first)
            if (h1.deadline != h2.deadline) {
                if (h1.deadline == 0) return 1;
                if (h2.deadline == 0) return -1;
                return Long.compare(h1.deadline, h2.deadline);
            }

            return h1.name.compareToIgnoreCase(h2.name);
        });

        displayList.addAll(source);

        boolean isEmpty = displayList.isEmpty();
        llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHabits.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        adapter.notifyDataSetChanged();
        refreshProgress();
    }

    private int getPriorityValue(String priority) {
        if (Habit.PRIORITY_HIGH.equalsIgnoreCase(priority)) return 3;
        if (Habit.PRIORITY_MEDIUM.equalsIgnoreCase(priority)) return 2;
        return 1;
    }

    private void refreshProgress() {
        List<Habit> all = HabitStore.get(requireContext()).getHabits();
        if (all.isEmpty()) {
            pbToday.setProgress(0);
            tvProgressCount.setText("0/0 habits complete");
            return;
        }

        float totalWeight = 0;
        float completedWeight = 0;
        int completedCount = 0;
        int activeHabitsCount = 0;
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Habit h : all) {
            boolean isResting = h.restDates.contains(todayStr);
            if (isResting && !h.completedToday) {
                continue;
            }
            activeHabitsCount++;
            float weight = getPriorityValue(h.priority);
            totalWeight += weight;
            if (h.completedToday) {
                completedWeight += weight;
                completedCount++;
            }
        }

        if (totalWeight == 0) {
            pbToday.setProgress(100);
            tvProgressCount.setText(getString(R.string.habits_complete, completedCount, 0));
            tvProgressLabel.setText(R.string.progress_done);
            return;
        }

        int pct = Math.round((completedWeight / totalWeight) * 100);

        tvProgressCount.setText(getString(R.string.habits_complete, completedCount, activeHabitsCount));
        pbToday.setProgress(pct);

        if (pct == 0)        tvProgressLabel.setText(R.string.progress_start);
        else if (pct < 50)   tvProgressLabel.setText(getString(R.string.progress_keep_going, pct));
        else if (pct < 100)  tvProgressLabel.setText(getString(R.string.progress_almost_there, pct));
        else                 tvProgressLabel.setText(R.string.progress_done);
    }

    private void updateStreakBadge() {
        int maxStreak = 0;
        for (Habit h : HabitStore.get(requireContext()).getHabits()) {
            if (h.currentStreak > maxStreak) maxStreak = h.currentStreak;
        }
        tvStreak.setText(getResources().getQuantityString(R.plurals.streak_days, maxStreak, maxStreak));
    }

    private void setGreeting() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "User";
        
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour < 12)      greetingRes = R.string.good_morning;
        else if (hour < 17) greetingRes = R.string.good_afternoon;
        else                greetingRes = R.string.good_evening;
        tvGreeting.setText(greetingRes);
        tvUsername.setText(name + " 👋");
    }

    /** Shows a new quote every day based on the current date. */
    private void setDailyQuote() {
        showLocalFallbackQuote(false, true);
        prefetchNextQuote();
    }

    private void setupSwipeAction() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                
                final Habit habit = displayList.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe left to delete
                    com.habitflow.util.ReminderManager.cancelReminder(requireContext(), habit);
                    HabitStore.get(requireContext()).delete(requireContext(), habit.id);
                    
                    View anchor = getView() != null ? getView() : viewHolder.itemView;
                    com.google.android.material.snackbar.Snackbar.make(
                        anchor,
                        "Deleted \"" + habit.name + "\"",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("Undo", v -> {
                        HabitStore.get(requireContext()).add(requireContext(), habit);
                        if (habit.notifyEnabled) {
                            com.habitflow.util.ReminderManager.scheduleReminder(requireContext(), habit);
                        }
                        loadHabits();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).notifyDataChanged();
                        }
                    }).setActionTextColor(getResources().getColor(R.color.primary_blue, null))
                      .show();
                } else {
                    // Swipe right to toggle completion
                    HabitStore.get(requireContext()).toggleComplete(requireContext(), habit.id);
                }
                
                loadHabits(); // Re-sort and refresh
                
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).notifyDataChanged();
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getBottom() - itemView.getTop();
                android.graphics.Paint p = new android.graphics.Paint();

                if (dX > 0) { // Swiping Right (Complete/Unmark)
                    int position = viewHolder.getBindingAdapterPosition();
                    boolean isCompleted = false;
                    if (position != RecyclerView.NO_POSITION && position < displayList.size()) {
                        Habit habit = displayList.get(position);
                        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                        isCompleted = habit.completedDates != null && habit.completedDates.contains(todayStr);
                    }

                    if (isCompleted) {
                        p.setColor(android.graphics.Color.parseColor("#757575")); // Grey
                        android.graphics.RectF background = new android.graphics.RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        c.drawRect(background, p);

                        // Draw Unmark Icon (Close)
                        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_close);
                        if (icon != null) {
                            int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.setTint(android.graphics.Color.WHITE);
                            icon.draw(c);
                        }
                    } else {
                        p.setColor(android.graphics.Color.parseColor("#4CAF50")); // Green
                        android.graphics.RectF background = new android.graphics.RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        c.drawRect(background, p);

                        // Draw Tick Icon
                        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_check);
                        if (icon != null) {
                            int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.setTint(android.graphics.Color.WHITE);
                            icon.draw(c);
                        }
                    }
                } else if (dX < 0) { // Swiping Left (Delete)
                    p.setColor(android.graphics.Color.parseColor("#F44336")); // Red
                    android.graphics.RectF background = new android.graphics.RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                    c.drawRect(background, p);

                    // Draw Delete Icon (Close as fallback if delete is missing)
                    android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_close);
                    if (icon != null) {
                        int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.setTint(android.graphics.Color.WHITE);
                        icon.draw(c);
                    }
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                // Disable ViewPager2 swiping when we start swiping an item
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (getActivity() instanceof MainActivity) {
                        ViewPager2 vp = getActivity().findViewById(R.id.view_pager);
                        if (vp != null) vp.setUserInputEnabled(false);
                    }
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Re-enable it
                    if (getActivity() instanceof MainActivity) {
                        ViewPager2 vp = getActivity().findViewById(R.id.view_pager);
                        if (vp != null) vp.setUserInputEnabled(true);
                    }
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvHabits);
    }

    private void showNewRandomQuote() {
        if (cachedQuoteText != null && cachedQuoteAuthor != null) {
            final String nextText = cachedQuoteText;
            final String nextAuthor = cachedQuoteAuthor;

            tvQuote.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvQuote.setText(getString(R.string.quote_format, nextText));
                tvQuote.animate().alpha(1f).setDuration(250).start();
            }).start();

            tvQuoteAuthor.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvQuoteAuthor.setText(getString(R.string.quote_author_format, nextAuthor));
                tvQuoteAuthor.animate().alpha(1f).setDuration(250).start();
            }).start();

            // Clear cache and trigger prefetch for next swipe
            cachedQuoteText = null;
            cachedQuoteAuthor = null;
            prefetchNextQuote();
        } else {
            // Cache not primed yet, load fallback immediately and start a prefetch
            showLocalFallbackQuote(true, false);
            prefetchNextQuote();
        }
    }

    private void prefetchNextQuote() {
        if (isPrefetching) return;
        isPrefetching = true;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            String jsonResult = null;
            try {
                java.net.URL url = new java.net.URL("https://zenquotes.io/api/random");
                java.net.HttpURLConnection urlConnection = (java.net.HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(4000);
                urlConnection.setReadTimeout(4000);
                try {
                    java.io.InputStream in = new java.io.BufferedInputStream(urlConnection.getInputStream());
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    jsonResult = result.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String response = jsonResult;
            handler.post(() -> {
                isPrefetching = false;
                if (!isAdded()) return;
                if (response != null && !response.isEmpty()) {
                    try {
                        com.google.gson.JsonArray array = com.google.gson.JsonParser.parseString(response).getAsJsonArray();
                        if (array.size() > 0) {
                            com.google.gson.JsonObject obj = array.get(0).getAsJsonObject();
                            cachedQuoteText = obj.get("q").getAsString();
                            cachedQuoteAuthor = obj.get("a").getAsString();
                            return; // Pre-fetch succeeded
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // Fallback: Populate cache locally
                cacheLocalRandomQuote();
            });
        });
    }

    private void cacheLocalRandomQuote() {
        if (!isAdded()) return;
        String[] raw = getResources().getStringArray(R.array.quotes);
        if (raw.length == 0) return;
        String entry = raw[new Random().nextInt(raw.length)];
        String[] parts = entry.split("\\|");
        cachedQuoteText = parts[0];
        cachedQuoteAuthor = parts.length > 1 ? parts[1] : "";
    }

    private void showLocalFallbackQuote(boolean animate, boolean useDailyFallback) {
        if (!isAdded()) return;
        String[] raw = getResources().getStringArray(R.array.quotes);
        if (raw.length == 0) return;
        
        String entry;
        if (useDailyFallback) {
            int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            int year = Calendar.getInstance().get(Calendar.YEAR);
            Random seededRandom = new Random(dayOfYear + year * 365L);
            entry = raw[seededRandom.nextInt(raw.length)];
        } else {
            entry = raw[new Random().nextInt(raw.length)];
        }

        String[] parts = entry.split("\\|");
        String text = parts[0];
        String author = parts.length > 1 ? parts[1] : "";

        if (animate) {
            tvQuote.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvQuote.setText(getString(R.string.quote_format, text));
                tvQuote.animate().alpha(1f).setDuration(250).start();
            }).start();

            tvQuoteAuthor.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvQuoteAuthor.setText(getString(R.string.quote_author_format, author));
                tvQuoteAuthor.animate().alpha(1f).setDuration(250).start();
            }).start();
        } else {
            tvQuote.setText(getString(R.string.quote_format, text));
            tvQuoteAuthor.setText(getString(R.string.quote_author_format, author));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupQuoteSwipe(View v) {
        final View quoteCard = v.findViewById(R.id.quote_card_container);
        if (quoteCard == null) return;

        quoteCard.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private boolean isDragging = false;
            private boolean isHorizontalLocked = false;
            private int touchSlop = -1;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (touchSlop < 0) {
                    touchSlop = android.view.ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        isHorizontalLocked = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isHorizontalLocked) return false;

                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;

                        if (!isDragging) {
                            // Check for horizontal movement to let ViewPager2 take over
                            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                                isHorizontalLocked = true;
                                return false;
                            }
                            // Check for vertical movement to start sliding the quote card
                            // Use a smaller threshold than touchSlop to be more responsive
                            if (Math.abs(dy) > 10 && Math.abs(dy) > Math.abs(dx)) {
                                isDragging = true;
                                view.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }

                        if (isDragging) {
                            // Translate the card 1:1 with finger movement for a natural feel
                            view.setTranslationY(dy);
                            view.setAlpha(Math.max(0.5f, 1.0f - Math.abs(dy) / 1000f));
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDragging) {
                            float totalDy = event.getRawY() - startY;
                            if (Math.abs(totalDy) > 200) {
                                // Animate away and refresh
                                float targetY = totalDy > 0 ? 1000 : -1000;
                                view.animate()
                                    .translationY(targetY)
                                    .alpha(0)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        showNewRandomQuote();
                                        view.setTranslationY(0);
                                        view.setAlpha(0);
                                        view.animate().alpha(1).setDuration(250).start();
                                    })
                                    .start();
                            } else {
                                // Snap back if swipe wasn't far enough
                                view.animate()
                                    .translationY(0)
                                    .alpha(1)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                                    .start();
                            }
                        } else if (event.getAction() == MotionEvent.ACTION_UP && !isHorizontalLocked) {
                            // Tap to refresh quote
                            showNewRandomQuote();
                            view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                                .start();
                        }
                        isDragging = false;
                        isHorizontalLocked = false;
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                }
                return false;
            }
        });
    }

    private void showRestDayDialog() {
        boolean isRest = HabitStore.get(requireContext()).isTodayRestDay();
        String title = isRest ? "⏰ Unmark Rest Day" : "😴 Rest Day";
        String message = isRest ? "Unmark today as a rest day? This will resume tracking for today's habits." : "Mark today as a rest day? Your streaks won't be broken.";
        String positiveBtn = isRest ? "Unmark rest day" : "Yes, rest today";

        new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveBtn, (d, w) -> {
                    if (isRest) {
                        HabitStore.get(requireContext()).unmarkRestDay(requireContext());
                        Toast.makeText(getContext(), "Rest day unmarked", Toast.LENGTH_SHORT).show();
                    } else {
                        HabitStore.get(requireContext()).markRestDay(requireContext());
                        Toast.makeText(getContext(), R.string.rest_day_toast, Toast.LENGTH_SHORT).show();
                    }
                    loadHabits();
                    updateRestDayButtonState();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRestDayButtonState() {
        if (btnRestDay == null) return;
        boolean isRest = HabitStore.get(requireContext()).isTodayRestDay();
        if (isRest) {
            btnRestDay.setText("Unmark Rest Day");
            btnRestDay.setTextColor(Color.WHITE);
            btnRestDay.setBackgroundResource(R.drawable.bg_chip_selected);
        } else {
            btnRestDay.setText(R.string.label_rest_day);
            btnRestDay.setTextColor(getResources().getColor(R.color.primary_blue, null));
            btnRestDay.setBackgroundResource(R.drawable.bg_chip_outline);
        }
    }

    private void openEditSheet(Habit habit) {
        AddHabitSheet sheet = AddHabitSheet.newInstance(habit);
        sheet.setOnSaveListener(this::refreshData);
        sheet.show(getParentFragmentManager(), "edit_habit");
    }
}
