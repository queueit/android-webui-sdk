package com.queue_it.androidsdk;

import android.content.BroadcastReceiver;

public interface IWaitingRoomStateBroadcaster {
    void broadcastChangedQueueUrl(String urlString);

    void broadcastQueuePassed(String queueItToken);

    void broadcastQueueActivityClosed();

    void broadcastUserExited();

    void broadcastQueueError(String errorMessage);

    void broadcastWebViewClosed();

    void registerReceivers(BroadcastReceiver queuePassedBroadcastReceiver,
                           BroadcastReceiver queueUrlChangedBroadcastReceiver,
                           BroadcastReceiver queueActivityClosedBroadcastReceiver,
                           BroadcastReceiver queueUserExitedBroadcastReceiver,
                           BroadcastReceiver queueErrorBroadcastReceiver,
                           BroadcastReceiver webViewClosedBroadcastReceiver);

    void unregisterReceivers(BroadcastReceiver queuePassedBroadcastReceiver,
                             BroadcastReceiver queueUrlChangedBroadcastReceiver,
                             BroadcastReceiver queueActivityClosedBroadcastReceiver,
                             BroadcastReceiver queueUserExitedBroadcastReceiver,
                             BroadcastReceiver queueErrorBroadcastReceiver,
                             BroadcastReceiver webViewClosedBroadcastReceiver);
}
