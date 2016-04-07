package com.meetup.demo.datasync.api;

import java.util.List;

import rx.Observable;

import com.meetup.demo.datasync.model.Member;

public interface DemoApi {
    Observable<Response<List<Member>>> members(int page, int pageSize);
    Observable<Response<Member>> member(long id);
    Observable<Response<Member>> editMember(long id, String name);
}
