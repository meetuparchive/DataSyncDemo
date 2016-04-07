package com.meetup.demo.datasync.application;

import android.app.Application;
import android.content.Context;

import com.meetup.demo.datasync.dagger.AppComponent;
import com.meetup.demo.datasync.dagger.AppModule;
import com.meetup.demo.datasync.dagger.DaggerAppComponent;

public class DemoApplication extends Application {
    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        this.component = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }

    public static AppComponent component(Context context) {
        return ((DemoApplication) context.getApplicationContext()).component;
    }
}
