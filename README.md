[ ![Download](https://api.bintray.com/packages/queueit/maven/com.queue_it.androidsdk/images/download.svg) ](https://bintray.com/queueit/maven/com.queue_it.androidsdk/_latestVersion)

# Queue-it Android WebUI SDK

Library for integrating Queue-it's virtual waiting room into an Android app written in java.

## Sample app

A sample app to try out functionality in the library can be found on the [Releases](https://github.com/queueit/android-sdk/releases) page.

## Installation

Using Gradle:

```gradle
compile 'com.queue_it.androidsdk:library:2.0.20'
```

## Usage

Invoke QueueITEngine as per example below. Parameters `layoutName` and `language` are optional.

```java
QueueITEngine engine = new QueueITEngine(YourActivity.this, customerId, eventOrAliasId, layoutName, language,
  new QueueListener() {
    
    // This callback will be triggered when the user has been through the queue.
    // Here you should store session information, so user will only be sent to queue again if the session has timed out. 
    @Override
    public void onQueuePassed(QueuePassedInfo queuePassedInfo) { 
    }

    // This callback will be triggered just before the webview (hosting the queue page) will be shown.
    // Here you can change some relevant UI elements. 
    @Override
    public void onQueueViewWillOpen() { 
    }

    // This callback will be triggered when the queue used (event alias ID) is in the 'disabled' state.
    // Most likely the application should still function, but the queue's 'disabled' state can be changed at any time, 
    // so session handling is important.
    @Override
    public void onQueueDisabled() { 
    }

    // This callback will be triggered when the mobile application can't reach Queue-it's servers.
    // Most likely because the mobile device has no internet connection.
    // Here you decide if the application should function or not now that is has no queue-it protection.
    @Override
    public void onQueueItUnavailable() { 
    }

    // This callback will be triggered when the mobile application can't reach Queue-it's servers.
    // It can be any one of these scenarioes:
    // 1) Queue-it's servers can't be reached (connectivity issue).
    // 2) SSL connection error if custom queue domain is used having an invalid certificate. 
    // 3) Client receives HTTP 4xx response. 
    // In all these cases is most likely a misconfiguration of the queue settings:
    // Invalid customer ID, event alias ID or cname setting on queue (GO Queue-it portal -> event settings).
    @Override
    public void onError(Error error, String errorMessage) { } // Called on connectivity problems
  });

  try {
    engine.run(YourActivity.this);
  }
  catch (QueueITException e) { } // Gets thrown when a request is already in progress. In general you can ignore this.
```
As the App developer your must manage the state (whether user was previously queued up or not) inside the apps storage.
After you have received the "onQueuePassed callback", the app must remember this, possibly with a date / time expiration.
When the user goes to the next page - you check this state, and only call QueueITEngine.run in the case where the user did not previously queue up.
When the user clicks back, the same check needs to be done.

![App Integration Flow](https://github.com/queueit/android-webui-sdk/blob/master/App%20integration%20flow.PNG "App Integration Flow")


## Required permissions

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

## Activities to include in your manifest

```xml
<activity android:name="com.queue_it.androidsdk.QueueActivity" />
```
