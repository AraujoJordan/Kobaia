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

**Three families of selector.** Every interaction comes in a text flavour (`find`, `click`), a
content-description flavour (`findDescription`, `clickDescription`) and a testTag flavour
(`findTag`, `clickTag`) — the last one is how Compose is supported, since `Modifier.testTag`
surfaces in the accessibility tree as a resource id once the app opts in with
`semantics { testTagsAsResourceId = true }`. Nothing about Compose needs a Compose dependency in
the library: UIAutomator reads the accessibility tree, so a `Text` and a `TextView` look the same
from here. The Compose dependencies in `sample/build.gradle` (and the Compose compiler plugin in
the root build file) exist only so the sample can prove it.

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
`launch` style, `KobaiaInstrumentedTest.kt` covers the rule + infix style, and
`ComposeSampleTest.kt` covers the testTag family against `ComposeSampleActivity`, all using the
activities in `sample/src/main/`. `SplashActivity → WelcomeActivity → LandingActivity →
LoginActivity` is a login flow, written in **Jetpack Compose**; `KobaiaTestActivity` is a single
screen of deliberately awkward **View** widgets (a button that only becomes clickable after a 5 s
countdown, a `TextWatcher`, a scrollable list), and `ComposeSampleActivity` is its Compose
counterpart (testTags, a `TextField`, a 40-item `LazyColumn`). Keep both toolkits represented —
that the same test drives either one is the point being demonstrated. New library behaviour should get a case in one of these.

## Conventions

**Renaming a public function is additive.** The old name stays as a `@Deprecated` delegate carrying
a `ReplaceWith`, so existing test suites keep compiling and the IDE quick fix migrates them. See the
block at the end of `Kobaia`'s companion object, and the migration table in the README.

**Names describe the action, not the mechanism** — `click`, not `textClick`; `find`, not `byText`.
The same name has to read well both plainly and infix (`click("SKIP")` / `kobaia click "SKIP"`),
which is why e.g. the substring variants are `containsText` / `clickContaining`.

**Nothing in the interaction path may wait on main-thread idleness.** Kobaia drives the app from
outside its process, and a Compose screen with a running animation is never idle, so an
Espresso-style idle wait would hang rather than fail. `KobaiaSleep` is a plain sleep for that
reason; Espresso survives only as `IdlingPolicies` in the two launch paths, for users who mix
Espresso assertions into a test.

**Nothing in the interaction path may wait on main-thread idleness.** Kobaia drives the app from
outside its process, and a Compose screen with a running animation is never idle, so an
Espresso-style idle wait hangs rather than fails. `KobaiaSleep` is a plain sleep for that reason;
Espresso survives only as `IdlingPolicies` in the two launch paths, for users who mix Espresso
assertions into a test.

**A miss costs the full `wait`.** Nothing on screen means polling until the timeout, so the
5000 ms default is what makes suites slow. `QUICK_WAITING_TIME` (50 ms) is the constant to reach
for when a test probes for something it expects to be absent. The scrolling functions are bounded
the same way: `maximumScrolls` is a swipe budget, and `scrollUntilFound` checks before every swipe
rather than delegating to `UiScrollable.scrollIntoView`, which rewinds to the top and swipes up to
30 times per call. `UiAutomatorTimeouts.tuneOnce()` in `KobaiaRules.kt` lowers UIAutomator's own
global timeouts (10 s per selector, 1 s per swipe) once from both entry points — leave it applied
from `Kobaia.apply()` and `launch()`, which are the two places that run before any interaction.

**Interactions never throw on a miss**, except the `assert*` family. A click that finds nothing
returns `false` and the test carries on; finders return `null`. That is why waits are generous by
default (5000 ms) — a negative check should pass a short `wait` explicitly.

**The README is API documentation.** It carries the cheat sheet, the migration table and an example
per function group, so an API change is not finished until the README matches.
