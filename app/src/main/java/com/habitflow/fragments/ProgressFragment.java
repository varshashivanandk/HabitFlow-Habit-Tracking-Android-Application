package com.habitflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.habitflow.R;
import com.habitflow.data.HabitStore;
import com.habitflow.model.Habit;
import com.habitflow.views.BarChartView;
import com.habitflow.views.HeatmapView;

import java.util.List;

public class ProgressFragment extends Fragment {

    private TextView    tvBestStreak, tvTotal, tvCurrentStreak;
    private RecyclerView rvBreakdown;
    private TabLayout   tabScope;
    private BarChartView barChart;
    private HeatmapView heatmapView;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvBestStreak    = view.findViewById(R.id.tv_best_streak);
        tvTotal         = view.findViewById(R.id.tv_total);
        tvCurrentStreak = view.findViewById(R.id.tv_current_streak);
        rvBreakdown     = view.findViewById(R.id.rv_breakdown);
        tabScope        = view.findViewById(R.id.tab_scope);
        barChart        = view.findViewById(R.id.bar_chart);
        heatmapView     = view.findViewById(R.id.heatmap_view);

        refreshData();
        setupTabs();
    }

    @Override public void onResume() { 
        super.onResume(); 
        refreshData();
    }

    public void onHabitAdded() {
        if (isAdded()) {
            refreshData();
        }
    }

    private void refreshData() {
        List<Habit> habits = HabitStore.get(requireContext()).getHabits();
        loadStats(habits);
        setupBreakdown(habits);
        if (barChart != null) {
            barChart.setData(habits);
        }
        if (heatmapView != null) {
            heatmapView.setData(habits);
        }
    }

    private void loadStats(List<Habit> habits) {
        int bestStreak = 0, currentStreak = 0, total = 0;
        for (Habit h : habits) {
            if (h.bestStreak    > bestStreak)    bestStreak    = h.bestStreak;
            if (h.currentStreak > currentStreak) currentStreak = h.currentStreak;
            total += h.totalCompletions;
        }
        tvBestStreak.setText(String.valueOf(bestStreak));
        tvCurrentStreak.setText(String.valueOf(currentStreak));
        tvTotal.setText(String.valueOf(total));
    }

    private void setupBreakdown(List<Habit> habits) {
        BreakdownAdapter adapter = new BreakdownAdapter(habits);
        rvBreakdown.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBreakdown.setAdapter(adapter);
        rvBreakdown.setNestedScrollingEnabled(false);
    }

    private void setupTabs() {
        tabScope.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override 
            public void onTabSelected(TabLayout.Tab tab) {
                if (barChart == null) return;
                
                int position = tab.getPosition();
                if (position == 0) barChart.setPeriod(BarChartView.Period.DAY);
                else if (position == 1) barChart.setPeriod(BarChartView.Period.WEEK);
                else if (position == 2) barChart.setPeriod(BarChartView.Period.MONTH);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Set initial selection to Week
        TabLayout.Tab weekTab = tabScope.getTabAt(1);
        if (weekTab != null) weekTab.select();
    }

    static class BreakdownAdapter extends RecyclerView.Adapter<BreakdownAdapter.VH> {
        private final List<Habit> items;
        BreakdownAdapter(List<Habit> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_habit_progress, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Habit habit = items.get(position);
            h.tvEmoji.setText(habit.emoji);
            h.tvName.setText(habit.name);
            h.tvStreak.setText("🔥 " + habit.currentStreak + " streak");
            h.tvTotal.setText("✅ " + habit.totalCompletions + " total");

            // Proxy completion percentage
            int pct = habit.totalCompletions > 0 ? Math.min(100, (habit.totalCompletions * 100) / 30) : 0;
            h.pbHabit.setProgress(pct);
            h.tvPct.setText(pct + "%");
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvName, tvStreak, tvTotal, tvPct;
            ProgressBar pbHabit;
            VH(View v) {
                super(v);
                tvEmoji  = v.findViewById(R.id.tv_emoji);
                tvName   = v.findViewById(R.id.tv_name);
                tvStreak = v.findViewById(R.id.tv_streak);
                tvTotal  = v.findViewById(R.id.tv_total);
                tvPct    = v.findViewById(R.id.tv_pct);
                pbHabit  = v.findViewById(R.id.pb_habit);
            }
        }
    }
}
