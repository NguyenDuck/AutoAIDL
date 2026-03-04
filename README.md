# AutoAIDL Gradle Plugin

A Gradle plugin for Android that automatically generates AIDL files from annotated Java interfaces, specifically designed for Shizuku services.

## ✨ Features

- **Zero-Config AIDL**: Generate `.aidl` files directly from your Java source code.
- **Shizuku Ready**: Use `@ShizukuInterface` to automatically include Shizuku-required methods.
- **Stable Method IDs**: Automatically assigns stable Transaction IDs via method signature hashing.
- **Manual Overrides**: Use `@MethodId` to manually specify transaction IDs for long-term API stability.
- **Per-Variant Generation**: Automatically integrates with Android build variants (Debug, Release, etc.).

## 🚀 Getting Started

### 1. Apply the Plugin

In your module's `build.gradle` (usually `app/build.gradle` or `library/build.gradle`):

```gradle
plugins {
    id 'com.android.application' // or 'com.android.library'
    id 'io.github.nguyenduck.autoaidl' version '1.1.0'
}

repositories {
    google()
    mavenCentral()
    // Add your maven repo if needed
}
```

### 2. Define your Interface

Create a Java interface and annotate it. The plugin will scan your `src/main/java` directory.

```java
package com.example.service;

import io.github.nguyenduck.autoaidl.annotations.ShizukuInterface;
import io.github.nguyenduck.autoaidl.annotations.MethodId;

@ShizukuInterface
public interface IMyRemoteService {

    // Automatically gets a stable ID via hash
    void basicOperation(String taskName);

    // Manually specify an ID (recommended for public APIs)
    @MethodId(10)
    int performCalculation(int x, int y);
}
```

### 3. Build the Project

Run a build command (e.g., `./gradlew assembleDebug`). The plugin will:
1. Generate `IMyRemoteService.aidl` in `build/generated/aidl/`.
2. Automatically include the generated AIDL in the compilation process.

### 4. Implement the Service

The Android Gradle Plugin (AGP) will compile the generated AIDL into a Java class with a `Stub`. You can extend this `Stub` to implement your logic.

```java
package com.example.service;

import com.example.service.IMyRemoteService;

public class MyServiceImpl extends IMyRemoteService.Stub {
    
    @Override
    public void basicOperation(String taskName) {
        System.out.println("Executing: " + taskName);
    }

    @Override
    public int performCalculation(int x, int y) {
        return x + y;
    }

    /**
     * The default method is automatically added to the AIDL interface
     * when using @ShizukuInterface.
     */
    @Override
    public void defaultMethod() {}
}
```

## 🛠️ Configuration (Optional)

Method IDs are assigned in the following priority:
1. `@MethodId(value)` annotation.
2. Stable CRC32 hash of the method signature.
3. Auto-increment fallback if a collision occurs (rare).
