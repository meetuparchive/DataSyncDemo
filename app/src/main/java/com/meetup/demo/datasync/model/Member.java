package com.meetup.demo.datasync.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;

import com.google.common.base.Objects;

public class Member implements Parcelable {
    public final long id;
    public final String name;

    public Member(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public @ColorInt int getColor() {
        int alpha = (int) (id % (255 - 32)) + 32;
        return Color.argb(alpha, 64, 84, 178);
    }

    public static Member dummy(long id) {
        String name = "Anon " + id;
        return new Member(id, name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o.getClass() != this.getClass())
            return false;

        Member that = (Member) o;
        return this.id == that.id &&
                Objects.equal(this.name, that.name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
    }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel source) {
            long id = source.readLong();
            String name = source.readString();
            return new Member(id, name);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };
}
