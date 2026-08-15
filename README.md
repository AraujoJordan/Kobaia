# Kobaia
An android UI test library made in Kotlin

[![Jitpack Enable](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
[![CircleCI](https://circleci.com/gh/AraujoJordan/Kobaia.svg?style=shield)](https://circleci.com/gh/AraujoJordan/Kobaia)
[![GitHub license](https://img.shields.io/badge/License-MIT-brightgreen)](https://github.com/AraujoJordan/Kobaia/LICENSE)

Kobaia is an Android library that provides an easy way to test UI with Kotlin. Built on top of UIAutomator2, it provides a simple and discoverable API, removing most of the boilerplate and verbosity of common UIAutomator tasks.

```kotlin
@Test
fun testApp() = launch<SplashActivity> {
    assertVisible("Kobaia")
    assertVisible("SKIP")
    assertVisible("NEXT")
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
If you would rather have a JUnit rule, Kobaia is one — and then every function reads as plain
English, because they are all **infix functions** too:

```kotlin
@get:Rule
val kobaia = Kobaia(SplashActivity::class.java)

@Test
fun testApp() {
    kobaia.launchActivity()
    kobaia assertVisible "Kobaia"
    kobaia click "SKIP"
    kobaia click "LOG IN"
    kobaia type "12345678" into "Enter your password"
    kobaia click "ENTER"
    kobaia assertVisible "Welcome to Kobaia!"
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

### 1. Launch the activity

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

Inside the block every interaction is available with no import at all, and `scenario` is there
for whatever they do not cover:

```kotlin
@Test
fun logsIn() = launch<SplashActivity> {
    scenario.onActivity { activity -> activity.viewModel.seedSession() }
    click("LOG IN")
}
```

You can name the activity instead of passing it as a type argument, which is handy when the test
reads better that way:

```kotlin
@Test
fun logsIn() = SplashActivity::class.launch { … }
```

And it takes the same optional arguments the rule does:

| Argument | Default | What it does |
| --- | --- | --- |
| `startIntent` | `null` | The intent used to start the activity, instead of a plain launch. |
| `flakyAttempts` | `5` | How many times a failing test is retried before it is reported as failed. |
| `waitLimit` | `5000` | Espresso's master and idling-resource timeout, in **seconds**. |

```kotlin
@Test
fun opensTheDeepLink() = launch<SplashActivity>(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    flakyAttempts = 1
) {
    assertVisible("GET STARTED")
}
```

One thing to know: the app's state is cleared *inside* `launch`, which runs after `@Before`. Seed
whatever your test needs from inside the block, not from a `@Before` method, or it will be wiped
before the first assertion.

### 2. …or declare the rule

`Kobaia` is also a JUnit `TestRule`, which is what you want when the test has to compose with
other rules, or to do part of its work before the activity exists.

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

The rule wires up an `ActivityTestRule` for the activity under test, retries flaky tests, and
clears the app's shared preferences, databases and files between tests. Its constructor takes two
optional arguments:

| Argument | Default | What it does |
| --- | --- | --- |
| `flakyAttempts` | `5` | How many times a failing test is retried before it is reported as failed. |
| `launchActivityAutomatically` | `false` | Launch the activity before the test body runs, instead of waiting for `launchActivity()`. |

There is also a reified factory if you prefer it:

```kotlin
@get:Rule
val kobaia = Kobaia.create<SplashActivity>()
```

With the default `launchActivityAutomatically = false`, start the activity from inside the
test. This lets you set up state (mock server, intent extras, …) first:

```kotlin
kobaia.launchActivity(
    startIntent = Intent(context, SplashActivity::class.java).putExtra("skipTutorial", true),
    waitLimit = 30
)
```

The underlying rule is exposed as `kobaia.activityTestRule` if you need the activity instance.

### 3. Call the interactions

Inside a `launch` block they are simply there. Everywhere else they live on `Kobaia`'s companion
object, so import the ones you need:

```kotlin
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
import com.araujo.jordan.kobaia.Kobaia.Companion.findDescription
```

Every function takes a trailing `wait` parameter — how long, in milliseconds, Kobaia keeps
polling the screen before giving up. It defaults to **5000 ms**, which is why you rarely need a
`Thread.sleep` in a Kobaia test.

### 4. …or call them as infix functions

Every function is also an **infix function** under the same name, on the rule or on the `launch`
block. Both flavours do exactly the same thing — pick the one you find more readable, and mix them
freely in the same test.

```kotlin
click("SKIP")                                   // imported, or inside a launch block
kobaia click "SKIP"                             // infix, same function

assertVisible("Welcome to Kobaia!")
kobaia assertVisible "Welcome to Kobaia!"

type("12345678", into = "Enter your password")
kobaia type "12345678" into "Enter your password"
```

The infix flavour needs a name on its left, which is what the rule gives you. Inside a `launch`
block there is none — the receiver is implicit, and `click "SKIP"` on its own is a parse error, so
use the plain calls there:

```kotlin
@Test
fun testApp() = launch<SplashActivity> {
    click("SKIP")
    type("12345678") into "Enter your password" // `into`'s left operand is the call itself
}
```

Two more things to keep in mind: infix functions always use the default `wait` (call the plain
function when you need another one), and the ones that return a view need parentheses before you
can chain on the result:

```kotlin
(kobaia find "Terms of use")?.longClick()
click("YOU CAN CLICK ME!", wait = 15000)
```

### Finding views

Returns a UIAutomator `UiObject2?`, or `null` if nothing showed up within `wait`.

```kotlin
find("ENTER")                           // first view whose text is exactly "ENTER"
find(Pattern.compile("Hello, .*!"))     // …or matches a regex
findDescription("Enter your email")     // by contentDescription — images, EditTexts, icons
findDescription(Pattern.compile("avatar_\\d+"))
```

Because you get the raw `UiObject2` back, anything UIAutomator can do is one step away:

```kotlin
find("Terms of use")?.longClick()
val price = findDescription("total")?.text
(kobaia find "Terms of use")?.longClick()    // same thing, infix
```

### Checking what is on screen

The checks return a `Boolean`; `assertVisible` fails the test with a readable message.

```kotlin
assertVisible("Welcome to Kobaia!")     // fails: "Welcome to Kobaia! should be visible"
assertVisible(Pattern.compile("Welcome, .*"))

if (isVisible("Rate this app", wait = 1000)) click("Later")
isVisible(Pattern.compile("\\d+ items"))
containsText("Welcome")                 // substring match, "Welcome to Kobaia!" counts
isDescriptionVisible("profile_picture")
isDescriptionVisible(Pattern.compile("avatar_\\d+"))
```

Use a short `wait` for elements you expect *not* to be there — otherwise the negative check pays
the full 5 s timeout.

### Interacting

Clicks are forgiving on purpose: if the element never appears, nothing is clicked and the test
keeps going. Assert first when the click must happen.

```kotlin
click("SKIP")                            // clicks every view with that exact text
click(Pattern.compile("(?i)skip"))
click("YOU CAN CLICK ME!", wait = 15000) // give a slow screen more time
clickContaining("Log")                   // clicks views containing "Log", e.g. "LOG IN"
clickDescription("fluffy")               // click by contentDescription
clickDescription(Pattern.compile("item_\\d+"))
```

They click first and report after: each one returns whether it clicked anything, which is enough
to handle a label that may or may not be there without a second lookup.

```kotlin
if (!click("Not now")) click("Dismiss")
```

Filling a field takes the text and the content description of the field it goes into:

```kotlin
type("right_email@kobaia.com", into = "Enter your email")
kobaia type "right_email@kobaia.com" into "Enter your email"
```

To exercise `TextWatcher`s, formatting masks and other typing-driven logic, type through the
actual soft keyboard one character at a time:

```kotlin
typeOnKeyboard("133.37", into = "editField")
kobaia typeOnKeyboard "133.37" into "editField"
```

### Scrolling

Each of these scrolls the first scrollable container (`RecyclerView`, `ListView`, `ScrollView`, …)
until the target is visible, then returns it as a `UiObject2?`. They give up after
`maximumScrolls` (5 by default) and return `null`.

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

### Going outside your app

`device()` hands you the UIAutomator `UiDevice`, so leaving your app is just another step in
the test — hardware keys, the launcher, the notification shade, other apps:

```kotlin
device().pressBack()
device().pressHome()
device().openNotification()
assertVisible("Your order has shipped")  // reading another app's UI works the same way
click("Kobaia")                          // relaunch from the launcher
```

### Waiting on purpose

Kobaia waits for you, but when you genuinely need to hold (an animation you cannot observe, a
scheduled job), `waitFor` sleeps without blocking Espresso's idling machinery:

```kotlin
waitFor(2000)
kobaia waitFor 2000
```

### Handling failures gracefully

`assertVisible` throws `AssertionError`, so an optional screen — a rating prompt, a cookie
banner — can be handled with a plain `try/catch`:

```kotlin
try {
    assertVisible("Rate this app", wait = 2000)
    click("Not now")
} catch (err: AssertionError) {
    // the dialog didn't show up, carry on
}
```

…or without the `try/catch` at all — at the cost of the full 5 s wait when the dialog is absent:

```kotlin
if (kobaia isVisible "Rate this app") kobaia click "Not now"
```

### Putting it together

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    @Test
    fun testApp() = launch<SplashActivity> {
        assertVisible("Kobaia")
        assertVisible("SKIP")
        assertVisible("NEXT")
        click("SKIP")
        click("GET STARTED")
        click("LOG IN")
        type("right_email@kobaia.com") into "Enter your email"
        type("12345678") into "Enter your password"
        click("ENTER")
        assertVisible("Welcome to Kobaia!")
    }
}
```

The same test with the rule, and with every interaction as an infix function:

```kotlin
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    @get:Rule
    val kobaia = Kobaia(SplashActivity::class.java)

    @Test
    fun testApp() {
        kobaia.launchActivity()
        kobaia assertVisible "Kobaia"
        kobaia assertVisible "SKIP"
        kobaia assertVisible "NEXT"
        kobaia click "SKIP"
        kobaia click "GET STARTED"
        kobaia click "LOG IN"
        kobaia type "right_email@kobaia.com" into "Enter your email"
        kobaia type "12345678" into "Enter your password"
        kobaia click "ENTER"
        kobaia assertVisible "Welcome to Kobaia!"
    }
}
```

Runnable versions of both, plus a test that leaves the app and comes back, live in the
[`sample`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample) module.

### API cheat sheet

Each name works both as a plain call and as an infix one.

| | Functions |
| --- | --- |
| **Find** | `find`, `findDescription` |
| **Check** | `isVisible`, `containsText`, `isDescriptionVisible`, `assertVisible` |
| **Click** | `click`, `clickContaining`, `clickDescription` |
| **Type** | `type … into`, `typeOnKeyboard … into` |
| **Scroll** | `scrollTo`, `scrollToDescription` |
| **Device** | `device`, `waitFor` |
| **Start** | `launch<T> { … }`, `T::class.launch { … }`, `Kobaia(T::class.java)` |

Every `String` overload above has a `java.util.regex.Pattern` twin, except the substring ones
(`containsText`, `clickContaining`) and the typing ones. `device()` is the only interaction
without an infix form — it takes no argument.

### Coming from an older version?

The functions were renamed for consistency in the latest version. The old names still work — they
are deprecated and delegate to the new ones, and the IDE's *Replace with* quick fix migrates them
for you.

| Old name | New name |
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

The two constructor arguments were renamed as well, from `DEFAULT_FLAKY_ATTEMPTS` and
`LAUNCH_ACTIVITY_AUTOMATICALLY` to `flakyAttempts` and `launchActivityAutomatically`. Parameter
names cannot be deprecated, so this one is a breaking change if you passed them by name.

Nothing about the rule changed otherwise: `@get:Rule val kobaia = Kobaia(…)` plus
`kobaia.launchActivity()` keeps working exactly as before. `launch { }` is an addition, not a
replacement.

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
core/runner/rules and JUnit as `api` dependencies, so you can mix Espresso assertions or an
`ActivityScenario` into a Kobaia test without declaring anything else.

**Tests are isolated by default.** Shared preferences, databases and files are cleared between
tests, so a test that logs in cannot leak a session into the next one. A test that fails every
attempt is the exception: it leaves its state behind so you can inspect it.

**Flaky tests are retried.** Up to 5 attempts by default; tune it with `flakyAttempts`, on
`launch` or on the rule's constructor, or set it to `1` in CI if you would rather see the
flakiness.

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
