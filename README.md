[ ![Download](https://api.bintray.com/packages/queueit/maven/com.queue_it.androidsdk/images/download.svg) ](https://bintray.com/queueit/maven/com.queue_it.androidsdk/_latestVersion)

# Queue-it Android SDK

Library for integrating Queue-it into an Android app.

## Sample app

A sample app to try out functionality in the library can be found on the [Releases](https://github.com/queueit/android-sdk/releases) page.

## Installation

Using Gradle:

    compile 'com.queue_it.androidsdk:library:2.0.13'

## Usage

Invoke QueueITEngine as per example below. Parameters `layoutName` and `language` are optional.

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
        engine.run();
      }
      catch (QueueITException e) { } // Gets thrown when a request is already in progress.


## Required permissions

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.INTERNET"/>

## Activities to include in your manifest

    <activity android:name="com.queue_it.androidsdk.QueueActivity" />
