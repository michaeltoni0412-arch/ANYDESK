# 🦉 Night Owl Remote

A consent-based remote support app: view, control, and send files to a
device — but **only** after the person on that device taps **Allow** on a
full-screen prompt, and only until either side disconnects. There is no
setting to skip this — every connection, every time, needs a real answer
from a real person at that moment.

## ⚠️ Required setup before this will build: Firebase

This app uses a **free Firebase project** as the signaling channel that
lets two phones find each other using your device code. Without this
step, the build will fail — `google-services.json` is required by the
Google Services Gradle plugin.

1. Go to https://console.firebase.google.com and sign in with a Google
   account (free, no credit card required for this usage tier).
2. Tap **Add project**, give it any name (e.g. "Night Owl Remote"),
   finish the wizard.
3. In your new project, tap **Build → Realtime Database → Create
   Database**. Choose any region. Start in **test mode** (fine for a
   personal hobby project between you and people you trust — see the
   security note below).
4. Tap **Build → Storage → Get Started**. Also start in **test mode**.
5. Back on the project **Overview** page, tap the **Android icon** to
   add an Android app to this Firebase project.
   - Package name: `com.nightowl.remote` (must match exactly)
   - Skip the SDK/Gradle steps Firebase shows you — this project already
     has them wired in.
   - Download the **`google-services.json`** file it gives you.
6. Upload that `google-services.json` file into your GitHub repo at the
   path `app/google-services.json` (same "Add file → Upload files"
   method you've used before).
7. Push/commit — the GitHub Actions build should now succeed.

### Security note

Test mode means anyone who discovers your database URL could technically
read/write it. For a small personal project this is a low-probability
risk, but it also means: **don't reuse a 6-digit code publicly**, and if
you want to lock this down properly later, Firebase's docs on Realtime
Database security rules cover restricting reads/writes — worth doing
before wider use.

## How the consent flow works

1. Each device gets a permanent 6-digit code (generated once, saved on
   the device).
2. To connect to someone, enter their code and tap **Request to
   Connect**.
3. On **their** device, a full-screen prompt appears: *"[your device]
   wants to control this device"* with **Allow** / **Deny** — this
   requires an answer every single time, with no way to turn it off.
4. If they tap **Allow**, Android's own system screen-recording consent
   dialog appears on their device too (this is Android's mandatory
   permission for any app that captures the screen — not something this
   app can bypass).
5. Once both are granted, you'll see their screen update every ~0.4s,
   you can tap to control it, and you can send files.
6. Their device shows an **ongoing, unmissable notification** the entire
   time ("Night Owl Remote is ACTIVE") with a one-tap **Disconnect**
   button, and so does the session screen on your side.

## Permissions this app requests, and why

- **Notifications** — to show the active-session and incoming-request
  alerts
- **Accessibility / "Control Access"** — required by Android for any app
  to simulate taps on your behalf; only used while an accepted session
  is running
- **Screen capture** (Android's built-in system dialog, shown fresh each
  session) — required to show your screen to the controller

## Known limitations (this is a hobby-project build, not a commercial one)

- Screen sharing is ~2–3 frames per second, not smooth video — Realtime
  Database isn't built for high-throughput video streaming. It's enough
  to see what's happening and tap around, not to watch anything
  fast-moving.
- Background listening for incoming requests may get paused by battery
  optimization on some phones (including some Fire OS builds) — if
  requests aren't arriving, check that battery optimization is disabled
  for this app in system settings.
- No two-way audio/video call — this is screen control only.

## Building it

Same as before: push this whole folder to a GitHub repo (don't forget
`app/google-services.json` from the Firebase steps above), then check
the **Actions** tab for the built APK.
