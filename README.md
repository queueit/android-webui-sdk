[ ![Download](https://api.bintray.com/packages/queueit/maven/com.queue_it.androidsdk/images/download.svg) ](https://bintray.com/queueit/maven/com.queue_it.androidsdk/_latestVersion)

# Queue-it Android WebUI SDK

Library for integrating Queue-it into an Android app.

## Sample app

A sample app to try out functionality in the library can be found on the [Releases](https://github.com/queueit/android-sdk/releases) page.

## Installation

Using Gradle:

```gradle
compile 'com.queue_it.androidsdk:library:2.0.17'
```

## Usage

Invoke QueueITEngine as per example below. Parameters `layoutName` and `language` are optional.

```java
QueueITEngine engine = new QueueITEngine(YourActivity.this, customerId, eventOrAliasId, layoutName, language,
  new QueueListener() {
    @Override
    public void onQueuePassed(QueuePassedInfo queuePassedInfo) { } // Called when the user has passed the queue

    @Override
    public void onQueueViewWillOpen() { } // Called right before the Queue-it view opens

    @Override
    public void onQueueDisabled() { } // Called when the event is disabled.

    @Override
    public void onQueueItUnavailable() { } // Called when Queue-it API could not be reached

    @Override
    public void onError(Error error, String errorMessage) { } // Called on connectivity problems
  });

  try {
    engine.run(YourActivity.this);
  }
  catch (QueueITException e) { } // Gets thrown when a request is already in progress. In general you can ignore this.
```
As the App developer your must manage the state (whether user was previously queued up or not) inside the apps storage.
After you have received the "On Queue Passed callback", the app must remember this, possibly with a date / time expiration.
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
