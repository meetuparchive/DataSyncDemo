package com.meetup.demo.datasync;

import android.content.Context;
import android.content.Intent;

import com.meetup.demo.datasync.model.Member;

public final class Intents {
    private Intents() {}

    public static Intent viewMember(Context context, Member member) {
        return new Intent(context, ViewMemberDetails.class)
                .putExtra("member", member);
    }
}
