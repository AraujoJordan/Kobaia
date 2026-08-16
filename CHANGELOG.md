# Changelog

All notable changes to Kobaia are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] — 2026-08-16

Failure messages that say what was on screen instead, and a set of fixes to the machinery around a
test. No public API was added, removed or renamed.

### Added

- **Assertion failures describe the screen.** A failed `assertVisible` (and every assertion that
  goes through it — the enabled, checked and clickable families) now reports the texts and testTags
  that *were* on screen, how long it looked, and the near misses: a candidate that differs only by
  case, by whitespace, or that contains the target as a substring is called out by name. The screen
  is read only once something has already failed, so a passing suite pays nothing for it.
- **The accessibility tree is dumped alongside the failure screenshot.** Each failed attempt now
  writes a `.xml` of the hierarchy next to its `.png`, through the same `ResultsReporter`. A picture
  of a label that is off by one space does not show the space; the dump does.
- **A test that only passes on a retry says so.** `retryOnFailure` logs a line naming the attempt it
  passed on, so flakiness stops being invisible in a green run.

### Fixed

- **`launch` labelled its retries with the activity, not the test.** Two tests launching the same
  activity logged under one name and overwrote each other's failure screenshots. Both entry points
  now resolve the running test and produce the same `method(Class)` label.
- **Housekeeping could become the failure a test reported.** `clearData()` ran its shared
  preferences, database and file wipes bare, inside the retried block — so an app whose database
  cannot be opened (encrypted, corrupt, or held open exclusively) failed every attempt of every test
  with a housekeeping error before the real assertion ever ran. Each step is now best-effort and
  logs its own failure, matching `resetRotation`.
- **`finishAllActivities` gave up silently.** After its five second budget it fell out of its loop
  with no trace, and the next test started on top of the screen the last one left behind. It now
  logs which activities were still running.
- **`setTextOn` did not verify its fallback.** It confirmed that the fast `setText` path arrived but
  not the character-by-character one, so a field that refused both ways of writing to it left the
  test to discover it several screens later. The fallback is now checked and logged too.
- `UiAutomatorTimeouts.tuneOnce` is no longer a racy check-then-set.
- `finishAllActivities` now sleeps through `KobaiaSleep` like the rest of the library.

## [0.4.4]

See the [release history](https://github.com/AraujoJordan/Kobaia/releases) for earlier versions.

[0.5.0]: https://github.com/AraujoJordan/Kobaia/releases/tag/0.5.0
[0.4.4]: https://github.com/AraujoJordan/Kobaia/releases/tag/0.4.4
