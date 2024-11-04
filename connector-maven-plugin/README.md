# Connector Maven Plugin

This maven plugin generates a proto file that describes the connector by
inspecting the connector class.

The connector class must implement the
`com.google.cloud.connector.api.Connector` interface and at least
one of `com.google.cloud.connector.api.SynchronousQueryExecutor`
or `com.google.cloud.connector.api.ParallelQueryExecutor` interfaces.
There should be exactly one `public` constructor that is annotated with
`com.google.cloud.connector.api.annotation.DataSource`.

## Connector Parameters

There are two ways that the connector class to declare a set of parameters that
it accepts, which is derived by the parameter of the annotated public
constructor of the connector class.

### Declare Parameters with Configuration class

The preferred way is to declare a `record` class and have it as the only
parameter to the annotated constructor of the connector class. By default, the
record field name is used as the parameter name. You can
optionally annotate the record field with
`com.google.cloud.connector.api.annotation.Parameter` to use a different name.
For example:

```java
// The connector class
public class OracleConnector implements Connector, SynchronousQueryExecutor {

  // The configuration class
  public record OracleConfig(@Parameter("host") String hostname, int port) {}

  private final OracleConfig config;

  // Public constructor that takes the config class as the only parameter
  @DataSource
  public OracleConnector(OracleConfig config) {
    this.config = config;
  }
}

```

If you don't use record class, you can use a simple Java class as the
configuration class by having exactly one public constructor annotated with
`com.google.cloud.connector.api.annotation.Config`, and with all the parameters
of that constructor annotated with
`com.google.cloud.connector.api.annotation.Parameter`. For example:

```java
public class SapConfig {

  private final String project;

  @Config
  public SapConfig(@Parameter("project") String project) {
    this.project = project;
  }
}
```

### Declare Parameters with Connector's Constructor

You can also save the declaration of the configuration class by declaring
supported configurations directly through the connector class constructor by
annotating all the constructor's parameters with
`com.google.cloud.connector.api.annotation.Parameter`. For example:

```java
public class SalesforceConnector implements Connector,
    SynchronousQueryExecutor {

  public SalesforceConnector(
      @Parameter("username") String username,
      @Parameter("password") String password) {
    // ...
  }
}
```

### Supported Types

The following types are supported as parameters:

* [Java primitive types](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html)
* [String](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html)
* [URL](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URL.html)
* [URI](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URI.html)
* [Record class](https://docs.oracle.com/en/java/javase/17/language/records.html)

  The record components will get flatten out as top level parameters. The type
  of each record component must be one of the types listed here.

By default, all parameters are required. To indicate a parameter is optional,
annotate it with `@Nullable` from any common library,
such as `javax.annotation.Nullable`.

### Semantic Types

You can provide the semantic of a parameter by using
the `com.google.cloud.connector.api.annotation.Semantic` annotation.
The annotation can be applied to a record class or to individual parameter. For
example:

```java
public record MyConfig(@Semantic(ENDPOINT) URL url, SecretConfig secretConfig) {}

@Semantic(AUTHENTICATION)
public record SecretConfig(URL kmsUrl, String keyName) {}

```

The `MyConfig` class above will generate three parameters for the connector:
`endpoint.url`, `authentication.kmsUrl`, and `authentication.keyName`.

There are built-in record classes for common configurations under
the `com.google.cloud.connector.api.config` package, such
as `HostAndPort`, `Oauth`, and `UsernameAndPassword`.

## Usage

You can use the plugin by adding it to the `<build>` section in the `pom.xml`
file.

```xml

<build>
  <plugins>
    <plugin>
      <groupId>com.google.cloud.connector</groupId>
      <artifactId>connector-maven-plugin</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <connectorClass>
              com.google.cloud.connector.salesforce.adapter.SalesforceAdapter
            </connectorClass>
          </configuration>
        </execution>
      </executions>
    </plugin>
    ...
  </plugins>
</build>
```

### Configurations

**Goal:** `generate`  
**Default phase:** `prepare-package`

| Configuration Name |  Command Line Property Name   | Required | Default Value                | Description                                       |
|:------------------:|:-----------------------------:|:--------:|------------------------------|---------------------------------------------------|
|  `connectorClass`  |    `connector.class.name`     |    Y     |                              | Fully qualified class name of the connector class |
|  `protoFileName`   |  `connector.proto.file.name`  |    N     | `connector.textproto`        | Name of the output text proto file                |
| `outputDirectory`  | `connector.output.directory`  |    N     | `${project.build.directory}` | Directory of the output proto file                |