# Security Policy

## Supported versions

Security fixes are applied to the latest release on the `main` branch.

## Reporting a vulnerability

Please report security issues privately:

- **Email:** acartoon7668@gmail.com
- Or use GitHub **Private vulnerability reporting** on this repository if enabled.

Please include steps to reproduce, affected version/commit, and impact.
Do not open public issues for actively exploitable vulnerabilities.

Reports related to optional **Shizuku / root** Advanced mode are especially welcome.

## Coordinated disclosure

We aim to acknowledge reports promptly and ship fixes for confirmed high-impact issues in a timely release.

## Threat model

What Fern is designed to guarantee, and where the boundaries are.

| Area | Design |
| --- | --- |
| Network | The app declares no `INTERNET` permission. It cannot send data off the device. |
| Data at rest | Only preferences (theme, thresholds, overlay layout) are stored. `allowBackup` is off. Metrics history lives in memory and is cleared on exit. |
| Permissions | Usage access (Active Apps) and Display over other apps (overlay) are optional and user-granted. Nothing is requested at install time. |
| Elevated mode | Off by default. Needs Shizuku or root that the user has already set up and explicitly granted to Fern. |
| Shell input | The only commands built from variable data are `am force-stop` and `pm clear --cache-only`. The package name must match `^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$` (max 255 chars) before it is used; anything else is refused. |
| Release integrity | CI refuses to publish an APK signed with the Android debug key and verifies the signature with `apksigner` before upload. |

Out of scope: a device that is already rooted by malware, or a compromised Shizuku service.
