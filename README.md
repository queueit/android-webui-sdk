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
implementation 'com.queue-it.androidsdk:library:2.1.4'
//For AndroidX
//implementation 'com.queue-it.androidsdk:library-androidx:2.1.4'
```

## How to use the library (Mobile SDK integration only, no API protection)

As the App developer, you must manage the state (whether the user was previously queued up or not) inside the app's storage.

After you have received the **onQueuePassed** callback, the app must remember to keep the state, possibly with a date/time expiration.
When the user wants to navigate to specific screens on the app which needs Queue-it protection, your code check this state/variable, and only call SDK methods / **QueueITEngine.run** in the case where the user did not previously queue up.

Please note that when the user clicks back to navigate back to a protected screen, the same check needs to be done.

### Simple SDK integration using run method. 
The simplest mobile SDK integration requires you to call one method, run() before showing the protected screen and potentially calling server API which needs peak traffic protection. 
Invoke QueueITEngine as per example below. Parameters `layoutName`, `language` and `options` are optional.

```java
QueueITEngine engine = new QueueITEngine(YourActivity.this, customerId, eventIdOrAlias, layoutName, language,
  new QueueListener() {

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
  });

  try {
    engine.run(YourActivity.this);
  }
  catch (QueueITException e) { } // Gets thrown when a request is already in progress. In general you can ignore this.
```



![App Integration Flow](https://github.com/queueit/android-webui-sdk/blob/master/App%20integration%20flow.PNG "App Integration Flow")


### QueueITEngine options

The QueueITEngine can be configured if you use the `options` argument in it's constructor. Here's an example.

```java
QueueItEngineOptions options = new QueueItEngineOptions();
// Use this if you want to disable the back button when the waiting room is shown
options.setBackButtonDisabledFromWR(true);
```

## Mobile SDK integration with tryPass and showQueue methods: 

If you need finner granularity control over the mobile integration, you can use tryPass and showQue instead of just using run method which will open a webview to the Queue when needed. 

This provides you more control of the logic before potentially opening the webview and showing the Queue page, as well as more control over the webview showing the queue page. 

### Checking status of Waiting room

It is possible to get the status of a waiting room to make sure it is ready to be visited. To do this, one of the below methods from **QueueITWaitingRoomProvider** class could be used.

- tryPass
- tryPassWithEnqueueToken
- tryPassWithEnqueueKey

Calling any of these methods will result in executing **onSuccess** or **onFailure** callbacks. These two callbacks must be provided by implementing **QueueITWaitingRoomProviderListener** interface and passed to the constructor of **QueueITWaitingRoomProvider** class, and will lead to below:

- If **isPassedThrough()** retuns true, queueittoken and more information will be available as an argument to **OnSuccess** function with type of **QueueTryPassResult**.
- If **isPassedThrough()** returns false, it means that the waiting room is active. The waitingroom page should be shown to the visitor by calling **showQueue** method of the **QueueITWaitingRoomView**, then the visitor will wait for its turn. The **showQueue** method needs **QueryTryPassResult** object from **OnSuccess** function.

## Showing the queue page to visitor

When waiting room is queueing the visitors, each visitor has to visit the waiting room page once. The queue page could be shown to visitors when it is necessary using **showQueue** method of **QueueITWaitingRoomView** class. 
Before calling **showQueue**, the status of the waiting room should be already retrieved as described in [Get waiting room status](#Get-waiting-room-status) to make sure that the waiting room is ready. 

sample code for showing the queue page:

```java
QueueITWaitingRoomView queueITWaitingRoomView = new QueueITWaitingRoomView(MainActivity.this, queueListener, queueItEngineOptions);
queueITWaitingRoomView.showQueue(_queuePassedInfo.getQueueUrl(), _queuePassedInfo.getTargetUrl());
```
## Mobile SDK Integration with proteced API (Queue-it connector on server side): 
If your application is using an API that's protected by a Queue-it connector (KnownUser) you can check out [this documentation](https://github.com/queueit/android-webui-sdk/blob/master/documentation/protected_apis.md).

## Required permissions

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

## Activities to include in your manifest

```xml
<activity android:name="com.queue_it.androidsdk.QueueActivity" />
```