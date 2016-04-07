package com.meetup.util;

import android.os.SystemClock;

import rx.Scheduler;

/**
 * Wraps a scheduler to change `now()` to return `SystemClock.elapsedRealtime()`
 */
public class RealtimeScheduler extends Scheduler {
    private Scheduler delegate;

    public RealtimeScheduler(Scheduler delegate) {
        this.delegate = delegate;
    }

    @Override
    public Worker createWorker() {
        return delegate.createWorker();
    }

    @Override
    public long now() {
        return SystemClock.elapsedRealtime();
    }
}
