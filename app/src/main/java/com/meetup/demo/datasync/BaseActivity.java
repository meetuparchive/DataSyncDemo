package com.meetup.demo.datasync;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public abstract class BaseActivity extends AppCompatActivity {
    private final String EXTRA_ELAPSED_REALTIME = "elapsedRealtime";

    // see `RxBus` docs
    protected long savedElapsedRealtime = -1L;
    protected long pausedElapsedRealtime = -1L;

    // Rx subscriptions that are active from `onCreate` to `onDestroy`
    protected CompositeSubscription subs = Subscriptions.from();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subs = Subscriptions.from();
        savedElapsedRealtime = -1L;
        if (savedInstanceState != null) {
            savedElapsedRealtime = savedInstanceState.getLong(EXTRA_ELAPSED_REALTIME, -1L);
        }
    }

    @Override
    protected void onPause() {
        pausedElapsedRealtime = SystemClock.elapsedRealtime();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        subs.unsubscribe();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_ELAPSED_REALTIME, SystemClock.elapsedRealtime());
    }
}
