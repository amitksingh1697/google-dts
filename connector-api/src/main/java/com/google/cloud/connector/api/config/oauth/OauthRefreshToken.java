package com.google.cloud.connector.api.config.oauth;

import static com.google.cloud.connector.api.annotation.Semantic.Category.AUTHENTICATION_OAUTH;

import com.google.cloud.connector.api.annotation.Parameter;
import com.google.cloud.connector.api.annotation.Semantic;

/** A type for OAuth refresh token configuration parameter. */
@Semantic(AUTHENTICATION_OAUTH)
public record OauthRefreshToken(@Parameter("refreshToken") String value) {}