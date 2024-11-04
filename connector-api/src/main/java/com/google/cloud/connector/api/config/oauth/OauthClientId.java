package com.google.cloud.connector.api.config.oauth;

import static com.google.cloud.connector.api.annotation.Semantic.Category.AUTHENTICATION_OAUTH;

import com.google.cloud.connector.api.annotation.Parameter;
import com.google.cloud.connector.api.annotation.Semantic;

/** A type for OAuth client id configuration parameter. */
@Semantic(AUTHENTICATION_OAUTH)
public record OauthClientId(@Parameter("clientId") String value) {}