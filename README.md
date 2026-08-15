# Kobaia

**The Android UI test library that reads like the test you would describe out loud.**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.araujojordan/kobaia.svg)](https://central.sonatype.com/artifact/io.github.araujojordan/kobaia)
[![Build](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml/badge.svg)](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen)](LICENSE)

Kobaia makes UI tests shorter, more readable and less flaky. It is built on UIAutomator, so it
waits for the screen instead of making you sleep, drives Views and Jetpack Compose with the same
functions, and can follow the user out of your app and back.

```kotlin
@Test
fun logsIn() = launch<SplashActivity> {
    assertVisible("Kobaia")
    click("SKIP")
    click("LOG IN")
    type("right_email@kobaia.com") into "Enter your email"
    type("12345678") into "Enter your password"
    click("ENTER")
    assertVisible("Welcome to Kobaia!")
}
```

No rule to declare, no `launchActivity()` to remember, and nothing to import but `launch` itself.

<p align="center">
<img src="https://raw.githubusercontent.com/AraujoJordan/Kobaia/master/doc/kobaiaExample.gif" width="1280" alt="A Kobaia test driving the sample app"/>
</p>

- [Download](#download)
- [Your first test](#your-first-test)
  - [Without a rule](#without-a-rule)
  - [With the rule](#with-the-rule)
- [Kobaia's API](#kobaias-api)
  - [Finding views](#finding-views)
  - [Assertions and checks](#assertions-and-checks)
  - [Clicking](#clicking)
  - [Typing](#typing)
  - [Scrolling](#scrolling)
  - [Device, permissions and rotation](#device-permissions-and-rotation)
  - [Leaving your app](#leaving-your-app)
  - [Waiting on purpose](#waiting-on-purpose)
  - [Every function, as an infix function](#every-function-as-an-infix-function)
- [Jetpack Compose](#jetpack-compose)
- [What Kobaia does for you](#what-kobaia-does-for-you)
- [Sample app](#sample-app)
- [Contributing](#contributing)
- [License](#license)

## Download

Kobaia is a test-only dependency, so `androidTestImplementation` keeps it out of your release APK.
Replace `x.x.x` with the version in the Maven Central badge above.

```gradle
androidTestImplementation 'io.github.araujojordan:kobaia:x.x.x'
```

It is published to Maven Central, so ensure `mavenCentral()` is declared in your repositories:

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

And your tests need the AndroidX runner:

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

**Requirements:** `minSdk` 23 · `compileSdk` 37 · JDK 17.

Kobaia already includes UIAutomator, Espresso, the AndroidX test core/runner/rules and JUnit as
`api` dependencies, so you can mix an Espresso assertion or an `ActivityScenario` into a Kobaia
test without declaring anything else.

## Your first test

### Without a rule

`launch` is the whole setup. It clears the app's shared preferences, databases and files, starts
the activity, runs your test, retries it if it fails, and closes the activity afterwards.

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class LoginTest {

    @Test
    fun logsIn() = launch<SplashActivity> {
        click("LOG IN")
        assertVisible("Welcome to Kobaia!")
    }
}
```

Everything Kobaia can do is in scope inside the block, with no imports. So is `scenario`, for what
the interactions do not cover:

```kotlin
@Test
fun logsIn() = launch<SplashActivity> {
    scenario.onActivity { it.viewModel.seedSession() }
    click("LOG IN")
}

@Test
fun opensTheDeepLink() = launch<SplashActivity>(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    flakyAttempts = 1
) {
    assertVisible("GET STARTED")
}

// name the activity instead of passing it as a type argument, if it reads better
@Test
fun logsInAgain() = SplashActivity::class.launch { … }
```

> **Note**
> `launch` clears the app's state inside the test method, which runs *after* `@Before`. Seed what
> your test needs from inside the block, or it will be wiped before the first assertion.

### With the rule

`Kobaia` is also a JUnit `TestRule`, which is what you want when a test composes with other rules,
or does part of its work before the activity exists.

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class LoginTest {

    @get:Rule
    val kobaia = Kobaia(SplashActivity::class.java)   // or Kobaia.create<SplashActivity>()

    @Test
    fun logsIn() {
        kobaia.launchActivity()
        kobaia click "LOG IN"
    }
}
```

```kotlin
Kobaia(SplashActivity::class.java)
Kobaia(SplashActivity::class.java, flakyAttempts = 1)
Kobaia(SplashActivity::class.java, launchActivityAutomatically = true)

kobaia.launchActivity()
kobaia.launchActivity(startIntent = Intent(context, SplashActivity::class.java))
kobaia.launchActivity(waitLimit = 30_000)   // Espresso's idling timeout, in milliseconds
kobaia.activityTestRule                     // the underlying rule, if you need the activity
```

Outside a `launch` block, import the functions you use:

```kotlin
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
```

## Kobaia's API

Every function takes a trailing `wait` — how long, in milliseconds, Kobaia keeps polling the
screen before giving up. It defaults to `5000`, which is why a Kobaia test rarely needs a
`Thread.sleep`. Every `String` also has a `Pattern` twin, except where noted.

### Finding views

```kotlin
find("ENTER")                              // first view with exactly this text
find(Pattern.compile("Hello, .*!"))
findDescription("Enter your email")        // by contentDescription — images, EditTexts, icons
findDescription(Pattern.compile("avatar_\\d+"))
findTag("loginButton")                     // by Compose testTag or View resource id
```

They return a UIAutomator `UiObject2?`, so anything UIAutomator can do is one step away:

```kotlin
find("Terms of use")?.longClick()
val price = findDescription("total")?.text
```

### Assertions and checks

```kotlin
assertVisible("Welcome to Kobaia!")        // fails with "Welcome to Kobaia! should be visible"
assertVisible(Pattern.compile("Welcome, .*"))
assertDescriptionVisible("profile_picture")
assertTagVisible("welcomeBanner")

// ...or not?
assertNotVisible("Wrong credentials!")
assertDescriptionNotVisible("error_icon")
assertTagNotVisible("errorBanner")
```

```kotlin
// state, for checkboxes, switches and buttons that come and go
assertEnabled("ENTER")
assertDisabled("ENTER")
assertChecked("Remember me")
assertUnchecked("Remember me")
assertTagEnabled("loginButton")            // and assertTagDisabled, assertTagChecked, …
```

```kotlin
// ...or just ask, and get a Boolean back
isVisible("Rate this app")
containsText("Welcome")                    // substring, so "Welcome to Kobaia!" counts (no Pattern)
isDescriptionVisible("profile_picture")
isTagVisible("welcomeBanner")
isEnabled("ENTER")
isChecked("Remember me")
isTagEnabled("loginButton")
isTagChecked("acceptCheckbox")
```

> **Note**
> A view that is not there costs the full `wait` before Kobaia gives up, so checking for absence
> with the 5 s default is the most common reason a suite crawls. Pass `QUICK_WAITING_TIME` (50 ms)
> when you expect a miss: `isVisible("Rate this app", wait = QUICK_WAITING_TIME)`.

For something that is on screen **now** and has to leave — a spinner, a splash, a toast — the wait
runs the other way round:

```kotlin
waitUntilGone("Loading…")                  // true once it has gone, false if it never did
waitUntilGone(Pattern.compile("\\d+"))
waitUntilDescriptionGone("progress_spinner")
waitUntilTagGone("loadingSpinner")
```

> **Note**
> `assertNotVisible` and `waitUntilGone` are not the same check with different endings. A longer
> `wait` on `assertNotVisible` makes it *stricter* — it fails if the view shows up at any point
> during it — while `waitUntilGone` is the one that gets more patient. Reach for `assertNotVisible`
> when something must never appear, and `waitUntilGone` when something has to go.

### Clicking

```kotlin
click("SKIP")                              // clicks every view with exactly this text
click(Pattern.compile("(?i)skip"))
click("YOU CAN CLICK ME!", wait = 15_000)  // give a slow screen more time
clickContaining("Log")                     // "LOG IN" counts (no Pattern)
clickDescription("fluffy")
clickTag("loginButton")

longClick("Terms of use")
longClickDescription("fluffy")
longClickTag("greeting")
```

Clicks are forgiving on purpose: if the view never appears, nothing is clicked and the test carries
on. Each one returns whether it clicked anything, which is enough for a screen that may not be
there:

```kotlin
if (!click("Not now", wait = QUICK_WAITING_TIME)) click("Dismiss", wait = QUICK_WAITING_TIME)
```

Assert first when the click has to happen.

### Typing

```kotlin
type("right_email@kobaia.com", into = "Enter your email")   // by contentDescription
typeIntoTag("12345678", tag = "passwordField")              // by Compose testTag
```

To exercise `TextWatcher`s, formatting masks and other typing-driven logic, tap the field open and
send the text one key press at a time, the way a person types it:

```kotlin
typeOnKeyboard("133.37", into = "editField")
typeOnKeyboardIntoTag("133.37", tag = "amountField")
```

```kotlin
clearText(into = "Enter your email")
clearTextInTag("emailField")
```

### Scrolling

```kotlin
scrollTo("SCROLL TO CLICK ME!")
scrollTo(Pattern.compile("Item #\\d+"))
scrollToDescription("footer_logo")
scrollToTag("item40")
scrollToDescription(Pattern.compile("row_\\d+"), maximumScrolls = 20)
```

Each one swipes the first scrollable container — `LazyColumn`, `RecyclerView`, `ListView`,
`ScrollView` — forward until the target shows up, then returns it as a `UiObject2?`. They look
before every swipe, stop as soon as the container reports the end of the list, and give up after
`maximumScrolls` swipes (10 by default).

> **Note**
> Scrolling searches forward from wherever the list currently sits; it does not rewind to the top
> first. Scroll back explicitly if your test has already gone past the target.

### Device, permissions and rotation

```kotlin
pressBack()
pressHome()
pressEnter()
pressRecentApps()
closeKeyboard()                            // presses back, so only call it with a keyboard open
openNotifications()
```

The runtime permission dialog belongs to the system rather than to your app, which is exactly what
UIAutomator can reach and Espresso cannot. Both return whether there was a dialog to answer:

```kotlin
click("Take a photo")
allowPermission()                          // ...or denyPermission()
```

Rotation stays frozen until you put it back, so the app cannot quietly rotate mid-test:

```kotlin
rotateLandscape()
assertVisible("Welcome to Kobaia!")
rotateNatural()                            // back to normal, and free to rotate again
```

All three return once the rotated screen has settled, not merely once the display has turned, so
the activity that was destroyed and recreated is on screen before the next line runs. A screen that
never settles gets two seconds and then the test carries on.

### Leaving your app

`device()` hands you the raw UIAutomator `UiDevice` for anything the functions above do not cover:

```kotlin
pressHome()
device().openQuickSettings()
assertVisible("Your order has shipped")    // reading another app's UI works the same way
click("Kobaia")                            // relaunch from the launcher
```

### Waiting on purpose

Kobaia waits for you, but when you genuinely need to hold — an animation you cannot observe, a
scheduled job — `waitFor` holds the test for exactly as long as you ask, without blocking the app:

```kotlin
waitFor(2000)
```

When what you are waiting for is "the screen to stop moving", `waitForStable` is the number you do
not have to guess. It returns as soon as the screen has held still for half a second, and tells you
whether it settled at all:

```kotlin
waitForStable()             // up to 5000 ms, like everything else
waitForStable(wait = 1500)
```

> **Note**
> It watches what the app reports to the accessibility tree — not the pixels, and not the main
> thread. A screen that animates forever never settles: that costs the full wait and returns
> `false`, the same way a finder that misses does. It never hangs, and it never fails your test on
> its own.

### Every function, as an infix function

Every function above is also an infix function under the same name. Same function, two ways to
write it — pick whichever reads better, and mix them freely:

```kotlin
kobaia click "SKIP"
kobaia assertVisible "Welcome to Kobaia!"
kobaia isVisible "Rate this app"
kobaia scrollTo "Delete account"
kobaia type "12345678" into "Enter your password"
kobaia type "12345678" intoTag "passwordField"
kobaia clickTag "loginButton"
kobaia longClick "Terms of use"
kobaia assertNotVisible "Wrong credentials!"
kobaia assertTagChecked "acceptCheckbox"
kobaia clearText "Enter your email"
kobaia waitFor 2000
```

The ones that take no argument — `pressBack()`, `allowPermission()`, `rotateLandscape()`,
`waitForStable()`, `device()` — are plain functions, available on the rule and inside a `launch`
block alike.

Three things to know: the infix form always uses the default `wait`, it needs a name on its left
(so `click "SKIP"` on its own is a parse error — inside a `launch` block, use the plain call), and
results need parentheses before you can chain:

```kotlin
(kobaia find "Terms of use")?.longClick()
```

## Jetpack Compose

Text and content descriptions need no special treatment — a `Text` is found by its text and an
`Image` by its `contentDescription`, with no setup and no Compose dependency in your test:

```kotlin
assertVisible("Welcome to Kobaia!")
click("LOG IN")
clickDescription("profile_picture")
```

`Modifier.testTag` is the one thing the app under test has to opt into, with a single `semantics`
block on a root composable:

```kotlin
setContent {
    MaterialTheme {
        MyScreen(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }   // androidx.compose.ui.semantics
        )
    }
}
```

With that in place, every tag becomes a target:

```kotlin
findTag("total")
assertTagVisible("welcomeBanner")
clickTag("loginButton")
typeIntoTag("12345678", tag = "passwordField")
scrollToTag("item40")                      // scrolls a LazyColumn like it scrolls a RecyclerView
```

The same functions match a View's resource id — pass the fully qualified name,
`"com.example.app:id/login_button"`, instead of a bare tag.

## What Kobaia does for you

**It waits, so you do not have to sleep.** Every finder polls until the view shows up or the
timeout runs out, which covers animations, slow requests and background work.

**It isolates your tests.** Shared preferences, databases and files are cleared between tests, so a
test that logs in cannot leak a session into the next one. A test that fails every attempt is the
exception: it keeps its state so you can inspect it.

**It retries flaky tests, out loud.** Up to 5 attempts by default — tune it with `flakyAttempts`,
or set it to `1` in CI if you would rather see the flakiness. Every failed attempt is logged with
its stack trace, so a test that only passes on the third try still leaves a trail:

```
W Kobaia: logsIn(com.example.LoginTest) failed on attempt 1 of 5, retrying
```

**And it photographs the failure.** Every failed attempt is captured before anything is torn down
and reported back to the instrumentation, so the screen the test actually saw turns up next to the
result instead of only in your imagination. The files land in the app's external media directory,
one per attempt:

```
logsIn.com.example.LoginTest-attempt-1.png
```

Between attempts it finishes the activities the failed one left behind and waits for them to
actually go away, so the retry starts on a fresh screen. Tests skipped by `assumeTrue` are not
retried — the assumption will not become true on the second try.

**It does not wait longer than it has to.** Before every look at the screen, UIAutomator waits for
the accessibility events to fall quiet — and gives itself ten seconds to see that happen. On a
screen that has settled the wait costs nothing, but a Compose screen with an animation running
never falls quiet, so a `find(text, wait = 50)` spends ten seconds on a single look and the `wait`
you passed stops meaning anything. Kobaia lowers that ceiling to half a second, once, before your
first test: a settled screen behaves exactly as it did, and a moving one stops charging for a
stillness that is never coming. Set `Kobaia.tuneUiAutomatorTimeouts = false` to keep the platform
default.

The knobs, all on `Kobaia`:

| | Default | |
| --- | --- | --- |
| `DEFAULT_WAITING_TIME` | `5000` | How long the finders poll before giving up, in milliseconds. |
| `QUICK_WAITING_TIME` | `50` | The wait to pass when you expect a miss. |
| `DEFAULT_MAXIMUM_SCROLLS` | `10` | How many swipes the scrolling functions get. |
| `DEFAULT_FLAKY_ATTEMPTS` | `5` | How many times a failing test is retried. |
| `DEFAULT_IDLING_LIMIT` | `60000` | Espresso's idling timeout, in milliseconds. |
| `tuneUiAutomatorTimeouts` | `true` | Whether Kobaia lowers UIAutomator's 10 s idle ceiling to 500 ms. |

One more thing, on your side: turning animations off makes any UI suite faster, Kobaia's included.

```gradle
android {
    testOptions {
        animationsDisabled = true
    }
}
```

## Sample app

The [`sample`](sample) module is a runnable app and its test suite.

| | |
| --- | --- |
| [`KobaiaSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaSampleTest.kt) | The login flow, driven end to end by `launch`. Those screens are **Jetpack Compose**, and the test did not change by a line when they were rewritten from XML layouts — nothing in it says which toolkit it drives. |
| [`KobaiaInstrumentedTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaInstrumentedTest.kt) | The rule and the infix flavour, against a **View** screen of deliberately awkward widgets: a button that only becomes clickable after a countdown, a `TextWatcher`, a list to scroll. It also leaves the app and comes back. |
| [`ComposeSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/ComposeSampleTest.kt) | The `testTag` family against a Compose screen with a `TextField` and a `LazyColumn`. |

```bash
./gradlew :sample:connectedDebugAndroidTest
```

## Contributing

Issues and pull requests are welcome. The sample module is where behaviour is proven, so a change
to an interaction should come with a case in one of its tests, and both of these should pass before
you open the PR:

```bash
./gradlew build                              # library, sample and lint
./gradlew :sample:connectedDebugAndroidTest  # the tests, on a device or emulator
```

## License

Released under the [MIT License](LICENSE). Copyright © 2020 Jordan L. A. Junior.
