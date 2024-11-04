package com.google.cloud.connector.api.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for annotating a public constructor of a data source class. There should be only one
 * public constructor annotated with this annotation. The argument of the constructor will be used
 * to determine the set of configurations supported by the data source.
 *
 * <p>The constructor arguments either has to be all annotated with {@link Parameter} to indicate
 * that they are configurations, or it should have a single argument, which will be taken as a
 * configuration class.
 *
 * <p>A configuration class is either a {@code record} class or a class with a public constructor
 * annotated with {@link Config}. If it is a {@code record} class, the parameter name will be based
 * on the record field name, which can be overridden with an optional {@link Parameter} annotation.
 */
@Retention(RUNTIME)
@Target({CONSTRUCTOR})
public @interface DataSource {

  /** Name of the data source. By default, it is the simple class name of the annotated class. */
  String value() default "";

  /**
   * A set of {@link Capability} supported by the data source. At least one of {@link
   * Capability#SUPPORTS_SYNCHRONOUS_QUERIES} or {@link Capability#SUPPORTS_PARALLEL_QUERIES} must
   * be included.
   */
  Capability[] capabilities() default {};

  /**
   * Specifies for how long can the client cache the datasource specific metadata. By default, it is
   * one hour.
   */
  long maxStalenessMillis() default 3600000L;

  // TODO: Add the supported functions when the API is finalized with Substrait
}
