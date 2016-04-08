package com.meetup.demo.datasync.api;

import android.os.Bundle;

public class DemoResponse<T> {
    public final Bundle meta;
    public final T data;

    public DemoResponse(Bundle meta, T data) {
        this.meta = meta;
        this.data = data;
    }
}
