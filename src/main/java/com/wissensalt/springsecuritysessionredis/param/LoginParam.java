package com.wissensalt.springsecuritysessionredis.param;

import com.wissensalt.springsecuritysessionredis.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record LoginParam(HttpServletRequest request, HttpServletResponse response, LoginRequest loginRequest) {

}
