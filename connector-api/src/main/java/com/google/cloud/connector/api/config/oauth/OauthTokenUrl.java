package com.google.cloud.connector.api.config.oauth;

import static com.google.cloud.connector.api.annotation.Semantic.Category.AUTHENTICATION_OAUTH;

import com.google.cloud.connector.api.annotation.Parameter;
import com.google.cloud.connector.api.annotation.Semantic;
import java.net.URI;
import javax.annotation.Nullable;

/** A type for OAuth token URL configuration parameter. */
@Semantic(AUTHENTICATION_OAUTH)
public record OauthTokenUrl(@Nullable @Parameter("tokenUrl") URI value) {}