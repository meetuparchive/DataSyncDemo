package com.meetup.demo.datasync;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import com.meetup.demo.datasync.api.DemoApi;
import com.meetup.demo.datasync.application.DemoApplication;
import com.meetup.demo.datasync.bus.MemberUpdate;
import com.meetup.util.ErrorUi;
import com.meetup.util.RxBus;

/**
 * View all members in a single RecyclerView.  Most of the work is done by `PaginatedMembers`,
 * an extension of `PaginationCache`.
 *
 * Demonstrates:
 *  - Fetching members from external API service in pages
 *  - Proactive fetching near page edges for a smoother UX
 *  - Identifying cache staleness from external API metadata
 *  - Identifying cache staleness from local changes made in other activities via RxBus events
 */
public class ViewMembers extends BaseActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    @Inject @Named("ui") Scheduler uiScheduler;
    @Inject DemoApi api;
    @Inject RxBus.Driver<MemberUpdate> memberUpdates;

    @InjectView(R.id.swipe_layout) SwipeRefreshLayout swipeLayout;
    @InjectView(R.id.recycler) RecyclerView recycler;
    @InjectView(R.id.toolbar) Toolbar toolbar;
    private Snackbar refreshBar;

    private CompositeSubscription onPauseSubs;

    private MembersAdapter adapter;
    private PaginatedMembers members;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DemoApplication.component(this).inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);

        // ui
        swipeLayout.setOnRefreshListener(this);

        // data
        members = (PaginatedMembers) getLastCustomNonConfigurationInstance();
        if (members == null) {
            members = new PaginatedMembers(api);
        }
        adapter = new MembersAdapter(this, members);
        recycler.setAdapter(adapter);

        // event handlers
        subs.add(adapter.memberClicks()
                // don't need to specify UI scheduler since click is a UI event
                .subscribe(member -> startActivity(Intents.viewMember(this, member))));
        subs.add(members.newTotals()
                .observeOn(uiScheduler)
                .subscribe(this::onNewTotal));
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPauseSubs = Subscriptions.from();
        // refresh if needed
        boolean needsRefreshing = !members.hasTotal();
        long updatesFromTime;
        if (needsRefreshing) {
            refresh(true);
            updatesFromTime = -1;  // if refreshing anyway, no need to observe past events
        } else {
            updatesFromTime = Math.max(pausedElapsedRealtime, savedElapsedRealtime);
        }

        // start listening to event broadcasts.
        onPauseSubs.add(memberUpdates.observable(updatesFromTime)
                // debounce so we don't show snackbar for each update emitted in succession
                .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(uiScheduler)
                .subscribe(update -> showRefreshBar()));
    }

    @Override
    protected void onPause() {
        onPauseSubs.unsubscribe();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        adapter.unsubscribe();
        super.onDestroy();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return members;
    }

    void onNewTotal(Pair<Integer, Integer> totals) {
        int oldTotal = totals.first;
        if (oldTotal != -1) {
            // alert the user that the content on screen may be outdated
            showRefreshBar();
        }
    }

    @Override
    public void onRefresh() {
        refresh(true);
    }

    private void refresh(boolean resetTotal) {
        // if not a user-triggered refresh and we're resetting, show the spinner
        if (resetTotal) {
            recycler.setVisibility(View.GONE);
            swipeLayout.post(() -> {
                if (swipeLayout != null) {
                    swipeLayout.setRefreshing(true);
                }
            });
            members.resetTotal();
        }
        // invalidate cache and trigger the adapter
        members.invalidateAll();
        adapter.notifyDataSetChanged();
        if (!members.hasTotal()) {
            // first fetch to set the total
            subs.add(members.get(0)
                    .compose(ErrorUi.catchAndToast(this))
                    .observeOn(uiScheduler)
                    .doOnUnsubscribe(() -> {
                        swipeLayout.setRefreshing(false);
                        recycler.setVisibility(View.VISIBLE);
                    })
                    .subscribe());
        }
        if (refreshBar != null && refreshBar.isShownOrQueued()) {
            refreshBar.dismiss();
        }
    }

    void showRefreshBar() {
        if (refreshBar != null && refreshBar.isShown())
            return;
        refreshBar = Snackbar.make(toolbar, R.string.content_outdated, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.refresh, v -> refresh(false));
        refreshBar.show();
    }
}
