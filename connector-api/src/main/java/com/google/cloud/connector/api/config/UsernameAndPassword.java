package com.google.cloud.connector.api.config;

import static com.google.cloud.connector.api.annotation.Semantic.Category.AUTHENTICATION;

import com.google.cloud.connector.api.annotation.Semantic;
import javax.annotation.Nullable;

/** A record for a username and password pair. */
@Semantic(AUTHENTICATION)
public record UsernameAndPassword(@Nullable String username, @Nullable String password) {}