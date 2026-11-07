# Changelog

All notable changes to Kisha. Versions follow [Semantic Versioning](https://semver.org); each release's notes on GitHub are taken from its section here.

## [0.1.0] - 2026-11-08

First public release.

### Added
- **Today's allowance**: income minus fixed costs, spread across the days left in the period and recalculated every midnight. Underspending raises tomorrow's number; overspending lowers it.
- **Bold Home screen**: the number on a full-colour header that turns teal, amber or red as the day goes, always with words and an icon too.
- **The pocket**: each finished day's unspent allowance drops in with a coin animation; overspending takes it back out. Includes an under-budget streak.
- **Quick entry**: two-tap logging with a large keypad, categories, notes and date/time. The colour previews what today will look like after the expense.
- **History**: one tile per day with spent vs allowance and what the day added to or took from the pocket; browse previous periods.
- **Setup** in four steps, with calendar-month or payday cycles (any day of the month or the last working day) and fixed costs.
- **Settings**: base income, extra income, fixed costs with optional due day, currency symbol and Indian or international number grouping.
- Undo for deleted expenses, confirmation before deleting fixed costs or income.
- Light and dark themes in Material 3 Expressive.

### Privacy
- No internet permission, no account, no analytics or ads, no Google Play Services. Cloud backup is disabled.

### Known limitations
- If you set up partway through a period, today's number assumes nothing was spent earlier in that period.
- No backup/export yet. Uninstalling the app deletes your data.
