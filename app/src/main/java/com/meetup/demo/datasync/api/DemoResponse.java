package com.meetup.demo.datasync.api;

import android.os.Bundle;

public class DemoResponse<T> {
    public final Bundle meta;
    public final T results;

    public DemoResponse(Bundle meta, T results) {
        this.meta = meta;
        this.results = results;
    }
}
