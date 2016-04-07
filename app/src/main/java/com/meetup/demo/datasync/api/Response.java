package com.meetup.demo.datasync.api;

public class Response<T> {
    public final int total;  // insert your own metadata here
    public final T data;

    public Response(int total, T data) {
        this.total = total;
        this.data = data;
    }
}
