package com.meetup.demo.datasync.dagger;

import javax.inject.Singleton;

import dagger.Component;

import com.meetup.demo.datasync.MembersAdapter;
import com.meetup.demo.datasync.ViewMemberDetails;
import com.meetup.demo.datasync.ViewMembers;

@Singleton
@Component(modules = { AppModule.class, SchedulerModule.class })
public interface AppComponent {
    void inject(MembersAdapter target);
    void inject(ViewMembers target);
    void inject(ViewMemberDetails target);
}
