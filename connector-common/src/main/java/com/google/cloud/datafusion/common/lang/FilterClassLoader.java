package com.google.cloud.datafusion.common.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * A {@link ClassLoader} that only load classes and resources that are allowed by the filter or
 * coming from the {@link ClassLoader#getPlatformClassLoader()}. This classloader never defines any
 * classes by itself.
 */
public class FilterClassLoader extends ClassLoader {

  private final Predicate<String> classFilter;
  private final Predicate<String> resourceFilter;

  /**
   * Creates a new instance with the given parent classloader and filters for classes. Resources
   * that are ended with {@code .class} will match with what's permitted by the class filter.
   *
   * @param parent the parent classloader. If it is {@code null}, the bootstrap classloader will be
   *     the parent
   * @param classFilter a {@link Predicate} that filters on class name
   */
  public FilterClassLoader(@Nullable ClassLoader parent, Predicate<String> classFilter) {
    this(parent, classFilter, classResourceFilter(classFilter));
  }

  /**
   * Creates a new instance with the given parent classloader and filters for classes and resources.
   *
   * @param parent the parent classloader. If it is {@code null}, the bootstrap classloader will be
   *     the parent
   * @param classFilter a {@link Predicate} that filters on class name
   * @param resourceFilter a {@link Predicate} that filters on resource name
   */
  public FilterClassLoader(
      @Nullable ClassLoader parent,
      Predicate<String> classFilter,
      Predicate<String> resourceFilter) {
    super(parent);
    this.classFilter = classFilter;
    this.resourceFilter = resourceFilter;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    try {
      return ClassLoader.getPlatformClassLoader().loadClass(name);
    } catch (ClassNotFoundException e) {
      if (classFilter.test(name)) {
        return super.loadClass(name, resolve);
      }
      throw new ClassNotFoundException(
          String.format("Class '%s' is either not permitted the filter or not available", name), e);
    }
  }

  @Override
  public URL getResource(String name) {
    URL resource = ClassLoader.getPlatformClassLoader().getResource(name);
    if (resource != null) {
      return resource;
    }
    return resourceFilter.test(name) ? super.getResource(name) : null;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> platformResources = ClassLoader.getPlatformClassLoader().getResources(name);
    Enumeration<URL> resources =
        resourceFilter.test(name) ? super.getResources(name) : Collections.emptyEnumeration();
    return new Enumeration<>() {
      @Override
      public boolean hasMoreElements() {
        return platformResources.hasMoreElements() || resources.hasMoreElements();
      }

      @Override
      public URL nextElement() {
        return platformResources.hasMoreElements()
            ? platformResources.nextElement()
            : resources.nextElement();
      }
    };
  }

  /**
   * Creates a resource filter that only allows {@code .class} files with the one permitted by the
   * class filter. It also allows any non {@code .class} resources.
   */
  private static Predicate<String> classResourceFilter(Predicate<String> classFilter) {
    return name ->
        !name.endsWith(".class")
            || classFilter.test(
                name.replace('/', '.').substring(0, name.length() - ".class".length()));
  }
}
