# NutriLens: AI Macro Tracker

An Android app that uses Claude AI to analyze food from photos or descriptions and track daily macros — calories, protein, carbs, and fat.

---

## Features

### Free Tier
- Log food entries manually with name, serving size, and full macro breakdown
- View today's calorie summary with an animated progress bar
- See today's food log with per-entry macro pills

### Pro (NutriLens Pro)
- **AI Food Scanner** — photograph any meal or describe it in plain text; Claude AI estimates macros automatically
- **Macro Breakdown** — protein, carbs, and fat progress bars on the home screen
- **Custom Daily Goals** — set precise calorie and macro targets with quick presets (Cut / Maintain / Bulk)
- **Full Food Log History** — browse and manage every date you've tracked, not just today
- **Unlimited Entries** — no cap on daily food logs

### Subscriptions
- Weekly, Monthly, Yearly, and Lifetime purchase options via Google Play Billing

---

## Requirements

| Item | Minimum |
|---|---|
| Android version | 8.0 (API 26) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Android Studio | Hedgehog or newer |
| Kotlin | 1.9+ |
| Java | 11 |
| Anthropic API key | Required for AI analysis |
| Google Play Billing | Required for Pro purchases (real device) |

---

## Setup & Build Instructions

### 1. Clone the repository

```bash
git clone <repo-url>
cd "NutriLens: AI Macro Tracker"
```

### 2. Add your Anthropic API key

Create or edit `local.properties` in the project root:

```properties
sdk.dir=/path/to/your/Android/Sdk
ANTHROPIC_API_KEY=sk-ant-your-key-here
```

> The key is injected at compile time via `BuildConfig.ANTHROPIC_API_KEY`. It is never committed to version control.

### 3. Open in Android Studio

Open the project root directory in Android Studio. Sync Gradle when prompted.

> **Note:** The project directory name contains a colon (`:`). Gradle redirects the build output to `/tmp/nutrilens-build` to work around a Java classpath limitation. This is handled automatically in `app/build.gradle.kts`.

### 4. Build and run

- **Debug:** Run `app` on an emulator or physical device (Android 8.0+)
- **Release:** `./gradlew assembleRelease` (requires a signing keystore configured in `build.gradle.kts`)

### 5. Pre-launch checklist

Before submitting to Google Play:

- [ ] Replace placeholder URLs in `PaywallScreen.kt`:
  - `PRIVACY_POLICY_URL` — must be a live, publicly accessible URL
  - `TERMS_OF_SERVICE_URL` — must be a live, publicly accessible URL
- [ ] Configure real Google Play product IDs in `BillingManager.kt`
- [ ] Set up a signing keystore for release builds
- [ ] Test purchases on a real device with a test Google account

---

## Project Structure

```
app/src/main/java/com/factory/nutrilensaimacrotracker/
│
├── MainActivity.kt                 # App entry point; applies theme, sets up navigation
├── NutriLensApp.kt                 # Application class; holds database, billing, and premium singletons
│
├── ai/
│   ├── ClaudeAIService.kt          # Anthropic API integration (image + text analysis via OkHttp)
│   └── FoodAnalysisResult.kt       # AnalysisState sealed class and FoodAnalysisResult data class
│
├── billing/
│   ├── BillingManager.kt           # Google Play Billing connection, product queries, purchases
│   └── PremiumManager.kt           # Persists premium status locally using DataStore Preferences
│
├── data/
│   ├── database/
│   │   ├── NutriLensDatabase.kt    # Room database (FoodEntry + DailyGoal tables)
│   │   ├── FoodEntryDao.kt         # Queries: daily entries, totals by date, all logged dates
│   │   ├── DailyGoalDao.kt         # Upsert single goal record
│   │   └── DailyTotals.kt          # Aggregated macro totals data class
│   ├── model/
│   │   ├── FoodEntry.kt            # Room entity: name, macros, serving, date, timestamp
│   │   └── DailyGoal.kt            # Room entity: target calories, protein, carbs, fat
│   └── repository/
│       ├── FoodRepository.kt       # Wraps FoodEntryDao with date-aware Flow queries
│       └── GoalRepository.kt       # Goal CRUD with defaults fallback via Flow
│
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt           # Today's calorie summary, macro card (Pro), food log
│   │   ├── ScanScreen.kt           # Camera/gallery capture + text description + AI analysis results
│   │   ├── AddFoodScreen.kt        # Manual food entry form with IME focus chain
│   │   ├── FoodLogScreen.kt        # Date-browsable food history (Pro) or today-only (free)
│   │   ├── GoalsScreen.kt          # Set daily macro targets (Pro) with quick presets
│   │   └── PaywallScreen.kt        # Subscription plans, feature highlights, purchase CTA
│   ├── components/
│   │   ├── FoodEntryCard.kt        # Food entry row with macro pills and delete action
│   │   └── MacroProgressCard.kt    # MacroProgressBar + CalorieSummaryCard composables
│   ├── navigation/
│   │   └── NutriLensNavigation.kt  # Screen routes and bottom nav item definitions
│   └── theme/
│       ├── Color.kt                # Brand + macro colors; full Material 3 light/dark schemes
│       ├── Theme.kt                # NutriLensTheme with dynamic color (Android 12+) support
│       └── Type.kt                 # Material 3 typography scale
│
└── viewmodel/
    ├── HomeViewModel.kt            # Today's entries, totals, goal; delete action
    ├── ScanViewModel.kt            # AI analysis state machine; save to database
    ├── FoodLogViewModel.kt         # Selected date state; entries and totals for that date
    ├── GoalsViewModel.kt           # Daily goal update
    └── PaywallViewModel.kt         # Billing connection, plan selection, purchase, restore
```

---

## Architecture

- **Pattern:** MVVM with Jetpack Compose
- **State management:** `StateFlow` + `collectAsStateWithLifecycle`
- **Local database:** Room (SQLite) with KSP code generation
- **Async:** Kotlin coroutines throughout ViewModels and repositories
- **Theming:** Material 3 with dynamic color on Android 12+, full light/dark mode
- **AI:** Anthropic Claude API over HTTPS using OkHttp; images are scaled to max 1024 px and base64-encoded before sending
- **Billing:** Google Play Billing Library 6.1
- **Permissions:** Camera (optional feature), media read (gallery picker)

---

## Tech Stack

| Library | Purpose |
|---|---|
| Jetpack Compose + Material 3 | UI |
| Room + KSP | Local database |
| CameraX | Camera preview and image capture |
| Coil | Image loading |
| OkHttp | Anthropic API HTTP client |
| Gson | JSON parsing |
| DataStore Preferences | Premium status persistence |
| Google Play Billing KTX 6.1 | Subscriptions and one-time purchases |
| Accompanist Permissions | Runtime permission handling |
| Splash Screen API | Android 12+ splash screen |
