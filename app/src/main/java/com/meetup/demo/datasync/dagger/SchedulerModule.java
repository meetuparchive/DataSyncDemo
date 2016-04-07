package com.meetup.demo.datasync.dagger;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import com.meetup.util.RealtimeScheduler;

@Module
public class SchedulerModule {
    @Provides
    @Singleton
    @Named("computation")
    Scheduler providesComputationScheduler() {
        return new RealtimeScheduler(Schedulers.computation());
    }

    @Provides
    @Singleton
    @Named("io")
    Scheduler providesIoScheduler() {
        return new RealtimeScheduler(Schedulers.io());
    }
}
