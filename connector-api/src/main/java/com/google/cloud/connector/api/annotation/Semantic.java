package com.google.cloud.connector.api.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.collect.ImmutableList;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Annotations for configuration classes and parameters for carrying semantic category information.
 */
@Retention(RUNTIME)
@Target({TYPE, RECORD_COMPONENT, PARAMETER})
public @interface Semantic {

  /** The semantic category. */
  Category value();

  /** Custom subcategory, for generic parameters. */
  String customSubcategory() default "";

  /** Semantic group of the parameter. */
  enum Category {
    UNKNOWN(),
    ENDPOINT("endpoint"),
    AUTHENTICATION("authentication"),
    AUTHENTICATION_OAUTH("authentication", "oauth");

    private final List<String> prefix;

    Category(String... prefix) {
      this.prefix = ImmutableList.copyOf(prefix);
    }

    public List<String> getPrefix() {
      return prefix;
    }
  }
}