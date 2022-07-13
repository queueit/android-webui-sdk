package com.example.demowithprotectedapi;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.demowithprotectedapi.api.Product;
import com.example.demowithprotectedapi.databinding.FragmentFirstBinding;
import com.example.demowithprotectedapi.exceptions.MustBeQueued;
import com.example.demowithprotectedapi.repos.IProductRepository;
import com.example.demowithprotectedapi.repos.RetrofitProductRepository;
import com.queue_it.androidsdk.Error;
import com.queue_it.androidsdk.QueueDisabledInfo;
import com.queue_it.androidsdk.QueueITApiClient;
import com.queue_it.androidsdk.QueueITEngine;
import com.queue_it.androidsdk.QueueITException;
import com.queue_it.androidsdk.QueueListener;
import com.queue_it.androidsdk.QueuePassedInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private IProductRepository _productRepo;
    private final Object queuedLock = new Object();
    private final AtomicBoolean _queuePassed = new AtomicBoolean(false);

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        _productRepo = new RetrofitProductRepository("https://fastly.v3.ticketania.com");
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(view1 -> {
            ProductHandler productHandler = new ProductHandler();
            productHandler.execute();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void queueUser(String value) {
        try {
            Uri valueUri = Uri.parse(URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
            String customerId = valueUri.getQueryParameter("c");
            String wrId = valueUri.getQueryParameter("e");
            QueueITApiClient.IsTest = true;
            final QueueITEngine q = new QueueITEngine(MainActivity.getInstance(), customerId, wrId, "", "", new QueueListener() {
                @Override
                protected void onSessionRestart(QueueITEngine queueITEngine) {
                    try {
                        queueITEngine.run(MainActivity.getInstance());
                    } catch (QueueITException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                protected void onQueuePassed(QueuePassedInfo queuePassedInfo) {
                    _productRepo.addQueueToken(queuePassedInfo.getQueueItToken());
                    _queuePassed.set(true);
                    Toast.makeText(MainActivity.getInstance(), "You passed the queue! You can try again.", Toast.LENGTH_SHORT).show();
                    synchronized (queuedLock) {
                        queuedLock.notify();
                    }
                }

                @Override
                public void onQueueViewWillOpen() {
                    Toast.makeText(MainActivity.getInstance(), "onQueueViewWillOpen", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUserExited() {
                    Toast.makeText(MainActivity.getInstance(), "onUserExited", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onQueueDisabled(QueueDisabledInfo queueDisabledInfo) {
                    Toast.makeText(MainActivity.getInstance(), "The queue is disabled. Your token: " + queueDisabledInfo.getQueueItToken()
                            , Toast.LENGTH_SHORT).show();
                    _productRepo.addQueueToken(queueDisabledInfo.getQueueItToken());
                    _queuePassed.set(true);
                    synchronized (queuedLock) {
                        queuedLock.notify();
                    }
                }

                @Override
                public void onQueueItUnavailable() {
                    Toast.makeText(MainActivity.getInstance(), "Queue-it is unavailable", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Error error, String errorMessage) {
                    Toast.makeText(MainActivity.getInstance(), "Critical error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onWebViewClosed() {
                    Toast.makeText(MainActivity.getInstance(), "WebView closed", Toast.LENGTH_SHORT).show();
                }
            });
            q.run(MainActivity.getInstance());
        } catch (QueueITException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public class ProductHandler extends AsyncTask<Void, Void, Product> {


        @Override
        protected Product doInBackground(Void[] objects) {
            synchronized (queuedLock) {
                try {
                    return _productRepo.getProduct();
                } catch (IOException e) {
                    if (!(e instanceof MustBeQueued)) {
                        e.printStackTrace();
                    }
                    assert e instanceof MustBeQueued;
                    Handler handler = new Handler(MainActivity.getInstance().getMainLooper());
                    handler.post(() -> queueUser(((MustBeQueued) e).getValue()));

                    //Maybe wait for completion and repeat the call? This is optional.
                    try {
                        while (!_queuePassed.get()) {
                            queuedLock.wait();
                        }
                        Thread.sleep(1000);
                        return _productRepo.getProduct();
                    } catch (InterruptedException | IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Product p) {
            if (p == null) {
                Toast.makeText(MainActivity.getInstance(), "Couldn't fetch product", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.getInstance(), "Fetched product: " + p.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}