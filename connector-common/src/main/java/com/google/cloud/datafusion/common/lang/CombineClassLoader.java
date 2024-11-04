package com.google.cloud.datafusion.common.lang;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

/**
 * A {@link ClassLoader} that load classes from list of other {@link ClassLoader}s. Note that this
 * ClassLoader just delegates to other ClassLoaders, but never define class, hence no Class loaded
 * by this class would have {@link Class#getClassLoader()}} returning this ClassLoader.
 */
public class CombineClassLoader extends ClassLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final List<ClassLoader> delegates;

  /**
   * Creates a CombineClassLoader with the given parent and a list of ClassLoaders for delegation.
   *
   * @param name class loader name; or {@code null} if not named
   * @param parent parent ClassLoader. If null, bootstrap ClassLoader will be the parent.
   * @param delegates list of ClassLoaders for delegation
   */
  public CombineClassLoader(
      @Nullable String name, @Nullable ClassLoader parent, ClassLoader... delegates) {
    this(name, parent, List.of(delegates));
  }

  /**
   * Creates a CombineClassLoader with the given parent and a list of ClassLoaders for delegation.
   *
   * @param name class loader name; or {@code null} if not named
   * @param parent parent ClassLoader. If null, bootstrap ClassLoader will be the parent.
   * @param delegates list of ClassLoaders for delegation
   */
  public CombineClassLoader(
      @Nullable String name,
      @Nullable ClassLoader parent,
      Collection<? extends ClassLoader> delegates) {
    super(name, parent);
    this.delegates = List.copyOf(delegates);
  }

  /** Returns an immutable list of {@link ClassLoader}s that this class delegates to. */
  public List<ClassLoader> getDelegates() {
    return delegates;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    for (ClassLoader classLoader : delegates) {
      try {
        return classLoader.loadClass(name);
      } catch (ClassNotFoundException e) {
        logger.atFinest().log(
            "Class '%s' not found in ClassLoader '%s' of type '%s'",
            name, classLoader.getName(), classLoader);
      }
    }

    throw new ClassNotFoundException("Class not found in all delegated ClassLoaders: " + name);
  }

  @Override
  public URL findResource(String name) {
    return delegates.stream()
        .map(cl -> cl.getResource(name))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * Finds all the resources with the given name. Override this method instead of the {@link
   * #findResources(String)} to avoid duplicated resource URLs.
   */
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    try {
      return Collections.enumeration(
          Stream.concat(
                  StreamSupport.stream(
                      Spliterators.spliteratorUnknownSize(super.getResources(name).asIterator(), 0),
                      false),
                  delegates.stream().flatMap(cl -> cl.resources(name)))
              .distinct()
              .toList());
    } catch (UncheckedIOException ex) {
      // Unwrap the wrapping done by the cl.resources() to make the stacktrace cleaner
      throw ex.getCause();
    }
  }
}
