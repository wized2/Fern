# Troubleshooting

## Per-app CPU / RAM is empty on Active apps
Per-app numbers need elevated access. Turn on **Advanced mode** in Settings, then grant Fern in [Shizuku](https://shizuku.rikka.app/) (or allow root). Without it Android only shows Fern what a normal app may see.

## Shizuku says Fern is not authorised
Open Shizuku, make sure it is running (restart it after a reboot unless you use root or wireless debugging auto-start), then open Fern and grant the permission again.

## The overlay does not appear
The overlay needs the "Display over other apps" permission. Enable it for Fern in system settings, then switch the overlay on again in Fern.

## Readings stop when the screen is off
Some vendors stop background work aggressively. Set Fern's battery usage to "Unrestricted" (wording varies by device) if you want continuous history.

## CPU shows a different value than another monitor
Fern samples the whole device from kernel counters over a short window. Other apps may use a different interval or exclude some cores, so small differences are normal.

## Exported CSV opens in one column
Fern writes comma-separated values with dot decimals. In your spreadsheet, choose "Comma" as the separator when importing.

## Still stuck?
Open an issue with your device model, Android version, and whether Advanced mode is on: https://github.com/wized2/Fern/issues/new/choose
