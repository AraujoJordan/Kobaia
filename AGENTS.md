# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## What this is

Kobaia is an Android UI test **library** (published to JitPack) built on UIAutomator2, plus a
`sample` app that exists to exercise it. Everything user-facing is the API surface — a change that
compiles is not necessarily a change that is acceptable, because every public name is something
someone's test file already imports.

## Commands

Requires JDK 17 (the build pins `jvmTarget`/`sourceCompatibility` to 17; a newer default JDK will
fail).

```bash
./gradlew build                              # what CI runs: assemble + lint + unit tests, both modules
./gradlew :kobaia:assembleDebug              # the library alone
./gradlew :kobaia:jacocoTestReport           # coverage XML for Codecov
./gradlew lint                               # lint only
```

Tests come in two kinds, and the useful ones need a device:

```bash
./gradlew :kobaia:testDebugUnitTest          # host-side; near-empty, they prove nothing about the lib
./gradlew :sample:connectedDebugAndroidTest  # the real tests — needs a device/emulator attached

# a single instrumented test class or method
./gradlew :sample:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.araujo.jordan.kobaiasample.KobaiaSampleTest#testApp
```

With no device attached, compiling the instrumented sources is the fastest real check that an API
change works from a caller's point of view — the sample tests are written to cover every calling
style:

```bash
./gradlew :sample:compileDebugAndroidTestKotlin :kobaia:compileDebugAndroidTestKotlin
```

## Architecture

The library is four files in `kobaia/src/main/java/com/araujo/jordan/kobaia/`, and the split
between them is the thing to understand before editing.

**`Kobaia.kt` — the companion object is the implementation.** Every interaction is a function on
`Kobaia.Companion` (`find`, `click`, `assertVisible`, `type`, `scrollTo`, `waitFor`, `device`, …),
which is what users static-import. Each one takes a trailing `wait: Long = DEFAULT_WAITING_TIME`.
The `String` and `Pattern` overloads, and the text/description variants, all funnel through two
private `BySelector` helpers — `findFirst` and `clickAll` — so a behaviour change belongs in those,
not in five overloads.

**`KobaiaInteractions.kt` — one declaration serves both call styles.** Each interaction appears
once more here as an `infix fun` that delegates to the companion. A Kotlin infix member is also
callable normally, so this single declaration provides `kobaia click "SKIP"` *and* `click("SKIP")`.
Both `Kobaia` (the rule) and `KobaiaScope` (the `launch` block receiver) implement this interface,
which is how the two entry points stay identical. **Adding an interaction means adding it in both
`Kobaia.kt` and `KobaiaInteractions.kt`**, under the same name.

**Two entry points, one behaviour.**

- `Kobaia<T>` in `Kobaia.kt` is the JUnit `TestRule`: `@get:Rule val kobaia = Kobaia(X::class.java)`
  then `kobaia.launchActivity()`. It chains `FlakyTestRule` → `ActivityTestRule` → `ClearDataRule`.
- `launch<X> { }` / `X::class.launch { }` in `KobaiaLaunch.kt` needs no rule. It reimplements the
  same guarantees around an `ActivityScenario` — clear state, launch, retry on failure, clear again
  on success — because a lambda can be wrapped the same way a JUnit `Statement` can.

Both paths call the same `AppUnderTest` object in `KobaiaRules.kt` for `clearData()` (shared prefs,
databases, files) and `finishAllActivities()`. Keep it that way: the two entry points must not
drift. One deliberate asymmetry, documented in the README: the rule clears state around `@Before`,
while `launch` clears inside the test method, so `@Before` seeding survives the rule but not
`launch`.

**The sample is the test suite.** `sample/src/androidTest/…/KobaiaSampleTest.kt` covers the
`launch` style and `KobaiaInstrumentedTest.kt` covers the rule + infix style, against the
activities in `sample/src/main/`. `SplashActivity → LandingActivity → LoginActivity →
WelcomeActivity` is a login flow; `KobaiaTestActivity` is a single screen of deliberately awkward
widgets (a button that only becomes clickable after a 5 s countdown, a `TextWatcher`, a scrollable
list). New library behaviour should get a case in one of these.

## Conventions

**Renaming a public function is additive.** The old name stays as a `@Deprecated` delegate carrying
a `ReplaceWith`, so existing test suites keep compiling and the IDE quick fix migrates them. See the
block at the end of `Kobaia`'s companion object, and the migration table in the README.

**Names describe the action, not the mechanism** — `click`, not `textClick`; `find`, not `byText`.
The same name has to read well both plainly and infix (`click("SKIP")` / `kobaia click "SKIP"`),
which is why e.g. the substring variants are `containsText` / `clickContaining`.

**Interactions never throw on a miss**, except the `assert*` family. A click that finds nothing
returns `false` and the test carries on; finders return `null`. That is why waits are generous by
default (5000 ms) — a negative check should pass a short `wait` explicitly.

**The README is API documentation.** It carries the cheat sheet, the migration table and an example
per function group, so an API change is not finished until the README matches.
