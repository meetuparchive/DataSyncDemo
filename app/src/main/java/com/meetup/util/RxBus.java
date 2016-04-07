package com.meetup.util;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Timestamped;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * Rx-based event bus.  Events are cached for some amount of time for replaying.  An example use
 * case would be an Activity that displays a list of photos, caches it `onSaveInstanceState`, and
 * also listens for deletion events.  Data sync bugs can spawn if deletions occur while the
 * Activity is destroyed and the Activity is re-created with the now-invalid list of photos.
 */
@Singleton
public class RxBus {
    public static final int TTL_SECONDS = 120;

    final Scheduler scheduler;
    final Subject<Timestamped<Object>, Timestamped<Object>> subject;

    public RxBus(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.subject = ReplaySubject.<Timestamped<Object>>createWithTime(TTL_SECONDS, TimeUnit.SECONDS, scheduler)
                .toSerialized();
    }

    private void post(Object value) {
        subject.onNext(new Timestamped<>(scheduler.now(), value));
    }

    private Observable<Timestamped<Object>> observable(long savedElapsedRealtime) {
        long since = savedElapsedRealtime >= 0 ? savedElapsedRealtime : scheduler.now();
        return subject.filter(t -> t.getTimestampMillis() >= since);
    }

    @Singleton
    public static class Driver<T> {
        private final RxBus bus;
        private final Class<T> klass;

        public Driver(RxBus bus, Class<T> klass) {
            this.bus = bus;
            this.klass = klass;
        }

        /**
         * Posts an event of type `T`.
         */
        public void post(T value) {
            bus.post(value);
        }

        /**
         * Get an Rx observable that emits events posted since `savedTime`.  If `savedTime` is negative,
         * no past events will be emitted.  The idea is that a client with no cached data doesn't have
         * a stale cache that needs updating.
         *
         * @param savedElapsedRealtime IMPORTANT: Must be obtained from `SystemClock.elapsedRealtime()`
         */
        @SuppressWarnings("unchecked")
        public Observable<T> observable(long savedElapsedRealtime) {
            return (Observable) bus.observable(savedElapsedRealtime)
                    .filter(t -> klass.isInstance(t.getValue()))
                    .map(Timestamped::getValue);
        }

        /**
         * Get an Rx observable that emits only events posted in the future.
         */
        public Observable<T> observable() {
            return observable(-1L);
        }
    }
}