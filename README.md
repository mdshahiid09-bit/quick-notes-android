# Quick Notes

A fast, private, offline Markdown note-taking and encrypted API key vault application for Android, built 100% in Kotlin with Jetpack Compose and Material 3.

---

## Highlights & Features

- **Notes Stored as Genuine Files:** Every note is saved as a real `.md` Markdown file in your device's `Documents/QuickNotes/` directory. Zero vendor lock-in; open and edit your notes in Obsidian, Markor, VS Code, or any text editor.
- **YAML Front Matter Metadata:** Notes store tags, color themes, pinned state, and timestamps in standard YAML front matter headers at the top of each file.
- **Two-Way Synchronization:** Room database index is automatically updated whenever notes are created, edited, moved, or edited externally.
- **Rich Markdown Editor:**
  - Fast formatting toolbar for bold, italic, underline, strikethrough, headings (H1–H3), lists, checklists, links, code blocks, and text highlights.
  - Live editing and rich formatted preview modes.
  - Multi-level Undo & Redo history stacks.
  - In-note Find & Replace with match navigation and match counter.
  - Live word count, character count, and estimated reading time.
  - Real-time autosave with 1-second debounce and status indicator.
  - Share notes directly as `.md` files via Android Share Sheet.
  - Subtle vector sparkle particle animation when completing task checkboxes.
- **Hardware-Encrypted Vault (API Keys):**
  - Keeps developer API keys (Gemini, Groq, OpenCode, OpenRouter, etc.) in app-private storage.
  - Encrypted with AES-256-GCM using hardware-backed Android Keystore keys.
  - Vault passcode hashed with PBKDF2-HMAC-SHA256 (120,000 iterations + random salt).
  - 32-character recovery key for emergency passcode recovery.
  - Rate-limited security lockout after 5 consecutive incorrect attempts.
  - Sensitive keys masked with show/hide toggle.
  - One-tap copy to clipboard with 30-second automatic clipboard clearing.
  - Auto-locks immediately when the application is backgrounded.
- **Disguise Launcher Mode:**
  - Disguise the app as a standard Calculator icon on your device's home screen.
  - Tapping the disguised icon launches directly into the encrypted vault passcode prompt.
  - Easily toggle disguise mode on or off from the Settings screen.
- **Organization & Search:**
  - Hierarchical Folders with real-time note counts.
  - Tag browser and tag filter.
  - Real-time full-text search matching titles, markdown body, and tags.
  - 30-day auto-purging Trash with restore and instant permanent deletion.
  - Archive for organizing completed or inactive notes.
- **Import & Export:**
  - One-click export of all notes into a ZIP archive.
  - Restore and import notes from any ZIP archive.
- **Material 3 Design & Motion:**
  - Adaptive Card Grid (2 columns on phones, 3–4 on tablets) and clean List view modes.
  - Fluid staggered entrance and item animations.
  - Shimmer skeleton loading effect on every cold launch.
  - Light, Dark, and System Material 3 themes with dynamic color on Android 12+.
  - 100% SVG vector icons with accessibility content descriptions. Zero emojis.

---

## How to Download and Install the APK

### 1. Download the Latest APK
1. Visit the [Releases](https://github.com/mdshahiid09-bit/quick-notes-android/releases) section of this GitHub repository.
2. Under the latest release, click on `app-debug.apk` to download it to your Android device.

### 2. Enable "Install Unknown Apps"
Because this APK is built directly from GitHub Actions and not from the Google Play Store, Android requires permission to sideload it:
1. Tap on the downloaded `app-debug.apk` in your notification bar or file manager.
2. When prompted: **"For your security, your phone is not allowed to install unknown apps from this source"**, tap **Settings**.
3. Toggle on **Allow from this source**.
4. Return to the installer and tap **Install**.

### 3. First Launch & Setup
1. Launch **Quick Notes**.
2. Swipe through the quick feature overview in Onboarding.
3. Set your 8+ character Vault Passcode and save your 32-character Recovery Key in a safe place.
4. Grant the **All Files Access** (`MANAGE_EXTERNAL_STORAGE`) permission so Quick Notes can create and read your `.md` files in `Documents/QuickNotes/`.

---

## Technical Specifications & Architecture

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI Framework | Jetpack Compose + Material 3 (Compose BOM 2024.11.00) |
| Architecture | MVVM with Repository Pattern & Single-Activity NavHost |
| Dependency Injection | Manual DI via `AppContainer` |
| Local Database | Room 2.6.1 (with KSP) for note metadata and indices |
| Storage Target | `Documents/QuickNotes/` for `.md` files |
| Key-Value Storage | Jetpack DataStore Preferences |
| Cryptography | Android Keystore + AES/GCM/NoPadding (256-bit) |
| Passcode Hashing | PBKDF2WithHmacSHA256 (120,000 rounds) |
| Minimum SDK | API 26 (Android 8.0 Oreo) |
| Target & Compile SDK | API 35 (Android 15) |

---

## Building from Source

To build the project locally with the Gradle wrapper:

```bash
# Clone the repository
git clone https://github.com/mdshahiid09-bit/quick-notes-android.git
cd quick-notes-android

# Build the debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
