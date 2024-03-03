package com.wissensalt.springsecuritysessionredis.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateItemRequest(@NotNull Long id, @NotBlank String name, @NotNull BigDecimal price) {

}
