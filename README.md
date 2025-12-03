# opr-clock-android

A digital clock for LD290EJS, inspired by the clock in the JMA's operation room

![simulator](simulator.png)

## Overview

`opr-clock-android` is a digital clock and news display designed for ultra-wide displays.
It presents Japan Standard Time (JST) above an automatically scrolling news feed, optimized for a 1920×540 resolution.

This project runs as an Android application on the LG in TOUCH Stretch Display “LD290EJS”, a unique 29-inch panel featuring an unusually wide 1920×540 aspect ratio.

The interface uses the DSEG7Classic and Kosugi Maru fonts to reproduce the segmented-digit aesthetic inspired by the Japan Meteorological Agency’s operation room.

## Installation

You can download the APK from the project’s release page:

https://github.com/9SQ/opr-clock-android/releases

If you are installing the app on an LG in TOUCH Stretch Display (LD290EJS), copy the APK to a USB flash drive and connect it using a USB Micro-B OTG cable.
Open the APK from the device’s file manager to begin the installation.

### Using on other Android devices

The app also runs on standard Android devices.
However, because it is designed specifically for a 1920×540 ultra-wide display, other aspect ratios may result in unused margins or empty space.

If you wish to use the clock on a different screen size or aspect ratio, feel free to customize the source code to adjust the layout for your device.

## Fonts

This project includes the following fonts:

*   **KosugiMaru**: Licensed under the Apache License, Version 2.0.
    A copy of the license is available in the [LICENSES/KosugiMaru-LICENSE.txt](LICENSES/KosugiMaru-LICENSE.txt) file.

*   **DSEG7Classic**: Licensed under the SIL Open Font License, Version 1.1.
    A copy of the license is available in the [LICENSES/DSEG-LICENSE.txt](LICENSES/DSEG-LICENSE.txt) file.
## RSS news

The bottom of the clock displays headlines from NHK's main news RSS feed.
Multiple headlines appear together and scroll upward continuously when the news
exceeds the available space. The feed refreshes every 5 minutes while
this screen is active. If a refresh fails, the previous headlines remain visible;
if no headlines have loaded yet, an error message appears until the next retry.
Change `rss_feed_url` in `app/src/main/res/values/strings.xml` to use another
HTTPS RSS or Atom feed. Long headlines wrap so their full text remains readable.
Scrolling pauses while the screen is inactive and loops back to the first headline.

## Nagoya rain radar

The panel to the right of the news shows JMA's latest observed rain radar,
centered on Nagoya (35.1815° N, 136.9066° E, zoom 8). It refreshes every five
minutes while the screen is active and includes the observation time in JST.
The background uses GSI pale map tiles served by JMA; attribution is shown in
the panel. Failed updates retain the last complete image and show a warning;
observations older than 20 minutes are also marked as awaiting an update.

Sources: https://www.jma.go.jp/bosai/nowc/ (radar),
https://www.jma.go.jp/tile/gsi/pale/ (map tiles).
The image endpoints are website resources and may change with the JMA website.

## 24-hour forecast

Next to the radar, the Nagoya forecast lists the next 24 hourly forecasts in two
columns (left first, then right), with dates/times in JST, weather, temperature
in Celsius and precipitation probability. Data comes from Open-Meteo and is
refreshed every 30 minutes. Missing values appear as a dash; failed updates retain
the previous forecast with an update warning. The footer shows the retrieval time,
not the model issuance time. Source and attribution: https://open-meteo.com/
(CC BY 4.0); API documentation: https://open-meteo.com/en/docs.
