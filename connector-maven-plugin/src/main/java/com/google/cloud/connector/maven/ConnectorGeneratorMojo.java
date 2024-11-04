package com.google.cloud.connector.maven;

import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_NATIVE_QUERIES;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_PARALLEL_QUERIES;
import static com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability.SUPPORTS_SYNCHRONOUS_QUERIES;
import static com.google.cloud.connector.api.annotation.Semantic.Category.UNKNOWN;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ASM9;

import com.google.cloud.bigquery.federation.v1alpha1.DataSource.Capability;
import com.google.cloud.bigquery.federation.v1alpha1.TypeKind;
import com.google.cloud.connector.Connector;
import com.google.cloud.connector.ConnectorConfig;
import com.google.cloud.connector.api.NativeQuerySchemaResolver;
import com.google.cloud.connector.api.ParallelQueryExecutor;
import com.google.cloud.connector.api.SynchronousQueryExecutor;
import com.google.cloud.connector.api.annotation.Config;
import com.google.cloud.connector.api.annotation.DataSource;
import com.google.cloud.connector.api.annotation.Parameter;
import com.google.cloud.connector.api.annotation.Semantic;
import com.google.cloud.connector.api.annotation.Semantic.Category;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;
import com.google.protobuf.Duration;
import com.google.protobuf.TextFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

/** A maven {@link Mojo} for generating {@link DataSource} proto for describing a data source. */
@Mojo(
    name = "generate",
    defaultPhase = PREPARE_PACKAGE,
    threadSafe = true,
    requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class ConnectorGeneratorMojo extends AbstractMojo {

  private static final String VALUE = "value";
  private static final String CUSTOM_SUBCATEGORY = "customSubcategory";
  private static final String CAPABILITIES = "capabilities";
  private static final String MAX_STALENESS_MILLIS = "maxStalenessMillis";

  private static final Type DATA_SOURCE_TYPE = Type.getType(DataSource.class);
  private static final Type CONFIG_TYPE = Type.getType(Config.class);
  private static final Type PARAMETER_TYPE = Type.getType(Parameter.class);
  private static final Type SEMANTIC_TYPE = Type.getType(Semantic.class);

  @org.apache.maven.plugins.annotations.Parameter(
      defaultValue = "${project}",
      readonly = true,
      required = true)
  private MavenProject project;

  @org.apache.maven.plugins.annotations.Parameter(
      property = "connector.class.name",
      required = true)
  private String connectorClass;

  @org.apache.maven.plugins.annotations.Parameter(
      property = "connector.proto.file.name",
      required = true,
      defaultValue = "connector.textproto")
  private String protoFileName;

  @org.apache.maven.plugins.annotations.Parameter(property = "connector.output.directory")
  private String outputDirectory;

  /**
   * Creates a new instance, only used for testing.
   *
   * @param targetDir correspond to the ${project.build.directory}
   * @param outputDir correspond to the ${project.build.outputDirectory}
   * @param connectorClass name of the connector class to inspect
   * @param protoFileName file name of the output text proto
   * @return a new mojo instance
   */
  @VisibleForTesting
  public static ConnectorGeneratorMojo create(
      Path targetDir, Path outputDir, String connectorClass, String protoFileName) {
    Build build = new Build();
    build.setDirectory(targetDir.toString());
    build.setOutputDirectory(outputDir.toString());

    // For testing (both in IDE and maven), we take the classpath as the dependency artifacts
    Set<Artifact> artifacts = new LinkedHashSet<>();
    for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
      File file = new File(path);

      // We need a unique artifact name.
      // If the path is a file, we assume it is an artifact jar, hence the name should be unique.
      // If the path is a directory, we assume it is the output directory of other maven module,
      // which has the form "{module-name}/target/classes".
      String artifactName;
      if (file.isDirectory()) {
        artifactName =
            Optional.ofNullable(file.getParentFile())
                .map(File::getParentFile)
                .map(File::getName)
                .orElse("unknown");
      } else {
        artifactName = file.getName();
      }

      DefaultArtifact artifact =
          new DefaultArtifact(
              artifactName,
              artifactName,
              "0.1-SNAPSHOT",
              "compile",
              "jar",
              null,
              new DefaultArtifactHandler("jar"));
      artifact.setFile(file);
      artifacts.add(artifact);
    }

    MavenProject project = new MavenProject();
    project.setArtifacts(artifacts);
    project.setBuild(build);

    return new ConnectorGeneratorMojo(project, connectorClass, protoFileName);
  }

  @SuppressWarnings("unused")
  public ConnectorGeneratorMojo() {
    // No-op. It is for maven to initialize this mojo.
  }

  private ConnectorGeneratorMojo(
      MavenProject project, String connectorClass, String protoFileName) {
    this.project = project;
    this.connectorClass = connectorClass;
    this.protoFileName = protoFileName;
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path outputProtoDir =
          Files.createDirectories(
              Path.of(
                  Optional.ofNullable(outputDirectory).orElse(project.getBuild().getDirectory())));
      Path outputProtoPath = outputProtoDir.resolve(protoFileName);
      getLog()
          .info(
              String.format(
                  "Processing data source class '%s', output to '%s'",
                  connectorClass, outputProtoPath));

      try (Writer writer = Files.newBufferedWriter(outputProtoPath)) {
        TextFormat.printer().print(createConnector(loadClassNode(connectorClass)), writer);
      }
    } catch (IOException e) {
      throw new MojoExecutionException(
          "Failed to generate connector configuration for class '" + connectorClass + "'", e);
    }
  }

  /** Creates a {@link Connector} proto message by inspecting the given class. */
  private Connector createConnector(ClassNode classNode)
      throws MojoExecutionException, IOException {

    // Find @DataSource annotated constructor with zero or one parameter.
    MethodNode constructor = findAnnotatedConstructor(DATA_SOURCE_TYPE, classNode);
    AnnotationNode dataSourceAnnotation =
        getAnnotation(DATA_SOURCE_TYPE, constructor.visibleAnnotations).orElseThrow();

    // Get the data source id from the @DataSource annotation
    String id =
        getAnnotationValue(dataSourceAnnotation, VALUE, Object::toString)
            .filter(s -> !s.isBlank())
            .orElse(getSimpleClassName(classNode));

    // Create the ConnectorConfig based on the parameters in the constructor
    ConnectorConfig connectorConfig = createConnectorConfig(constructor);

    return Connector.newBuilder()
        .setClassName(connectorClass)
        .setConfig(connectorConfig)
        .setDataSource(
            createDataSource(id, connectorConfig, classNode, dataSourceAnnotation, constructor))
        .build();
  }

  /**
   * Creates a {@link com.google.cloud.bigquery.federation.v1alpha1.DataSource} proto message.
   *
   * @param dataSourceId the id of the data source
   * @param connectorConfig the {@link ConnectorConfig} message generated based on the constructor
   *     of this data source
   * @param dataSourceAnnotation the node representing the {@link DataSource} annotation
   * @param constructor the constructor annotated with {@link DataSource}
   * @return a new instance of {@link com.google.cloud.bigquery.federation.v1alpha1.Data}
   */
  private com.google.cloud.bigquery.federation.v1alpha1.DataSource createDataSource(
      String dataSourceId,
      ConnectorConfig connectorConfig,
      ClassNode classNode,
      AnnotationNode dataSourceAnnotation,
      MethodNode constructor)
      throws IOException, MojoExecutionException {

    EnumSet<Capability> capabilities =
        getAnnotationValue(dataSourceAnnotation, CAPABILITIES, this::getCapabilities)
            .orElse(EnumSet.noneOf(Capability.class));

    if (capabilities.remove(SUPPORTS_SYNCHRONOUS_QUERIES)) {
      getLog()
          .warn(
              String.format(
                  "The '%s' capability should not be declared through annotation and it is"
                      + " ignored.",
                  SUPPORTS_SYNCHRONOUS_QUERIES));
    }
    if (capabilities.remove(SUPPORTS_PARALLEL_QUERIES)) {
      getLog()
          .warn(
              String.format(
                  "The '%s' capability should not be declared through annotation and it is"
                      + " ignored.",
                  SUPPORTS_PARALLEL_QUERIES));
    }

    boolean isConnector = false;
    for (ClassNode intf : getAllInterfaces(classNode)) {
      if (intf.name.equals(Type.getInternalName(com.google.cloud.connector.api.Connector.class))) {
        isConnector = true;
      } else if (intf.name.equals(Type.getInternalName(SynchronousQueryExecutor.class))) {
        capabilities.add(SUPPORTS_SYNCHRONOUS_QUERIES);
      } else if (intf.name.equals(Type.getInternalName(ParallelQueryExecutor.class))) {
        capabilities.add(SUPPORTS_PARALLEL_QUERIES);
      } else if (intf.name.equals(Type.getInternalName(NativeQuerySchemaResolver.class))) {
        capabilities.add(SUPPORTS_NATIVE_QUERIES);
      }
    }

    checkArgument(
        isConnector,
        "DataSource class must implement the '%s' interface",
        com.google.cloud.connector.api.Connector.class.getName());
    checkArgument(
        capabilities.contains(SUPPORTS_SYNCHRONOUS_QUERIES)
            || capabilities.contains(SUPPORTS_PARALLEL_QUERIES),
        "DataSource class must at least implements one of the interfaces '%s' or '%s'",
        SynchronousQueryExecutor.class.getName(),
        ParallelQueryExecutor.class.getName());

    ImmutableList<com.google.cloud.bigquery.federation.v1alpha1.Parameter> configParameters =
        getConfigParameters(dataSourceId, connectorConfig, constructor);

    // Validate all parameter names are unique
    Set<String> names = new HashSet<>();
    for (com.google.cloud.bigquery.federation.v1alpha1.Parameter parameter : configParameters) {
      checkArgument(
          names.add(parameter.getName()),
          "Parameter with name '%s' in data source '%s' was already defined.",
          parameter.getName(),
          dataSourceId);
    }

    com.google.cloud.bigquery.federation.v1alpha1.DataSource.Builder builder =
        com.google.cloud.bigquery.federation.v1alpha1.DataSource.newBuilder()
            .setId(dataSourceId)
            .setName(String.format("datasources/%s", dataSourceId))
            .addAllCapabilities(capabilities)
            .addAllParameters(configParameters);

    getAnnotationValue(dataSourceAnnotation, MAX_STALENESS_MILLIS, Long.class::cast)
        .map(this::millisToDuration)
        .ifPresent(builder::setMaxStaleness);

    return builder.build();
  }

  /**
   * Gets a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter} based on the
   * data source configuration.
   *
   * @param dataSourceId the id of the data source
   * @param connectorConfig the {@link ConnectorConfig} message generated based on the constructor
   *     of this data source
   * @param constructor the constructor annotated with {@link DataSource}
   * @return a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter}
   */
  private ImmutableList<com.google.cloud.bigquery.federation.v1alpha1.Parameter>
      getConfigParameters(
          String dataSourceId, ConnectorConfig connectorConfig, MethodNode constructor)
          throws IOException, MojoExecutionException {

    String namePrefix = String.format("datasources/%s/parameters", dataSourceId);

    // If the configuration comes from a custom class, extract parameters from that class,
    // otherwise, we extract parameters from the data source constructor directly.
    return switch (connectorConfig.getType()) {
      case CUSTOM_CLASS -> getConfigParameters(
          dataSourceId, namePrefix, loadClassNode(connectorConfig.getClassName()));
      case MULTI_PARAMS -> getConfigParameters(
          dataSourceId, namePrefix, connectorClass, constructor);
      default -> throw new IllegalArgumentException("ConnectorConfig type is not set");
    };
  }

  /**
   * Gets a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter} based on the
   * given configuration class. The configuration class can be a {@code record} class or a regular
   * java class with a public constructor annotated with {@link Config}.
   *
   * @param dataSourceId the id of the data source.
   * @param namePrefix the prefix for the {@link
   *     com.google.cloud.bigquery.federation.v1alpha1.Parameter#getName()}
   * @param classNode a {@link ClassNode} representing the configuration class.
   * @return a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter}
   */
  private ImmutableList<com.google.cloud.bigquery.federation.v1alpha1.Parameter>
      getConfigParameters(String dataSourceId, String namePrefix, ClassNode classNode)
          throws MojoExecutionException, IOException {

    List<RecordComponentNode> recordComponents = classNode.recordComponents;
    // If it is a record class, we can get the names and types from the record components.
    if (recordComponents != null) {
      ImmutableList.Builder<com.google.cloud.bigquery.federation.v1alpha1.Parameter> builder =
          ImmutableList.builder();
      for (RecordComponentNode componentNode : recordComponents) {
        addDataSourceParameters(
            dataSourceId,
            Type.getType(componentNode.descriptor),
            namePrefix,
            componentNode.name,
            componentNode.visibleAnnotations,
            builder);
      }
      return builder.build();
    }

    // For regular class, find the constructor annotated with @Config and extract parameters from it
    return getConfigParameters(
        dataSourceId,
        namePrefix,
        Type.getObjectType(classNode.name).getClassName(),
        findAnnotatedConstructor(CONFIG_TYPE, classNode));
  }

  /**
   * Gets a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter} from the given
   * constructor's parameters. Each of the parameter must be annotated with {@link Parameter}.
   *
   * @param dataSourceId the id of the data source.
   * @param namePrefix the prefix for the {@link
   *     com.google.cloud.bigquery.federation.v1alpha1.Parameter#getName()}
   * @param className name of the class
   * @param constructor the constructor to inspect
   * @return a list of {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter}
   */
  private ImmutableList<com.google.cloud.bigquery.federation.v1alpha1.Parameter>
      getConfigParameters(
          String dataSourceId, String namePrefix, String className, MethodNode constructor)
          throws MojoExecutionException, IOException {
    Type[] argumentTypes = Type.getArgumentTypes(constructor.desc);
    List<AnnotationNode>[] argumentAnnotations = constructor.visibleParameterAnnotations;

    int annotationsLength = Optional.ofNullable(argumentAnnotations).map(a -> a.length).orElse(0);

    if (argumentTypes.length != annotationsLength) {
      throw new MojoExecutionException(
          String.format(
              "All parameters in the constructor of class '%s' must be annotated with '@%s'",
              className, PARAMETER_TYPE.getClassName()));
    }

    ImmutableList.Builder<com.google.cloud.bigquery.federation.v1alpha1.Parameter> builder =
        ImmutableList.builder();
    for (int i = 0; i < argumentTypes.length; i++) {
      addDataSourceParameters(
          dataSourceId, argumentTypes[i], namePrefix, null, argumentAnnotations[i], builder);
    }

    return builder.build();
  }

  /**
   * Adds {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter}(s) based on the java type
   * in a configuration field.
   *
   * @param dataSourceId the id of the data source.
   * @param type the java type of the configuration field
   * @param namePrefix the prefix for the {@link
   *     com.google.cloud.bigquery.federation.v1alpha1.Parameter#getName()}
   * @param defaultName the default name of the parameter for unannotated field
   * @param annotations list of annotations on the field
   * @param builder the builder for adding the resulting parameters
   */
  private void addDataSourceParameters(
      String dataSourceId,
      Type type,
      String namePrefix,
      @Nullable String defaultName,
      @Nullable List<AnnotationNode> annotations,
      ImmutableList.Builder<com.google.cloud.bigquery.federation.v1alpha1.Parameter> builder)
      throws IOException {

    if (type.getSort() == Type.OBJECT) {
      ClassNode classNode = loadClassNode(type.getClassName());
      // Special handling of Record class to flatten out the record components to parameters.
      // Otherwise, we just pass through for the regular parameter logic.
      if (classNode.recordComponents != null) {
        addParametersFromRecord(dataSourceId, namePrefix, annotations, builder, classNode);
        return;
      }
    }

    // Regular parameter handling
    String id =
        getAnnotation(PARAMETER_TYPE, annotations)
            .flatMap(annotation -> getAnnotationValue(annotation, VALUE, Object::toString))
            .filter(s -> !s.isBlank())
            .orElse(defaultName);

    checkArgument(
        id != null,
        "Missing parameter name for type '%s' in datasource '%s'",
        type.getClassName(),
        dataSourceId);

    SemanticInfo semanticInfo =
        getAnnotation(SEMANTIC_TYPE, annotations)
            .map(
                annotation ->
                    new SemanticInfo(
                        getAnnotationValue(annotation, VALUE, o -> getEnumValue(Category.class, o))
                            .orElse(UNKNOWN),
                        getAnnotationValue(
                                annotation,
                                CUSTOM_SUBCATEGORY,
                                ConnectorGeneratorMojo::toOptionalString)
                            .orElse(Optional.empty())))
            .orElse(SemanticInfo.NONE);

    builder.add(createParameter(semanticInfo, namePrefix, id, type));
  }

  /**
   * Adds {@link com.google.cloud.bigquery.federation.v1alpha1.Parameter}(s) from a record class.
   *
   * @param dataSourceId the id of the data source.
   * @param namePrefix the prefix for the {@link
   *     com.google.cloud.bigquery.federation.v1alpha1.Parameter#getName()}
   * @param parameterAnnotations list of annotations on the field
   * @param builder the builder for adding the resulting parameters
   * @param classNode the {@link ClassNode} of the record class
   */
  private void addParametersFromRecord(
      String dataSourceId,
      String namePrefix,
      @Nullable List<AnnotationNode> parameterAnnotations,
      ImmutableList.Builder<com.google.cloud.bigquery.federation.v1alpha1.Parameter> builder,
      ClassNode classNode)
      throws IOException {
    // Get the @Semantic annotation from the record class. If it is missing, fallback to the
    // annotation from the parameter.
    AnnotationNode semanticAnnotation =
        Optional.ofNullable(classNode.visibleAnnotations).stream()
            .flatMap(List::stream)
            .filter(annotationFilter(SEMANTIC_TYPE))
            .findFirst()
            .or(() -> getAnnotation(SEMANTIC_TYPE, parameterAnnotations))
            .orElse(null);

    for (RecordComponentNode componentNode : classNode.recordComponents) {
      List<AnnotationNode> componentAnnotations = componentNode.visibleAnnotations;
      if (semanticAnnotation != null) {
        componentAnnotations =
            Optional.ofNullable(componentAnnotations).map(ArrayList::new).orElseGet(ArrayList::new);

        // Add the semantic annotation from the record class to the component if the component
        // doesn't have it
        if (componentAnnotations.stream().noneMatch(annotationFilter(SEMANTIC_TYPE))) {
          componentAnnotations.add(semanticAnnotation);
        }
      }

      addDataSourceParameters(
          dataSourceId,
          Type.getType(componentNode.descriptor),
          namePrefix,
          componentNode.name,
          componentAnnotations,
          builder);
    }
  }

  private com.google.cloud.bigquery.federation.v1alpha1.Parameter createParameter(
      SemanticInfo semanticInfo, String namePrefix, String id, Type parameterType) {
    String paramId = parameterId(semanticInfo, id);
    return com.google.cloud.bigquery.federation.v1alpha1.Parameter.newBuilder()
        .setId(paramId)
        .setName(String.format("%s/%s", namePrefix, paramId))
        .setType(toProtoType(parameterType))
        .build();
  }

  /**
   * Returns the string representation of the parameter id, fully qualified by the semantic
   * category.
   */
  private String parameterId(SemanticInfo semanticInfo, String id) {
    var idParts = ImmutableList.builder();
    idParts.addAll(semanticInfo.category().getPrefix());
    semanticInfo.subcategory().ifPresent(idParts::add);
    idParts.add(id);
    return Joiner.on(".").join(idParts.build());
  }

  /** Finds the only public constructor annotated with the given annotation from the given class. */
  private MethodNode findAnnotatedConstructor(Type annotationType, ClassNode classNode)
      throws MojoExecutionException {

    List<MethodNode> constructors =
        classNode.methods.stream()
            .filter(m -> m.name.equals("<init>"))
            .filter(m -> (m.access & ACC_PUBLIC) == ACC_PUBLIC)
            .filter(m -> getAnnotation(annotationType, m.visibleAnnotations).isPresent())
            .toList();

    if (constructors.isEmpty()) {
      throw new MojoExecutionException(
          String.format(
              "No constructor annotated with '%s' was found in class '%s'",
              annotationType.getClassName(), Type.getObjectType(classNode.name).getClassName()));
    }
    if (constructors.size() > 1) {
      throw new MojoExecutionException(
          String.format(
              "Exactly one constructor should be annotated with '%s' in class '%s'",
              annotationType.getClassName(), Type.getObjectType(classNode.name).getClassName()));
    }

    return constructors.get(0);
  }

  private String getSimpleClassName(ClassNode classNode) {
    // For top level class, return the last part of the class descriptor
    if (classNode.nestHostClass == null) {
      int idx = classNode.name.lastIndexOf('/');
      return idx < 0 ? classNode.name : classNode.name.substring(idx + 1);
    }
    // For inner class, return the part after the last '$' sign
    int idx = classNode.name.lastIndexOf('$');
    return idx < 0 ? classNode.name : classNode.name.substring(idx + 1);
  }

  private Optional<AnnotationNode> getAnnotation(
      Type annotationType, @Nullable List<AnnotationNode> annotations) {
    if (annotations == null) {
      return Optional.empty();
    }

    return annotations.stream().filter(annotationFilter(annotationType)).findFirst();
  }

  /**
   * Returns a {@link Predicate} that matches {@link AnnotationNode} to the given annotation type.
   *
   * @param annotationType the annotation {@link Type} to match
   */
  private Predicate<AnnotationNode> annotationFilter(Type annotationType) {
    return annotationNode -> annotationType.getDescriptor().equals(annotationNode.desc);
  }

  /**
   * Gets the value for an attribute in the given annotation.
   *
   * @param <T> type of the result
   * @param annotation the annotation to search in
   * @param attribute the name of the annotation attribute to search for
   * @param converter a function to apply on the attribute value object to get the result
   * @return a {@link Optional} which contains the result of applying the converter function on the
   *     value. If no such attribute was found, an empty {@link Optional} will be returned.
   */
  private <T> Optional<T> getAnnotationValue(
      AnnotationNode annotation, String attribute, Function<Object, T> converter) {
    if (annotation.values == null) {
      return Optional.empty();
    }

    // The annotation.values contains pairs of (name, values) in the list.
    for (int i = 0; i < annotation.values.size(); i += 2) {
      if (Objects.equals(attribute, annotation.values.get(i))) {
        return Optional.of(annotation.values.get(i + 1)).map(converter);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the federation API type based on the ASM {@link Type}. Currently only supports
   * primitive types, {@link String}, {@link URL}, and {@link URI}.
   */
  private com.google.cloud.bigquery.federation.v1alpha1.Type toProtoType(Type type) {
    com.google.cloud.bigquery.federation.v1alpha1.Type.Builder typeBuilder =
        com.google.cloud.bigquery.federation.v1alpha1.Type.newBuilder();

    // Unwrap if it is a boxed type.
    Type unwrappedType =
        Primitives.allWrapperTypes().stream()
            .filter(t -> Type.getType(t).equals(type))
            .map(Primitives::unwrap)
            .map(Type::getType)
            .findFirst()
            .orElse(type);

    return switch (unwrappedType.getSort()) {
      case Type.BOOLEAN -> typeBuilder.setTypeKind(TypeKind.BOOL).build();
      case Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> typeBuilder
          .setTypeKind(TypeKind.INT32)
          .build();
      case Type.LONG -> typeBuilder.setTypeKind(TypeKind.INT64).build();
      case Type.FLOAT -> typeBuilder.setTypeKind(TypeKind.FLOAT).build();
      case Type.DOUBLE -> typeBuilder.setTypeKind(TypeKind.DOUBLE).build();
      case Type.OBJECT -> {
        if (Type.getType(String.class).equals(unwrappedType)
            || Type.getType(URL.class).equals(unwrappedType)
            || Type.getType(URI.class).equals(unwrappedType)) {
          yield typeBuilder.setTypeKind(TypeKind.STRING).build();
        }
        throw new IllegalArgumentException("Unsupported config type " + type.getClassName());
      }
      default -> throw new IllegalArgumentException(
          "Unsupported config type " + type.getClassName());
    };
  }

  /**
   * Loads the bytecode of a given class.
   *
   * @param className name of the class to load
   * @return a {@link ClassNode} representing the bytecode of the class
   * @throws IOException if failed to load the class
   */
  private ClassNode loadClassNode(String className) throws IOException {
    try (InputStream is = openClassStream(className)) {
      ClassNode classNode = new ClassNode(ASM9);
      new ClassReader(is).accept(classNode, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
      return classNode;
    }
  }

  /**
   * Opens an {@link InputStream} to the .class file of the given class by searching in the project
   * build path and dependencies.
   */
  private InputStream openClassStream(String className) throws IOException {
    String classFileName = className.replace('.', File.separatorChar) + ".class";
    Path classFile = Path.of(project.getBuild().getOutputDirectory(), classFileName);
    if (Files.exists(classFile)) {
      return Files.newInputStream(classFile);
    }

    // If the class is coming from dependency classes, look it up from dependencies.
    for (Artifact artifact : project.getArtifacts()) {
      if (artifact.getFile().isDirectory()) {
        File file = new File(artifact.getFile(), classFileName);
        if (file.isFile()) {
          return new FileInputStream(file);
        }
      } else if (artifact.getFile().isFile()) {
        JarFile jarFile = new JarFile(artifact.getFile());
        try {
          JarEntry jarEntry = jarFile.getJarEntry(className.replace('.', '/') + ".class");
          if (jarEntry != null) {
            return new FilterInputStream(jarFile.getInputStream(jarEntry)) {
              @Override
              public void close() throws IOException {
                try {
                  super.close();
                } finally {
                  jarFile.close();
                }
              }
            };
          }
        } catch (IOException e) {
          jarFile.close();
          throw e;
        }
      }
    }

    // Search the class from the Java platform
    return Optional.ofNullable(
            ClassLoader.getPlatformClassLoader().getResourceAsStream(classFileName))
        .orElseThrow(
            () -> new IOException("Failed to open class file for class '" + className + "'"));
  }

  /**
   * Returns a list of {@link Capability} from the {@link DataSource#capabilities()} annotation
   * value. Since the type is an array of enum, based on {@link AnnotationNode#values}, it will be a
   * {@link List} of {@link String} array with two elements, with the first element as the type
   * descriptor of {@link Capability}, and the second element as the enum name.
   */
  private EnumSet<Capability> getCapabilities(Object annotationValue) {
    checkArgument(annotationValue instanceof List, "The capabilities value must be a List");

    return ((List<?>) annotationValue)
        .stream()
            .map(o -> getEnumValue(Capability.class, o))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
  }

  /**
   * Returns an enum value from the annotation value.
   *
   * @param enumClass the enum class
   * @param annotationValue the value from the annotation node. Expected to be a string array of
   *     size two, with the first element as the enum type, and the second element as the enum name.
   * @param <T> the enum type
   * @return the enum value
   * @see AnnotationNode#values
   */
  private <T extends Enum<T>> T getEnumValue(Class<T> enumClass, Object annotationValue) {
    checkArgument(annotationValue instanceof String[], "Enum value must be a string array");

    String[] values = (String[]) annotationValue;
    checkArgument(values.length == 2, "Enum value must be a string array of size 2");

    checkArgument(
        Type.getType(values[0]).equals(Type.getType(enumClass)),
        "Expected enum class as '%s', but get '%s'",
        enumClass.getName(),
        Type.getType(values[0]).getClassName());

    return Enum.valueOf(enumClass, values[1]);
  }

  private Duration millisToDuration(long millis) {
    java.time.Duration duration = java.time.Duration.ofMillis(millis);
    return Duration.newBuilder()
        .setSeconds(duration.getSeconds())
        .setNanos(duration.getNano())
        .build();
  }

  private ConnectorConfig createConnectorConfig(MethodNode constructor) {
    ConnectorConfig.Builder builder = ConnectorConfig.newBuilder();

    Type[] argumentTypes = Type.getArgumentTypes(constructor.desc);
    if (argumentTypes.length != 1) {
      return builder.setType(ConnectorConfig.Type.MULTI_PARAMS).build();
    }

    if (constructor.visibleParameterAnnotations != null
        && constructor.visibleParameterAnnotations.length == argumentTypes.length) {
      return builder.setType(ConnectorConfig.Type.MULTI_PARAMS).build();
    }

    return builder
        .setType(ConnectorConfig.Type.CUSTOM_CLASS)
        .setClassName(argumentTypes[0].getClassName())
        .build();
  }

  /** Returns all the interfaces that the given class implemented. */
  private Set<ClassNode> getAllInterfaces(ClassNode classNode) throws IOException {
    Set<ClassNode> result = new HashSet<>();
    for (String intf : classNode.interfaces) {
      ClassNode intfClassNode = loadClassNode(Type.getObjectType(intf).getClassName());
      result.add(intfClassNode);
      result.addAll(getAllInterfaces(intfClassNode));
    }

    if (classNode.superName != null
        && !classNode.superName.equals(Type.getInternalName(Object.class))) {
      result.addAll(
          getAllInterfaces(loadClassNode(Type.getObjectType(classNode.superName).getClassName())));
    }

    return result;
  }

  /**
   * Returns the string representation of the object as {@code Optional}, or {@code
   * Optional.empty()} if the representation is an empty string.
   */
  private static Optional<String> toOptionalString(Object o) {
    var string = o.toString();
    return Strings.isNullOrEmpty(string) ? Optional.empty() : Optional.of(string);
  }

  /** A record for holding information declared by the {@link Semantic} annotation. */
  private record SemanticInfo(Category category, Optional<String> subcategory) {

    static final SemanticInfo NONE = new SemanticInfo(UNKNOWN, Optional.empty());
  }
}