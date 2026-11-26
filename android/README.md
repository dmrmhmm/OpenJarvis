# JARVIS Mobile

Native Android client foundation for the OpenJarvis repository.

## Current scope

- Native Kotlin + Jetpack Compose application.
- Configurable OpenAI-compatible chat-completions endpoint.
- User-selected model and optional bearer API key.
- Non-streaming chat request/response flow.
- No automatic connection, background execution, microphone access, or device-control permissions.

## Build

Requires JDK 17, Android SDK 35, and Gradle 8.11.1 or compatible.

```bash
gradle -p android :app:assembleDebug
```

The debug APK is produced at:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Connect to a local model

Enter the OpenAI-compatible API base URL exposed by the trusted server, ending in `/v1` (for example, an Ollama-compatible endpoint), and enter the model name supported by that server.

Use HTTPS whenever possible. Plain HTTP traffic, including any bearer token, can be observed by other devices on the network. Only connect to a server you control or trust.

## Known limitations

This is an initial buildable client foundation, not a finished assistant. Settings are not persisted; API keys remain in memory only for the current app session. Streaming, voice, local on-device inference, OpenJarvis-specific agent tools, secure credential persistence, and end-to-end device testing remain future work.
