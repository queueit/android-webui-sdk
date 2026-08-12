package com.queue_it.androidsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

class WaitingRoomStateBroadcaster {

    private final Context _context;

    WaitingRoomStateBroadcaster(Context context) {
        _context = context;
    }

    public void registerReceivers(BroadcastReceiver onPassed,
                                  BroadcastReceiver onUrlChanged,
                                  BroadcastReceiver onActivityClosed,
                                  BroadcastReceiver onUserExited,
                                  BroadcastReceiver onError,
                                  BroadcastReceiver onWebViewClosed,
                                  BroadcastReceiver onSessionRestartReceiver) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(_context);

        localBroadcastManager.registerReceiver(onPassed, new IntentFilter("on-queue-passed"));
        localBroadcastManager.registerReceiver(onUrlChanged, new IntentFilter("on-changed-queue-url"));
        localBroadcastManager.registerReceiver(onActivityClosed, new IntentFilter("queue-activity-closed"));
        localBroadcastManager.registerReceiver(onUserExited, new IntentFilter("queue-user-exited"));
        localBroadcastManager.registerReceiver(onError, new IntentFilter("on-queue-error"));
        localBroadcastManager.registerReceiver(onWebViewClosed, new IntentFilter("on-webview-close"));
        localBroadcastManager.registerReceiver(onSessionRestartReceiver, new IntentFilter("on-session-restart"));
    }

    public void unregisterReceivers(BroadcastReceiver onPassed,
                                    BroadcastReceiver onUrlChanged,
                                    BroadcastReceiver onActivityClosed,
                                    BroadcastReceiver onUserExited,
                                    BroadcastReceiver onError,
                                    BroadcastReceiver onWebViewClosed,
                                    BroadcastReceiver onSessionRestartReceiver) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(_context);

        localBroadcastManager.unregisterReceiver(onPassed);
        localBroadcastManager.unregisterReceiver(onUrlChanged);
        localBroadcastManager.unregisterReceiver(onActivityClosed);
        localBroadcastManager.unregisterReceiver(onUserExited);
        localBroadcastManager.unregisterReceiver(onError);
        localBroadcastManager.unregisterReceiver(onWebViewClosed);
        localBroadcastManager.unregisterReceiver(onSessionRestartReceiver);
    }

    public void broadcastChangedQueueUrl(String urlString) {
        Intent intentChangedQueueUrl = new Intent("on-changed-queue-url");
        intentChangedQueueUrl.putExtra("url", urlString);
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intentChangedQueueUrl);
    }

    public void broadcastQueuePassed() {
        // Signal only: the token is delivered durably via PendingPassStore. This
        // broadcast just triggers immediate delivery while the process is alive.
        Intent intent = new Intent("on-queue-passed");
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    public void broadcastQueueActivityClosed() {
        Intent intent = new Intent("queue-activity-closed");
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    public void broadcastUserExited() {
        Intent intent = new Intent("queue-user-exited");
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    public void broadcastQueueError(String errorMessage) {
        Intent intent = new Intent("on-queue-error");
        intent.putExtra("error-message", errorMessage);
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    public void broadcastWebViewClosed() {
        Intent intent = new Intent("on-webview-close");
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    public void broadcastOnSessionRestart() {
        Intent intent = new Intent("on-session-restart");
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }
}
