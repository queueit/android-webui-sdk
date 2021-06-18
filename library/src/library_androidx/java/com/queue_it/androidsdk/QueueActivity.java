package com.queue_it.androidsdk;

import android.app.Activity;
import android.os.Bundle;

public class QueueActivity extends Activity {

    private QueueActivityBase base = new QueueActivityBase(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        base.initialize(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (base.getOptions().isBackButtonDisabledFromWR()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        base.saveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        base.destroy();
        super.onDestroy();
    }
}
