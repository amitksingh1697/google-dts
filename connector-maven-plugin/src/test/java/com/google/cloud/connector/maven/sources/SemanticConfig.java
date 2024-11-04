package com.google.cloud.connector.maven.sources;

import static com.google.cloud.connector.api.annotation.Semantic.Category.AUTHENTICATION;
import static com.google.cloud.connector.api.annotation.Semantic.Category.ENDPOINT;

import com.google.cloud.connector.api.annotation.Semantic;
import com.google.cloud.connector.api.config.UsernameAndPassword;
import com.google.cloud.connector.api.config.oauth.OauthAccessToken;
import com.google.cloud.connector.api.config.oauth.OauthClientId;
import com.google.cloud.connector.api.config.oauth.OauthClientSecret;
import com.google.cloud.connector.api.config.oauth.OauthTokenUrl;

/**
 * Test configuration class that uses {@link com.google.cloud.connector.api.annotation.Semantic}.
 */
public record SemanticConfig(
    @Semantic(ENDPOINT) EndpointConfig endpointConfig,
    UsernameAndPassword usernameAndPassword,
    OauthClientId clientId,
    OauthClientSecret clientSecret,
    OauthAccessToken clientToken, // non-standard field name is used on purpose
    OauthTokenUrl tokenUrl,
    @Semantic(value = AUTHENTICATION, customSubcategory = "genericAuth") String genericParam,
    long timeoutMillis) {}