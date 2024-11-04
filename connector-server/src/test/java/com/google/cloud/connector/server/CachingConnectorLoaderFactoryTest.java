package com.google.cloud.connector.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

/** Unit test for {@link CachingConnectorLoaderFactory}. */
public class CachingConnectorLoaderFactoryTest {

  @Test
  public void get_sameLoaderInstance() {
    ConnectorLoaderFactory mockLoaderFactory = mock(ConnectorLoaderFactory.class);
    when(mockLoaderFactory.get(anyString())).thenAnswer(invocation -> mock(ConnectorLoader.class));

    CachingConnectorLoaderFactory cachingLoaderFactory =
        new CachingConnectorLoaderFactory(
            new ConnectorLoaderConfig("notused", true), mockLoaderFactory);

    ConnectorLoader loader1 = cachingLoaderFactory.get("datasource");
    ConnectorLoader loader2 = cachingLoaderFactory.get("datasource");

    // Verify the same cached loader instance was returned
    assertThat(loader1).isSameInstanceAs(loader2);
    // There is only one actual loader instance created
    verify(mockLoaderFactory).get(anyString());
  }

  @Test
  public void get_differentDataSources() {
    ConnectorLoaderFactory mockLoaderFactory = mock(ConnectorLoaderFactory.class);
    when(mockLoaderFactory.get(anyString())).thenAnswer(invocation -> mock(ConnectorLoader.class));

    CachingConnectorLoaderFactory cachingLoaderFactory =
        new CachingConnectorLoaderFactory(
            new ConnectorLoaderConfig("notused", true), mockLoaderFactory);

    ConnectorLoader loader1 = cachingLoaderFactory.get("datasource1");
    ConnectorLoader loader2 = cachingLoaderFactory.get("datasource2");

    // Verify different loader instance for different data source
    assertThat(loader1).isNotSameInstanceAs(loader2);

    // Verify one actual instance per data source was created
    verify(mockLoaderFactory).get("datasource1");
    verify(mockLoaderFactory).get("datasource2");
  }

  @Test
  public void loader_closeSkipped() {
    ConnectorLoaderFactory mockLoaderFactory = mock(ConnectorLoaderFactory.class);
    ConnectorLoader mockLoader = mock(ConnectorLoader.class);
    when(mockLoaderFactory.get(anyString())).thenReturn(mockLoader);

    CachingConnectorLoaderFactory cachingLoaderFactory =
        new CachingConnectorLoaderFactory(
            new ConnectorLoaderConfig("notused", true), mockLoaderFactory);

    ConnectorLoader loader = cachingLoaderFactory.get("datasource");
    loader.close();

    verify(mockLoader, never()).close();
  }

  @Test
  public void loader_closeOnCacheEviction() {
    ConnectorLoaderFactory mockLoaderFactory = mock(ConnectorLoaderFactory.class);
    ConnectorLoader mockLoader = mock(ConnectorLoader.class);
    when(mockLoaderFactory.get(anyString())).thenReturn(mockLoader);

    CachingConnectorLoaderFactory cachingLoaderFactory =
        new CachingConnectorLoaderFactory(
            new ConnectorLoaderConfig("notused", true), mockLoaderFactory);

    cachingLoaderFactory.get("datasource1");
    cachingLoaderFactory.get("datasource2");

    // Closing the cache will invalidate all entries, hence closing all loaders.
    cachingLoaderFactory.close();
    verify(mockLoader, times(2)).close();
  }
}
