# HabitFlow: Product and Project Metadata

## Problem Statement
Maintaining consistent personal habits and routines can be challenging in a fast-paced world. Individuals often lose motivation without clear visibility into their progress and struggle to track habits across multiple devices seamlessly. HabitFlow addresses this by providing an intuitive, offline-first habit tracking application that ensures individuals can monitor daily tasks, visualize their streaks via heatmaps, and synchronize their progress across devices in real-time, thereby fostering accountability and consistency.

## 4. Tools and Technologies

### 4.1 Programming Language
- **Java**: Primary programming language utilized for application architecture and business logic.
- **Kotlin**: Utilized in Gradle configuration and secondary module logic.
- **XML**: Used extensively for designing responsive and dynamic Android UI layouts and vector drawables.

### 4.2 IDE
- **Android Studio**: The primary Integrated Development Environment (IDE) used to write, compile, profile, and debug the application.

### 4.3 Database
- **SQLite (Local)**: Used via `SQLiteOpenHelper` to persist habit data locally, ensuring complete offline functionality.
- **Firebase Realtime Database (Cloud)**: Used as the primary backend infrastructure to synchronize user habits bidirectionally across devices in real-time.

### 4.4 APIs and Technologies Used
- **Firebase Authentication**: For managing secure user identity and session states via Email/Password and Google OAuth.
- **ZenQuotes API**: An external REST API used to fetch daily motivational quotes dynamically.
- **Android AlarmManager & BroadcastReceiver**: Native Android framework tools used for the precision scheduling of local device notifications.

## 5. Implementation

### 5.1 Project Modules
The project architecture is organized by feature and functionality into the following distinct packages:
- `activities`: Core UI entry points (e.g., `MainActivity`, `SplashActivity`).
- `adapters`: Components responsible for binding data arrays to UI lists (e.g., `HabitAdapter`, `MainPagerAdapter`).
- `data`: Database architecture and cloud synchronization management (`HabitStore`, `HabitDao`, `FirebaseSyncManager`).
- `fragments`: Modular user interface screens (`HomeFragment`, `CalendarFragment`, `SettingsFragment`).
- `model`: Plain Old Java Objects (POJOs) representing business entities (`Habit`, `ChecklistItem`).
- `util`: Global utilities and background services (`ReminderReceiver`, `ThemeManager`).

### 5.2 Application Features
- Multi-user secure authentication and data isolation.
- Habit and subtask creation with customizable tracking frequencies, emojis, and priority levels.
- Granular analytical tracking through heatmap progress visualization and active streak calculation.
- Daily automated system-level reminder notifications.
- Extensive theming support (Light, Dark, Ocean, AMOLED) driven by dynamic attributes.

### 5.3 UI Screens and Navigation
- **Bottom Navigation Bar**: Anchors the user experience, allowing quick swapping of modular fragments.
- **Home Screen (`HomeFragment`)**: A daily interactive checklist displaying habits for the current day. Features swipe-to-delete with Undo capabilities.
- **Calendar Screen (`CalendarFragment`)**: A chronological overview showing monthly completions via an interactive grid system.
- **Progress Screen (`ProgressFragment`)**: Statistical data visualization representing overall app usage and specific habit adherence metrics.
- **Settings Screen (`SettingsFragment`)**: Configuration options for themes, profile management, and notification schedules.

### 5.4 API & Backend Integration
- **REST Integration**: The application executes an HTTP GET request to `https://zenquotes.io/api/random` utilizing `HttpURLConnection` within a background `ExecutorService`. The resulting JSON payload is parsed to display daily inspiration without blocking the main UI thread.
- **Firebase Integration**: The application implements real-time listeners (`ValueEventListener`) via the Firebase SDK to monitor changes in the cloud and reflect them immediately in the local SQLite cache.

## 6. Testing

### 6.1 Test Cases
- **Authentication Resilience**: Validating successful login, secure handling of invalid credentials, and account recovery paths.
- **Offline Mode Integrity**: Disabling internet connectivity, modifying habit states, and validating that changes sync automatically and accurately upon network restoration without data collision.
- **Theme Persistence**: Dynamically changing display themes and ensuring correct color attributes load consistently on application restart.
- **Notification Precision**: Scheduling custom reminder intervals and validating that the `BroadcastReceiver` intercepts the alarm precisely on time, regardless of whether the app is active or killed.

### 6.2 Results and Debugging
- Extensively utilized Android Studio's **Logcat** to trace Firebase connection states, monitor HTTP API responses, and diagnose synchronization latency.
- Debugged complex UI layout alignments using the **Layout Inspector**, particularly refining the Calendar grid weightings to ensure an even geometric distribution of days across devices with varying screen densities.

---

# Android Concepts Study Guide: HabitFlow

This section explores core Android development concepts, ranging from basic UI components to advanced backend synchronization. Each concept includes a detailed description, a reference to its implementation in this project, and an in-depth explanation of how the code works to serve as comprehensive study material.

## 1. Basic: Efficient List Rendering with RecyclerView

**Description:**
Android uses `RecyclerView` to display large datasets or long lists of UI elements efficiently. Creating a new View object in memory for every single item in a list (especially one with thousands of entries) would consume immense memory and cause UI frame stuttering (lag). `RecyclerView` solves this by creating only enough views to fill the screen. As the user scrolls, views that disappear off the top of the screen are "recycled" and moved to the bottom to display the new incoming data.

**Code Reference:** `app/src/main/java/com/habitflow/adapters/HabitAdapter.java`

```java
public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitVH> {
    // ...
    @NonNull
    @Override
    public HabitVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the XML layout for a single list item (item_habit.xml)
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitVH holder, int position) {
        // Binds the underlying data object to the recycled view holder
        Habit h = habits.get(position);
        holder.tvName.setText(h.name);
        holder.tvEmoji.setText(h.emoji);
        // ... (updates UI state for completion, streaks, etc.)
    }
}
```

**Explanation:**
- `Adapter`: The bridge between your underlying data array (`habits`) and the UI visually representing them.
- `onCreateViewHolder`: Called *only* when the RecyclerView needs a brand new view to represent an item (e.g., when initially loading the screen). It inflates the XML layout into a Java View object.
- `onBindViewHolder`: Called much more frequently. As the user scrolls, old views that disappeared are passed back into this method with a new `position` integer. The method's job is to update the text, colors, and images of that old view to represent the new data, avoiding the costly operation of inflating XML again.
- `ViewHolder (HabitVH)`: A static inner class that caches the results of `findViewById()` lookups. Calling `findViewById` is an expensive operation that traverses the view hierarchy; caching these references inside the ViewHolder ensures smooth 60fps scrolling performance.

## 2. Intermediate: Local Persistence with SQLite

**Description:**
To ensure an application remains fully functional without an internet connection (Offline-First architecture), data must be persisted to the device's local file system. Android provides built-in support for SQLite, a lightweight relational database. Developers typically use `SQLiteOpenHelper` to manage database creation, schema definitions, and version migrations safely.

**Code Reference:** `app/src/main/java/com/habitflow/data/db/HabitDbHelper.java` and `HabitDao.java`

```java
public class HabitDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "habitflow.db";
    private static final int DATABASE_VERSION = 1;

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Defines the schema and creates the table
        String createTable = "CREATE TABLE " + TABLE_HABITS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_COMPLETED_TODAY + " INTEGER)";
        db.execSQL(createTable);
    }
}
```

**Explanation:**
- `SQLiteOpenHelper`: A structural helper class that abstracts away the complexity of opening database connections and manages the lifecycle of the database file on the Android file system.
- `onCreate`: Triggered automatically the very first time the app attempts to access the database. Here, we execute raw SQL commands to construct our relational tables (like `habits` and `checklists`).
- `onUpgrade`: (Not shown in snippet) Handles schema migrations. If an update to the app requires a new database column, the developer increments `DATABASE_VERSION`. The system then detects the mismatch and triggers `onUpgrade` to alter the tables without losing the user's existing data.
- **DAO (Data Access Object)**: Located in `HabitDao.java`, this pattern abstracts raw SQL queries into clean Java methods. It uses `db.query()` to read and `db.insert()`/`db.update()` to write, leveraging Android's `ContentValues` to safely map Java variables to database columns, protecting against SQL injection.

## 3. Intermediate: Background Processing (BroadcastReceivers)

**Description:**
Android enforces strict background execution limits to preserve battery life. However, certain features—like triggering a daily habit reminder at exactly 9:00 AM—require the app to wake up and execute code even when the user isn't actively using it. A `BroadcastReceiver` is an Android component designed to listen for and respond to these specific system-wide or scheduled announcements.

**Code Reference:** `app/src/main/java/com/habitflow/util/ReminderReceiver.java`

```java
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Called when the AlarmManager triggers the intent
        String habitName = intent.getStringExtra("habit_name");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "habit_reminders")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Habit Reminder")
                .setContentText("Don't forget to: " + habitName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(notificationId, builder.build());
    }
}
```

**Explanation:**
- `AlarmManager`: Used elsewhere in the project to schedule exact times. It holds a `PendingIntent` that acts as a token, granting the Android system permission to fire an `Intent` on behalf of our app at a future time.
- `onReceive`: The singular entry point for the receiver. When the scheduled time arrives, the OS wakes the app up for a few seconds and executes this block on the main thread.
- `Intent Extras`: The `Intent` passed in contains string data (`habit_name`) injected when the alarm was originally scheduled, allowing the receiver to display context-specific information.
- `NotificationCompat`: A backward-compatible builder class used to construct the visual notification. It requires a specific Notification Channel ID (e.g., `"habit_reminders"`) which is mandatory in Android 8.0+ for categorizing user alerts.

## 4. Advanced: Modular UI with Fragments and ViewPager2

**Description:**
Modern Android architecture favors a single-activity design, where one `MainActivity` acts as a host container that dynamically swaps out different UI "Screens" known as `Fragments`. This makes the codebase highly modular and adaptable to tablet layouts. `ViewPager2` is a specialized container that allows users to gesture horizontally to swipe between these fragments.

**Code Reference:** `app/src/main/java/com/habitflow/activities/MainActivity.java`

```java
ViewPager2 viewPager = findViewById(R.id.view_pager);
BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

// Adapter that provides Fragments to the ViewPager
MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
viewPager.setAdapter(pagerAdapter);

// Syncs ViewPager swipes with the Bottom Navigation Bar
viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
    @Override
    public void onPageSelected(int position) {
        bottomNav.getMenu().getItem(position).setChecked(true);
    }
});
```

**Explanation:**
- `Fragment`: A self-contained portion of the user interface with its own distinct lifecycle (e.g., `onCreateView` for inflating layouts, `onViewCreated` for setting up listeners). This isolation means `HomeFragment` doesn't need to know anything about `CalendarFragment`.
- `MainPagerAdapter`: A specialized adapter extending `FragmentStateAdapter`. Instead of recycling UI Views like a standard RecyclerView, it instantiates and manages the lifecycle of entire Fragments based on the user's current swipe position.
- `registerOnPageChangeCallback`: A listener that actively monitors physical swipe gestures. When a user swipes left to a new page, it intercepts the event and programmatically highlights the corresponding icon in the `BottomNavigationView`, ensuring the UI tab indicators stay perfectly synchronized with the active fragment.

## 5. Advanced: Real-time Cloud Synchronization

**Description:**
To provide a seamless cross-device experience, modern mobile apps must synchronize their local state with a cloud backend. Firebase Realtime Database is a cloud-hosted NoSQL database where data is stored as JSON and synchronized in real-time to every connected client.

**Code Reference:** `app/src/main/java/com/habitflow/data/FirebaseSyncManager.java`

```java
public class FirebaseSyncManager {
    private DatabaseReference getHabitsRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Generates path: /users/{user_id}/habits
        return FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("habits");
    }

    public void syncHabitToCloud(Habit habit) {
        getHabitsRef().child(habit.id).setValue(habit)
            .addOnSuccessListener(aVoid -> Log.d("Sync", "Successfully synced: " + habit.name))
            .addOnFailureListener(e -> Log.e("Sync", "Failed to sync: " + habit.name));
    }
}
```

**Explanation:**
- `FirebaseAuth`: Ensures that the operation is securely tied to the verified identity of the current user.
- `DatabaseReference`: Acts as a pointer to a specific node in the remote JSON tree. By structuring the path as `users -> {uid} -> habits`, the database enforces strict multi-tenant data isolation—users can only query their own data branch.
- `setValue(habit)`: Firebase utilizes reflection to automatically serialize the Java `Habit` object into a JSON dictionary and pushes it to the cloud. 
- **Offline Capabilities**: If `setValue` is called while the device is in an airplane mode, the Firebase SDK queues the operation locally. The success listener won't fire immediately, but the moment network connectivity is re-established, the SDK transparently executes the queued push in the background, making it remarkably robust for mobile environments.
