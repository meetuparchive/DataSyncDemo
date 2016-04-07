package com.meetup.demo.datasync.bus;

import com.meetup.demo.datasync.model.Member;

public class MemberUpdate {
    public final Member oldMember;
    public final Member newMember;

    public MemberUpdate(Member oldMember, Member newMember) {
        this.oldMember = oldMember;
        this.newMember = newMember;
    }
}
