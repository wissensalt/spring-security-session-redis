package com.wissensalt.springsecuritysessionredis.service;

import com.wissensalt.springsecuritysessionredis.param.LoginParam;
import com.wissensalt.springsecuritysessionredis.request.RegisterRequest;
import com.wissensalt.springsecuritysessionredis.response.AccountResponse;

public interface AccountService {

  Boolean register(RegisterRequest request);

  AccountResponse login(LoginParam loginParam);
}
