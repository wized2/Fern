# Contributing to Fern

Thanks for helping improve Fern — a free, offline Material 3 system monitor for Android.

## Ways to contribute

- Bug reports and reproductions
- Feature ideas that fit offline monitoring (no accounts, no ads)
- Code: UI polish, metrics accuracy, Shizuku/root paths, widgets, docs
- Translations (when i18n lands)
- Awesome-list mentions and honest reviews

## Development setup

1. Clone `https://github.com/wized2/Fern`
2. Open in Android Studio (Ladybug+) or build with JDK 17:
   ```bash
   ./gradlew :app:assembleRelease
   ```
3. Optional: Shizuku installed + running to test Advanced mode

## Branch & PR rules

1. Branch from `main` with a short name (`fix/color-picker`, `feat/per-app-cpu`).
2. One concern per PR when possible.
3. Keep the app offline-first: no new network SDKs, ads, or trackers.
4. Do not commit secrets, keystores, or large binaries except workflow-published APKs.
5. Match existing Kotlin style (minimal comments, clear names).
6. UI: Material 3 expressive, accessible contrast, no emoji-only labels.
7. Wait for CI green (`Build`) before asking for review.
8. Update `CHANGELOG.md` for user-visible changes.
9. Bump `versionCode` / `versionName` only when releasing (maintainers may handle).

## Code of conduct (short)

Be respectful. No harassment. Assume good intent. Maintainers may close hostile or off-topic issues.

## License

By contributing you agree your work is licensed under the MIT License (see `LICENSE`).
