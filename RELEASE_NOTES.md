# Fern 1.12.0

Faster, lighter and safer. No new screens; everything you already use got cheaper to run.

**Security**
- App names are now strictly validated before Force stop / Clear cache reach the Shizuku or root shell (closes a command-injection path).

**Speed and battery**
- Settings save in the background instead of blocking the screen.
- Nothing redraws while Fern is not visible.
- Far fewer system file reads per refresh; device info that cannot change is read once.
- The floating pill reads only what it shows and sleeps while the screen is off.

**Fixes**
- Status bar and navigation bar icons are readable in dark theme.

Fern still asks for no internet permission and sends nothing anywhere.
