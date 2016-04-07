package com.meetup.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import static com.google.common.base.Preconditions.*;

/**
 * Loading cache that provides a clean interface for fetching items by absolute position.
 * Behind the scenes, pages of items are fetched and cached to minimize expensive data retrieval
 * calls.  Adjacent pages may also be fetched proactively when `prefetchThreshold` is specified.
 */
public abstract class PaginationCache<T> {
    final int pageSize;
    final int prefetchThreshold;
    final LoadingCache<Integer, Observable<List<T>>> pages;
    final ConcurrentMap<Integer, Subscription> subs;

    /**
     * Implementations should use `getPageSize()` to fetch the appropriate number of items.
     */
    public abstract Observable<List<T>> fetchPage(int page);

    /**
     * @param pageSize Page size. Ensure this corresponds with `fetchPage(int)`
     * @param prefetchThreshold Distance threshold to trigger pre-fetching an adjacent page.
     *                          See `get(int)` for more info.  Set to 0 to disable.
     * @param maxPagesCached
     */
    public PaginationCache(int pageSize, int prefetchThreshold, int maxPagesCached) {
        this.pageSize = pageSize;
        this.prefetchThreshold = prefetchThreshold;
        this.subs = Maps.newConcurrentMap();
        this.pages = CacheBuilder.newBuilder()
                .maximumSize(maxPagesCached)
                .removalListener(notification -> {
                    Integer key = (Integer) notification.getKey();
                    if (key != null) {
                        Subscription sub = subs.remove(key);
                        if (sub != null) {
                            sub.unsubscribe();
                        }
                    }
                })
                .build(new CacheLoader<Integer, Observable<List<T>>>() {
                    @Override
                    public Observable<List<T>> load(Integer key) throws Exception {
                        ConnectableObservable<List<T>> obs = fetchPage(key).replay(1);
                        Subscription oldSub = subs.replace(key, obs.connect());
                        if (oldSub != null) {
                            oldSub.unsubscribe();
                        }
                        return obs;
                    }
                });
    }

    /**
     * Get item at position `index`. May trigger fetching of the page and an adjacent page if
     * `prefetchThreshold` is set.  For example, if page size is 10 and `prefetchThreshold` is 2,
     * page 3 will be fetched upon `get(30)` and `get(31)` and page 5 will be fetched upon `get(38)`
     * and `get(39)`.
     */
    public Observable<T> get(int index) {
        checkArgument(index >= 0);
        int page = index / pageSize;
        // proactively fetch prev/next page if we're close
        if (prefetchThreshold > 0) {
            if (index % pageSize < prefetchThreshold && page >= 1) {
                getPage(page - 1);
            } else if (pageSize - (index % pageSize) <= prefetchThreshold) {
                getPage(page + 1);
            }
        }
        return getPage(page)
                .map((results) -> results.get(index % pageSize))
                .onErrorResumeNext(error ->
                    error instanceof IndexOutOfBoundsException ?
                            Observable.empty() :
                            Observable.error(error)
                );
    }

    public Observable<List<T>> getPage(int page) {
        checkArgument(page >= 0);
        return pages.getUnchecked(page).defaultIfEmpty(Collections.emptyList());
    }

    /**
     * Replaces an item at `index` with `value`.  Does nothing if item/page isn't cached.
     */
    public void replace(int index, T value) {
        int page = index / pageSize;
        Observable<List<T>> pageObs = pages.getIfPresent(page);
        if (pageObs == null) {
            return;
        }
        pageObs.onErrorResumeNext(Observable.empty()).subscribe((items) -> {
            List<T> newItems = Lists.newArrayList(items);
            newItems.set(index % pageSize, value);
            pages.put(page, Observable.just(newItems).cache());
        });
    }

    public int getPageSize() {
        return pageSize;
    }

    public void invalidateAll() {
        pages.invalidateAll();
    }
}
