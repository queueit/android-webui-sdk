package com.queue_it.shopdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.queue_it.androidsdk.*;
import com.queue_it.androidsdk.Error;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionButton queue_button = (FloatingActionButton)findViewById(R.id.queue_button);

        final EditText customerIdEditText = (EditText)findViewById(R.id.customerid_edittext);
        final EditText eventIdEditText = (EditText)findViewById(R.id.eventid_edittext);
        final EditText layoutNameEditText = (EditText)findViewById(R.id.layoutname_edittext);
        final EditText languageEditText = (EditText)findViewById(R.id.language_edittext);
        final RadioButton testRadioButton = (RadioButton)findViewById(R.id.radio_environment_test);
        final RadioButton enableCacheRadioButton = (RadioButton)findViewById(R.id.radio_cache_enabled);

        customerIdEditText.addTextChangedListener(getRequiredTextValidator(customerIdEditText));
        eventIdEditText.addTextChangedListener(getRequiredTextValidator(eventIdEditText));

        final SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        String customerId = sharedPreferences.getString("customerId", "");
        String eventOrAliasId = sharedPreferences.getString("eventOrAliasId", "");
        String layoutName = sharedPreferences.getString("layoutName", "");
        String language = sharedPreferences.getString("language", "");

        customerIdEditText.setText(customerId);
        eventIdEditText.setText(eventOrAliasId);
        layoutNameEditText.setText(layoutName);
        languageEditText.setText(language);

        queue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(customerIdEditText.getError()) || !TextUtils.isEmpty(eventIdEditText.getError())) {
                    return;
                }

                queue_button.setEnabled(false);
                QueueService.IsTest = testRadioButton.isChecked();
                hideKeyboard();

                String customerId = customerIdEditText.getText().toString();
                String eventOrAliasId = eventIdEditText.getText().toString();
                String layoutName = layoutNameEditText.getText().toString();
                String language = languageEditText.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("customerId", customerId);
                editor.putString("eventOrAliasId", eventOrAliasId);
                editor.putString("layoutName", layoutName);
                editor.putString("language", language);
                editor.commit();

                Toast.makeText(getApplicationContext(), "Please wait for your turn.", Toast.LENGTH_SHORT).show();

                QueueITEngine queueITEngine = new QueueITEngine(MainActivity.this, customerId, eventOrAliasId, layoutName, language, new QueueListener() {
                    @Override
                    public void onQueuePassed(QueuePassedInfo queuePassedInfo) {
                        showResultActivity("You passed the queue! Your token: " + queuePassedInfo.getQueueItToken(), true);
                        queue_button.setEnabled(true);
                    }

                    @Override
                    public void onQueueViewWillOpen() {
                        Toast.makeText(getApplicationContext(), "onQueueViewWillOpen", Toast.LENGTH_SHORT).show();
                        queue_button.setEnabled(true);
                    }

                    @Override
                    public void onQueueDisabled() {
                        showResultActivity("The queue is disabled.", false);
                        queue_button.setEnabled(true);
                    }

                    @Override
                    public void onQueueItUnavailable() {
                        showResultActivity("Queue-it is unavailable", false);
                        queue_button.setEnabled(true);
                    }

                    @Override
                    public void onError(Error error, String errorMessage) {
                        showResultActivity("Critical error: " + errorMessage, false);
                        queue_button.setEnabled(true);
                    }
                });
                try {
                    //queueITEngine.run(MainActivity.this, !enableCacheRadioButton.isChecked());
                    queueITEngine.run(MainActivity.this);
                }
                catch (QueueITException e) {
                    Toast.makeText(getApplicationContext(), "Please try again.", Toast.LENGTH_LONG).show();
                    queue_button.setEnabled(true);
                }
            }
        });
    }

    private void showResultActivity(String result, boolean success)
    {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("success", success);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    private boolean isAlphaNumeric(String s){
        String pattern = "^[a-zA-Z0-9]*$";
        return s.matches(pattern);
    }

    private void hideKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private TextValidator getRequiredTextValidator(TextView textView)
    {
        return new TextValidator(textView) {
            @Override public void validate(TextView textView, String text) {
                if (TextUtils.isEmpty(text))
                {
                    textView.setError("Field required");
                }
                else if (!isAlphaNumeric(text))
                {
                    textView.setError("Must be alphanumeric");
                }
            }
        };
    }
}
