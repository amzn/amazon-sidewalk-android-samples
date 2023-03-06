# Change Log
All notable changes to this project will be documented in this file.

---
## [1.0.0]
Sidewalk Mobile Android SDK provides a one-stop solution for everything related to Sidewalk on Android mobiles. The initial release contains APIs including:
* `scan()`, `secureConnect()`, `register()`, `deregister()`, and `clearAccountCache()` in `Sidewalk` to interact with end devices.
* `subscribe()`, `write()`, `startCoverageTest()`, `stopCoverageTest()`, `isAvailable()` and `disconnect()` in `SidewalkConnection` to perform operations in a connection with a Sidewalk device.
* `getToken()` in `SidewalkAuthProvider` to provide LWA token to Sidewalk Mobile SDK for communication with the Sidewalk cloud.
* `log()` in `SidewalkLogging` to receive logs from Sidewalk Mobile SDK.