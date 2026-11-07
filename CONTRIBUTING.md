# Contributing to Kisha

Thanks for helping! A few ground rules keep Kisha small, private and dependable.

## Principles

- **Offline and private.** Kisha has no internet permission. Changes that add network access, analytics, ads, crash reporting or Google Play Services won't be merged.
- **Free dependencies only.** Everything must be open source so Kisha can be built for F-Droid.
- **Money is `Long` paise**, never `Float` or `Double`.
- **The maths lives in `core/allowance`.** It's pure Kotlin and every rule in it has a unit test. Change the tests first.
- **Material 3 Expressive only.** Colours, type and shapes come from the theme (`MaterialTheme` or `KishaTheme`), not hard-coded values in screens.
- **Accessible by default.** Labels on every control, touch targets of at least 48 dp, and status never shown by colour alone.

## Workflow

1. Open an issue first for anything bigger than a small fix, so we can agree on the approach.
2. Write a failing test, make it pass, and keep the suite green:
   ```sh
   ./gradlew :core:allowance:test :app:testDebugUnitTest :app:lintDebug
   ./gradlew :app:connectedDebugAndroidTest   # on a phone or emulator
   ```
3. Database changes need a schema version bump, an exported schema in `app/schemas/`, and a migration test.
4. Keep pull requests focused, with one change per PR, and describe what a user will notice.
