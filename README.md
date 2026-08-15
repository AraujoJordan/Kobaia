# Kobaia

**Android UI testing in Kotlin, without the boilerplate.**

[![JitPack](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
[![Build](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml/badge.svg)](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen)](LICENSE)

Kobaia is a UI test library built on UIAutomator. It gives you a small, discoverable API that
reads like a description of what the user does, and it waits for the screen so you do not have to.
Because it works through the accessibility tree rather than the view hierarchy, the same test
drives Views and Jetpack Compose, and can follow the user out of your app and back.

```kotlin
@Test
fun logsIn() = launch<SplashActivity> {
    assertVisible("Kobaia")
    click("SKIP")
    click("GET STARTED")
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

**Contents** — [Why Kobaia](#-why-kobaia) · [Install](#-install) · [Usage](#-usage) ·
[API reference](#-api-reference) · [How it behaves](#-how-it-behaves) ·
[Migrating](#-migrating-from-an-older-version) · [Sample app](#-sample-app)

## 🚀 Why Kobaia

- **Tests read like user behaviour.** High-level functions — `click`, `assertVisible`, `type … into`
  — keep a test about what the user does, not about how the view tree is built.
- **It waits for the UI so you do not.** Every finder polls the screen until the element shows up
  or the timeout runs out, so animations, slow requests and background work rarely need a sleep.
- **It can leave your app.** Press home, open the notification shade, read another app's screen,
  come back. The same functions work everywhere, because none of them are tied to your process.
- **Views and Compose, the same way.** A `TextView` and a `Text` look identical from the
  accessibility tree, and Compose `testTag`s are first-class targets.

## 📦 Install

**Requirements:** `minSdk` 23 · `compileSdk` 37 · JDK 17

<details open>
<summary><b>Step 1</b> — add the JitPack repository</summary>

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

On an older project whose repositories live in the root `build.gradle`, add the same
`maven { url 'https://jitpack.io' }` line to `allprojects { repositories { … } }` instead.

</details>

<details open>
<summary><b>Step 2</b> — add the dependency</summary>

Kobaia is a test-only dependency, so `androidTestImplementation` keeps it out of your release APK.
Replace `x.x.x` with the version in the JitPack badge above.

```gradle
// build.gradle (module: app)
dependencies {
    androidTestImplementation 'com.github.AraujoJordan:Kobaia:x.x.x'
}
```

</details>

<details open>
<summary><b>Step 3</b> — point the instrumentation runner at AndroidX</summary>

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

</details>

Kobaia brings its stack with it — UIAutomator, Espresso, the AndroidX test core/runner/rules and
JUnit are exposed as `api` dependencies, so you can mix an Espresso assertion or an
`ActivityScenario` into a Kobaia test without declaring anything else.

## 📖 Usage

### Starting a test

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

Inside the block every interaction is available with no import, and `scenario` is there for
whatever they do not cover:

```kotlin
@Test
fun logsIn() = launch<SplashActivity> {
    scenario.onActivity { activity -> activity.viewModel.seedSession() }
    click("LOG IN")
}
```

Naming the activity instead of passing it as a type argument reads better in some tests:

```kotlin
@Test
fun logsIn() = SplashActivity::class.launch { … }
```

| Argument | Default | What it does |
| --- | --- | --- |
| `startIntent` | `null` | Intent used to start the activity, instead of a plain launch. |
| `flakyAttempts` | `5` | How many times a failing test is retried before it is reported as failed. |
| `waitLimit` | `60000` | Espresso's master and idling-resource timeout, in milliseconds. Only relevant if you mix Espresso interactions into the test. |

```kotlin
@Test
fun opensTheDeepLink() = launch<SplashActivity>(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    flakyAttempts = 1
) {
    assertVisible("GET STARTED")
}
```

> **Note**
> `launch` clears the app's state *inside* the test method, which runs after `@Before`. Seed
> whatever your test needs from inside the block, or it will be wiped before the first assertion.

### Starting a test with the rule

`Kobaia` is also a JUnit `TestRule`, which is what you want when a test has to compose with other
rules, or to do part of its work before the activity exists.

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class LoginTest {

    @get:Rule
    val kobaia = Kobaia(SplashActivity::class.java)

    @Test
    fun logsIn() {
        kobaia.launchActivity()
        kobaia click "LOG IN"
    }
}
```

The rule wires up an `ActivityTestRule`, retries flaky tests, and clears the app's state between
tests. Its constructor takes `flakyAttempts` (default `5`) and `launchActivityAutomatically`
(default `false`); there is a reified factory, `Kobaia.create<SplashActivity>()`, if you prefer it.

With `launchActivityAutomatically = false`, you start the activity from inside the test, which
leaves room to set up a mock server or intent extras first:

```kotlin
kobaia.launchActivity(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    waitLimit = 30_000
)
```

The underlying rule is exposed as `kobaia.activityTestRule` if you need the activity instance.

### Calling the interactions

Inside a `launch` block the interactions are simply in scope. Everywhere else they live on
`Kobaia`'s companion object, so import the ones you need:

```kotlin
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
```

Every one of them is also an **infix function** under the same name. Both forms are the same
function — pick whichever reads better, and mix them freely:

```kotlin
click("SKIP")                                       // imported, or inside a launch block
kobaia click "SKIP"                                 // infix, through the rule

assertVisible("Welcome to Kobaia!")
kobaia assertVisible "Welcome to Kobaia!"

type("12345678", into = "Enter your password")
kobaia type "12345678" into "Enter your password"
```

Every function takes a trailing `wait` — how long, in milliseconds, Kobaia keeps polling before
giving up. It defaults to `5000`, which is why a Kobaia test rarely needs a `Thread.sleep`.

Three things worth knowing about the infix form: it always uses the default `wait`, it needs a
name on its left (so `click "SKIP"` alone is a parse error — inside a `launch` block, use the plain
call), and results need parentheses before you can chain on them:

```kotlin
(kobaia find "Terms of use")?.longClick()
click("YOU CAN CLICK ME!", wait = 15_000)
```

### Finding views

Returns a UIAutomator `UiObject2?`, or `null` if nothing showed up within `wait`.

```kotlin
find("ENTER")                            // first view whose text is exactly "ENTER"
find(Pattern.compile("Hello, .*!"))      // …or matches a regex
findDescription("Enter your email")      // by contentDescription — images, EditTexts, icons
findDescription(Pattern.compile("avatar_\\d+"))
```

Because you get the raw `UiObject2` back, anything UIAutomator can do is one step away:

```kotlin
find("Terms of use")?.longClick()
val price = findDescription("total")?.text
```

### Checking what is on screen

The checks return a `Boolean`; `assertVisible` fails the test with a readable message.

```kotlin
assertVisible("Welcome to Kobaia!")      // fails with "Welcome to Kobaia! should be visible"
assertVisible(Pattern.compile("Welcome, .*"))

isVisible("Rate this app", wait = 1000)
containsText("Welcome")                  // substring match, so "Welcome to Kobaia!" counts
isDescriptionVisible("profile_picture")
```

> **Note**
> A view that is not there costs the full `wait` before Kobaia gives up, so checking for absence
> with the 5 s default is the most common reason a suite crawls. Pass `QUICK_WAITING_TIME` (50 ms)
> when you expect a miss.

### Interacting

Clicks are forgiving on purpose: if the element never appears, nothing is clicked and the test
carries on. Assert first when the click has to happen.

```kotlin
click("SKIP")                             // clicks every view with that exact text
click(Pattern.compile("(?i)skip"))
click("YOU CAN CLICK ME!", wait = 15_000) // give a slow screen more time
clickContaining("Log")                    // clicks views containing "Log", e.g. "LOG IN"
clickDescription("fluffy")                // click by contentDescription
```

Each one returns whether it clicked anything, which is enough to handle a label that may or may
not be there without a second lookup:

```kotlin
if (!click("Not now", wait = QUICK_WAITING_TIME)) click("Dismiss", wait = QUICK_WAITING_TIME)
```

Filling a field takes the text and the content description of the field it goes into:

```kotlin
type("right_email@kobaia.com", into = "Enter your email")
kobaia type "right_email@kobaia.com" into "Enter your email"
```

To exercise `TextWatcher`s, formatting masks and other typing-driven logic, type through the real
soft keyboard, one character at a time:

```kotlin
typeOnKeyboard("133.37", into = "editField")
```

### Scrolling

Each of these swipes the first scrollable container (`LazyColumn`, `RecyclerView`, `ListView`,
`ScrollView`, …) forward until the target is visible, then returns it as a `UiObject2?`. They look
before every swipe, so a target already on screen costs none, and they stop as soon as the
container reports it has reached the end. After `maximumScrolls` swipes (10 by default) they give
up and return `null`.

```kotlin
scrollTo("SCROLL TO CLICK ME!")
scrollTo(Pattern.compile("Item #\\d+"))
scrollToDescription("footer_logo")
scrollToDescription(Pattern.compile("row_\\d+"), maximumScrolls = 20)
```

They pair naturally with an assertion or a click:

```kotlin
scrollTo("Delete account")
click("Delete account")
```

> **Note**
> Scrolling searches forward from wherever the list currently sits; it does not rewind to the top
> first. Scroll back explicitly if your test has already gone past the target.

### Jetpack Compose

Compose needs no special treatment for text and content descriptions — a `Text` is found by its
text and an `Image` by its `contentDescription`, with no setup and no Compose dependency in your
test:

```kotlin
assertVisible("Welcome to Kobaia!")
click("LOG IN")
clickDescription("profile_picture")
```

`Modifier.testTag` is the one thing the app under test has to opt into. Publishing tags to the
accessibility tree is a single `semantics` block on a root composable:

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
clickTag("loginButton")
assertTagVisible("welcomeBanner")
findTag("total")?.text
typeIntoTag("12345678", tag = "passwordField")
scrollToTag("item40")                     // scrolls a LazyColumn like it scrolls a RecyclerView

kobaia clickTag "loginButton"             // the infix flavour, as always
kobaia type "12345678" intoTag "passwordField"
```

The same functions match a View's resource id — pass the fully qualified name,
`"com.example.app:id/login_button"`, instead of a bare tag.

### Leaving your app

`device()` hands you the UIAutomator `UiDevice`, so stepping outside the app is just another line
of the test — hardware keys, the launcher, the notification shade, other apps:

```kotlin
device().pressBack()
device().pressHome()
device().openNotification()
assertVisible("Your order has shipped")   // reading another app's UI works the same way
click("Kobaia")                           // relaunch from the launcher
```

### Waiting on purpose

Kobaia waits for you, but when you genuinely need to hold — an animation you cannot observe, a
scheduled job — `waitFor` holds the test for exactly as long as you ask, without blocking the app:

```kotlin
waitFor(2000)
kobaia waitFor 2000
```

### Optional screens

`assertVisible` throws `AssertionError`, so a screen that may or may not appear — a rating prompt,
a cookie banner — can be handled with a `try/catch`:

```kotlin
try {
    assertVisible("Rate this app", wait = 2000)
    click("Not now")
} catch (error: AssertionError) {
    // the dialog did not show up, carry on
}
```

Usually the forgiving click is enough, and cheaper:

```kotlin
click("Not now", wait = QUICK_WAITING_TIME)
```

## 🧭 API reference

Every name below works both as a plain call and as an infix one.

| | Functions |
| --- | --- |
| **Start** | `launch<T> { … }`, `T::class.launch { … }`, `Kobaia(T::class.java)` |
| **Find** | `find`, `findDescription`, `findTag` |
| **Check** | `isVisible`, `containsText`, `isDescriptionVisible`, `isTagVisible`, `assertVisible`, `assertTagVisible` |
| **Click** | `click`, `clickContaining`, `clickDescription`, `clickTag` |
| **Type** | `type … into`, `type … intoTag`, `typeOnKeyboard … into`, `typeOnKeyboard … intoTag` |
| **Scroll** | `scrollTo`, `scrollToDescription`, `scrollToTag` |
| **Device** | `device`, `waitFor` |

Every `String` overload has a `java.util.regex.Pattern` twin, except the substring ones
(`containsText`, `clickContaining`) and the typing ones. `device()` is the only interaction with no
infix form — it takes no argument.

Constants and knobs, all on `Kobaia`:

| | Default | |
| --- | --- | --- |
| `DEFAULT_WAITING_TIME` | `5000` | How long the finders poll before giving up, in milliseconds. |
| `QUICK_WAITING_TIME` | `50` | The wait to pass when you expect a miss. |
| `DEFAULT_MAXIMUM_SCROLLS` | `10` | How many swipes the scrolling functions get. |
| `DEFAULT_FLAKY_ATTEMPTS` | `5` | How many times a failing test is retried. |
| `DEFAULT_IDLING_LIMIT` | `60000` | Espresso's idling timeout, in milliseconds. |
| `tuneUiAutomatorTimeouts` | `true` | Whether Kobaia lowers UIAutomator's global timeouts. |

## ⚙️ How it behaves

**Tests are isolated.** Shared preferences, databases and files are cleared between tests, so a
test that logs in cannot leak a session into the next one. A test that fails every attempt is the
exception: it keeps its state so you can inspect it.

**Flaky tests are retried, and the retries are not silent.** Up to 5 attempts by default; tune it
with `flakyAttempts`, or set it to `1` in CI if you would rather see the flakiness. Every failed
attempt is logged with its stack trace, so a test that only passes on the third try still leaves a
trail in logcat:

```
W Kobaia: logsIn(com.example.LoginTest) failed on attempt 1 of 5, retrying
```

Between attempts Kobaia finishes the activities the failed one left behind and waits for them to
actually go away, so the retry starts on a fresh screen. Tests skipped by `assumeTrue` are not
retried — the assumption will not become true on the second try.

**It does not wait longer than it has to.** UIAutomator gives itself ten seconds to resolve a
selector and a second of acknowledgement per swipe, underneath everything Kobaia does. Kobaia
lowers both once, before the first test, because it already waits for what it looks for. Set
`Kobaia.tuneUiAutomatorTimeouts = false` to keep the platform defaults.

**Turning animations off is worth it.** Add this to the app under test and the suite stops paying
for animations it cannot see:

```gradle
android {
    testOptions {
        animationsDisabled = true
    }
}
```

## 🔄 Migrating from an older version

The interactions were renamed for consistency. The old names still compile — they are deprecated
and delegate to the new ones, and the IDE's *Replace with* quick fix migrates them for you.

| Old | New |
| --- | --- |
| `byText` | `find` |
| `byDescription` | `findDescription` |
| `textExists` | `isVisible` |
| `descriptionExist` | `isDescriptionVisible` |
| `assertTextExist` | `assertVisible` |
| `textClick` | `click` |
| `containsClick` | `clickContaining` |
| `descriptionClick` | `clickDescription` |
| `slowingTypeNumberInKeyboard(field, text)` | `typeOnKeyboard(text, into = field)` |
| `scrollUntilFindText` / `scrollUntilFindPattern` | `scrollTo` |
| `scrollUntilFindDescription` | `scrollToDescription` |
| `waitTest` | `waitFor` |
| `uiDevice` | `device` |

Three changes are not source-compatible and need an edit rather than a quick fix:

- **Constructor arguments** were renamed from `DEFAULT_FLAKY_ATTEMPTS` and
  `LAUNCH_ACTIVITY_AUTOMATICALLY` to `flakyAttempts` and `launchActivityAutomatically`. Parameter
  names cannot be deprecated, so passing them by name breaks.
- **`waitLimit` is milliseconds now**, on both `launchActivity` and `launch`, like every other wait
  in the library. It used to be handed to a seconds-based API while defaulting to a milliseconds
  constant, which made the effective timeout 83 minutes; it is now Espresso's own default of 60
  seconds. If you passed it explicitly, multiply by 1000.
- **The clicks return `Boolean`** instead of `Unit?`. Source-compatible if you ignore the result,
  but it changes the signature, so a recompile is needed rather than a jar swap.

`waitFor` also no longer waits for the main thread to go idle before sleeping — it holds for
exactly as long as you ask. Waiting for idle never returns on a screen that animates continuously,
which is most Compose screens with a spinner or a focused text field.

Nothing else about the rule changed: `@get:Rule val kobaia = Kobaia(…)` plus
`kobaia.launchActivity()` works exactly as before. `launch { }` is an addition, not a replacement.

## 📱 Sample app

The [`sample`](sample) module is a runnable app and its test suite:

| | |
| --- | --- |
| [`KobaiaSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaSampleTest.kt) | The login flow, driven end to end by `launch`. Those screens are **Jetpack Compose**, and the test did not change by a line when they were rewritten from XML layouts — nothing in it says which toolkit it is driving. |
| [`KobaiaInstrumentedTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaInstrumentedTest.kt) | The **rule** and the infix flavour, against a View screen of deliberately awkward widgets: a button that only becomes clickable after a countdown, a `TextWatcher`, a list to scroll. It also leaves the app and comes back. |
| [`ComposeSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/ComposeSampleTest.kt) | The `testTag` family against a Compose screen with a `TextField` and a `LazyColumn`. |

```bash
./gradlew :sample:connectedDebugAndroidTest
```

## 📄 License

Released under the [MIT License](LICENSE). Copyright © 2020 Jordan L. A. Junior.
