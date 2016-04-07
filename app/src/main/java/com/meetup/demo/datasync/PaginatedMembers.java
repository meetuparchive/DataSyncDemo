package com.meetup.demo.datasync;

import android.util.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.subjects.PublishSubject;

import com.meetup.demo.datasync.api.DemoApi;
import com.meetup.demo.datasync.model.Member;
import com.meetup.util.PaginationCache;

/**
 * This is a `PaginationCache` of `Member`s but extends it further by tracking
 * the total number of members as reported in API response metadata for each
 * page fetch request.  Clients of `PaginatedMembers` can receive the `total`
 * both via pull (`getTotal()`) or push (`newTotals()`).
 *
 * Total will be -1 when undetermined.
 */
public class PaginatedMembers extends PaginationCache<Member> {
    private static final int PAGE_SIZE = 50;
    private static final int PREFETCH_THRESHOLD = 10;
    private static final int MAX_PAGES_CACHED = 3;

    private final DemoApi api;
    private final PublishSubject<Pair<Integer, Integer>> newTotals = PublishSubject.create();
    private final AtomicInteger currentTotal;

    public PaginatedMembers(DemoApi api) {
        super(PAGE_SIZE, PREFETCH_THRESHOLD, MAX_PAGES_CACHED);
        this.api = api;
        this.currentTotal = new AtomicInteger(-1);
    }

    public void resetTotal() {
        currentTotal.set(-1);
    }

    // returns -1 if undetermined
    public int getTotal() {
        return currentTotal.get();
    }

    public boolean hasTotal() {
        return getTotal() != -1;
    }

    public Observable<Pair<Integer, Integer>> newTotals() {
        return newTotals.asObservable();
    }

    @Override
    public Observable<List<Member>> fetchPage(int page) {
        return api.members(page, getPageSize())
                .doOnNext(response -> {
                    int oldTotal = currentTotal.getAndSet(response.total);
                    if (oldTotal != response.total) {
                        newTotals.onNext(new Pair<>(oldTotal, response.total));
                    }
                })
                .map(response -> response.data);
    }
}
