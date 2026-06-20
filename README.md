<div align="center">

# HabitFlow
### Habit tracking Android app

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![Android](https://img.shields.io/badge/Android-11%2B-green.svg)](https://developer.android.com/) [![Java](https://img.shields.io/badge/java-11-blue.svg)](https://www.oracle.com/java/) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/auraflaa/Habit-Tracking-Android-Application/pulls)

</div>

---

# HabitFlow

HabitFlow is a modern, intuitive habit-tracking application designed to help users build consistency and achieve their personal goals. It combines a clean Material Design 3 interface with powerful tracking features and cloud synchronization.

## 🚀 Key Features

*   **Dual Tracking**: Support for both recurring **Habits** (daily/custom days) and one-time **Tasks**.
*   **Visual Progress**:
    *   **Activity Heatmap**: A GitHub-style contribution grid to see long-term consistency.
    *   **Bar Charts**: Weekly and monthly trends for habit completion.
    *   **Streak System**: Real-time tracking of current and best streaks with fire badges.
*   **Dynamic Theming**: 6 beautiful built-in themes:
    *   Dark (Default), Light, Ocean, Sunset, Forest, and high-contrast **Amoled**.
    *   Subtle outlines on UI containers for perfect visibility in dark modes.
*   **Smart Organization**: 
    *   Categorize habits (Fitness, Learning, Wellness, etc.).
    *   Priority levels (High, Medium, Low) with visual indicators.
    *   Subtask checklists for complex habits.
*   **Data Security**:
    *   **Firebase Authentication**: Secure sign-in via Email or Google.
    *   **Cloud Sync**: Realtime Database synchronization to keep your habits safe across devices.
    *   **Offline First**: Full functionality even without an internet connection using local SQLite storage.

## 🎨 UI Components

The app leverages **Material Design 3** to provide a premium user experience:
*   **CoordinatorLayout & ViewPager2**: For smooth navigation and transitions between the Today, Calendar, Progress, and Settings screens.
*   **Bottom Sheets**: Interactive panels for creating and editing habits.
*   **Custom View Drawing**: Hand-crafted `HeatmapView` and `BarChartView` for data visualization.
*   **Confirmation Dialogs**: Standardized Material dialogs for destructive actions (Delete/Logout).

## 🛠 Tech Stack

*   **Language**: Java (Android)
*   **UI Framework**: Material Design 3, XML Layouts
*   **Database**: SQLite (Local) & Firebase Realtime Database (Cloud)
*   **Auth**: Firebase Auth & Google Sign-In
*   **Serialization**: Google Gson
*   **Architecture**: Singleton pattern with DAO (Data Access Object) for data persistence.

## 📂 Project Structure

```
app/src/main/
├── java/com/habitflow/
│   ├── activities/    # Splash, Login, and Main Activity
│   ├── adapters/      # Recycler adapters for Habits and Reminders
│   ├── data/          # HabitStore, SQLite Helper, and Firebase Sync
│   ├── fragments/     # Main screens (Home, Progress, Calendar, Settings)
│   ├── model/         # Data classes (Habit, ChecklistItem)
│   ├── util/          # ThemeManager, NotificationHelper, ReminderManager
│   └── views/         # Custom UI views (Charts/Heatmaps)
└── res/
    ├── color/         # State selectors for UI elements
    ├── drawable/      # Background shapes and icons
    ├── layout/        # XML screen definitions
    └── values/        # Themes, colors, and strings
```

## 🏁 Getting Started

### Prerequisites

*   Android Studio Ladybug (or newer)
*   JDK 17+
*   Android device/emulator running API 26 (Android 8.0) or higher.

### Installation
1.  Clone the repository:
    ```sh
    git clone https://github.com/auraflaa/Habit-Tracking-Android-Application.git
    ```
2.  Open the project in Android Studio.
3.  Perform a **Gradle Sync**.

### Enabling Firebase (Mandatory for Cloud Sync)
By default, the Google Services plugin is disabled in `app/build.gradle` to allow the project to compile without keys. To enable it:
1.  Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2.  Add an Android App with the package name `com.habitflow`.
3.  Download the `google-services.json` and place it in the `app/` directory.
4.  In `app/build.gradle`, uncomment the plugin line:
    ```gradle
    id 'com.google.gms.google-services'
    ```
5.  In `res/values/strings.xml`, update the `default_web_client_id` with your actual Web Client ID from the Firebase console.
6.  **Clean & Rebuild** the project.

---

*Built with ❤️ for a better routine.*
