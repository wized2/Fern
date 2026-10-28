# Contributing to Fern

Thanks for helping — Fern is a free, **offline** Material 3 system monitor for Android.

**Read first:** [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) (setup, map of the code, PR flow).

## TL;DR

```bash
git clone https://github.com/wized2/Fern.git
cd Fern && git checkout beta
# … make changes …
./gradlew testStableDebugUnitTest
# open a Pull Request against **beta**
```

| Do | Don’t |
|----|--------|
| PR → **`beta`** | PR → `main` (unless urgent stable hotfix) |
| Keep features offline-first | Add ads, analytics, or silent network SDKs |
| Add unit tests for parsers | Commit keystores / secrets |
| Use `strings.xml` for UI text | Emoji-only labels without content descriptions |

## Good places to start

Open issues labeled **`good first issue`** or **`help wanted`**:

- https://github.com/wized2/Fern/labels/good%20first%20issue

Ideas that are usually welcome:

- Accessibility `contentDescription`s
- Wiring `HistoryExport` to a share sheet
- Translations (`values-xx/strings.xml`)
- Extra unit tests around `MetricsParse`

Comment on the issue when you start so others don’t duplicate work.

## Project docs

| Doc | Purpose |
|-----|---------|
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Setup, commands, file map |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | How pieces fit |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community norms |
| [SECURITY.md](SECURITY.md) | Vulnerability reporting |
| [README.md](README.md) | Product overview |

## Code of conduct

Be respectful. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

By contributing, you agree your work is under the MIT License ([LICENSE](LICENSE)).

## Finding work

- [Good first issues](https://github.com/wized2/Fern/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
- [Help wanted](https://github.com/wized2/Fern/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22)
- Prefer PRs against **beta** for features; **main** for docs/ci fixes only when agreed.
