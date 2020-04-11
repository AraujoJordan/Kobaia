# LaTest
**Not the first UI test library, but is easy to use**

LaTest is an Android library that provides an easy way to test UI with Kotlin.

```kotloin
uiDevice.apply {
    textClick("Skip", 2000)
    textClick("Get Started")
    textClick("Log In", 2000)

    byDescription("Enter your email").text = "test562@remitbee.com"
    byDescription("Enter your password").text = "Qwertyuiop"

    descriptionClick("Login Button")

    textClick("USE MY PASSWORD", 2500)
}
```

## 🚀 Why you should use LaTest?

1. Behaviour Driven Development testing
   * LaTest is an high-level test library that create tests readable as close to what the user are doing with the app. So you can focus yours UI tests in a generic user interaction approach.
2. No more wait for your UI
   * LaTest wait your UI be visible in the screen. Animations and networks requests doesn't need wait/sleep threads workarounds.
3. Interact with elements outside your app
   * Close the app and open notifications tray? Or even the apps drawer? It's an easy pie with LaTest.
4. Uses UiAutomator2.
   * Tests running Android 6/UiAutomator 2 are in average 20-30% faster then the same ones on Android 5.1/UiAutomator. ![Source](https://stackoverflow.com/a/50131368 "Source")

## 📖 Usage

Coming Soon

## 📦 Installation

#### Step 1. Add the JitPack repository to your project build file

+ build.gradle (Project: YourProjectName)
```gradle
allprojects {
	repositories {
	     ...
		maven { url 'https://jitpack.io' }
	}
}
```

#### Step 2. Add the dependency to your app build file

+ build.gradle (Module: app) [![Jitpack Enable](https://jitpack.io/v/AraujoJordan/latest.svg)](https://jitpack.io/AraujoJordan/latest/)
```gradle
dependencies {
    ...
	implementation 'com.github.AraujoJordan:latest:x.x.x'
}
```

And that's it!

## 🌟 Extras

Coming Soon

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

