package com.habitflow.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.habitflow.R;
import com.habitflow.activities.MainActivity;
import com.habitflow.data.HabitStore;
import com.habitflow.model.ChecklistItem;
import com.habitflow.model.Habit;
import com.habitflow.util.ReminderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddHabitSheet extends BottomSheetDialogFragment {

    private Habit habitToEdit;
    private Runnable onSaveListener;

    private EditText etName, etDesc;
    private TextView tvSheetTitle, tvReminderTime, tvSelectedEmoji;
    private ChipGroup chipsCategory, chipsPriority, chipsType, chipsFrequency, chipsDays;
    private LinearLayout llChecklistContainer, llFrequencyContainer, llCustomDays;
    private FrameLayout btnCustomEmoji;
    private SwitchMaterial switchNotify;
    private String selectedEmoji = "🏃";
    private String selectedTime = "08:00";
    private final List<ChecklistItem> tempChecklist = new ArrayList<>();

    public static AddHabitSheet newInstance(Habit habit) {
        AddHabitSheet fragment = new AddHabitSheet();
        fragment.habitToEdit = habit;
        return fragment;
    }

    public void setOnSaveListener(Runnable listener) {
        this.onSaveListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_habit, container, false);
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.app.Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                handleCloseAttempt();
                return true;
            }
            return false;
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupEmojiPicker();
        setupTimePicker();
        setupChecklist();

        if (habitToEdit != null) {
            setupEditMode(view);
        }

        view.findViewById(R.id.btn_close).setOnClickListener(v -> handleCloseAttempt());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveHabit());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirmation());
    }

    private void bindViews(View v) {
        tvSheetTitle = v.findViewById(R.id.tv_sheet_title);
        etName = v.findViewById(R.id.et_name);
        etDesc = v.findViewById(R.id.et_desc);
        chipsCategory = v.findViewById(R.id.chips_category);
        chipsPriority = v.findViewById(R.id.chips_priority);
        chipsType = v.findViewById(R.id.chips_type);
        chipsFrequency = v.findViewById(R.id.chips_frequency);
        llFrequencyContainer = v.findViewById(R.id.ll_frequency_container);
        llCustomDays = v.findViewById(R.id.ll_custom_days);
        chipsDays = v.findViewById(R.id.chips_days);
        btnCustomEmoji = v.findViewById(R.id.btn_custom_emoji);
        tvSelectedEmoji = v.findViewById(R.id.tv_selected_emoji);
        llChecklistContainer = v.findViewById(R.id.ll_checklist_container);
        switchNotify = v.findViewById(R.id.switch_reminder);
        tvReminderTime = v.findViewById(R.id.tv_reminder_time);

        setupTypeToggle();
    }

    private void setupTypeToggle() {
        chipsType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean isHabit = checkedIds.contains(R.id.chip_type_habit);
            llFrequencyContainer.setVisibility(isHabit ? View.VISIBLE : View.GONE);
            tvSheetTitle.setText(isHabit ? R.string.add_habit_title : R.string.add_task_title);
            if (!isHabit) llCustomDays.setVisibility(View.GONE);
            else if (chipsFrequency.getCheckedChipId() == R.id.chip_freq_custom) llCustomDays.setVisibility(View.VISIBLE);
        });

        chipsFrequency.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean isCustom = checkedIds.contains(R.id.chip_freq_custom);
            llCustomDays.setVisibility(isCustom ? View.VISIBLE : View.GONE);
        });
    }

    private void setupTimePicker() {
        View row = getView() != null ? getView().findViewById(R.id.row_reminder_time) : null;
        if (row != null) {
            row.setOnClickListener(v -> showTimePicker());
        }
        tvReminderTime.setOnClickListener(v -> showTimePicker());
    }
    
    private void showTimePicker() {
        String[] parts = selectedTime.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            tvReminderTime.setText(selectedTime);
            switchNotify.setChecked(true);
        }, h, m, true).show();
    }

    private void setupEmojiPicker() {
        btnCustomEmoji.setOnClickListener(v -> showEmojiPickerDialog());
        tvSelectedEmoji.setText(selectedEmoji);
    }

    private void showEmojiPickerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_emoji_input, null);
        com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.til_emoji_input);
        EditText et = dialogView.findViewById(R.id.et_emoji_input);
        et.setText(selectedEmoji);
        et.setSelection(selectedEmoji.length());

        // Restrict input in real-time to only allow emoji characters
        et.setFilters(new android.text.InputFilter[]{new android.text.InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; ) {
                    int codePoint = Character.codePointAt(source, i);
                    int charCount = Character.charCount(codePoint);
                    // Filter out letters, digits, and printable standard ASCII punctuation/letters/numbers
                    if (!Character.isLetterOrDigit(codePoint) && (codePoint < 0x20 || codePoint > 0x7E)) {
                        sb.append(source.subSequence(i, i + charCount));
                    }
                    i += charCount;
                }
                if (source.length() == sb.length()) {
                    return null; // Keep original
                }
                return sb.toString(); // Return filtered (emoji only)
            }
        }});

        // Instantly limit input to exactly one visual emoji using BreakIterator
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String input = s.toString();
                if (input.isEmpty()) return;

                java.text.BreakIterator boundary = java.text.BreakIterator.getCharacterInstance();
                boundary.setText(input);
                int count = 0;
                int lastEnd = 0;
                while (boundary.next() != java.text.BreakIterator.DONE) {
                    count++;
                    if (count == 1) {
                        lastEnd = boundary.current();
                    }
                }
                if (count > 1) {
                    et.removeTextChangedListener(this);
                    s.replace(0, s.length(), input.substring(0, lastEnd));
                    et.addTextChangedListener(this);
                }
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setView(dialogView)
                .setPositiveButton("Select", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String input = et.getText().toString().trim();
            if (isSingleEmoji(input)) {
                selectedEmoji = input;
                tvSelectedEmoji.setText(selectedEmoji);
                dialog.dismiss();
            } else {
                til.setError("Please enter exactly one valid emoji");
            }
        });
    }

    private boolean isSingleEmoji(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        text = text.trim();
        
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (Character.isLetterOrDigit(codePoint)) {
                return false;
            }
            if (codePoint >= 0x20 && codePoint <= 0x7E) {
                return false;
            }
            i += Character.charCount(codePoint);
        }

        java.text.BreakIterator boundary = java.text.BreakIterator.getCharacterInstance();
        boundary.setText(text);
        int count = 0;
        while (boundary.next() != java.text.BreakIterator.DONE) {
            count++;
        }

        return count == 1;
    }

    private void setupChecklist() {
        View addBtn = getView() != null ? getView().findViewById(R.id.btn_add_checklist_item) : null;
        if (addBtn != null) {
            addBtn.setOnClickListener(v -> {
                ChecklistItem item = new ChecklistItem("");
                tempChecklist.add(item);
                addChecklistItemView(item);
            });
        }
    }

    private void addChecklistItemView(ChecklistItem item) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_checklist, llChecklistContainer, false);
        EditText etTitle = v.findViewById(R.id.et_checklist_title);
        CheckBox checkBox = v.findViewById(R.id.checkbox);
        View btnRemove = v.findViewById(R.id.btn_remove);

        etTitle.setText(item.title);
        checkBox.setChecked(item.isCompleted);

        btnRemove.setOnClickListener(view -> {
            tempChecklist.remove(item);
            llChecklistContainer.removeView(v);
        });

        llChecklistContainer.addView(v);
    }

    private void setupEditMode(View view) {
        tvSheetTitle.setText(R.string.edit_habit_title);
        etName.setText(habitToEdit.name);
        etDesc.setText(habitToEdit.description);
        selectedEmoji = habitToEdit.emoji;
        tvSelectedEmoji.setText(selectedEmoji);
        selectedTime = habitToEdit.notifyTime.isEmpty() ? "08:00" : habitToEdit.notifyTime;
        tvReminderTime.setText(selectedTime);
        switchNotify.setChecked(habitToEdit.notifyEnabled);

        setChipSelected(chipsCategory, habitToEdit.category);
        setChipSelected(chipsPriority, habitToEdit.priority);
        setChipSelected(chipsType, habitToEdit.type);
        setChipSelected(chipsFrequency, habitToEdit.frequency);

        if (habitToEdit.frequency != null && habitToEdit.frequency.startsWith("Custom:")) {
            llCustomDays.setVisibility(View.VISIBLE);
            String[] days = habitToEdit.frequency.replace("Custom:", "").split(",");
            for (String day : days) {
                for (int i = 0; i < chipsDays.getChildCount(); i++) {
                    Chip c = (Chip) chipsDays.getChildAt(i);
                    if (c.getText().toString().equalsIgnoreCase(day)) {
                        c.setChecked(true);
                    }
                }
            }
        }

        llFrequencyContainer.setVisibility(Habit.TYPE_HABIT.equals(habitToEdit.type) ? View.VISIBLE : View.GONE);

        if (habitToEdit.checklist != null) {
            tempChecklist.clear();
            tempChecklist.addAll(habitToEdit.checklist);
            for (ChecklistItem item : tempChecklist) {
                addChecklistItemView(item);
            }
        }

        view.findViewById(R.id.btn_delete).setVisibility(View.VISIBLE);
    }

    private void setChipSelected(ChipGroup group, String text) {
        if (text == null || text.isEmpty()) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(text)) {
                chip.setChecked(true);
                return;
            }
        }
        if (group.getId() == R.id.chips_frequency) {
            View customChip = group.findViewById(R.id.chip_freq_custom);
            if (customChip instanceof Chip) {
                ((Chip) customChip).setChecked(true);
            }
        }
    }

    private void handleCloseAttempt() {
        if (hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> dismiss())
                    .setNegativeButton("Keep Editing", null)
                    .show();
        } else {
            dismiss();
        }
    }

    private boolean hasUnsavedChanges() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        boolean notify = switchNotify.isChecked();
        
        if (habitToEdit == null) {
            if (!name.isEmpty()) return true;
            if (!desc.isEmpty()) return true;
            if (!"🏃".equals(selectedEmoji)) return true;
            if (notify) return true;
            if (llChecklistContainer.getChildCount() > 0) return true;
            return false;
        } else {
            if (!name.equals(habitToEdit.name)) return true;
            String originalDesc = habitToEdit.description != null ? habitToEdit.description : "";
            if (!desc.equals(originalDesc)) return true;
            if (!selectedEmoji.equals(habitToEdit.emoji)) return true;
            if (notify != habitToEdit.notifyEnabled) return true;
            if (notify && !selectedTime.equals(habitToEdit.notifyTime.isEmpty() ? "08:00" : habitToEdit.notifyTime)) return true;
            
            // Check category
            String cat = getSelectedChipText(chipsCategory);
            String origCat = habitToEdit.category != null ? habitToEdit.category : "";
            String cleanedHabitCat = origCat.replaceAll("[^a-zA-Z]", "").trim();
            if (!cat.equalsIgnoreCase(cleanedHabitCat)) return true;

            // Check priority
            String pri = getSelectedChipText(chipsPriority);
            String origPri = habitToEdit.priority != null ? habitToEdit.priority : "";
            if (!pri.equalsIgnoreCase(origPri)) return true;

            // Check type
            String type = getSelectedChipText(chipsType);
            String origType = habitToEdit.type != null ? habitToEdit.type : "";
            if (!type.equalsIgnoreCase(origType)) return true;

            // Check frequency
            String freq = getSelectedChipText(chipsFrequency);
            String currentFreq = freq;
            if ("Custom".equalsIgnoreCase(freq)) {
                List<Integer> ids = chipsDays.getCheckedChipIds();
                StringBuilder sb = new StringBuilder("Custom:");
                for (int id : ids) {
                    Chip c = chipsDays.findViewById(id);
                    sb.append(c.getText()).append(",");
                }
                currentFreq = sb.toString();
            }
            String origFreq = habitToEdit.frequency != null ? habitToEdit.frequency : "";
            if (!currentFreq.equalsIgnoreCase(origFreq)) return true;

            // Check checklist
            int currentChecklistCount = llChecklistContainer.getChildCount();
            int originalChecklistCount = habitToEdit.checklist != null ? habitToEdit.checklist.size() : 0;
            if (currentChecklistCount != originalChecklistCount) return true;
            
            if (habitToEdit.checklist != null) {
                for (int i = 0; i < currentChecklistCount; i++) {
                    View v = llChecklistContainer.getChildAt(i);
                    EditText et = v.findViewById(R.id.et_checklist_title);
                    CheckBox cb = v.findViewById(R.id.checkbox);
                    ChecklistItem origItem = habitToEdit.checklist.get(i);
                    if (!et.getText().toString().equals(origItem.title)) return true;
                    if (cb.isChecked() != origItem.isCompleted) return true;
                }
            }
            
            return false;
        }
    }

    private void saveHabit() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_enter_name, Toast.LENGTH_SHORT).show();
            return;
        }

        Habit h = (habitToEdit != null) ? habitToEdit : new Habit();
        h.name = name;
        h.description = etDesc.getText().toString().trim();
        h.type = getSelectedChipText(chipsType);
        
        String freq = getSelectedChipText(chipsFrequency);
        if ("Custom".equalsIgnoreCase(freq)) {
            List<Integer> ids = chipsDays.getCheckedChipIds();
            StringBuilder sb = new StringBuilder("Custom:");
            for (int id : ids) {
                Chip c = chipsDays.findViewById(id);
                sb.append(c.getText()).append(",");
            }
            h.frequency = sb.toString();
        } else {
            h.frequency = freq;
        }

        h.emoji = selectedEmoji;
        h.category = getSelectedChipText(chipsCategory);
        h.priority = getSelectedChipText(chipsPriority);
        h.notifyEnabled = switchNotify.isChecked();
        h.notifyTime = selectedTime;

        h.checklist.clear();
        for (int i = 0; i < llChecklistContainer.getChildCount(); i++) {
            View v = llChecklistContainer.getChildAt(i);
            EditText et = v.findViewById(R.id.et_checklist_title);
            CheckBox cb = v.findViewById(R.id.checkbox);
            ChecklistItem item = new ChecklistItem(et.getText().toString());
            item.isCompleted = cb.isChecked();
            h.checklist.add(item);
        }

        if (habitToEdit != null) {
            HabitStore.get(requireContext()).update(requireContext(), h);
        } else {
            HabitStore.get(requireContext()).add(requireContext(), h);
        }

        ReminderManager.scheduleReminder(requireContext(), h);

        if (onSaveListener != null) onSaveListener.run();
        dismiss();
    }

    private String getSelectedChipText(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return "";
        Chip chip = group.findViewById(id);
        String text = chip.getText().toString();
        return text.replaceAll("[^a-zA-Z]", "").trim();
    }

    private void showDeleteConfirmation() {
        if (habitToEdit == null) return;

        int titleRes = Habit.TYPE_TASK.equals(habitToEdit.type) ?
                R.string.delete_confirm_title_task : R.string.delete_confirm_title_habit;

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_HabitFlow_Dialog)
                .setTitle(getString(titleRes))
                .setMessage(getString(R.string.delete_confirm_message, habitToEdit.name))
                .setPositiveButton(R.string.btn_confirm_delete, (d, which) -> deleteHabit())
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
              .setTextColor(getResources().getColor(R.color.error_red, null));
    }

    private void deleteHabit() {
        if (habitToEdit != null) {
            final Habit deletedHabit = habitToEdit;
            ReminderManager.cancelReminder(requireContext(), deletedHabit);
            HabitStore.get(requireContext()).delete(requireContext(), deletedHabit.id);
            if (onSaveListener != null) onSaveListener.run();
            dismiss();

            if (getActivity() instanceof MainActivity) {
                View rootView = getActivity().findViewById(android.R.id.content);
                if (rootView != null) {
                    com.google.android.material.snackbar.Snackbar.make(
                        rootView,
                        "Deleted \"" + deletedHabit.name + "\"",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("Undo", v -> {
                        HabitStore.get(rootView.getContext()).add(rootView.getContext(), deletedHabit);
                        if (deletedHabit.notifyEnabled) {
                            ReminderManager.scheduleReminder(rootView.getContext(), deletedHabit);
                        }
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).notifyDataChanged();
                        }
                    }).setActionTextColor(rootView.getContext().getResources().getColor(R.color.primary_blue, null))
                      .show();
                }
            }
        }
    }
}
