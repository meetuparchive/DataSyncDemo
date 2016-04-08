package com.meetup.demo.datasync.api;

import java.util.List;

import rx.Observable;

import com.meetup.demo.datasync.model.Member;

public interface DemoApi {
    Observable<DemoResponse<List<Member>>> members(int page, int pageSize);
    Observable<DemoResponse<Member>> member(long id);
    Observable<DemoResponse<Member>> editMember(long id, String name);
}
