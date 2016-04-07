package com.meetup.demo.datasync;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import com.meetup.demo.datasync.application.DemoApplication;
import com.meetup.demo.datasync.model.Member;

/**
 * A RecyclerView.Adapter powered by `PaginationCache`.  The fact that data is fetched in pages
 * is abstracted away so that we can simply bind a Member to a View by absolute position.
 */
public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.ViewHolder> {
    @Inject @Named("ui") Scheduler uiScheduler;

    // data source
    private PaginatedMembers members;

    // subscriptions to data source
    private CompositeSubscription subs = Subscriptions.from();

    // Member click events exposed to containing activity to handle
    private PublishSubject<Member> memberClicks = PublishSubject.create();

    public MembersAdapter(Context context, PaginatedMembers members) {
        DemoApplication.component(context).inject(this);
        this.members = members;
    }

    // emits Member that was clicked
    public Observable<Member> memberClicks() {
        return memberClicks;
    }

    // should call this in `onDestroy`
    public void unsubscribe() {
        subs.unsubscribe();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // first, hide/reset view
        holder.hide();

        // cleanup previous tenant's subscription
        subs.remove(holder.subscription);

        // subscribe to member at `position` from `members` data source and
        // show when we receive it
        holder.subscription = members.get(position)
                .observeOn(uiScheduler)
                .subscribe(holder::show);

        // add subscription for global cleanup
        subs.add(holder.subscription);
    }

    @Override
    public int getItemCount() {
        // `members` total will be -1 if undetermined
        return Math.max(0, members.getTotal());
    }

    @Override
    public int getItemViewType(int position) {
        // all the same
        return 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.content) ViewGroup content;
        @InjectView(R.id.member_name) TextView nameView;
        @InjectView(R.id.member_color) View colorView;
        @InjectView(R.id.progress) ProgressBar progressBar;

        Subscription subscription = Subscriptions.empty();

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        public void hide() {
            progressBar.setVisibility(View.VISIBLE);
            content.setVisibility(View.INVISIBLE);
        }

        public void show(Member member) {
            progressBar.setVisibility(View.INVISIBLE);
            // push click events to `memberClicks` subject
            content.setOnClickListener(v -> memberClicks.onNext(member));
            nameView.setText(member.name);
            colorView.setBackgroundColor(member.getColor());
            content.setVisibility(View.VISIBLE);
        }
    }
}
