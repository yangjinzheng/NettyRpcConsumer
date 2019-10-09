package com.netty.user.remote;

import com.netty.param.Response;
import com.netty.user.bean.User;

import java.util.List;

public interface UserRemote {
    public Response saveUser(User user);
    public Response saveUsers(List<User> users);
}
