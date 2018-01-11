# HearingAidAndroid
This application aims to do one simple task: Record the microphone sound, amplify it and play it back on the user's headphones to act as a simple, yet powerful hearing aid. 

## Download
Hang on, we've just started. But once the app is in a publishable state, it will be published to the google play store. But for now, you need to clone the repo and build it for yourself in Android Studio.

## Features
- [x] Stream the amplified mic sound to the headphones
- [x] Stream in realtime (~10 ms delay)
- [ ] Apply an equalizer to match personal hearing loss
- [ ] Easy setup wizard

(Unchecked items are still on our to do list)

## Build
1. Clone the repo
2. Create a file called `local.properties` according to the instructions below.
3. Run `gradlew build` (`./gradlew build` on *nix)

### The local properties file
All Android projects require a file called `local.properties` that contains info about the location of your Android SDK.
For this project, the `local.properties` file requires additional attention as the app utilizes the _Superpowered SDK_ for fast audio processing.

The `local.properties` file should be located in the repository root and should look like this:
```
ndk.dir=/path/to/ndk/without/a/terminating/slash
sdk.dir=/path/to/sdk/without/a/terminating/slash
superpowered.dir=/path/to/the/repository/root/Superpowered/
```

**Important:** If you use Windows, use `\\` as your path delimiter for `ndk.dir` and `sdk.dir` (instead of the usual `\ `).
For `superpowered.dir`, please use `/` much like on Linux and Mac. This is due to `cmake` being weird.

**Note:** All `:` must be escaped, i. e. Windows paths should start like this: `C:\path\\to\\sdk` or `D:\path/to/repo/root/Superpowered/`

### Why is the build so incredibly slow?
To be honest, we don't know too much why. It's probably due to `cmake` taking ages to compile the C++ code.
All we know for sure is that incremental builds should be faster than the initial build.

**TL;DR** Use the gradle daemon if possible, incremental builds will be faster

# License
This application is published under the Apache License v2 (see the license file). It utilizes the [Superpowered Audio Sdk](http://superpowered.com/) whose license can be found [here](http://superpowered.com/license).
