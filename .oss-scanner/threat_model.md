# Fern — threat model (OSS Scanner)

## What this project does
Fern is a **local Android system monitor** (CPU, RAM, battery, storage, network, thermals).
Optional **Shizuku / root** enables elevated shell (`dumpsys`, `/proc`, force-stop) for per-app metrics.
There is **no account**, **no backend**, and the app does **not** require the `INTERNET` permission for core monitoring.

Homepage: https://get-fern.vercel.app  
Repo: https://github.com/wized2/Fern

## Untrusted input / attack surface
Treat as adversarial where relevant:
- **Intent extras** and deep links into activities/services/receivers
- **Exported components** (if any) and pending intents used by notifications/widgets/overlays
- **Shell command construction** when Advanced mode is on (Shizuku/root): argument injection, unexpected `dumpsys`/`svc`/`am` behavior
- **Parsing of shell / dumpsys / /proc / sysfs** text (malformed OEM output must not crash into unsafe handling)
- **SharedPreferences / local JSON** if corrupted
- **Overlay / display-over-apps** path (permission abuse is mostly OS-level; focus on privilege confusion inside the app)
- **Web assets** under the repo (`index.html`, download helpers) if any native code loads them — prefer static-site only

Out of scope unless Fern code is at fault:
- Bugs solely in **Shizuku**, **Android framework**, or OEM ROMs
- Google Play / signing infrastructure
- Third-party libraries without a Fern-controlled misuse

## Components that matter most
1. Advanced / elevated shell execution and result parsing (`dumpsys`, `/proc`, thermal zones)
2. Force-stop / process actions when elevated
3. Permission and Shizuku grant flows (confused deputy, over-broad shell)
4. Exported receivers/services for boot, widgets, overlays
5. Any path that could escalate from normal app UID to broader device control via crafted state

Less critical but still in scope:
- Compose UI, charts, theme pickers (prefer crashes/DoS only if local untrusted input triggers them)
- Marketing site HTML in-repo

## How to exercise it
- Gradle Android project: `./gradlew assembleDebug` (debug build is enough for static/agent review)
- Primary code under `app/src/main/java/` (or kotlin package `com.endroid.fern`)
- Manifest: `app/src/main/AndroidManifest.xml`
- No server to run; offline review of source + built APK/classes is appropriate

## Severity guidance
- **Critical**: remote or local privilege escalation to shell/root via Fern without user understanding; arbitrary command execution via injection in elevated paths; auth bypass of Shizuku gate
- **High**: sensitive data exposure beyond intended metrics; persistent elevation without consent; force-stop of arbitrary packages without clear user action
- **Medium**: reliable local DoS of Fern or system UI via malformed dumpsys parsing; permission misconfiguration (exported components)
- **Low**: pure UI crashes, cosmetic issues, defense-in-depth nits with no realistic exploit path

## Dedup / report preferences
- Prefer one report per root cause (e.g. single shell-escaping bug, not one report per command string)
- Candidate patches: minimal, focused on Fern code; do not require network
- Ignore “missing INTERNET permission” style findings — intentional

## Why this project is security-relevant
Fern optionally runs with **Shizuku or root** on end-user devices and executes/parses privileged diagnostics. Flaws in command construction, parsing, or component export can affect device integrity for users who enable Advanced mode.
