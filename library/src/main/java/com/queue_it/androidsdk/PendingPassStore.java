package com.queue_it.androidsdk;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Durable, single-slot store for a queue pass token.
 *
 * <p>The waiting-room result is normally delivered in-process (see
 * {@link WaitingRoomStateBroadcaster}), which does not survive the app process
 * being killed while backgrounded. This store persists the token the moment it
 * is intercepted so the pass can still be delivered after the process is
 * restarted, via {@link QueueITEngine#consumePendingPass(Context, QueueListener)}.
 *
 * <p>{@link #takeToken(Context)} reads and clears atomically, so the token is
 * delivered at most once regardless of whether the live path or the recovery
 * path runs first.
 */
final class PendingPassStore {
    private static final String PREFS_NAME = "queueit_sdk_pending_pass";
    private static final String KEY_TOKEN = "queue_it_token";

    private PendingPassStore() {
    }

    static void save(Context context, String queueItToken) {
        // commit() (synchronous) so the token is durable even if the process is
        // killed immediately after the pass.
        prefs(context).edit().putString(KEY_TOKEN, queueItToken).commit();
    }

    static synchronized String takeToken(Context context) {
        SharedPreferences prefs = prefs(context);
        String queueItToken = prefs.getString(KEY_TOKEN, null);
        if (queueItToken != null) {
            prefs.edit().remove(KEY_TOKEN).commit();
        }
        return queueItToken;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
