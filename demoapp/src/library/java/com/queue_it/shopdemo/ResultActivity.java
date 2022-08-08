package com.queue_it.shopdemo;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String result;
        boolean success;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                result = null;
                success = false;
            } else {
                result = extras.getString("result");
                success = extras.getBoolean("success");
            }
        } else {
            result = (String) savedInstanceState.getSerializable("result");
            success = (Boolean) savedInstanceState.getSerializable("success");
        }

        final TextView resultText = (TextView)findViewById(R.id.result_text);
        final ImageView checkedImageView = (ImageView)findViewById(R.id.image_view_checked);
        final FloatingActionButton retry_button = (FloatingActionButton)findViewById(R.id.retry_button);

        resultText.setText(result);
        if (success){
            checkedImageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_check_black_24dp, null));
        } else {
            checkedImageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_black_24dp, null));
        }

        retry_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
