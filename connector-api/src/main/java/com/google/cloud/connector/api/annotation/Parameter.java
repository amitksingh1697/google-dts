package com.google.cloud.connector.api.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Annotation for data source parameter. */
@Retention(RUNTIME)
@Target({RECORD_COMPONENT, PARAMETER})
public @interface Parameter {

  /** Name of the parameter. */
  String value();
}
