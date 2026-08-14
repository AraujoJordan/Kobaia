# Kobaia
An android UI test library made in Kotlin

[![Jitpack Enable](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
[![CircleCI](https://circleci.com/gh/AraujoJordan/Kobaia.svg?style=shield)](https://circleci.com/gh/AraujoJordan/Kobaia)
[![GitHub license](https://img.shields.io/badge/License-MIT-brightgreen)](https://github.com/AraujoJordan/Kobaia/LICENSE)

Kobaia is an Android library that provides an easy way to test UI with Kotlin. Built on top of UIAutomator2, it provides a simple and discoverable API, removing most of the boilerplate and verbosity of common UIAutomator tasks.

```kotlin
@get:Rule
val kobaiaRules = Kobaia(SplashActivity::class.java)

@Test
fun testApp() {
    kobaiaRules.launchActivity()
    assertTextExist("Kobaia")
    assertTextExist("SKIP")
    assertTextExist("NEXT")
    textClick("SKIP")
    textClick("GET STARTED")
    textClick("LOG IN")
    byDescription("Enter your email")?.text = "right_email@kobaia.com"
    byDescription("Enter your password")?.text = "12345678"
    textClick("ENTER")
    assertTextExist("Welcome to Kobaia!")
}
```

<p float="left" align="center">
<img src="https://raw.githubusercontent.com/AraujoJordan/Kobaia/master/doc/kobaiaExample.gif" width="1280"/>
</p>

## 🚀 Why you should use Kobaia?

1. Behaviour Driven Development testing
   * Kobaia is an high-level test library that create tests readable as close to what the user are doing with the app. So you can focus yours UI tests in a generic user interaction approach.
2. Automatic wait for your UI
   * Kobaia wait your UI be visible in the screen. Animations, long processes and networks requests doesn't need wait/sleep threads workarounds.
3. Interact with elements outside your app
   * Close the app and open and read an push notification? Or maybe test share a text message to an messenger app? It's easy with Kobaia.

## 📖 Usage

### 1. Declare the rule

`Kobaia` is a JUnit `TestRule`. Declaring it is the only setup you need — it wires up an
`ActivityTestRule` for the activity under test, retries flaky tests, and clears the app's
shared preferences, databases and files between tests.

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class LoginTest {

    @get:Rule
    val kobaia = Kobaia(SplashActivity::class.java)
}
```

The constructor takes two optional arguments:

| Argument | Default | What it does |
| --- | --- | --- |
| `DEFAULT_FLAKY_ATTEMPTS` | `5` | How many times a failing test is retried before it is reported as failed. |
| `LAUNCH_ACTIVITY_AUTOMATICALLY` | `false` | Launch the activity before the test body runs, instead of waiting for `launchActivity()`. |

There is also a reified factory if you prefer it:

```kotlin
@get:Rule
val kobaia = Kobaia.create<SplashActivity>()
```

### 2. Launch the activity

With the default `LAUNCH_ACTIVITY_AUTOMATICALLY = false`, start the activity from inside the
test. This lets you set up state (mock server, intent extras, …) first:

```kotlin
@Test
fun opensTheApp() {
    kobaia.launchActivity()
    // …
}
```

`launchActivity()` accepts an optional start `Intent`, and a `waitLimit` (in **seconds**, default
`5000`) that is applied to Espresso's master and idling-resource timeouts:

```kotlin
kobaia.launchActivity(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    waitLimit = 30
)
```

The underlying rule is exposed as `kobaia.activityTestRule` if you need the activity instance.

### 3. Import the interactions

Everything else lives on `Kobaia`'s companion object, so import the functions you need
statically and your tests read like plain sentences:

```kotlin
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTextExist
import com.araujo.jordan.kobaia.Kobaia.Companion.byDescription
import com.araujo.jordan.kobaia.Kobaia.Companion.textClick
```

Every function below takes a trailing `wait` parameter — how long, in milliseconds, Kobaia keeps
polling the screen before giving up. It defaults to **5000 ms**, which is why you rarely need a
`Thread.sleep` in a Kobaia test.

### Finding views

Returns a UIAutomator `UiObject2?`, or `null` if nothing showed up within `wait`.

```kotlin
byText("ENTER")                              // first view whose text is exactly "ENTER"
byText(Pattern.compile("Hello, .*!"))        // …or matches a regex
byDescription("Enter your email")            // by contentDescription — images, EditTexts, icons
byDescription(Pattern.compile("avatar_\\d+"))
```

Because you get the raw `UiObject2` back, anything UIAutomator can do is one step away:

```kotlin
byDescription("Enter your email")?.text = "right_email@kobaia.com"
byText("Terms of use")?.longClick()
val price = byDescription("total")?.text
```

### Checking what is on screen

`*Exists` functions return a `Boolean`; `assertTextExist` fails the test with a readable message.

```kotlin
assertTextExist("Welcome to Kobaia!")        // fails: "Welcome to Kobaia! should be visible"
assertTextExist(Pattern.compile("Welcome, .*"))

if (textExists("Rate this app", wait = 1000)) textClick("Later")
textExists(Pattern.compile("\\d+ items"))
containsText("Welcome")                      // substring match, "Welcome to Kobaia!" counts
descriptionExist("profile_picture")
descriptionExist(Pattern.compile("avatar_\\d+"))
```

Use a short `wait` for elements you expect *not* to be there — otherwise the negative check pays
the full 5 s timeout.

### Interacting

Clicks are forgiving on purpose: if the element never appears, nothing is clicked and the test
keeps going. Assert first when the click must happen.

```kotlin
textClick("SKIP")                            // clicks every view with that exact text
textClick(Pattern.compile("(?i)skip"))
textClick("YOU CAN CLICK ME!", wait = 15000) // give a slow screen more time
containsClick("Log")                         // clicks views containing "Log", e.g. "LOG IN"
descriptionClick("fluffy")                   // click by contentDescription
descriptionClick(Pattern.compile("item_\\d+"))
```

To exercise `TextWatcher`s, formatting masks and other typing-driven logic, type through the
actual soft keyboard one character at a time:

```kotlin
slowingTypeNumberInKeyboard(fieldDescription = "editField", text = "133.37")
```

### Scrolling

Each of these scrolls the first scrollable container (`RecyclerView`, `ListView`, `ScrollView`, …)
until the target is visible, then returns it as a `UiObject2?`.

```kotlin
scrollUntilFindText("SCROLL TO CLICK ME!")
scrollUntilFindPattern(Pattern.compile("Item #\\d+"))
scrollUntilFindDescription("footer_logo")
scrollUntilFindDescription(Pattern.compile("row_\\d+"))
```

They pair naturally with an assertion or a click:

```kotlin
scrollUntilFindText("Delete account")
textClick("Delete account")
```

### Going outside your app

`uiDevice()` hands you the UIAutomator `UiDevice`, so leaving your app is just another step in
the test — hardware keys, the launcher, the notification shade, other apps:

```kotlin
uiDevice()?.pressBack()
uiDevice()?.pressHome()
uiDevice()?.openNotification()
assertTextExist("Your order has shipped")    // reading another app's UI works the same way
textClick("Kobaia")                          // relaunch from the launcher
```

### Waiting on purpose

Kobaia waits for you, but when you genuinely need to hold (an animation you cannot observe, a
scheduled job), `waitTest` sleeps without blocking Espresso's idling machinery:

```kotlin
waitTest(2000)
```

### Handling failures gracefully

`assertTextExist` throws `AssertionError`, so an optional screen — a rating prompt, a cookie
banner — can be handled with a plain `try/catch`:

```kotlin
try {
    assertTextExist("Rate this app", wait = 2000)
    textClick("Not now")
} catch (err: AssertionError) {
    // the dialog didn't show up, carry on
}
```

### Putting it together

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    @get:Rule
    val kobaiaRules = Kobaia(SplashActivity::class.java)

    @Test
    fun testApp() {
        kobaiaRules.launchActivity()
        assertTextExist("Kobaia")
        assertTextExist("SKIP")
        assertTextExist("NEXT")
        textClick("SKIP")
        textClick("GET STARTED")
        textClick("LOG IN")
        byDescription("Enter your email")?.text = "right_email@kobaia.com"
        byDescription("Enter your password")?.text = "12345678"
        textClick("ENTER")
        assertTextExist("Welcome to Kobaia!")
    }
}
```

A runnable version of this, plus a second test that leaves the app and comes back, lives in the
[`sample`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample) module.

### API cheat sheet

| | Functions |
| --- | --- |
| **Find** | `byText`, `byDescription` |
| **Check** | `textExists`, `containsText`, `descriptionExist`, `assertTextExist` |
| **Click** | `textClick`, `containsClick`, `descriptionClick` |
| **Type** | `slowingTypeNumberInKeyboard` |
| **Scroll** | `scrollUntilFindText`, `scrollUntilFindPattern`, `scrollUntilFindDescription` |
| **Device** | `uiDevice`, `waitTest` |

Every `String` overload above has a `java.util.regex.Pattern` twin, except `containsText`,
`containsClick` and `scrollUntilFindText` (use `scrollUntilFindPattern` for the regex version).

## 📦 Installation

**Requirements:** `minSdk` 23, JDK 17, Android Gradle Plugin 8.x.

#### Step 1. Add the JitPack repository to your project

+ settings.gradle (Gradle 7+)
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

<details>
<summary>Using an older project with repositories in <code>build.gradle</code>?</summary>

+ build.gradle (Project: YourProjectName)
```gradle
allprojects {
	repositories {
	     ...
		maven { url 'https://jitpack.io' }
	}
}
```
</details>

#### Step 2. Add the dependency to your app build file

Kobaia is a test-only dependency, so add it to `androidTestImplementation` — it will not be
packaged into your release APK.

+ build.gradle (Module: app) [![Jitpack Enable](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
```gradle
dependencies {
    ...
	androidTestImplementation 'com.github.AraujoJordan:Kobaia:x.x.x'
}
```

Replace `x.x.x` with the version shown in the JitPack badge above.

#### Step 3. Point your instrumentation runner at AndroidX

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

And that's it!

## 🌟 Extras

**Kobaia brings its test stack with you.** It exposes UIAutomator, Espresso, the AndroidX test
runner/rules and JUnit as `api` dependencies, so you can mix Espresso assertions into a Kobaia
test without declaring anything else.

**Tests are isolated by default.** Shared preferences, databases and files are cleared between
tests, so a test that logs in cannot leak a session into the next one.

**Flaky tests are retried.** Up to 5 attempts by default; tune it with the second constructor
argument, or set it to `1` in CI if you would rather see the flakiness.

## 📄 License

```
MIT License

Copyright (c) 2020 Jordan L. A. Junior

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
