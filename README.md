<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/56e404f4-8421-406d-a1a2-ed75bb89e316

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example). The key is optional — without it, the AI script generator falls back to built-in sample captions.
5. Run the app on an emulator or physical device. The debug build uses Android's default debug keystore, so no extra keystore file is required.

### Building a signed release (optional)

Release signing is driven by environment variables, so no secrets are committed:

```
export KEYSTORE_PATH=/absolute/path/to/your-upload-key.jks
export STORE_PASSWORD=...
export KEY_ALIAS=upload
export KEY_PASSWORD=...
./gradlew assembleRelease
```

If these variables are absent, the release build still compiles (debug-signed) for local testing.
