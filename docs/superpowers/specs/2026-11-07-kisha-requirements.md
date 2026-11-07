# Kisha — Functional Requirements

7 Nov 2026 · @Ben

## Overview

Kisha (കീശ, "pocket") is an open-source, offline-first Android expense tracker that answers one question on its home screen: how much can I spend today? It is a functional clone of the daily-allowance idea behind Chelav, built as a fully native Android app (Kotlin, Jetpack Compose, minSdk 29) with Indian defaults. No cross-platform frameworks (Flutter, React Native, Compose Multiplatform UI), no WebView screens, and no PWA wrapper.

**v1 scope:** set up a budget period, log expenses in seconds, see today's allowance update live, review history, and back up or export data. Everything works without an account or internet.

**Non-goals for v1:** bank account linking, investment tracking, multi-currency wallets, cloud sync (optional sync is planned for Later), SMS or notification parsing, ads, and any analytics or tracking SDK.

**Open-source principles:** Apache-2.0 licence, no proprietary dependencies (F-Droid buildable), all data stays on the device unless the user exports it, and original name, branding and UI copy — nothing copied from Chelav.

## Allowance calculation

Today's allowance is the money left for flexible spending in the current period, divided by the days remaining, minus what has already been spent today.

$$\text{base}_d = \frac{I - F - S_{<d}}{D_d}$$

$$\text{today} = \text{base}_d - S_d$$

Where I = period income (or budget), F = fixed costs and savings set aside for the period, S<d = flexible spending before today, Sd = spending so far today, and Dd = days left in the period including today.

### Rules

1. The period is either a calendar month or a custom cycle starting on a chosen payday (e.g. the 1st, 7th or last working day).
2. Fixed costs (rent, EMI, subscriptions) and a savings target are reserved at the start of the period and never appear in the daily number.
3. The base allowance is recalculated at local midnight, so underspending raises tomorrow's number and overspending lowers it (default "spread" mode).
4. Optional "carry" mode: unspent money from a day rolls only into the next day instead of spreading across the period.
5. Extra income logged mid-period is added to I and spread over the remaining days.
6. If the period's flexible money is exhausted, today shows a negative number and the deficit, not zero.
7. Today's figure updates instantly when an expense is added, edited or deleted.

Both behaviours are user toggles in Settings (ST-3): unspent money either spreads across the period or carries to the next day, and overspending either reduces the rest of the period or only shows today as negative, with tomorrow starting fresh.

### Worked example (spread mode)

| Item | Value |
|---|---|
| Income for November | ₹60,000 |
| Fixed costs + savings | ₹36,000 |
| Flexible money | ₹24,000 |
| Days in period | 30 |
| Day 1 allowance | ₹800 |
| Spent on day 1 | ₹500 |
| Day 2 allowance | (24,000 − 500) ÷ 29 = ₹810 |

## Functional requirements

There are 63 requirements across 13 areas, tagged MVP (first installable build), v1 (first public release) or Later.

### 1. Onboarding

| ID | Requirement | Priority |
|---|---|---|
| ON-1 | First launch shows a 3–4 step setup with no sign-up or account. | MVP |
| ON-2 | User enters period income or a total budget for the period. | MVP |
| ON-3 | User picks the period type: calendar month or custom cycle with a start day. | MVP |
| ON-4 | User adds fixed costs (name, amount); this step can be skipped. | MVP |
| ON-5 | User sets an optional savings target per period. | v1 |
| ON-6 | Setup ends on a preview of today's allowance before entering the app. | MVP |
| ON-7 | User can restore from a backup file instead of setting up fresh. | v1 |

### 2. Home (Today)

| ID | Requirement | Priority |
|---|---|---|
| HM-1 | Home shows today's remaining allowance as the dominant number. | MVP |
| HM-2 | The number changes colour state: on track, low (under 20% left), over (negative). | MVP |
| HM-3 | Home shows money left in the period and days left. | MVP |
| HM-4 | Home lists today's expenses, newest first, with swipe to delete and undo. | MVP |
| HM-5 | A prominent add button opens quick entry in one tap. | MVP |
| HM-6 | A small trend shows the last 7 days against their allowance. | v1 |
| HM-7 | Tomorrow's projected allowance is shown when today is underspent or overspent. | v1 |

### 3. Expense entry

| ID | Requirement | Priority |
|---|---|---|
| EX-1 | Quick entry opens with a numeric keypad focused; amount plus save takes two taps. | MVP |
| EX-2 | Optional fields: category, note, date and time (defaults to now). | MVP |
| EX-3 | User can edit or delete any past expense; the allowance recalculates. | MVP |
| EX-4 | Keypad supports simple arithmetic (e.g. 120+45). | v1 |
| EX-5 | Recent and frequent expenses can be re-added with one tap. | v1 |
| EX-6 | An expense can be marked as not affecting the allowance (e.g. reimbursable). | v1 |
| EX-7 | Entry is reachable from a launcher shortcut and a Quick Settings tile. | v1 |

### 4. Income

| ID | Requirement | Priority |
|---|---|---|
| IN-1 | User can log extra income mid-period; it spreads over the remaining days. | MVP |
| IN-2 | User can edit the period's base income; the allowance recalculates. | MVP |
| IN-3 | Recurring income (salary) auto-fills each new period. | v1 |

### 5. Fixed costs and recurring bills

| ID | Requirement | Priority |
|---|---|---|
| FC-1 | User manages a list of fixed costs reserved each period. | MVP |
| FC-2 | Each fixed cost has a name, amount, and optional due day. | MVP |
| FC-3 | User can mark a bill as paid; paid status resets each period. | v1 |
| FC-4 | Bill reminders notify N days before the due day. | v1 |
| FC-5 | Non-monthly bills (yearly insurance) are spread into a monthly reserve. | Later |

### 6. Categories

| ID | Requirement | Priority |
|---|---|---|
| CT-1 | The app ships default categories: food, transport, groceries, shopping, bills, health, entertainment, other. | MVP |
| CT-2 | User can add, rename, reorder, hide categories and pick an icon and colour. | v1 |
| CT-3 | Deleting a category reassigns its expenses to another category. | v1 |
| CT-4 | Optional soft limits per category for the period, shown as progress. | Later |

### 7. History

| ID | Requirement | Priority |
|---|---|---|
| HS-1 | History lists expenses grouped by day, each day showing spent vs allowance. | MVP |
| HS-2 | User can browse previous periods. | MVP |
| HS-3 | Search by note and filter by category, amount range and date range. | v1 |
| HS-4 | Calendar view colours each day by on track, under or over. | Later |

### 8. Insights

| ID | Requirement | Priority |
|---|---|---|
| IS-1 | Period summary: total spent, saved vs plan, average per day. | v1 |
| IS-2 | Spending by category for the period (chart and list). | v1 |
| IS-3 | Comparison with the previous period. | Later |
| IS-4 | Streak of days kept under allowance. | Later |

### 9. Budget periods

| ID | Requirement | Priority |
|---|---|---|
| BP-1 | A new period starts automatically on the cycle date, copying income and fixed costs. | MVP |
| BP-2 | At period end the user sees a short summary and chooses what happens to leftover money: savings or carry into the next period. | v1 |
| BP-3 | User can change cycle settings; changes apply from the next period. | v1 |
| BP-4 | Allowance math uses the device's local time zone and handles date and time zone changes safely. | MVP |

### 10. Notifications

| ID | Requirement | Priority |
|---|---|---|
| NT-1 | Optional daily reminder at a chosen time to log expenses. | v1 |
| NT-2 | Optional morning notification with today's allowance. | v1 |
| NT-3 | All notifications are off by default and use separate channels. | v1 |

### 11. Widgets

| ID | Requirement | Priority |
|---|---|---|
| WG-1 | Home-screen widget shows today's allowance and refreshes after each change and at midnight. | v1 |
| WG-2 | Widget has an add button that opens quick entry. | v1 |

### 12. Settings

| ID | Requirement | Priority |
|---|---|---|
| ST-1 | Currency symbol and number format, defaulting to ₹ with lakh grouping (1,00,000). | MVP |
| ST-2 | Theme: system, light, dark, built on Material 3 Expressive colour schemes; dynamic colour on Android 12+, a Kisha fallback scheme on Android 10–11. | v1 |
| ST-3 | Two allowance toggles: unspent money spreads across the period or carries to the next day (default spread); overspending reduces the rest of the period or only shows today as negative (default reduce). | v1 |
| ST-4 | Language: English and Malayalam. | v1 |
| ST-5 | First day of week setting for weekly views. | Later |

### 13. Data, backup and privacy

| ID | Requirement | Priority |
|---|---|---|
| DT-1 | All data is stored locally; the app has no INTERNET permission in v1. | MVP |
| DT-2 | Export expenses to CSV through the system file picker. | v1 |
| DT-3 | Full backup and restore to a single JSON file, versioned for future migrations. | v1 |
| DT-4 | Optional scheduled automatic backup to a user-chosen folder. | Later |
| DT-5 | Optional app lock with biometric or device PIN. | v1 |
| DT-6 | "Erase all data" with confirmation. | v1 |
| DT-7 | Import from CSV with column mapping. | Later |
| DT-8 | Optional, opt-in sync across devices (e.g. self-hosted WebDAV or a Syncthing folder); the app stays fully usable without it. | Later |

## Data model

Six tables in a local Room (SQLite) database. Amounts are stored as Long in paise to avoid floating-point errors.

| Entity | Key fields | Notes |
|---|---|---|
| BudgetConfig | periodType (MONTH, CUSTOM), cycleStartDay, baseIncome, savingsTarget, allowanceMode (SPREAD, CARRY), currency | Single row; drives new periods |
| Period | id, startDate, endDate, income, fixedTotal, savingsTarget, leftoverAction, closedAt | One row per cycle; snapshot so history stays correct after settings change |
| Expense | id, periodId, amountPaise, categoryId, note, occurredAt, affectsAllowance, createdAt, updatedAt | Core table; index on occurredAt |
| Income | id, periodId, amountPaise, note, occurredAt, recurring | Extra income spreads over remaining days |
| FixedCost | id, name, amountPaise, dueDay, active, paidInPeriodId | Template copied into each period's fixedTotal |
| Category | id, name, icon, colour, sortOrder, hidden, isDefault | Seeded on first launch |

Today's allowance is always derived from these tables at read time, never stored, so edits to past expenses can't leave it stale.

## Non-functional requirements

| ID | Area | Requirement |
|---|---|---|
| NF-1 | Platform | minSdk 29 (Android 10), targetSdk = latest stable; tested on Android 13. |
| NF-2 | Offline | Every feature works with no network; no INTERNET permission in v1. |
| NF-3 | Performance | Cold start to Home under 1 s on a mid-range phone; saving an expense updates Home in under 100 ms. |
| NF-4 | Size | Release APK under 10 MB. |
| NF-5 | Reliability | Database migrations are tested; no data loss across app updates. |
| NF-6 | Accessibility | TalkBack labels on all controls, 48 dp touch targets, supports font scaling to 200%, WCAG AA contrast. |
| NF-7 | Localization | All strings in resources; English and Malayalam at v1; Indian number grouping via ICU. |
| NF-8 | Privacy | No analytics, ads, crash-reporting SDKs or Google Play Services dependency. |
| NF-9 | Distribution | Reproducible builds; publishable on F-Droid, GitHub Releases, and optionally Play Store. |
| NF-10 | Quality | Allowance engine is a pure Kotlin module with unit tests covering month lengths, leap years, payday cycles, overspend and time zone change. |

## UI design: Material 3 Expressive only

Every screen uses Material 3 Expressive and nothing else: no custom design system, no XML views, no third-party UI kits.

| ID | Requirement |
|---|---|
| UI-1 | The app root uses MaterialExpressiveTheme with the expressive motion scheme; all components come from androidx.compose.material3 expressive APIs. |
| UI-2 | Colour, type and shape come only from theme tokens; no hard-coded colours, font sizes or corner radii in screens. |
| UI-3 | Home's allowance uses emphasized display typography; period progress uses a wavy progress indicator. |
| UI-4 | The add action is a FAB menu (expense, income) on Home; quick entry opens in a modal bottom sheet. |
| UI-5 | Choice controls (period type, allowance mode, history filters) use connected button groups. |
| UI-6 | Navigation uses the short navigation bar on phones and a navigation rail on wide screens. |
| UI-7 | Loading states use the expressive loading indicator; pressed buttons and selected chips use shape morphing. |
| UI-8 | Motion respects the system "remove animations" setting. |
| UI-9 | Expressive APIs marked experimental are opted into in one place, and the Compose BOM is pinned so updates are deliberate. |

### Design principles

Kisha applies M3 Expressive with restraint: Google's own research cautions that money apps like banking need more caution than media apps, and a sizeable minority of users prefer calmer designs. Expressiveness goes where it speeds up the core loop (see today's number, log an expense), not into decoration.

The principles below come from Google's research on M3 Expressive, which ran 46 studies with over 18,000 participants. In eye-tracking tests, people found key UI elements up to 4× faster in expressive designs, and age gaps in finding them largely disappeared for users 45 and over.

| ID | Principle (from the research) | How Kisha applies it |
|---|---|---|
| DP-1 | Use colour, shape, size, motion and containment to make the key action stand out. | Today's allowance is the largest element on Home; the add FAB is large, in a strong container colour, and sits in thumb reach at the bottom. |
| DP-2 | Put primary actions where the user's attention and hands already are. | Quick entry's Save button sits directly above the keypad, not in a top app bar. |
| DP-3 | Group related elements in visible containers. | Today's expenses, period summary and bills each live in their own surface container, so Home scans as three blocks. |
| DP-4 | Keep familiar patterns; never trade clarity for flair. | History stays a plain vertical list grouped by day; no novel layouts for core screens. |
| DP-5 | Keep text labels on actions. | Navigation items, button groups and the FAB menu always show labels, not icons alone. |
| DP-6 | Exceed accessibility minimums for touch targets and contrast. | Keypad keys and primary buttons are larger than 48 dp; state colours (on track, low, over) also use an icon and text, never colour alone. |
| DP-7 | Use motion to give feedback and make waits feel shorter. | Expressive spring motion when the allowance number updates after saving; the wavy progress indicator for period progress. |
| DP-8 | Start from user needs and iterate with real users. | Test the MVP with 3–5 people on the two core tasks (check today's number, log an expense) before polishing visuals. |

Sources: Expressive Design: Google's UX Research · Material Design 3 guidelines

## Tech stack and milestones

A standard modern Android stack keeps Kisha easy for contributors and F-Droid friendly.

| Layer | Choice |
|---|---|
| Language | Kotlin (100% native Android, no cross-platform layer) |
| UI | Jetpack Compose + Material 3 Expressive only |
| Architecture | MVVM with a separate :core:allowance pure-Kotlin module |
| Storage | Room (SQLite) + DataStore for settings |
| DI | Manual DI (an AppContainer in the Application class), no Hilt |
| Async | Coroutines + Flow |
| Background | WorkManager for midnight refresh, reminders, auto-backup |
| Widget | Jetpack Glance |
| Testing | JUnit, Turbine, Compose UI tests |
| CI | GitHub Actions: lint, tests, signed release APK |

### Milestones

1. **MVP (installable build):** all MVP requirements — setup, Home, quick entry, history by day, fixed costs, auto period rollover, ₹ formatting.
2. **v1 (first public release):** backup and export, widget, notifications, categories editing, insights, Malayalam, app lock, F-Droid submission.
3. **Later:** optional sync (DT-8), calendar view, category limits, CSV import, yearly bills, shared budgets.

## Decisions

Decided:

- Licence: Apache-2.0.
- Allowance rollover: user toggle between spread and carry (ST-3).
- Overspending: user toggle between reducing the rest of the period and showing only today negative (ST-3).
- SMS and notification parsing: removed from scope; no READ_SMS or notification-listener permission.
- Sync: optional, planned for Later (DT-8).
- Dependency injection: manual DI through an AppContainer created in the Application class; no Hilt or Dagger. Revisit only if the wiring outgrows one file.
