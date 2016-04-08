package com.meetup.demo.datasync.api;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import rx.Observable;
import rx.Scheduler;

import com.meetup.demo.datasync.model.Member;

@Singleton
public class DemoApiBase implements DemoApi {
    private final Scheduler scheduler;

    // demo fake stuff
    private final Random random;
    private final AtomicLong nextId = new AtomicLong(1);
    private final List<Member> membersList = Lists.newArrayList();

    public DemoApiBase(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.random = new Random();
        initData();
    }

    private void initData() {
        for (int i = 0; i < 260; i++) {
            long id = nextId.getAndIncrement();
            membersList.add(Member.dummy(id));
        }
    }

    @Override
    public Observable<DemoResponse<List<Member>>> members(int page, int pageSize) {
        return create(subscriber -> {
            List<Member> allInts = fetchAllMembers();
            int total = allInts.size();
            int start = page * pageSize;
            int end = Math.min(start + pageSize, total);
            if (start >= total) {
                subscriber.onCompleted();
            } else {
                Bundle meta = new Bundle();
                meta.putInt("totalMembers", total);
                subscriber.onNext(new DemoResponse<>(meta, allInts.subList(start, end)));
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public Observable<DemoResponse<Member>> member(long id) {
        return create(subscriber -> {
            // randomly error for demo's sake
            if (random.nextFloat() < .4) {
                subscriber.onError(new RuntimeException("A random error suddenly appeared!"));
                return;
            }
            Member member = getMember(id);
            if (member != null) {
                Bundle meta = new Bundle();
                meta.putInt("totalMembers", membersList.size());
                subscriber.onNext(new DemoResponse<>(meta, member));
            } else {
                // custom `ApiError` would be better
                subscriber.onError(new IllegalStateException("member doesn't exist"));
            }
            subscriber.onCompleted();
        });
    }

    public Observable<DemoResponse<Member>> editMember(long id, String newName) {
        return create(subscriber -> {
            Member member = updateMemberName(id, newName);
            if (member != null) {
                Bundle meta = new Bundle();
                meta.putInt("totalMembers", membersList.size());
                subscriber.onNext(new DemoResponse<>(meta, member));
            } else {
                // custom `ApiError` would be better
                subscriber.onError(new IllegalStateException("member doesn't exist"));
            }
            subscriber.onCompleted();
        });
    }

    // common helper
    private <T> Observable<T> create(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.<T>create(subscriber -> {
            fakeLatency();
            onSubscribe.call(subscriber);
        }).subscribeOn(scheduler);
    }

    private synchronized List<Member> fetchAllMembers() {
        // sometimes add one
        if (random.nextFloat() < .1) {
            int index = random.nextInt(membersList.size());
            long id = nextId.getAndIncrement();
            membersList.add(index, Member.dummy(id));
        }
        // sometimes remove one
        if (random.nextFloat() < .1) {
            membersList.remove(random.nextInt(membersList.size()));
        }
        return Lists.newArrayList(membersList);
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    private synchronized Member getMember(long id) {
        int index = Iterables.indexOf(membersList, member -> member.id == id);
        if (index < 0) return null;
        // sometimes change the member name
        if (random.nextFloat() < .3) {
            return updateMemberName(id, String.format("Changed! %d + %d", id, random.nextInt(100)));
        } else {
            return membersList.get(index);
        }
    }

    @Nullable
    private synchronized Member updateMemberName(long id, String name) {
        int index = Iterables.indexOf(membersList, member -> member.id == id);
        if (index < 0) return null;
        Member member = new Member(id, name);
        membersList.set(index, member);
        return member;
    }

    private void fakeLatency() {
        SystemClock.sleep(500 + random.nextInt(1000));  // artificial delay
    }
}
