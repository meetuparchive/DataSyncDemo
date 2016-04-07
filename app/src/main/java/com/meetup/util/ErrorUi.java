package com.meetup.util;

import android.content.Context;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public final class ErrorUi {
    private ErrorUi() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }

    // should probably only use this for debugging
    public static <T> Observable.Transformer<T, T> catchAndToast(Context context) {
        final Action1<Throwable> action = toast(context);

        return obs -> obs.onErrorResumeNext((t) -> {
            Observable.just(t)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(action);

            return Observable.empty();
        });
    }

    public static Action1<Throwable> toast(Context context) {
        return toast(new WeakReference<>(context));
    }

    public static Action1<Throwable> toast(final WeakReference<Context> contextRef) {
        return (t) -> {
            Context ctx = contextRef.get();
            if (ctx != null) {
                Toast.makeText(ctx, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }
}
