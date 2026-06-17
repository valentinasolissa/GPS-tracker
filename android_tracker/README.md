# GPS Tracker Receiver

An Android application that monitors and visualizes the location of a remote GPS tracking device (e.g., Raspberry Pi).

## Overview

GPS Tracker Receiver fetches encrypted/encoded location data from a **Supabase** backend, decodes the **Protocol Buffer (Protobuf)** payload, and displays the device's real-time position on a **Google Map**. It also features a geofencing system that sends push notifications if the tracked device wanders too far from a designated "Home" location.

## Features

- **Real-time Monitoring**: Fetches the latest location entry from Supabase.
- **Protobuf Integration**: Efficiently decodes binary location data received from the tracker.
- **Google Maps Visualization**: Automatically updates a map marker and centers the camera on the device's latest coordinates.
- **Geofencing Alerts**: Monitors the distance between the device and a fixed "Home" coordinate (default: 10km radius).
- **Push Notifications**: Alerts the user via system notifications when a geofence breach occurs.

## Tech Stack

- **Language**: Kotlin
- **Networking**: [Retrofit](https://square.github.io/retrofit/) with Gson Converter.
- **Data Serialization**: [Protocol Buffers (Protobuf)](https://protobuf.dev/) for compact binary data transfer.
- **Maps**: [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview).
- **Backend**: [Supabase](https://supabase.com/) (PostgreSQL + REST API).
- **Concurrency**: Kotlin Coroutines for non-blocking network calls and UI updates.

## Project Structure

- `MainActivity.kt`: The core logic handling map initialization, data fetching, geofencing, and UI updates.
- `SupabaseApi.kt`: Retrofit interface defining the communication with the Supabase REST API.
- `locationResponse.kt`: Data model for the raw JSON response from Supabase.
- `tracker.proto`: Protobuf schema definition for the `DeviceStatus` message.

## Setup & Configuration

1. **Supabase Setup**:
   - Ensure your Supabase table (`locations`) has columns for `device_id`, `raw_payload` (Base64 encoded Protobuf), and `created_at`.
   - Update `supabaseUrl` and `supabaseKey` in `MainActivity.kt` if necessary.

2. **Google Maps API**:
   - Ensure you have a valid Google Maps API key configured in `AndroidManifest.xml`.

3. **Permissions**:
   - The app requires `android.permission.INTERNET` to fetch data and load maps.
   - For Android 13+ (API 33+), `POST_NOTIFICATIONS` permission may be required for alerts.

4. **Geofencing**:
   - The "Home" location is currently hardcoded in `MainActivity.kt`:
     ```kotlin
     private const val HOME_LAT = 49.1865
     private const val HOME_LON = -122.8772
     private const val RADIUS_THRESHOLD_METERS = 10000.0 // 10km
     ```

## How it Works

1. The app calls `fetchLatestLocation()` on startup.
2. It queries Supabase for the most recent entry for the specified `deviceId`.
3. The `raw_payload` (Base64 string) is decoded into binary bytes.
4. `DeviceStatus.parseFrom(binaryBytes)` reconstructs the location object.
5. The map marker is moved to the new Lat/Lng.
6. `isFarFromHome()` calculates the distance using Android's `Location` API and triggers a notification if the threshold is exceeded.
