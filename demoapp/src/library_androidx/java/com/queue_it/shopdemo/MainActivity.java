package com.queue_it.shopdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.queue_it.androidsdk.*;
import com.queue_it.androidsdk.Error;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton queue_button;
    FloatingActionButton queueSession_button;
    FloatingActionButton show_queue_button;
    EditText customerIdEditText;
    EditText eventIdEditText;
    EditText layoutNameEditText;
    EditText languageEditText;
    EditText enqueueTokenEditText;
    EditText enqueueKeyEditText;
    RadioButton testRadioButton;
    private QueueTryPassResult _queueTryPassResult;


    QueueListener queueListener = new QueueListener() {

        @Override
        public void onSessionRestart(QueueITEngine queueITEngine) {
            try {
                runQueue(queueITEngine);
            } catch (QueueITException e) {
                Toast.makeText(getApplicationContext(), "Please try again.", Toast.LENGTH_LONG).show();
                queue_button.setEnabled(true);
            }
        }

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
        public void onUserExited() {
            Toast.makeText(getApplicationContext(), "onUserExited", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onQueueDisabled(QueueDisabledInfo queueDisabledInfo) {
            showResultActivity("The queue is disabled. Your token: " + queueDisabledInfo.getQueueItToken(), false);
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

        @Override
        public void onWebViewClosed() {
            showResultActivity("WebView closed ", false);
            queue_button.setEnabled(true);
        }
    };

    private void runQueue(QueueITEngine queueITEngine) throws QueueITException {
        String enqueueToken = enqueueTokenEditText.getText().toString();
        String enqueueKey = enqueueKeyEditText.getText().toString();
        if (enqueueToken.length() > 0) {
            queueITEngine.runWithEnqueueToken(this, enqueueToken);
        } else if (enqueueKey.length() > 0) {
            queueITEngine.runWithEnqueueKey(this, enqueueKey);
        } else {
            queueITEngine.run(this);
        }
    }

    private void runSessionEngine(QueueITWaitingRoomProvider queueITWaitingRoomProvider) throws QueueITException {
        String enqueueToken = enqueueTokenEditText.getText().toString();
        String enqueueKey = enqueueKeyEditText.getText().toString();
        if (enqueueToken.length() > 0) {
            queueITWaitingRoomProvider.tryPassWithEnqueueToken(enqueueToken);
        } else if (enqueueKey.length() > 0) {
            queueITWaitingRoomProvider.tryPassWithEnqueueKey(enqueueKey);
        } else {
            queueITWaitingRoomProvider.tryPass();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue_button = findViewById(R.id.queue_button);
        queueSession_button = findViewById(R.id.queueSession_button);
        show_queue_button = findViewById(R.id.show_queue_button);
        customerIdEditText = findViewById(R.id.customerid_edittext);
        eventIdEditText = findViewById(R.id.eventid_edittext);
        layoutNameEditText = findViewById(R.id.layoutname_edittext);
        languageEditText = findViewById(R.id.language_edittext);
        testRadioButton = findViewById(R.id.radio_environment_test);
        customerIdEditText.addTextChangedListener(getRequiredTextValidator(customerIdEditText));
        eventIdEditText.addTextChangedListener(getRequiredTextValidator(eventIdEditText));
        enqueueTokenEditText = findViewById(R.id.enqueuetoken_edittext);
        enqueueKeyEditText = findViewById(R.id.enqueuekey_edittext);

        final SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        String customerId = sharedPreferences.getString("customerId", "");
        String eventOrAliasId = sharedPreferences.getString("eventOrAliasId", "");
        String layoutName = sharedPreferences.getString("layoutName", "");
        String language = sharedPreferences.getString("language", "");
        String enqueueToken = sharedPreferences.getString("enqueueToken", "");
        String enqueueKey = sharedPreferences.getString("enqueueKey", "");

        customerIdEditText.setText(customerId);
        eventIdEditText.setText(eventOrAliasId);
        layoutNameEditText.setText(layoutName);
        languageEditText.setText(language);
        enqueueTokenEditText.setText(enqueueToken);
        enqueueKeyEditText.setText(enqueueKey);

        queue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueueITEngine queueITEngine = getQueueITEngine(sharedPreferences);
                try {
                    runQueue(queueITEngine);
                } catch (QueueITException e) {
                    e.printStackTrace();
                }
            }
        });

        queueSession_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueueITWaitingRoomProvider queueITWaitingRoomProvider = getSessionEngine(sharedPreferences);
                try {
                    runSessionEngine(queueITWaitingRoomProvider);
                } catch (QueueITException e) {
                    e.printStackTrace();
                }
            }
        });

        show_queue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_queueTryPassResult != null && _queueTryPassResult.getRedirectType() == RedirectType.queue) {
                    QueueItEngineOptions queueItEngineOptions = getQueueItEngineOptions();
                    QueueITWaitingRoomView queueITWaitingRoomView =
                            new QueueITWaitingRoomView(MainActivity.this, queueListener, queueItEngineOptions);
                    queueITWaitingRoomView.showQueue(_queueTryPassResult, queueItEngineOptions.getWebViewUserAgent() );
                    _queueTryPassResult = null;
                }
            }
        });
    }

    private QueueITEngine getQueueITEngine(SharedPreferences sharedPreferences) {
        if (!TextUtils.isEmpty(customerIdEditText.getError()) || !TextUtils.isEmpty(eventIdEditText.getError())) {
            return null;
        }

        queue_button.setEnabled(false);
        QueueITApiClient.IsTest = testRadioButton.isChecked();
        hideKeyboard();

        String customerId = customerIdEditText.getText().toString();
        String eventOrAliasId = eventIdEditText.getText().toString();
        String layoutName = layoutNameEditText.getText().toString();
        String language = languageEditText.getText().toString();
        String enqueueToken = enqueueTokenEditText.getText().toString();
        String enqueueKey = enqueueKeyEditText.getText().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("customerId", customerId);
        editor.putString("eventOrAliasId", eventOrAliasId);
        editor.putString("layoutName", layoutName);
        editor.putString("language", language);
        editor.putString("enqueueToken", enqueueToken);
        editor.putString("enqueueKey", enqueueKey);
        editor.commit();

        Toast.makeText(getApplicationContext(), "Please wait for your turn.", Toast.LENGTH_SHORT).show();
        QueueItEngineOptions options = getQueueItEngineOptions();
        return new QueueITEngine(MainActivity.this, customerId, eventOrAliasId, layoutName, language, queueListener, options);
    }

    @NonNull
    private QueueItEngineOptions getQueueItEngineOptions() {
        QueueItEngineOptions options = new QueueItEngineOptions();
        options.setBackButtonDisabledFromWR(false);
        return options;
    }


    private QueueITWaitingRoomProvider getSessionEngine(SharedPreferences sharedPreferences) {
        if (!TextUtils.isEmpty(customerIdEditText.getError()) || !TextUtils.isEmpty(eventIdEditText.getError())) {
            return null;
        }

        queue_button.setEnabled(false);
        QueueITApiClient.IsTest = testRadioButton.isChecked();
        hideKeyboard();

        String customerId = customerIdEditText.getText().toString();
        String eventOrAliasId = eventIdEditText.getText().toString();
        String layoutName = layoutNameEditText.getText().toString();
        String language = languageEditText.getText().toString();
        String enqueueToken = enqueueTokenEditText.getText().toString();
        String enqueueKey = enqueueKeyEditText.getText().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("customerId", customerId);
        editor.putString("eventOrAliasId", eventOrAliasId);
        editor.putString("layoutName", layoutName);
        editor.putString("language", language);
        editor.putString("enqueueToken", enqueueToken);
        editor.putString("enqueueKey", enqueueKey);
        editor.commit();

        Toast.makeText(getApplicationContext(), "Please wait for your turn.", Toast.LENGTH_SHORT).show();


        QueueITWaitingRoomProviderListener queueITWaitingRoomProviderListener = new QueueITWaitingRoomProviderListener() {
            @Override
            public void onSuccess(QueueTryPassResult queueTryPassResult) {
                _queueTryPassResult = queueTryPassResult;
                Toast.makeText(getApplicationContext(), String.format("Queue status: %s", _queueTryPassResult.getRedirectType() ), Toast.LENGTH_SHORT).show();
                queue_button.setEnabled(true);
            }

            @Override
            public void onFailure(String errorMessage, Error errorCode) {
                Log.e("QueueITEngine",errorMessage);
                Toast.makeText(getApplicationContext(), String.format("Error! %s", errorMessage), Toast.LENGTH_SHORT).show();
            }
        };

        return new QueueITWaitingRoomProvider(MainActivity.this, customerId, eventOrAliasId, layoutName, language, queueITWaitingRoomProviderListener);
    }


    private void showResultActivity(String result, boolean success) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("success", success);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    private boolean isAlphaNumeric(String s) {
        String pattern = "^[a-zA-Z0-9]*$";
        return s.matches(pattern);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private TextValidator getRequiredTextValidator(TextView textView) {
        return new TextValidator(textView) {
            @Override
            public void validate(TextView textView, String text) {
                if (TextUtils.isEmpty(text)) {
                    textView.setError("Field required");
                } else if (!isAlphaNumeric(text)) {
                    textView.setError("Must be alphanumeric");
                }
            }
        };
    }
}
