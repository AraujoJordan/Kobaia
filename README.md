# Kobaia
An android UI test library made in Kotlin

[![Jitpack Enable](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
[![CircleCI](https://circleci.com/gh/AraujoJordan/Kobaia.svg?style=shield)](https://circleci.com/gh/AraujoJordan/Kobaia)
[![GitHub license](https://img.shields.io/badge/License-MIT-brightgreen)](https://github.com/AraujoJordan/ExcuseMe/LICENSE)
[![HitCount](http://hits.dwyl.com/AraujoJordan/Kobaia.svg)](http://hits.dwyl.com/AraujoJordan/Kobaia)

Kobaia is an Android library that provides an easy way to test UI with Kotlin. Built on top of UIAutomator2, it provides a simple and discoverable API, removing most of the boilerplate and verbosity of common UIAutomator tasks.

```kotlin
@Test
fun testAppLogin() = uiDevice().apply {
    textClick("Skip", 2000)
    textClick("Get Started")
    textClick("Log In", 2000)

    byDescription("Enter your email").text = "test123@email.com"
    byDescription("Enter your password").text = "123456789"

    descriptionClick("Login Button")
}
```

<p float="left" align="center">
    <img src="https://raw.githubusercontent.com/AraujoJordan/Kobaia/master/doc/code.webp" width="600"/>
    <img src="https://raw.githubusercontent.com/AraujoJordan/Kobaia/master/doc/usage.webp" width="200"/>
</p>

## 🚀 Why you should use Kobaia?

1. Behaviour Driven Development testing
   * Kobaia is an high-level test library that create tests readable as close to what the user are doing with the app. So you can focus yours UI tests in a generic user interaction approach.
2. Automatic wait for your UI
   * Kobaia wait your UI be visible in the screen. Animations and networks requests doesn't need wait/sleep threads workarounds.
3. Interact with elements outside your app
   * Close the app and open notifications tray? Or even the apps drawer? It's an easy pie with Kobaia.

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

+ build.gradle (Module: app) [![Jitpack Enable](https://jitpack.io/v/AraujoJordan/Kobaia.svg)](https://jitpack.io/p/AraujoJordan/Kobaia/)
```gradle
dependencies {
    ...
	implementation 'com.github.AraujoJordan:Kobaia:x.x.x'
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

