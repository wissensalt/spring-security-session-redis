package com.wissensalt.springsecuritysessionredis.request;

import com.wissensalt.springsecuritysessionredis.model.Role.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotBlank
    String email,
    @NotBlank
    String password,
    @NotNull
    RoleName role) {

}
