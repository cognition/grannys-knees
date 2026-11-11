# Upload signing for Google Play

Release bundles must be signed with your **upload key**, not the Android debug certificate (`CN=Android Debug`). This project keeps keystores and passwords **outside Git**. Gradle reads credentials from a local `keystore.properties` file and/or environment variables.

## Property names

Use these exact keys in `keystore.properties` at the **repository root** (same level as `settings.gradle.kts`):

| Property | Meaning |
|----------|---------|
| `storeFile` | Path to the upload keystore, relative to the repo root (for example `signing/upload-keystore.jks`) |
| `storePassword` | Keystore password |
| `keyAlias` | Key alias inside the keystore |
| `keyPassword` | Password for that key (often the same as `storePassword`) |

Equivalent **environment variables** (useful in CI with injected secrets):

| Environment variable | Maps to |
|---------------------|---------|
| `RELEASE_STORE_FILE` | `storeFile` |
| `RELEASE_STORE_PASSWORD` | `storePassword` |
| `RELEASE_KEY_ALIAS` | `keyAlias` |
| `RELEASE_KEY_PASSWORD` | `keyPassword` |

Environment values override `keystore.properties` when both are set.

## Create an upload keystore (owner, one time)

Run on a trusted machine. **Do not** commit the `.jks` file or passwords.

```bash
keytool -genkeypair -v \
  -keystore signing/upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Store the keystore and passwords in a password manager or hardware-backed backup. If the upload key is lost and Play App Signing is enabled, Google may allow a reset subject to their process; treat loss as a publication emergency.

## Local `keystore.properties` (gitignored)

Example layout (values are placeholders — use your own):

```properties
storeFile=signing/upload-keystore.jks
storePassword=<your-store-password>
keyAlias=upload
keyPassword=<your-key-password>
```

Ensure `signing/upload-keystore.jks` exists at the path you configured. The repo already ignores `*.jks`, `*.keystore`, and `keystore.properties`.

Passwords are read as plain text (not Java `Properties` escaping), so characters such as `\`, `:`, and `#` may appear literally. Do not wrap values in quotes.

## Play App Signing

In Play Console, enable **Play App Signing** for the app. Google holds the **app signing key** used for installs; you upload builds signed with the **upload key** configured above. Register the upload key certificate in Console when prompted (export with `keytool -list -v -keystore signing/upload-keystore.jks`).

Official reference: [Android app signing](https://developer.android.com/studio/publish/app-signing).

## Build a Play-uploadable AAB locally

From the repo root, with credentials in place:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Without upload credentials, `assembleRelease` and `bundleRelease` **fail by default**. That prevents a mistaken Play upload of a debug-signed artefact.

## Local debug-signed release (opt-in only)

To inspect a release-type artefact signed with the debug key when credentials are absent:

```bash
ALLOW_DEBUG_RELEASE_SIGNING=true ./gradlew bundleRelease
```

Gradle prints a prominent warning that the bundle is **not** Play-uploadable. Use this only for local smoke tests, never for Console upload.

## Verify the certificate is not debug

After building a real upload-signed AAB:

```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

You must **not** see `CN=Android Debug` in the signer certificate. The distinguished name should match your upload key (for example your name or organization).

Alternatively, unzip the AAB and inspect the signing block with `apksigner` / Play-internal tools; the debug CN check above is usually sufficient for a quick local check.

## Crash symbols in the release AAB

Release builds enable R8 (`isMinifyEnabled`) and set `ndk.debugSymbolLevel = SYMBOL_TABLE`. With Android App Bundles, Play extracts the ProGuard/R8 mapping and native debug symbols from the uploaded AAB automatically — no separate mapping upload is required for AAB uploads.

Requires a local Android NDK install so Gradle can strip/extract symbols from packaged `.so` files (for example `sdkmanager "ndk;28.2.13676358"`).

After `bundleRelease`, confirm artefacts exist:

- `app/build/outputs/mapping/release/mapping.txt` (also embedded as `BUNDLE-METADATA/.../proguard.map` in the AAB)
- native symbols under `app/build/outputs/native-debug-symbols/release/` when extractable

Play may still warn about missing native debug symbols when the only `.so` files come from dependencies that ship already stripped (currently `libandroidx.graphics.path.so` from AndroidX). That warning is expected and safe to acknowledge for this app; there is no unstripped symbol file to upload for those libraries.

## Owner follow-ups

- Confirm Play Console registration uses `applicationId` `com.aetherealmind.grannysknees` before the first upload.
- Bump `versionCode` / `versionName` in a dedicated release commit when shipping.
- Wire CI to inject `RELEASE_*` secrets on any job that runs `bundleRelease` (CI currently uses `assembleDebug` only).
- Back up the upload keystore and document recovery with Play Console.
