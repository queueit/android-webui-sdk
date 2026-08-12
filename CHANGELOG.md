# Changelog

## 3.0.0

### Added
- **Process-death recovery for queue passes.** New public API `QueueITEngine.consumePendingPass(context, queueListener)`. Call it from your launching Activity's `onResume()`/`onCreate()` to deliver a pass that completed while the OS killed your app in the background. The pass token is now persisted so it survives process death, instead of being lost with the in-memory listener. See the README section "Recovering a queue pass after the app is killed (process death)".
- **Resilient waiting-room reload.** After a load failure (for example a cold-network reload right after the OS restarts the process), the waiting room now retries with exponential backoff and reloads automatically when connectivity returns, instead of getting stuck on the browser "Webpage not available" page.

### Breaking changes
- **Artifact renamed to `webui`.** The SDK is now published as `com.queue-it.androidsdk:webui`. The previous artifact ids `com.queue-it.androidsdk:library` and `com.queue-it.androidsdk:library-androidx` are **discontinued** and now publish **relocation** POMs that point to `:webui` — consumers still on the old ids get a "relocated" warning at build time and should update their dependency to:

  ```gradle
  implementation 'com.queue-it.androidsdk:webui:3.0.0'
  ```

  Background: the SDK previously shipped two artifacts under a Gradle `androidx` flavor dimension (`library` = legacy, `library-androidx` = AndroidX). The flavors were later collapsed so `library` alone carried the AndroidX build while `library-androidx` was left frozen — which was confusing. `webui` is the single, clearly-named, AndroidX artifact going forward.
- **Internal SDK types changed** (no code using the documented public API — `QueueITEngine`, `QueueListener`, `QueueITWaitingRoomProvider`, `QueueITWaitingRoomView`, `QueueItEngineOptions`, `QueueTryPassResult` — is impacted):
  - `WaitingRoomStateBroadcaster` and `UriOverrider` were made package-private.
  - Removed the unused internal interfaces `IWaitingRoomStateBroadcaster` and `IUriOverrider`.
  - Changed the internal broadcaster signature `broadcastQueuePassed(String)` to `broadcastQueuePassed()` (the pass token is now delivered via the persistent store, not the broadcast payload).

If you integrated only via the documented public API, upgrading is a drop-in change: update the dependency coordinate to `com.queue-it.androidsdk:webui:3.0.0` and add the `consumePendingPass` call to get process-death recovery.
