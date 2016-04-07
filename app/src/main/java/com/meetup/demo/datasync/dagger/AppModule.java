package com.meetup.demo.datasync.dagger;

import android.app.Application;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import com.meetup.demo.datasync.api.DemoApi;
import com.meetup.demo.datasync.api.DemoApiBase;
import com.meetup.demo.datasync.bus.MemberUpdate;
import com.meetup.util.RealtimeScheduler;
import com.meetup.util.RxBus;

@Module(includes = SchedulerModule.class)
public class AppModule {
    Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    @Named("ui")
    Scheduler provideUiScheduler() {
        return new RealtimeScheduler(AndroidSchedulers.mainThread());
    }

    @Provides
    @Singleton
    DemoApi provideDemoApi(@Named("io") Scheduler scheduler) {
        return new DemoApiBase(scheduler);
    }

    @Provides
    @Singleton
    RxBus provideBus(@Named("computation") Scheduler scheduler) {
        return new RxBus(scheduler);
    }

    @Provides
    @Singleton
    RxBus.Driver<MemberUpdate> provideMemberEditBus(RxBus bus) {
        return new RxBus.Driver<>(bus, MemberUpdate.class);
    }
}
