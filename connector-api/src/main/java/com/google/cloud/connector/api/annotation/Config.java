package com.google.cloud.connector.api.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for annotating a public constructor of the configuration class to be used by a {@kink
 * DataSource} class. Each argument of the constructor should be annotated with {@link Parameter} to
 * provide the name of the parameter. There should be only one public constructor annotated with
 * this annotation.
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE, CONSTRUCTOR})
public @interface Config {}
