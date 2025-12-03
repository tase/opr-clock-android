# opr-clock-android

A digital clock for LD290EJS, inspired by the clock in the JMA's operation room

![simulator](simulator.png)

## Overview

`opr-clock-android` is a dual-timezone digital clock designed for ultra-wide displays.
It presents Japan Standard Time (JST) and Greenwich Mean Time (GMT/UTC) in a vertically stacked layout optimized for a 1920×540 resolution.

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