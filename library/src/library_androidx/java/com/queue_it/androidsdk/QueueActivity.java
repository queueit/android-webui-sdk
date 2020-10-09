package com.queue_it.androidsdk;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class QueueActivity extends AppCompatActivity {

    private QueueActivityBase base = new QueueActivityBase(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        base.initialize(savedInstanceState);
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
