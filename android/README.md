# OpenJarvis Android

Android wrapper for the **existing OpenJarvis frontend**. The mobile app packages the same React interface used by the desktop/web client instead of introducing a separate chat UI.

## Included interface

- Existing OpenJarvis chat, message rendering, streaming, conversation history, model picker, and system pulse.
- Existing navigation for Dashboard, Data Sources, Agents, Logs, and Settings.
- Existing backend features are used through the OpenJarvis server API, subject to backend configuration and mobile browser support.
- Frontend assets are built from `frontend/` and packaged into the APK.
- Android WebView storage keeps the frontend's local preferences and conversation state on the device.

## Connect to OpenJarvis

1. Start the OpenJarvis server on your PC or another trusted host.
2. Install and open the Android app.
3. Open **Settings** in the existing OpenJarvis interface.
4. Set **API URL** to the OpenJarvis server base address, for example `http://192.168.1.10:8000`.
5. Save the setting and return to Chat.

This is the OpenJarvis server URL, not a raw Ollama `/v1` endpoint. OpenJarvis connects to its configured inference provider and exposes the application API.

The packaged WebView uses the origin `http://tauri.localhost`, which is already included in the server's default CORS allowlist. If `OPENJARVIS_CORS_ORIGINS` is explicitly configured, include `http://tauri.localhost` in that list.

## Build

Current Android app version: `0.2.0`.

Requires Node.js 22.22+, npm 11, JDK 17, Android SDK 35, and Gradle 8.11.1.

```bash
gradle -p android :app:assembleDebug
```

Gradle runs `npm ci`, builds the existing frontend into the Android assets directory, then packages the APK.

Output:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Network and security

The Android app requires network access to reach the OpenJarvis server. HTTP is supported for trusted private networks, but HTTP traffic is not encrypted. Use HTTPS for remote or untrusted networks. Never enter an API key for a server you do not control or trust.

The app does not request contacts, SMS, accessibility, device-admin, or background device-control permissions.

## Remaining validation

A successful CI build verifies frontend compilation and APK packaging. A real Android install and end-to-end test against a running OpenJarvis server are still required before calling the app fully verified. Some desktop-only integrations may remain unavailable on Android until explicitly adapted.
