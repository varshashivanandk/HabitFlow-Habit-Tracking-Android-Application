package com.habitflow.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitflow.R;
import com.habitflow.model.ChecklistItem;
import com.habitflow.model.Habit;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitVH> {

    public interface OnHabitClick {
        void onCheck(Habit habit, int position);
        void onLongPress(Habit habit, int position);
        void onSubtaskToggle(Habit habit, ChecklistItem item, int position);
    }

    private final List<Habit> habits;
    private final OnHabitClick listener;

    public HabitAdapter(List<Habit> habits, OnHabitClick listener) {
        this.habits = habits;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HabitVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitVH holder, int position) {
        Habit h = habits.get(position);
        
        holder.tvName.setText(h.name);
        holder.tvEmoji.setText(h.emoji);
        
        if (Habit.TYPE_HABIT.equals(h.type)) {
            holder.tvCategory.setText(holder.itemView.getContext().getString(R.string.habit_summary_format, h.category, h.frequency));
            holder.tvStreak.setVisibility(View.VISIBLE);
            holder.tvStreak.setText("🔥 " + h.currentStreak);
        } else {
            holder.tvCategory.setText(holder.itemView.getContext().getString(R.string.task_summary_format, h.category));
            holder.tvStreak.setVisibility(View.GONE);
        }

        // Deadline
        if (h.deadline > 0) {
            holder.tvDeadline.setVisibility(View.VISIBLE);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());
            holder.tvDeadline.setText("📅 " + sdf.format(new java.util.Date(h.deadline)));
            
            // Highlight if overdue
            if (h.deadline < System.currentTimeMillis() && !h.completedToday) {
                holder.tvDeadline.setTextColor(Color.parseColor("#FF5252"));
            } else {
                holder.tvDeadline.setTextColor(Color.parseColor("#8A8880"));
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }

        // Completion state
        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        boolean isRestDay = h.restDates != null && h.restDates.contains(todayStr);

        if (h.completedToday) {
            holder.btnCheck.setBackgroundResource(R.drawable.bg_check_done);
            holder.ivCheckIcon.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.5f); // Make whole box translucent
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else if (isRestDay) {
            holder.btnCheck.setBackgroundResource(R.drawable.bg_check_empty);
            holder.ivCheckIcon.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.3f); // Even more translucent for rest day
            holder.tvName.setText(h.name + holder.itemView.getContext().getString(R.string.rest_day_suffix));
            holder.btnCheck.setEnabled(false);
            holder.btnCheck.setAlpha(0.5f);
        } else {
            holder.btnCheck.setBackgroundResource(R.drawable.bg_check_empty);
            holder.ivCheckIcon.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            holder.btnCheck.setEnabled(true);
            holder.btnCheck.setAlpha(1.0f);
        }

        // Priority dot
        GradientDrawable priorityBg = new GradientDrawable();
        priorityBg.setShape(GradientDrawable.OVAL);
        if (Habit.PRIORITY_HIGH.equals(h.priority)) priorityBg.setColor(Color.parseColor("#FF5252"));
        else if (Habit.PRIORITY_LOW.equals(h.priority)) priorityBg.setColor(Color.parseColor("#7AD326"));
        else priorityBg.setColor(Color.parseColor("#FFD600"));
        holder.viewPriority.setBackground(priorityBg);

        holder.btnCheck.setOnClickListener(v -> {
            if (listener != null) {
                // Perform toggle locally for immediate UI response
                h.completedToday = !h.completedToday;
                notifyItemChanged(position);
                
                // Then notify listener to persist change
                listener.onCheck(h, position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
             if (listener != null) listener.onLongPress(h, position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(h, position);
            return true;
        });

        // Setup Checklist
        if (h.checklist != null && !h.checklist.isEmpty()) {
            holder.llChecklistContainer.setVisibility(View.VISIBLE);
            holder.llChecklistContainer.removeAllViews();
            
            for (ChecklistItem item : h.checklist) {
                View subtaskView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_checklist_main, holder.llChecklistContainer, false);
                
                TextView tvTitle = subtaskView.findViewById(R.id.tv_checklist_title);
                ImageView ivCheck = subtaskView.findViewById(R.id.iv_checklist_check);
                
                tvTitle.setText(item.title);
                if (item.isCompleted) {
                    ivCheck.setImageResource(R.drawable.bg_check_done); // Reusing drawable as icon for now or use a proper ic_check_circle
                    ivCheck.setAlpha(1.0f);
                    tvTitle.setAlpha(0.5f);
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    ivCheck.setImageResource(R.drawable.bg_check_empty);
                    ivCheck.setAlpha(0.5f);
                    tvTitle.setAlpha(1.0f);
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                }
                
                subtaskView.setOnClickListener(v -> {
                    if (listener != null) {
                        item.isCompleted = !item.isCompleted;
                        listener.onSubtaskToggle(h, item, position);
                        notifyItemChanged(position);
                    }
                });
                
                holder.llChecklistContainer.addView(subtaskView);
            }
        } else {
            holder.llChecklistContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class HabitVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmoji, tvCategory, tvStreak, tvDeadline;
        View viewAccent, viewPriority;
        FrameLayout btnCheck;
        ImageView ivCheckIcon;
        LinearLayout llChecklistContainer;

        HabitVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvEmoji = v.findViewById(R.id.tv_emoji);
            tvCategory = v.findViewById(R.id.tv_category);
            tvStreak = v.findViewById(R.id.tv_streak);
            tvDeadline = v.findViewById(R.id.tv_deadline);
            viewAccent = v.findViewById(R.id.view_accent);
            viewPriority = v.findViewById(R.id.view_priority);
            btnCheck = v.findViewById(R.id.btn_check);
            ivCheckIcon = v.findViewById(R.id.iv_check_icon);
            llChecklistContainer = v.findViewById(R.id.ll_checklist_container);
        }
    }
}