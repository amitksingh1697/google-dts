package com.google.cloud.datafusion.common.lang;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link CombineClassLoader}. */
public class CombineClassLoaderTest {

  @Test
  public void load_platformClass() throws ClassNotFoundException {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public Class<?> loadClass(String name) throws ClassNotFoundException {
                // This shouldn't get called
                called.set(true);
                throw new ClassNotFoundException(name);
              }
            });

    assertThat(cl.loadClass(String.class.getName())).isEqualTo(String.class);
    assertFalse(called.get());
  }

  @Test
  public void load_ClassFromDelegate() throws ClassNotFoundException {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (Objects.equals(name, CombineClassLoaderTest.class.getName())) {
                  called.set(true);
                }
                return CombineClassLoaderTest.class.getClassLoader().loadClass(name);
              }
            });

    assertThat(cl.loadClass(CombineClassLoaderTest.class.getName()))
        .isEqualTo(CombineClassLoaderTest.class);
    assertTrue(called.get());
  }

  @Test
  public void load_missingClass() {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public Class<?> loadClass(String name) throws ClassNotFoundException {
                called.set(true);
                throw new ClassNotFoundException(name);
              }
            });

    assertThrows(ClassNotFoundException.class, () -> cl.loadClass("some.missing.package.Class"));
    assertTrue(called.get());
  }

  @Test
  public void get_platformResource() throws IOException {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public URL getResource(String name) {
                called.set(true);
                return null;
              }
            });

    String resourceName = String.class.getName().replace('.', '/') + ".class";
    assertNotNull(cl.getResource(resourceName));
    try (InputStream is = cl.getResourceAsStream(resourceName)) {
      assertNotNull(is);
    }
    assertFalse(called.get());
  }

  @Test
  public void get_resourceFromDelegate() throws IOException {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public URL getResource(String name) {
                called.set(true);
                return ClassLoader.getSystemResource(name);
              }
            });

    String resourceName = CombineClassLoaderTest.class.getName().replace('.', '/') + ".class";
    assertNotNull(cl.getResource(resourceName));
    assertTrue(called.get());

    called.set(false);
    try (InputStream is = cl.getResourceAsStream(resourceName)) {
      assertNotNull(is);
    }
    assertTrue(called.get());
  }

  @Test
  public void get_missingResource() {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              public URL getResource(String name) {
                called.set(true);
                return ClassLoader.getSystemResource(name);
              }
            });

    assertNull(cl.getResource("missing_resource"));
    assertTrue(called.get());
  }

  @Test
  public void get_resources() throws IOException {
    AtomicBoolean called = new AtomicBoolean();
    CombineClassLoader cl =
        new CombineClassLoader(
            "test",
            ClassLoader.getPlatformClassLoader(),
            new ClassLoader() {
              @Override
              protected Enumeration<URL> findResources(String name) throws IOException {
                called.set(true);
                return ClassLoader.getSystemResources(name);
              }
            });

    Enumeration<URL> urls =
        cl.getResources("META-INF/services/java.nio.file.spi.FileSystemProvider");
    assertTrue(urls.hasMoreElements());
    assertNotNull(urls.nextElement());
    assertFalse(urls.hasMoreElements());
    assertTrue(called.get());

    called.set(false);

    urls = cl.getResources(CombineClassLoaderTest.class.getName().replace('.', '/') + ".class");
    assertTrue(urls.hasMoreElements());
    assertNotNull(urls.nextElement());
    assertFalse(urls.hasMoreElements());
    assertTrue(called.get());

    called.set(false);

    urls = cl.getResources("no.such.resource");
    assertFalse(urls.hasMoreElements());
    assertTrue(called.get());
  }
}
