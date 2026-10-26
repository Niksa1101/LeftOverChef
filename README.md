# LeftOverChef

A native Android app that turns ingredients you already have into recipe ideas. Built independently in Kotlin as a university project.

## What it does

- **Pantry:** add, edit, categorize, and search ingredients with quantities and units.
- **Recipe search:** find recipes from pantry ingredients through the Spoonacular API and narrow the results with filters.
- **Recipe details:** view ingredients, instructions, and nutrition; save recipes for later.
- **Cook mode:** follow instructions one step at a time, adjust servings, and use a cooking timer.
- **Calorie log:** record meals and track daily calories, protein, carbohydrates, and fat.
- **Local accounts:** register and sign in on the device, with an optional email password-reset demo.

## Stack

| Area | Tools |
| --- | --- |
| Language and UI | Kotlin, XML layouts, Material Components, View Binding |
| App state | ViewModel, LiveData, Kotlin coroutines |
| Local storage | Room / SQLite with database migrations |
| Networking | Retrofit, Gson, Spoonacular API |
| Images | Glide |
| Build | Gradle 8.7, Android Gradle Plugin 8.5.2 |

Activities and fragments render the UI. `MainViewModel` manages screen data and asynchronous work, while `RecipeRepository` connects recipe and pantry operations to Room and Retrofit.

## Run locally

1. Open the project in Android Studio with JDK 17 or 21 and Android SDK 35 installed.
2. Let Android Studio create `local.properties` with the SDK location.
3. Copy the entries from `local.properties.example` into that file and set your own Spoonacular API key.
4. Sync Gradle and run the `app` configuration on Android 8.0 (API 26) or later.

Recipe search requires an internet connection and a valid Spoonacular key. API availability and quotas depend on your account. The app builds without keys, but remote features require configuration. SendGrid settings are optional and used only by the password-reset demo; the sender must be verified in your SendGrid account.

On Windows, build and run the included local unit test with:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

On macOS or Linux:

```bash
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest
```

## Project structure

```text
app/src/main/java/com/leftoverchef/app/
├── auth/         Local account and session helpers
├── data/api/     Retrofit service and API models
├── data/db/      Room database, DAOs, and migrations
├── data/model/   Local entities
├── email/        Password-reset email integration
├── repository/   Recipe and pantry data access
├── ui/           Screens grouped by feature
└── viewmodel/    UI state and asynchronous operations
```

## Project scope

This is a completed university project, not a production authentication service. Accounts and their data are stored on the device, and the session-signing key in this public version is a demo value. Production use would require server-side authentication, stronger password storage, and moving privileged API calls (especially email delivery) to a backend. Keeping credentials in `local.properties` prevents committing them; it does not hide them inside a compiled Android app.

The repository includes the original starter unit and instrumentation tests; it does not claim comprehensive test coverage.

## Author

**Nikša Halas** · [Portfolio](https://www.niksahalas.com/) · [GitHub](https://github.com/Niksa1101)
