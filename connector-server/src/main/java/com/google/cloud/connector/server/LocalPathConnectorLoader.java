package com.google.cloud.connector.server;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.bigquery.federation.v1alpha1.Parameter;
import com.google.cloud.connector.Connector;
import com.google.cloud.connector.ConnectorConfig;
import com.google.cloud.connector.api.annotation.Config;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.datafusion.common.lang.FilterClassLoader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Defaults;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Resources;
import com.google.gson.internal.Primitives;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.TextFormat;
import com.google.protobuf.Value;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A {@link ConnectorLoader} that loads connector from a local directory. The content of the
 * directory will be used to create the {@link ClassLoader} for loading the connector class.
 *
 * <p>The {@link ClassLoader} created should also provide a text proto file for the {@link
 * Connector} message, which describes the connector. The text proto file should be named as {@code
 * connector.textproto}.
 */
final class LocalPathConnectorLoader implements ConnectorLoader {

  @VisibleForTesting static final String CONNECTOR_PROTO_FILE = "connector.textproto";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Value NULL_VALUE =
      Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
  private static final ClassLoader CONNECTOR_PARENT_CLASSLOADER =
      new FilterClassLoader(
          LocalPathConnectorLoader.class.getClassLoader(),
          name ->
              name.startsWith("javax.annotation.")
                  || name.startsWith("com.google.protobuf.")
                  || name.startsWith("com.google.cloud.connector.api.")
                  || name.startsWith("cdata.jdbc.")
                  || name.startsWith("com.google.cloud.bigquery.federation.v1alpha1."));

  private final URLClassLoader classLoader;
  private final Connector connector;
  private final List<Object> instances;

  /**
   * Creates an instance that loads connector from the given path.
   *
   * @param connectorPath the local path for the connector
   * @throws UncheckedIOException if failed to create the connector from the given path
   */
  LocalPathConnectorLoader(Path connectorPath) {
    this(connectorPath, CONNECTOR_PROTO_FILE);
  }

  @VisibleForTesting
  LocalPathConnectorLoader(Path connectorPath, String protoFileName) {
    this.classLoader = createConnectorClassLoader(connectorPath);
    this.connector = loadConnectorSpec(classLoader, protoFileName);
    this.instances = new CopyOnWriteArrayList<>();
  }

  @Override
  public com.google.cloud.bigquery.federation.v1alpha1.DataSource getDataSource() {
    return connector.getDataSource();
  }

  @Override
  public <T> T instantiateConnector(Struct parameters, Class<T> parentType) {
    try {
      Class<?> connectorClass = classLoader.loadClass(connector.getClassName());
      if (!parentType.isAssignableFrom(connectorClass)) {
        throw new ClassCastException(
            String.format(
                "Connector '%s' implementation class '%s' cannot be cast to type '%s'",
                getDataSource().getId(), connector.getClassName(), parentType.getName()));
      }

      ConnectorConfig connectorConfig = connector.getConfig();
      Function<Constructor<?>, Object[]> argumentsFunction =
          createArgumentsFunction(parameters, getDataSource().getParametersList().iterator());

      T instance =
          parentType.cast(
              switch (connectorConfig.getType()) {
                case MULTI_PARAMS ->
                    instantiate(connectorClass, DataSource.class, argumentsFunction);
                case CUSTOM_CLASS -> {
                  // Create an instance of the config class, followed by creating the connector
                  // class by
                  // providing the config object.
                  Class<?> configClass = classLoader.loadClass(connectorConfig.getClassName());
                  Object config = instantiate(configClass, Config.class, argumentsFunction);
                  yield instantiate(
                      connectorClass,
                      DataSource.class,
                      constructor -> {
                        checkArgument(
                            constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0].isAssignableFrom(configClass),
                            "Expected to have an argument of type '%s' to construct class '%s'",
                            configClass.getName(),
                            connectorClass.getName());
                        return new Object[] {config};
                      });
                }
                default ->
                    throw new IllegalArgumentException(
                        "Unknown configuration type for data source '"
                            + getDataSource().getName()
                            + "'");
              });
      instances.add(instance);
      return instance;
    } catch (InvocationTargetException e) {
      Throwable targetException = e.getCause();
      if (targetException instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) targetException;
      } else {
        throw new RuntimeException(
            "Failed to instantiate data source class '" + connector.getClass() + "'", e);
      }
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException e) {
      throw new RuntimeException(
          "Failed to instantiate data source class '" + connector.getClass() + "'", e);
    }
  }

  @Override
  public void close() {
    try {
      for (Object instance : instances) {
        if (instance instanceof AutoCloseable) {
          try {
            ((AutoCloseable) instance).close();
          } catch (Exception e) {
            logger.atWarning().withCause(e).log(
                "Exception raised when closing instance of type '%s'",
                instance.getClass().getName());
          }
        }
      }
      instances.clear();
      classLoader.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Creates a {@link Function} that provides java objects for each of the parameters as defined by
   * the {@link com.google.cloud.bigquery.federation.v1alpha1.DataSource#getParametersList()}.
   */
  private Function<Constructor<?>, Object[]> createArgumentsFunction(
      Struct parameters, Iterator<Parameter> paramIterator) {
    return constructor -> {
      Annotation[][] annotations = constructor.getParameterAnnotations();
      Class<?>[] paramTypes = constructor.getParameterTypes();
      Object[] result = new Object[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        Class<?> paramType = paramTypes[i];
        Object object;
        if (paramType.isRecord()) {
          try {
            object =
                instantiate(
                    paramType, Config.class, createArgumentsFunction(parameters, paramIterator));
          } catch (InvocationTargetException
              | InstantiationException
              | IllegalAccessException
              | NoSuchMethodException e) {
            throw new RuntimeException(
                "Failed to instantiate config class '" + paramType.getName() + "'", e);
          }
        } else {
          checkArgument(paramIterator.hasNext(), "Missing parameter information");
          Parameter configParam = paramIterator.next();
          object =
              toJavaObject(
                  paramType, parameters.getFieldsOrDefault(configParam.getId(), NULL_VALUE));
          if (object == null && !hasNullable(annotations[i])) {
            throw new IllegalArgumentException(
                String.format(
                    "Null value is not allowed for the '%s' as the '%s' argument to constructor"
                        + " '%s'",
                    configParam.getId(), i, constructor));
          }
        }
        result[i] = object;
      }
      return result;
    };
  }

  private boolean hasNullable(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getName().equals(Nullable.class.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a new instance of a given class by invoking a public constructor of the class. The
   * constructor being invoked is either the canonical constructor if it is a {@link Record} class;
   * otherwise, a constructor that is annotated with the given annotation class will be called.
   *
   * @param cls the class for the new instance
   * @param annotation the annotation class on the constructor if the class is not a {@link Record}
   * @param argumentsFunction a {@link Function} that takes a constructor and return a list of
   *     {@link Object} as arguments for calling the constructor.
   * @return a new instance of the given class
   * @throws InvocationTargetException if failed to call the constructor
   * @throws InstantiationException if failed to instantiate the class
   * @throws IllegalAccessException if blocked by the security manager for calling the constructor
   * @throws NoSuchMethodException if no constructor was found that satisfy the contract
   */
  private Object instantiate(
      Class<?> cls,
      Class<? extends Annotation> annotation,
      Function<Constructor<?>, Object[]> argumentsFunction)
      throws InvocationTargetException,
          InstantiationException,
          IllegalAccessException,
          NoSuchMethodException {

    Constructor<?> constructor =
        cls.isRecord() ? findRecordConstructor(cls) : findAnnotatedConstructor(cls, annotation);
    return constructor.newInstance(argumentsFunction.apply(constructor));
  }

  /**
   * Finds the canonical constructor of the record class.
   *
   * @param cls the record class
   * @return the canonical constructor
   * @throws NoSuchMethodException if no such constructor was found
   */
  private Constructor<?> findRecordConstructor(Class<?> cls) throws NoSuchMethodException {
    checkArgument(cls.isRecord(), "Class '%s' is not a record", cls.getName());
    return cls.getConstructor(
        Arrays.stream(cls.getRecordComponents())
            .map(RecordComponent::getType)
            .toArray(Class[]::new));
  }

  /**
   * Finds the constructor annotated with the given annotation class.
   *
   * @param cls the class to search for
   * @param annotation the constructor annotation to match
   * @return the annotated constructor
   * @throws NoSuchMethodException if no such constructor was found
   */
  private Constructor<?> findAnnotatedConstructor(
      Class<?> cls, Class<? extends Annotation> annotation) throws NoSuchMethodException {
    for (Constructor<?> constructor : cls.getConstructors()) {
      if (constructor.isAnnotationPresent(annotation)) {
        return constructor;
      }
    }
    throw new NoSuchMethodException(
        String.format(
            "No public constructor annotated with '%s' in class '%s'",
            annotation.getName(), cls.getName()));
  }

  /**
   * Converts a proto {@link Value} into java object that can be assigned to the given type.
   *
   * @param javaType the type of the java object
   * @param value the proto value
   * @param <T> the type of the resulting java object
   * @return the java object
   */
  private <T> T toJavaObject(Class<T> javaType, Value value) {
    return switch (value.getKindCase()) {
      case KIND_NOT_SET, NULL_VALUE -> Defaults.defaultValue(javaType);
      case BOOL_VALUE -> javaType.cast(value.getBoolValue());
      case NUMBER_VALUE -> castNumber(javaType, value.getNumberValue());
      case STRING_VALUE -> {
        Object result = value.getStringValue();
        if (URI.class.equals(javaType)) {
          result = URI.create(value.getStringValue());
        } else if (URL.class.equals(javaType)) {
          try {
            result = new URL(value.getStringValue());
          } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
          }
        }
        yield javaType.cast(result);
      }
      default -> throw new IllegalArgumentException("Unsupported type " + value.getKindCase());
    };
  }

  private <T> T castNumber(Class<T> javaType, Double value) {
    Class<T> type = Primitives.wrap(javaType);
    if (type.equals(Byte.class)) {
      return type.cast(value.byteValue());
    }
    if (type.equals(Character.class)) {
      return type.cast((char) value.shortValue());
    }
    if (type.equals(Short.class)) {
      return type.cast(value.shortValue());
    }
    if (type.equals(Integer.class)) {
      return type.cast(value.intValue());
    }
    if (type.equals(Long.class)) {
      return type.cast(value.longValue());
    }
    if (type.equals(Float.class)) {
      return type.cast(value.floatValue());
    }
    return type.cast(value);
  }

  private static Connector loadConnectorSpec(ClassLoader classLoader, String fileName) {
    URL specFileUrl = classLoader.getResource(fileName);
    checkArgument(specFileUrl != null, "Missing connector specification file '%s'", fileName);

    try (Reader reader = Resources.asCharSource(specFileUrl, UTF_8).openStream()) {
      Connector.Builder builder = Connector.newBuilder();
      TextFormat.merge(reader, builder);
      return builder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static URLClassLoader createConnectorClassLoader(Path path) {
    try (Stream<Path> paths = Files.walk(path)) {
      return new URLClassLoader(
          path.getFileName().toString(),
          paths.map(LocalPathConnectorLoader::pathToUrl).toArray(URL[]::new),
          CONNECTOR_PARENT_CLASSLOADER);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static URL pathToUrl(Path path) {
    try {
      return path.toUri().toURL();
    } catch (MalformedURLException e) {
      // This shouldn't happen
      throw new IllegalStateException(e);
    }
  }
}
