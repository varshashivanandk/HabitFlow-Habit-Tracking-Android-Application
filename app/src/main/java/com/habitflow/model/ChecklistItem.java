package com.habitflow.model;

import java.io.Serializable;
import java.util.UUID;

public class ChecklistItem implements Serializable {
    public String id;
    public String title;
    public boolean isCompleted;

    public ChecklistItem() {
        this.id = UUID.randomUUID().toString();
        this.isCompleted = false;
    }

    public ChecklistItem(String title) {
        this();
        this.title = title;
    }
}
