# Android Build Repair Guide

This document outlines the steps required to resolve the current build failures in the Hearing Aid Android application. The failures are primarily due to deprecated C++ SDK usage, missing/unauthorized dependencies, and environment configuration.

## 1. Fix C++ Compilation Errors

The `Superpowered` SDK interactions in `app/src/main/jni/HearingAidAudioProcessor.cpp` are outdated compared to the headers included in the project.

### Issue: `fftSize` Member Missing

The `Superpowered::FrequencyDomain` class no longer exposes an `fftSize` member variable.
**File:** `app/src/main/jni/HearingAidAudioProcessor.cpp`
**Action:** Replace all instances of `frequencyDomain->fftSize` with the constant `FFT_SIZE` (which is already defined as 2048).

**Locations to Update:**

- Line ~39: `stepSize = frequencyDomain->fftSize / 4;` -> `stepSize = FFT_SIZE / 4;`
- Lines ~42-45: `malloc(frequencyDomain->fftSize * sizeof(float))` -> `malloc(FFT_SIZE * sizeof(float))`

### Issue: `SuperpoweredAndroidAudioIO` Constructor Mismatch

The constructor works with 8 arguments, but the code passes 9.
**Action:** Remove the last argument (`0` or similar integer) from the constructor call.

### Issue: Deprecated Function Names

Old global functions have been moved to the `Superpowered` namespace and renamed.
**Action:**

- `SuperpoweredShortIntToFloat(...)` -> `ShortIntToFloat(...)`
- `SuperpoweredFloatToShortInt(...)` -> `FloatToShortInt(...)`

Ensure `using namespace Superpowered;` is present at the top of the file.

---

## 2. Resolve Dependency Issues

Several dependencies in `app/build.gradle` are failing to resolve, returning `401 Unauthorized` or `404 Not Found`.

### Action: Remove Missing/Private Dependencies

The following libraries are hosted on repositories that are either private (requiring authentication) or no longer exist. They should be removed if the source code is not available locally.

**Remove from `dependencies { ... }` in `app/build.gradle`:**

1. `com.github.ohoussein:playpauseview:1.0.2`
2. `com.github.vatbub:common.view.motd.core:0.1.6-SNAPSHOT`
3. `com.github.vatbub:common.core:0.1.6-SNAPSHOT`
4. `org.rm3l:maoni:4.1.2@aar`
5. `com.bugsnag:bugsnag-android-ndk:6.1.0` (unless you have a valid API key and subscription)

_Note: Removing these will likely cause compilation errors in the Java code (e.g., `MainActivity.java`, `StreamingFragment.java`) wherever these libraries are imported. You will need to comment out or remove the usage of these classes (like `PlayPauseView` or `Bugsnag`) in the Java source files._

### Action: Fix Kotlin Standard Library Conflicts

Transitive dependencies are pulling in conflicting versions of the Kotlin standard library.
**Add this block to `app/build.gradle`:**

```groovy
configurations.all {
    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
        if (details.requested.group == 'org.jetbrains.kotlin' && details.requested.name.startsWith('kotlin-stdlib')) {
            details.useVersion '1.8.10'
        }
    }
}
```

---

## 3. Environment Configuration

### Issue: Missing `JAVA_HOME`

The build fails because it cannot locate the Java Development Kit (JDK).
**Action:**

1. Locate your JDK installation (e.g., `C:\Program Files\Android\Android Studio\jbr` or `C:\Program Files\Java\jdk-11...`).
2. Set the `JAVA_HOME` environment variable to this path.
3. Ensure `%JAVA_HOME%\bin` is in your system `PATH`.

---

## 4. Summary of Changes already applied (if any)

_Note: Any changes previously applied by the assistant should be verified against this guide._
