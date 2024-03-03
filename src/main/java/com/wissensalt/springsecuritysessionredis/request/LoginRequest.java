package com.wissensalt.springsecuritysessionredis.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {

}
