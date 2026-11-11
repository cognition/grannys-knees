# Android build

The native Android project is a Jetpack Compose and Material 3 phone app with weather forecast fetch, experimental index scoring and an on-device journal. The home-screen widget and Wear OS companion are deferred and stripped from the 1.0 tree.

## Pinned toolchain

| Component | Version |
| --- | --- |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| Compose BOM | 2026.09.00 |
| Compile / target SDK | 37 |
| Android SDK Build Tools | 36.0.0 |
| Minimum Android API | 26 |
| Java bytecode target | 17 |
| CI Java runtime | 21 |

Android Gradle Plugin 9.1.1 supports API 37 and requires Java 17 or newer and Gradle 9.3.1. The project uses Gradle 9.3.1 so the host's Java 25 runtime is also supported. CI uses Java 21 for a conservative long-term-support runtime.

## Clean-checkout build

1. Install a JDK from version 17 through 25 and ensure `java -version` reports that JDK.
2. Install Android SDK platform **37.0** (`sdkmanager "platforms;android-37.0"`) and Build Tools 36.0.0. The platform directory is `android-37.0`, not `android-37`.
3. Make the SDK discoverable using `ANDROID_HOME` (recommended for shells and CI), `ANDROID_SDK_ROOT`, or an untracked `local.properties` file:

   ```properties
   sdk.dir=/absolute/path/to/Android/Sdk
   ```

4. From the repository root, run:

   ```sh
   ./gradlew test assembleDebug
   ```

The debug APK is written under `app/build/outputs/apk/debug/`. Unit-test reports are written under `app/build/reports/tests/`.

On the current host, the SDK is at `/home/rbrooker/Android/Sdk`, so the equivalent one-off command is:

```sh
ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew test assembleDebug
```

## Run the shell

Open the repository in Android Studio, select the `app` run configuration, and deploy to an API 26 or newer emulator or device. The only screen is a navigation-ready foundation shell. No emulator image or Android Virtual Device was available during T01, so device launch was not verified.

## Continuous integration

`.github/workflows/android.yml` installs Java 21, Android platform `platforms;android-37.0` and Build Tools 36.0.0, validates the Gradle Wrapper, and runs the same `./gradlew test assembleDebug` checks on pushes to `main` and on pull requests.

## Deferred application ID

Both the namespace and application ID are temporarily `ca.aeo3.grannysknees`. The final reverse-domain identifier and Play Store publisher identity remain an explicit product/release decision. Change them before any store build; published application IDs cannot be changed in place.

## Branding boundary

The shell uses text only. It does not package the repository's SVG logo recreations as approved branding and does not require the missing approved PNG artwork to build.
