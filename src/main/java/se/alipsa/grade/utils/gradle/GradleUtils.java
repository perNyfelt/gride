package se.alipsa.grade.utils.gradle;

import static se.alipsa.grade.Constants.MavenRepositoryUrl.MAVEN_CENTRAL;
import static se.alipsa.grade.menu.GlobalOptions.GRADLE_HOME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.*;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.gradle.util.GradleVersion;
import se.alipsa.grade.Grade;
import se.alipsa.grade.console.ConsoleTextArea;
import se.alipsa.grade.model.Dependency;
import se.alipsa.grade.utils.FileUtils;
import se.alipsa.grade.utils.MavenRepoLookup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GradleUtils {

  private static final Logger log = LogManager.getLogger();

  private final GradleConnector connector;

  public GradleUtils(Grade gui) throws FileNotFoundException {
    this(
        gui.getInoutComponent().projectDir(),
        new File(gui.getPrefs().get(GRADLE_HOME, GradleUtils.locateGradleHome()))
    );
  }

  public GradleUtils(File gradleInstallationDir, File projectDir) throws FileNotFoundException {
    if (!gradleInstallationDir.exists()) {
      throw new FileNotFoundException("Gradle home " + gradleInstallationDir + " does not exist");
    }
    if (!projectDir.exists()) {
      throw new FileNotFoundException("Project dir " + projectDir + " does not exist");
    }
    connector = GradleConnector.newConnector();
    connector.useInstallation(gradleInstallationDir);
    connector.forProjectDirectory(projectDir);
  }

  public static String locateGradleHome() {
    String gradleHome = System.getProperty("GRADLE_HOME", System.getenv("GRADLE_HOME"));
    if (gradleHome == null) {
      gradleHome = locateGradle();
    }
    return gradleHome;
  }

  private static String locateGradle() {
    String path = System.getenv("PATH");
    String[] pathElements = path.split(System.getProperty("path.separator"));
    for (String elem : pathElements) {
      File dir = new File(elem);
      if (dir.exists()) {
        String [] files = dir.list();
        if (files != null) {
          boolean foundMvn = Arrays.asList(files).contains("gradle");
          if (foundMvn) {
            return dir.getParentFile().getAbsolutePath();
          }
        }
      }
    }
    return "";
  }

  public String getGradleVersion() {
    return GradleVersion.current().getVersion();
  }

  public List<String> getGradleTaskNames() {
    List<GradleTask> tasks = getGradleTasks();
    return tasks.stream().map(Task::getName).collect(Collectors.toList());
  }

  public List<GradleTask> getGradleTasks() {
    List<GradleTask> tasks;
    try (ProjectConnection connection = connector.connect()) {
      GradleProject project = connection.getModel(GradleProject.class);
      tasks = new ArrayList<>(project.getTasks());
    }
    return tasks;
  }

  public void buildProject(String... tasks) {
    ProgressListener listener = new ProgressListener() {
      final ConsoleTextArea console = Grade.instance().getConsoleComponent().getConsole();
      @Override
      public void statusChanged(ProgressEvent progressEvent) {
        console.appendFx(progressEvent.getDescription(), true);
      }
    };
    buildProject(listener, tasks);
  }

  /**
   *
   * @param progressListener may be null
   * @param tasks the tasks to run e.g. clean build
   */
  public void buildProject(ProgressListener progressListener, String... tasks) {
    try(ProjectConnection connection = connector.connect()) {
      BuildLauncher build = connection.newBuild();
      if (progressListener != null) {
        build.addProgressListener(progressListener);
      }
      if (tasks.length > 0) {
        build.forTasks(tasks);
      }
      build.run();
    }
  }

  public List<String> getProjectDependencyNames() {
    return getProjectDependencies().stream()
        .map(File::getName)
        .collect(Collectors.toList());

  }

  public List<File> getProjectDependencies() {
    List<File> dependencyFiles = new ArrayList<>();
    try (ProjectConnection connection = connector.connect()) {
      IdeaProject project = connection.getModel(IdeaProject.class);
      for (IdeaModule module : project.getModules()) {
        for (IdeaDependency dependency : module.getDependencies()) {
          IdeaSingleEntryLibraryDependency ideaDependency = (IdeaSingleEntryLibraryDependency) dependency;
          File file = ideaDependency.getFile();
          dependencyFiles.add(file);
        }
      }
    }
    return dependencyFiles;
  }

  public ClassLoader createGradleCLassLoader(ClassLoader parent) throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    for (File f : getProjectDependencies()) {
      urls.add(f.toURI().toURL());
    }
    try (ProjectConnection connection = connector.connect()) {
      IdeaProject project = connection.getModel(IdeaProject.class);
      for (IdeaModule module : project.getModules()) {
        urls.add(module.getCompilerOutput().getOutputDir().toURI().toURL());
      }
    }
    return new URLClassLoader(urls.toArray(new URL[0]), parent);
  }

  public static void purgeCache(Dependency dependency) {
    File cachedFile = cachedFile(dependency);
    if (cachedFile.exists()) {
      if (cachedFile.delete()) {
        return;
      }
      log.info("Failed to delete {}, it will be purged on application exit", dependency);
      cachedFile.deleteOnExit();
    }
  }

  public static File getCacheDir() {
    File dir = new File(FileUtils.getUserHome(), ".grade/cache");
    if (!dir.exists()) {
      if(!dir.mkdirs()) {
        throw new RuntimeException("Failed to create cache dir " + dir);
      }
    }
    return dir;
  }

  public static File cachedFile(Dependency dependency) {
    String subDir = MavenRepoLookup.subDir(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    String fileName = MavenRepoLookup.jarFile(dependency.getArtifactId(), dependency.getVersion());
    return new File(getCacheDir(), subDir + fileName);
  }

  public static File downloadArtifact(Dependency dependency) throws IOException {
    File cachedFile = cachedFile(dependency);
    if (cachedFile.exists()) {
      return cachedFile;
    }
    String url = MavenRepoLookup.artifactUrl(dependency, MAVEN_CENTRAL.baseUrl);
    URL artifactUrl = new URL(url);
    if (!cachedFile.getParentFile().exists()) {
      if (!cachedFile.getParentFile().mkdirs()) {
        throw new IOException("Failed to create directory " + cachedFile.getParentFile());
      }
    }
    if (!cachedFile.createNewFile()) {
      throw new IOException("Failed to create file " + cachedFile);
    }
    ReadableByteChannel readableByteChannel = Channels.newChannel(artifactUrl.openStream());
    try(FileOutputStream fileOutputStream = new FileOutputStream(cachedFile)) {
      fileOutputStream.getChannel()
          .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }
    return cachedFile;
  }
}
