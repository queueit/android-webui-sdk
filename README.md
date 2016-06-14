# Queue-it Android SDK

Library for integrating Queue-it into an Android app.

## Installation

Using Gradle:

    compile 'com.queue_it.com.sdk:sdk:2.0'

## Usage

    QueueITEngine engine = new QueueITEngine(YourActivity.this, customerId, eventId, layoutName, language,
      new QueueListener() {
        @Override
        public void onQueuePassed() { } // Your logic

        @Override
        public void onQueueViewWillOpen() { } // Your logic

        @Override
        public void onQueueDisabled() { } // Your logic

        @Override
        public void onQueueItUnavailable() { } // Your logic

        @Override
        public void onError(String errorMessage) { } // Your logic
      });

      try {
        engine.run();
      }
      catch (QueueITException e) { } // Your logic

## Required permissions

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.INTERNET"/>

## Activities to include in your manifest

    <activity android:name="com.queue_it.androidsdk.QueueActivity" />
