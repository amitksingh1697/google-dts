package com.google.cloud.connector.server;

import com.google.cloud.bigquery.federation.v1alpha1.DataSource;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.protobuf.Struct;
import java.io.Closeable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * A {@link ConnectorLoaderFactory} that caches {@link ConnectorLoader} created by a delegated
 * {@link ConnectorLoaderFactory}.
 */
public class CachingConnectorLoaderFactory implements ConnectorLoaderFactory, Closeable {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER})
  @BindingAnnotation
  @interface InternalFactory {}

  private final LoadingCache<String, ConnectorLoader> loaderCache;

  @Inject
  CachingConnectorLoaderFactory(
      ConnectorLoaderConfig config, @InternalFactory ConnectorLoaderFactory loaderFactory) {
    Preconditions.checkArgument(config.isEnableCache(), "ConnectorLoader caching is not enabled");
    this.loaderCache = createLoaderCache(loaderFactory);
  }

  @Override
  public ConnectorLoader get(String datasourceId) {
    try {
      return loaderCache.get(datasourceId);
    } catch (Exception e) {
      Throwables.throwIfUnchecked(e.getCause());
      throw new UncheckedExecutionException(e.getCause());
    }
  }

  /** Invalidates all cached entries. */
  @Override
  public void close() {
    loaderCache.invalidateAll();
  }

  private static LoadingCache<String, ConnectorLoader> createLoaderCache(
      ConnectorLoaderFactory loaderFactory) {
    return CacheBuilder.newBuilder()
        .removalListener(
            (RemovalListener<String, ConnectorLoader>)
                notification ->
                    Optional.ofNullable(notification.getValue())
                        .map(ForwardingConnectorLoader.class::cast)
                        .ifPresent(ForwardingConnectorLoader::closeDelegate))
        .build(
            new CacheLoader<>() {
              @Override
              public ConnectorLoader load(String datasourceId) {
                return new ForwardingConnectorLoader(loaderFactory.get(datasourceId));
              }
            });
  }

  /** A {@link ConnectorLoader} that forwards to another {@link ConnectorLoader}. */
  private record ForwardingConnectorLoader(ConnectorLoader delegate) implements ConnectorLoader {

    @Override
    public DataSource getDataSource() {
      return delegate.getDataSource();
    }

    @Override
    public <T> T instantiateConnector(Struct parameters, Class<T> parentType) {
      return delegate.instantiateConnector(parameters, parentType);
    }

    @Override
    public void close() {
      // no-op
    }

    void closeDelegate() {
      delegate.close();
    }
  }
}
