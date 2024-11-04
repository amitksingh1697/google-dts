package com.google.cloud.connector.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** A simple jar builder that creates a jar file from class and resource files. */
public class JarBuilder {
  private static final String RESOURCE_DIR_NAME = "resources";
  private final JarOutputStream jarOutputStream;

  /**
   * Constructs a {@link JarBuilder} that will produce output jar file at the specified file path.
   *
   * @param jarFile the final output jar file path.
   * @throws IOException if any io error occurs.
   */
  public JarBuilder(Path jarFile) throws IOException {
    this.jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile.toFile()));
  }

  /**
   * Adds a list class files to the jar that will be built.
   *
   * @param classFiles a list of class file paths (including class base path) to be added.
   * @param classBasePath the root directory of java classes.
   * @return {@link JarBuilder} that can be used to add more files to the jar.
   * @throws IOException if any io error occurs.
   */
  public JarBuilder addClassPaths(List<Path> classFiles, Path classBasePath) throws IOException {
    for (Path classFile : classFiles) {
      String entryName = classFile.toString().substring(classBasePath.toString().length() + 1);
      entryName = entryName.replace(File.separator, "/");
      JarEntry entry = new JarEntry(entryName);
      addJarEntry(classFile, entry);
    }
    return this;
  }

  /**
   * Adds a list resource files to the jar that will be built.
   *
   * @param resourceFiles a list of resource file paths (including class base path) to be added.
   * @param classBasePath the root directory of java classes.
   * @return {@link JarBuilder} that can be used to add more files to the jar.
   * @throws IOException if any io error occurs.
   */
  public JarBuilder addResourcePaths(List<Path> resourceFiles, Path classBasePath)
      throws IOException {
    for (Path resourceFile : resourceFiles) {
      String entryName = resourceFile.toString().substring(classBasePath.toString().length() + 1);
      entryName = RESOURCE_DIR_NAME + "/" + entryName.replace(File.separator, "/");
      JarEntry entry = new JarEntry(entryName);
      addJarEntry(resourceFile, entry);
    }
    return this;
  }

  /**
   * Finalizes the output jar file based on the list of files added. No more file can be added after
   * this point.
   *
   * @throws IOException if any io error occurs.
   */
  public void build() throws IOException {
    jarOutputStream.close();
  }

  private void addJarEntry(Path file, JarEntry entry) throws IOException {
    jarOutputStream.putNextEntry(entry);
    byte[] classBytes = Files.readAllBytes(file);
    jarOutputStream.write(classBytes);
    jarOutputStream.closeEntry();
  }
}
