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
./gradlew build                              # assemble + lint, both modules
./gradlew :kobaia:assembleDebug              # the library alone
./gradlew lint                               # lint only
```

There are no host-side unit tests, and there is no point adding any: every interaction goes
through a device. The suite is the sample's instrumented tests, and coverage comes from that same
run — `:kobaia:jacocoTestReport` measures this module's classes against execution data the sample
writes on the device.

```bash
./gradlew :sample:connectedDebugAndroidTest  # the tests — needs a device or emulator attached
./gradlew :kobaia:jacocoTestReport           # the same run, plus a coverage report for the library

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

**What cannot exist here.** Kobaia sees the accessibility tree, not the view hierarchy, so
anything needing the `View` object — drawables, backgrounds, text colours, `SeekBar` progress,
`TextInputLayout` errors, adapter positions — is out of reach by construction, however often it is
asked for. What UIAutomator can reach and Espresso-based libraries cannot is the other half of the
trade: system permission dialogs, rotation, the notification shade, other apps.

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
block at the end of `Kobaia`'s companion object. That annotation is the whole migration story — the
README documents the API as it is now, and deliberately does not list the old names.

**Names describe the action, not the mechanism** — `click`, not `textClick`; `find`, not `byText`.
The same name has to read well both plainly and infix (`click("SKIP")` / `kobaia click "SKIP"`),
which is why e.g. the substring variants are `containsText` / `clickContaining`.

**Nothing in the interaction path may wait on main-thread idleness.** Kobaia drives the app from
outside its process, and a Compose screen with a running animation is never idle, so an
Espresso-style idle wait would hang rather than fail. `KobaiaSleep` is a plain sleep for that
reason; Espresso survives only as `IdlingPolicies` in the two launch paths, for users who mix
Espresso assertions into a test.

`waitForStable` is the one thing that looks like an exception and is not: it watches the
accessibility tree rather than the main thread, it is bounded by the caller's `wait`, and a screen
that never settles makes it return false instead of hanging. Keep both of those — the
`requireStableScreenshot = false` argument in particular, since comparing pixels would mean a
blinking cursor counts as motion.

Internally it is used **once**, in `settleAfterRotation`, and the reason is the test for whether it
belongs anywhere else. UIAutomator returns from a rotation when the display has swapped its width
and height, which is well before the recreated activity has drawn, so Kobaia would otherwise be
reporting a state change as finished while the screen is still mid-change. Everywhere else the
finders already poll for what they want, and stability is both weaker than that and slower: it
answers "nothing is moving", not "the thing is here". Do not put it in `scrollUntilFound` — a
500 ms settle per swipe would make scrolling an order of magnitude slower — and do not put it in
the launch paths, where it would tax every test in the suite for a race the finders already win.

**A miss costs the full `wait`.** Nothing on screen means polling until the timeout, so the
5000 ms default is what makes suites slow. `QUICK_WAITING_TIME` (50 ms) is the constant to reach
for when a test probes for something it expects to be absent. The scrolling functions are bounded
the same way: `maximumScrolls` is a swipe budget, and `scrollUntilFound` checks before every swipe
rather than delegating to a scroll-into-view helper that rewinds to the top and swipes dozens of
times per call.

**Only the `By`/`UiObject2` half of UIAutomator.** Nothing here goes through `UiObject`,
`UiSelector` or `UiScrollable` — scrolling finds its container with `By.scrollable(true)` and
swipes it with `UiObject2.scroll`. Reaching back for the legacy classes would put
`Configurator`'s `waitForSelectorTimeout` and `scrollAcknowledgmentTimeout` — which only those
classes read — underneath everything again.

**The idle ceiling is the one that matters, and it is not obvious.** `UiDevice.getWindowRoots()`
opens with `waitForIdle()`, and so does `UiObject2.getAccessibilityNodeInfo()` — so *every* poll of
*every* finder, and every click, text read and state check on a view already found, first waits for
the accessibility events to go quiet for 500 ms, bounded by `Configurator.getWaitForIdleTimeout()`.
That default is **10 seconds**, which on a screen that never goes quiet is charged in full to a
`wait` the caller asked to be 50 ms. `UiAutomatorTimeouts.tuneOnce()` lowers it to 500 ms from both
entry points, and that single line is worth more to suite speed than everything else in this file.
Leave it applied from `Kobaia.apply()` and `launch()`, the two places that run before any
interaction.

**Interactions never throw on a miss**, except the `assert*` family. A click that finds nothing
returns `false` and the test carries on; finders return `null`. That is why waits are generous by
default (5000 ms) — a negative check should pass a short `wait` explicitly.

**Setting text can be refused, and UIAutomator only mentions it in the log.**
`UiObject2.setText` performs one accessibility action and, when the field says no, writes
`performAction(ACTION_SET_TEXT) failed` and returns `void`. A Compose `TextField` reached by its
content description refuses it. `setTextOn` therefore checks the text arrived and types it in when
it did not — clearing first, so a field that masks what it holds does not end up with the text
twice. Anything new that writes into a field belongs behind that helper, not on `setText` directly.

**A view can go stale between being found and being used.** The `click`/`longClick` family matches
every view at once and then acts on them one at a time, so the first one navigating away leaves the
rest pointing at nodes that no longer exist — `UiObject2` throws `StaleObjectException` for that,
which would break the rule above. `actOnAll` catches it per view and carries on. Any new
interaction that acts on a *list* of views needs the same treatment.

**Absence has two meanings and they run opposite ways.** `assertNotVisible` polls `Until.hasObject`
and fails the moment the view appears, so a longer `wait` makes it *stricter*; `waitUntilGone`
polls `Until.gone` and returns once the view has left, so a longer `wait` makes it more patient.
The default waits differ for that reason (50 ms and 5000 ms). Do not "fix" one to match the other.

**Never reach into `androidx.test.uiautomator.internal`, directly or through a helper that does.**
Its `UtilsKt` resolves an external storage directory in the same class initialiser as its cached
`UiDevice` and `KeyCharacterMap`, and throws an `Error` when no storage is mounted — which takes
every other field down with it, permanently. That is why `typeCharacterByCharacter` loads its own
`KeyCharacterMap` instead of calling `UiDevice.type`, and why `FailureScreenshot` catches
`Throwable` rather than `Exception` around `ResultsReporter`.

**The README is API documentation.** It carries the cheat sheet and an example per function group,
so an API change is not finished until the README matches.
