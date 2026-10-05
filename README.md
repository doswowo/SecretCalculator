# SecretCalculator

A privacy calculator app that looks like a normal calculator but hides a lightweight browser.

## Features

- **Calculator**: Full-featured calculator with basic operations
- **Secret Browser**: Enter result (default: 123456) after pressing "=" to open browser
- **Browser Features**:
  - WebView-based lightweight browser
  - History tracking
  - Bookmark/favorites
  - Forward/Back navigation
  - Refresh/Stop page loading
  - Home button

## How to Use

1. Open the app - you'll see a normal calculator
2. Do any calculation (e.g., 100 + 23 = 123)
3. Press the result number (123456)
4. Press "=" again
5. If password matches, browser opens!

## Default Password

The default secret code is **123456**

To change it, modify this line in `CalculatorActivity.kt`:
```kotlin
val storedCode = getSharedPreferences("secret", MODE_PRIVATE).getInt("code", 123456)
```

## Build

This project uses GitHub Actions to automatically build the APK on every push.

The APK will be available as a GitHub Actions artifact after the build completes.
