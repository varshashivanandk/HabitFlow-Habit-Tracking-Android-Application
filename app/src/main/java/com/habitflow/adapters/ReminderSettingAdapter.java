package com.habitflow.adapters;

import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.habitflow.R;
import com.habitflow.model.Habit;

import java.util.List;
import java.util.Locale;

public class ReminderSettingAdapter extends RecyclerView.Adapter<ReminderSettingAdapter.VH> {

    private final List<Habit> habits;
    private final OnReminderChanged listener;

    public interface OnReminderChanged {
        void onChanged(Habit habit);
    }

    public ReminderSettingAdapter(List<Habit> habits, OnReminderChanged listener) {
        this.habits = habits;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder_setting, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Habit h = habits.get(position);
        holder.tvName.setText(h.name);
        holder.tvTime.setText(h.notifyTime.isEmpty() ? "08:00" : h.notifyTime);
        holder.tvEmoji.setText(h.emoji == null || h.emoji.isEmpty() ? "🏃" : h.emoji);
        holder.switchNotify.setChecked(h.notifyEnabled);

        holder.tvTime.setOnClickListener(v -> {
            String time = h.notifyTime.isEmpty() ? "08:00" : h.notifyTime;
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);

            new TimePickerDialog(v.getContext(), (view, hOfDay, m) -> {
                String newTime = String.format(Locale.getDefault(), "%02d:%02d", hOfDay, m);
                h.notifyTime = newTime;
                h.notifyEnabled = true;
                holder.tvTime.setText(newTime);
                holder.switchNotify.setChecked(true);
                if (listener != null) listener.onChanged(h);
            }, hour, min, true).show();
        });

        holder.switchNotify.setOnCheckedChangeListener((btn, checked) -> {
            if (h.notifyEnabled != checked) {
                h.notifyEnabled = checked;
                if (listener != null) listener.onChanged(h);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvEmoji;
        SwitchMaterial switchNotify;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_habit_name);
            tvTime = v.findViewById(R.id.tv_reminder_time);
            tvEmoji = v.findViewById(R.id.tv_habit_emoji);
            switchNotify = v.findViewById(R.id.switch_habit_notify);
        }
    }
}
