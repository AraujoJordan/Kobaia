# Kobaia

**The Android UI test library that reads like the test you would describe out loud.**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.araujojordan/kobaia.svg)](https://central.sonatype.com/artifact/io.github.araujojordan/kobaia)
[![Build](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml/badge.svg)](https://github.com/AraujoJordan/Kobaia/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen)](LICENSE)

Kobaia makes UI tests concise, readable, and reliable. Built on UIAutomator, it waits for elements automatically, drives both Views and Jetpack Compose seamlessly, and handles system interactions with ease.

```kotlin
@Test
fun testApp() = launch<SplashActivity> {
    assertTagVisible("splashTitle")
    assertVisible("Kobaia")
    clickTag("welcomeSkipButton")

    assertTagVisible("welcomeGetStartedButton")
    clickTag("welcomeGetStartedButton")

    assertTagVisible("landingLoginButton")
    clickTag("landingLoginButton")

    assertTagVisible("loginTitle")
    typeIntoTag("right_email@kobaia.com", tag = "emailField")
    typeIntoTag("12345678", tag = "passwordField")

    clickTag("loginButton")
    assertTagVisible("loggedInText")
    assertVisible("Welcome to Kobaia!")
}
```

<p align="center">
<img src="https://raw.githubusercontent.com/AraujoJordan/Kobaia/master/doc/kobaiaExample.gif" width="1280" alt="A Kobaia test driving the sample app"/>
</p>

## 📦 Download

```gradle
androidTestImplementation 'io.github.araujojordan:kobaia:x.x.x'
```

---

## 🎯 Finding Views

Finders return UIAutomator `UiObject2?` nodes, allowing direct access to underlying interactions:

```kotlin
find("ENTER")                              // by exact text
find(Pattern.compile("Hello, .*!"))        // by regex Pattern
findDescription("Enter your email")        // by contentDescription
findDescription(Pattern.compile("avatar_\\d+"))
findTag("loginButton")                     // by Compose testTag or View resource ID
```

---

## 🔍 Assertions & Verifications

```kotlin
// Visibility
assertVisible("Welcome to Kobaia!")
assertVisible(Pattern.compile("Welcome, .*"))
assertDescriptionVisible("profile_picture")
assertTagVisible("welcomeBanner")

assertNotVisible("Wrong credentials!")
assertDescriptionNotVisible("error_icon")
assertTagNotVisible("errorBanner")

// State checks
assertEnabled("ENTER")
assertDisabled("ENTER")
assertChecked("Remember me")
assertUnchecked("Remember me")
assertTagEnabled("loginButton")
assertTagChecked("acceptCheckbox")

assertClickable("ENTER")
assertNotClickable("ENTER")
assertTagClickable("loginButton")
assertTagNotClickable("disabledButton")

// Boolean queries
isVisible("Rate this app")
containsText("Welcome")                    // substring match
isDescriptionVisible("profile_picture")
isTagVisible("welcomeBanner")
isEnabled("ENTER")
isChecked("Remember me")
isTagEnabled("loginButton")
isTagChecked("acceptCheckbox")

// Await disappearance
waitUntilGone("Loading…")
waitUntilGone(Pattern.compile("\\d+"))
waitUntilDescriptionGone("progress_spinner")
waitUntilTagGone("loadingSpinner")
```

### When an assertion fails

A failing assertion tells you what was on screen instead — including the near miss, which is the
single most common way a finder misses:

```
"SIGN IN" should be visible, but was not found after 5000ms.

On screen now:
  text: "Sign in", "Forgot password?", "Kobaia"
  tags: loginTitle, emailField, passwordField, loginButton

Did you mean "Sign in"? (differs by case)
```

The screen is only read once something has already failed, so a passing suite pays nothing for it.
Every failed attempt is also photographed **and** dumped: a `.png` of the screen and a `.xml` of the
accessibility tree land in the app's external media directory, reported back to the instrumentation
so they show up alongside the test.

---

## 👆 Clicking & Tapping

```kotlin
click("SKIP")
click(Pattern.compile("(?i)skip"))
click("YOU CAN CLICK ME!", wait = 15_000)  // custom timeout in ms
clickContaining("Log")                     // substring match
clickDescription("fluffy")
clickTag("loginButton")

longClick("Terms of use")
longClickDescription("fluffy")
longClickTag("greeting")

// Checkboxes & switches, set to a known state instead of toggled —
// calling check on an already checked box changes nothing
check("Accept terms")
uncheck("Accept terms")
checkDescription("termsCheckbox")
checkTag("acceptCheckbox")
uncheckTag("acceptCheckbox")
```

---

## ⌨️ Typing & Text Input

```kotlin
type("user@kobaia.com", into = "Enter your email")   // into contentDescription
typeIntoTag("12345678", tag = "passwordField")       // into Compose testTag

// Character-by-character typing (triggers TextWatchers & input masks)
typeOnKeyboard("133.37", into = "editField")
typeOnKeyboardIntoTag("133.37", tag = "amountField")

// Clearing fields
clearText(into = "Enter your email")
clearTextInTag("emailField")
```

---

## 📜 Scrolling

Scrolls scrollable containers (`LazyColumn`, `RecyclerView`, `ScrollView`, `ListView`) forward until the target view appears, the list runs out, or `maximumScrolls` swipes have been made:

```kotlin
scrollTo("SCROLL TO CLICK ME!")
scrollTo(Pattern.compile("Item #\\d+"))
scrollToDescription("footer_logo")
scrollToTag("item40")
scrollToDescription(Pattern.compile("row_\\d+"), maximumScrolls = 20)
```

---

## 🤏 Gestures

The raw gestures, for the things a scroll does not reach — pulling a list down to refresh it or
turning a pager's page. A swipe moves across the middle of the first scrollable view (or of the
screen when there is none), so it lands on the thing it is meant to move. To *find* something
inside a list, keep using `scrollTo`:

```kotlin
swipe(Direction.UP)                      // across the middle of the first scrollable view
swipe(Direction.LEFT, percent = 0.9f)    // turn a pager's page

doubleClick("Like")
doubleClickDescription("profile_picture")
doubleClickTag("mapMarker")

pinchOut("Map")                          // zoom in
pinchInTag("photoView")                  // zoom out
pinchOutDescription("floor_plan", percent = 0.8f)
```

---

## 📱 Device, System & Permissions

Handle device controls and OS dialogs outside the application window:

```kotlin
pressBack()
pressHome()
pressEnter()
pressRecentApps()
closeKeyboard()
openNotifications()

wakeUp()
pressKey(KeyEvent.KEYCODE_DEL)             // any key, by its keycode
screenshot(File(context.cacheDir, "screen.png"))

// The device clipboard
copyToClipboard("user@kobaia.com")
clipboardText()                            // what is on the clipboard now

// System Runtime Permissions
allowPermission()
denyPermission()

// Screen Rotation
rotateLandscape()
rotateNatural()
```

---

## ⏱️ Waiting & Settling

```kotlin
waitFor(2000)                               // pause for specified milliseconds
waitForStable()                             // wait until screen stops changing (up to default wait)
waitForStable(wait = 1500)
```

---

## 🧩 Jetpack Compose

Compose works out of the box with `Text` and `contentDescription`. To enable `Modifier.testTag` lookup, add `testTagsAsResourceId = true` on the root layout:

```kotlin
setContent {
    MaterialTheme {
        MyScreen(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
        )
    }
}
```

---

## 🔀 Infix Notation

Every interaction is also available as an infix function on the `kobaia` instance or rule:

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

---

## ⚙️ Configuration

Global defaults configurable on `Kobaia`:

| Property | Default | Description |
| :--- | :--- | :--- |
| `DEFAULT_WAITING_TIME` | `5000` | Finder polling timeout (ms) |
| `QUICK_WAITING_TIME` | `50` | Fast probing timeout (ms) |
| `DEFAULT_MAXIMUM_SCROLLS` | `20` | Max swipes during scroll search |
| `DEFAULT_FLAKY_ATTEMPTS` | `5` | Retry attempts for flaky tests |
| `DEFAULT_IDLING_LIMIT` | `60000` | Espresso idling timeout (ms) |
| `tuneUiAutomatorTimeouts` | `true` | Lowers UIAutomator 10s idle ceiling to 500ms |

---

## 📱 Sample App

Explore the [`sample`](sample) module for working examples across different patterns:

- [`KobaiaSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaSampleTest.kt): End-to-end login flow in Jetpack Compose using `launch`.
- [`KobaiaInstrumentedTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/KobaiaInstrumentedTest.kt): View-based widgets, countdowns, scrolling, and infix calls with JUnit TestRule.
- [`ComposeSampleTest`](sample/src/androidTest/java/com/araujo/jordan/kobaiasample/ComposeSampleTest.kt): End-to-end Compose login flow targeting `Modifier.testTag` selectors with `launch` and infix `TestRule`.

---

## 🤝 Contributing

Contributions are welcome! Pull requests should ensure all checks and instrumented tests pass:

```bash
./gradlew build
./gradlew :sample:connectedDebugAndroidTest
```

---

## 📄 License

Released under the [MIT License](LICENSE).
