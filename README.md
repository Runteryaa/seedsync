# Seed Sync (Fabric 1.21.8)

A client-only Fabric mod that scans your single-player worlds (in `.minecraft/saves`) and POSTs their **name, seed, and dates** to `https://seed-syncmc.com/sync` so you can access them later on your website.

## Build
- JDK 21
- `./gradlew build`
- Drop the jar from `build/libs` into `.minecraft/mods` along with Fabric API

The mod automatically syncs on **client start** and on **world join**.