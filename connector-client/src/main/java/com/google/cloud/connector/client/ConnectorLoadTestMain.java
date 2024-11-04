package com.google.cloud.connector.client;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Connector Load Test Main class.
 */
@CommandLine.Command()
public class ConnectorLoadTestMain implements Callable {

  @CommandLine.Option(names = {"-c", "--connector"},
      required = true,
      description = "Name of the connector for which the load test to be executed.")
  private String connector;

  @Override
  public Object call() throws Exception {
    // Validate that the connector config files are present
    if (validateConfigFile(connector)) {
      File propFile = new File(getPropertyFileName(connector));
      Properties datasourceProps = getProperties(new FileInputStream(propFile), "datasource");
      StringBuilder configuration = new StringBuilder();
      configuration.append("-c ");
      datasourceProps.forEach((key, value) ->
          configuration.append(key).append("=").append(value).append(","));
      configuration.deleteCharAt(configuration.length() - 1);

      Properties runs = getProperties(new FileInputStream(propFile), "run");
      runs.forEach((key, value) -> {
        String arg = configuration.toString();
        arg = arg + " " + value;
        System.out.println("Received Load Test '" + key + "' having args:" + arg);
        PrintStream out = System.out;
        PrintStream err = System.err;
        try {
          String outFileName = getLoadTestOutFileName(connector, (String) key);
          Files.createParentDirs(new File(outFileName));

          // Divert the out and err stream to file
          System.setOut(new PrintStream(outFileName));
          System.setErr(new PrintStream(outFileName + ".err"));

          ArrayList<String> args = getCliArgumentList(arg);
          System.out.println("Starting Load Test '" + key + "' using args:"
              + String.join(" ", args));
          new CommandLine(new ConnectorClientMain()).execute(args.toArray(String[]::new));
        } catch (Exception ex) {
          System.out.println("Failure while running Load Test '" + key + "': " + ex.getMessage());
          return;
        } finally {

          // Revert the out and err stream back to original
          System.setOut(out);
          System.setErr(err);
        }
        System.out.println("Completed Load Test '" + key + "'.");
      });
      System.out.println("Completed Load Test for connector '" + connector + "'.");
    }

    return null;
  }

  private ArrayList<String> getCliArgumentList(String arg) {
    ArrayList<String> args = new ArrayList<String>();

    // Handle the Native Query specially to ensure the args split is proper.
    if (arg.contains("-nq")) {
      String nativeQuery = arg.substring(arg.indexOf("\"") + 1, arg.lastIndexOf("\""));
      args.addAll(
          Arrays.stream(arg.substring(0, arg.indexOf("-nq ")).split(" ")).toList());
      args.add("-nq");
      args.add(nativeQuery);
    } else {
      args.addAll(Arrays.stream(arg.split(" ")).toList());
    }

    return args;
  }

  private String getLoadTestOutFileName(String connector, String runName) {
    return String.format("LoadTestResults/%s/%s", connector, runName);
  }

  private boolean validateConfigFile(String connector) {
    File loadTestFile = new File(getPropertyFileName(connector));
    Preconditions.checkArgument(loadTestFile.exists(),
        "Load Test file not found for " + connector);
    return true;
  }

  private static String getPropertyFileName(String connector) {
    return "src/main/resources/" + connector + "_load_test_config.properties";
  }

  private Properties getProperties(InputStream is, String prefix) throws IOException {
    Properties props = new Properties();
    Properties result = new Properties();
    props.load(is);
    props.forEach(
        (key, value) -> {
          if (key instanceof String && ((String) key).startsWith(prefix)) {
            result.put(key.toString().replaceFirst(prefix + ".", ""), value);
          }
        });
    return result;
  }

  public static void main(String[] args) {
    new CommandLine(new ConnectorLoadTestMain()).execute(args);
  }
}
