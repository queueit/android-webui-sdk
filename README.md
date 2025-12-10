[ ![Download](https://maven-badges.herokuapp.com/maven-central/com.queue-it.androidsdk/library/badge.svg) ](https://repo1.maven.org/maven2/com/queue-it/androidsdk/)

# Queue-it Android WebUI SDK

Library for integrating Queue-it's virtual waiting room into an Android app written in java.

## Sample app

A sample app to try out functionality in the library can be found on the [Releases](https://github.com/queueit/android-sdk/releases) page.
This sample app uses the first approach of integration calling QueueITEngine run method.

## Installation

Before starting please download the whitepaper "Mobile App Integration" from GO Queue-it Platform.
This whitepaper contains the needed information to perform a successful integration.

Using Gradle:

```gradle
implementation 'com.queue-it.androidsdk:library:2.2.1'
```

## How to use the library (Mobile SDK integration only, no API protection)

As the App developer, you must manage the state (whether the user was previously queued up or not) inside the app's storage.

After you have received the `onQueuePassed` callback, the app must remember to keep the state, possibly with a date/time expiration.
When the user wants to navigate to specific screens on the app which needs Queue-it protection, your code check this state/variable, and only call SDK methods / `QueueITEngine.run` in the case where the user did not previously queue up.

Please note that when the user clicks back to navigate back to a protected screen, the same check needs to be done.

### Simple SDK integration using run method.

The simplest mobile SDK integration requires you to call one method, run() before showing the protected screen and potentially calling server API which needs peak traffic protection.
Invoke QueueITEngine as per example below. Parameters `layoutName`, `language`, `waitingRoomDomain`, `queuePathPrefix ` and `options` are optional. Use `null` to let the SDK make the decision for those parameters (see below parameter table).

```java
QueueListener queueListener = new QueueListener() {

    // This callback will be called when the user has been through the queue.
    // Here you should store session information, so user will only be sent to queue again if the session has timed out.
    @Override
    public void onQueuePassed(QueuePassedInfo queuePassedInfo) {
    }

    // This callback will be called just before the webview (hosting the queue page) will be shown.
    // Here you can change some relevant UI elements.
    @Override
    public void onQueueViewWillOpen() {
    }

    // This callback will be called when the queue used (event alias ID) is in the 'disabled' state.
    // Most likely the application should still function, but the queue's 'disabled' state can be changed at any time,
    // so session handling is important.
    @Override
    public void onQueueDisabled(QueueDisabledInfo queueDisabledInfo) {
    }

    // This callback will be called when the mobile application can't reach Queue-it's servers.
    // Most likely because the mobile device has no internet connection.
    // Here you decide if the application should function or not now that is has no queue-it protection.
    @Override
    public void onQueueItUnavailable() {
    }

    // This callback will be called when the mobile application can't reach Queue-it's servers.
    // It can be any one of these scenarios:
    // 1) Queue-it's servers can't be reached (connectivity issue).
    // 2) SSL connection error if custom queue domain is used having an invalid certificate.
    // 3) Client receives HTTP 4xx response.
    // In all these cases is most likely a misconfiguration of the queue settings:
    // Invalid customer ID, event alias ID or cname setting on queue (GO Queue-it portal -> event settings).
    @Override
    public void onError(Error error, String errorMessage) {
    } // Called on connectivity problems

    // This callback will be called after a user clicks a close link in the layout and the WebView closes.
    // The close link is "queueit://close". Whenever the user navigates to this link, the SDK intercepts the navigation
    // and closes the WebView.
    @Override
    public void onWebViewClosed(){
    }

    // This callback will be called when the user clicks on a link to restart the session.
    // The link is 'queueit://restartSession'. Whenever the user navigates to this link, the SDK intercepts the navigation,
    // closes the WebView, clears the URL cache and calls this callback.
    // In this callback you would normally call run/runWithToken/runWithKey in order to restart the queueing.
    @Override
    public void onSessionRestart(QueueITEngine queueITEngine) {
    }
};

QueueITEngine engine = new QueueITEngine(
    YourActivity.this,
    customerId,
    eventIdOrAlias,
    layoutName, // @Nullable (optional)
    language, // @Nullable (optional)
    waitingRoomDomain, // @Nullable (optional)
    queuePathPrefix, // @Nullable (optional)
    queueListener,
    options // @Nullable (optional)
);

try {
    engine.run(YourActivity.this);
}
catch (QueueITException e) { } // Gets thrown when a request is already in progress. In general you can ignore this.
```

#### `QueueITEngine.run` Parameters

> Note all the parameters must be passed to the `QueueITEngine` constructor. `run` is an instance function.

| Parameter         | Required (Default value)                 | Description                                                                                                                                                                                   |
| ----------------- | ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| activityContext   | Yes                                      | Your `Context`                                                                                                                                                                                |
| customerId        | Yes                                      | Your customer id                                                                                                                                                                              |
| eventIdOrAlias    | Yes                                      | Id of the waiting room or alias                                                                                                                                                               |
| layoutName        | No (Waiting Room's default theme)        | Layout name to use for the waiting room. If omitted, the Waiting Room's default layout will be used                                                                                           |
| language          | No (Waiting Room's default language)     | Language id to use for the waiting room. If omitted, the Waiting Room's default language will be used                                                                                         |
| waitingRoomDomain | No (`{customerId}.queue-it.net`)         | Custom Waiting Room domain to use for the requests from Mobile to Queue-it. Can be a Proxy Domain, if you are running Queue-it Behind Proxy                                                   |
| queuePathPrefix   | No (none)                                | Queue Path Prefix to use, if you are running Waiting Room on same domain as your normal website. Requires waitingRoomDomain to also be provided. If not, then this parameter will be ignored. |
| queueListener     | Yes                                      | Listener with callback functions. Must implement the `QueueListener` interface.                                                                                                               |
| options           | No (`QueueItEngineOptions.getDefault()`) | Allows you to configure the WebView used to show the Waiting Room. Can disable back button (default: disabled) and set a custom User Agent (default: "")                                      |

![App Integration Flow](https://github.com/queueit/android-webui-sdk/blob/master/App%20integration%20flow.PNG "App Integration Flow")

### QueueITEngine options

The QueueITEngine can be configured if you use the `options` argument in it's constructor. Here's an example.

```java
QueueItEngineOptions options = new QueueItEngineOptions();
// Use this if you want to disable the back button when the waiting room is shown
options.setBackButtonDisabledFromWR(true);

// Use this if you want to set a custom User Agent for the WebView when the waiting room is shown
options.setWebViewUserAgent("<user-agent>");

// ------ //

// If null is provided for the QueueItEngineOptions, the default options will be used:
QueueItEngineOptions.getDefault();
```

## Mobile SDK integration with tryPass and showQueue methods:

If you need finer granularity control over the mobile integration, you can use tryPass and showQueue instead of just using run method (which will open a webview to the Queue when needed).

`tryPass` allows you to check the state of the Waiting Room. Later you can show the waiting room using the `showQueue` function. QueueITEngine.run does the same, but with the granular setup, you can decide in which layer of your app, each step happens.
This provides you more control of the logic before potentially opening the webview and showing the Queue page, as well as more control over the webview showing the queue page.

### Checking status of Waiting room

It is possible to get the status of a waiting room to make sure it is ready to be visited. To do this, one of the below methods from `QueueITWaitingRoomProvider` class could be used.

- tryPass
- tryPassWithEnqueueToken
- tryPassWithEnqueueKey

Calling any of these methods will result in executing `onSuccess` or `onFailure` callbacks. These two callbacks must be provided by implementing `QueueITWaitingRoomProviderListener` interface and passed to the constructor of `QueueITWaitingRoomProvider` class, and will lead to below:

- If `isPassedThrough()` returns true, queueittoken and more information will be available as an argument to `OnSuccess` function with type of `QueueTryPassResult`.
- If `isPassedThrough()` returns false, it means that the waiting room is active. The waiting room page should be shown to the visitor by calling `showQueue` method of the `QueueITWaitingRoomView`, then the visitor will wait for its turn. The `showQueue` method needs `QueryTryPassResult` object from `OnSuccess` function.

### `QueueITWaitingRoomProvider` parameters

> Note the parameters are constructor parameters. The tree `tryPass` variants has different parameters (none, `enqueueToken` and `enqueueKey`).

| Parameter                          | Required (Default value)             | Description                                                                                                                                                                                   |
| ---------------------------------- | ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| activityContext                    | Yes                                  | Your `Context`                                                                                                                                                                                |
| customerId                         | Yes                                  | Your customer id                                                                                                                                                                              |
| eventIdOrAlias                     | Yes                                  | Id of the waiting room or alias                                                                                                                                                               |
| layoutName                         | No (Waiting Room's default theme)    | Layout name to use for the waiting room. If omitted, the Waiting Room's default layout will be used                                                                                           |
| language                           | No (Waiting Room's default language) | Language id to use for the waiting room. If omitted, the Waiting Room's default language will be used                                                                                         |
| waitingRoomDomain                  | No (`{customerId}.queue-it.net`)     | Custom Waiting Room domain to use for the requests from Mobile to Queue-it. Can be a Proxy Domain, if you are running Queue-it Behind Proxy                                                   |
| queuePathPrefix                    | No (none)                            | Queue Path Prefix to use, if you are running Waiting Room on same domain as your normal website. Requires waitingRoomDomain to also be provided. If not, then this parameter will be ignored. |
| queueITWaitingRoomProviderListener | Yes                                  | Listener with callback functions. Must implement the `QueueITWaitingRoomProviderListener` interface.                                                                                          |

### Showing the queue page to visitor

When waiting room is queueing the visitors, each visitor has to visit the waiting room page once. The queue page could be shown to visitors when it is necessary using `showQueue` method of `QueueITWaitingRoomView` class.
Before calling `showQueue`, the status of the waiting room should be already retrieved as described in [Get waiting room status](#Get-waiting-room-status) to make sure that the waiting room is ready.

sample code for showing the queue page:

```java
QueueITWaitingRoomView queueITWaitingRoomView = new QueueITWaitingRoomView(MainActivity.this, queueListener, queueItEngineOptions);
queueITWaitingRoomView.showQueue(_queuePassedInfo.getQueueUrl(), _queuePassedInfo.getTargetUrl());
```

## Client-side mobile integration with Queue-it Behind Proxy (Bring your own CDN)

> Note: This only applies if you are using the Mobile SDK as a client-side protection only. I.e. this does not apply if you are protecting the API endpoints your Mobile app is using.

> If you have server-side Queue-it protection on your API endpoints, please see section [below](#mobile-sdk-integration-with-protected-api-queue-it-connector-on-server-side).

If you are running Queue-it behind your own reverse proxy the Mobile Integration can also be setup to run behind your proxy.

To do this simply use your Proxy Domain as the `waitingRoomDomain` parameter to `QueueITEngine.run`. If you are running Queue-it Waiting Room on the same domain as your normal website, you also need to provide the `queuePathPrefix` parameter, to ensure your proxy can route the request to Queue-it origin.

## Mobile SDK Integration with protected API (Queue-it connector on server side):

If your application is using an API that's protected by a Queue-it server-side connector (KnownUser) you can check out [this documentation](https://github.com/queueit/android-webui-sdk/blob/master/documentation/protected_apis.md).

## Required permissions

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

## Activities to include in your manifest

```xml
<activity android:name="com.queue_it.androidsdk.QueueActivity" />
```
