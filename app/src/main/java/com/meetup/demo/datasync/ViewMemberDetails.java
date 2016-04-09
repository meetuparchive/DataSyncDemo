package com.meetup.demo.datasync;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Scheduler;

import com.meetup.demo.datasync.api.DemoApi;
import com.meetup.demo.datasync.application.DemoApplication;
import com.meetup.demo.datasync.bus.MemberUpdate;
import com.meetup.demo.datasync.model.Member;
import com.meetup.util.ErrorUi;
import com.meetup.util.RxBus;

/**
 * View a single member's profile in detail.
 *
 * Demonstrates:
 *  - Posting edits and responding to both success and failure
 *  - Broadcasting successful edits by pushing to an Rx-based event bus
 *  - Simple refreshing
 */
public class ViewMemberDetails extends BaseActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    @Inject DemoApi api;
    @Inject @Named("ui") Scheduler uiScheduler;
    @Inject RxBus.Driver<MemberUpdate> memberUpdates;

    @InjectView(R.id.toolbar) Toolbar toolbar;
    @InjectView(R.id.swipe_layout) SwipeRefreshLayout swipeLayout;
    @InjectView(R.id.member_color) View colorView;
    @InjectView(R.id.member_name) TextView nameText;
    @InjectView(R.id.member_id) TextView idText;
    @InjectView(R.id.edit_name) EditText nameEdit;
    @InjectView(R.id.edit_container) ViewGroup editContainer;

    private Member member;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_member);
        ButterKnife.inject(this);
        DemoApplication.component(this).inject(this);
        setSupportActionBar(toolbar);

        swipeLayout.setOnRefreshListener(this);

        member = getIntent().getParcelableExtra("member");
        bindMember(member);
    }

    public void bindMember(Member member) {
        this.member = member;
        nameText.setText(member.name);
        colorView.setBackgroundColor(member.getColor());
        idText.setText(getString(R.string.id_label, member.id));
    }

    @OnClick(R.id.member_name)
    public void onNameClicked() {
        if (member == null)
            return;
        nameEdit.setText(member.name);
        nameText.setVisibility(View.GONE);
        editContainer.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.done_button)
    public void onDoneClicked() {
        String name = nameEdit.getText().toString().trim();
        if (Strings.isNullOrEmpty(name)) {
            Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        editContainer.setVisibility(View.GONE);
        nameText.setText(name);
        nameText.setEnabled(false);
        nameText.setVisibility(View.VISIBLE);
        hideKeyboard(this, nameEdit);
        subs.add(api.editMember(member.id, name)
                .observeOn(uiScheduler)
                .doOnUnsubscribe(() -> nameText.setEnabled(true))
                .subscribe(
                    response -> {
                        Member newMember = response.results;
                        memberUpdates.post(new MemberUpdate(this.member, newMember));
                        bindMember(newMember);
                        showEditSuccessBar();
                    },
                    error -> {
                        ErrorUi.toast(this).call(error);
                        bindMember(this.member);
                    }
                ));
    }

    @Override
    public void onRefresh() {
        subs.add(api.member(member.id)
                .map(response -> response.results)
                .compose(ErrorUi.catchAndToast(this))
                .observeOn(uiScheduler)
                .doOnUnsubscribe(() -> swipeLayout.setRefreshing(false))
                .subscribe(member -> {
                    if (!member.equals(this.member)) {
                        memberUpdates.post(new MemberUpdate(this.member, member));
                        bindMember(member);
                    }
                }));
    }

    private static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    void showEditSuccessBar() {
        Snackbar bar = Snackbar.make(toolbar, R.string.edit_success, Snackbar.LENGTH_LONG);
        bar.setAction(android.R.string.ok, v -> bar.dismiss());
        bar.show();
    }
}
