package se.alipsa.grade.menu;

import static se.alipsa.grade.Constants.*;
import static se.alipsa.grade.console.ConsoleTextArea.CONSOLE_MAX_LENGTH_DEFAULT;
import static se.alipsa.grade.menu.GlobalOptions.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import se.alipsa.grade.Grade;
import se.alipsa.grade.utils.ExceptionAlert;
import se.alipsa.grade.utils.GuiUtils;
import se.alipsa.grade.utils.IntField;

import java.io.File;
import java.util.*;

class GlobalOptionsDialog extends Dialog<GlobalOptions> {

  private IntField intField;
  private ComboBox<String> themes;
  private ComboBox<String> locals;
  private TextField gradleHome;
  private CheckBox useGradleFileClasspath;
  private CheckBox restartSessionAfterGradleRun;
  private CheckBox addBuildDirToClasspath;
  private CheckBox enableGit;
  private CheckBox autoRunGlobal;
  private CheckBox autoRunProject;
  private CheckBox addImports;


  GlobalOptionsDialog(Grade gui) {
    try {
      setTitle("Global options");
      getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      GridPane grid = new GridPane();
      grid.setHgap(10);
      grid.setVgap(15);
      grid.setPadding(new Insets(10, 15, 10, 10));
      getDialogPane().setContent(grid);

      Label consoleMaxSizeLabel = new Label("Console max size");
      grid.add(consoleMaxSizeLabel, 0, 0);
      intField = new IntField(1000, Integer.MAX_VALUE, gui.getPrefs().getInt(CONSOLE_MAX_LENGTH_PREF, CONSOLE_MAX_LENGTH_DEFAULT));
      grid.add(intField, 1, 0);

      Label styleTheme = new Label("Style theme");
      grid.add(styleTheme, 0, 1);
      themes = new ComboBox<>();
      themes.getItems().addAll(DARK_THEME, BRIGHT_THEME, BLUE_THEME);
      themes.getSelectionModel().select(gui.getPrefs().get(THEME, BRIGHT_THEME));
      grid.add(themes, 1, 1);

      Label defaultLocale = new Label("Default locale");
      grid.add(defaultLocale, 2, 1);

      locals = new ComboBox<>();
      Set<String> languageTags = new TreeSet<>();
      languageTags.add(new Locale("sv", "SE").toLanguageTag());
      for (var loc : Locale.getAvailableLocales()) {
        languageTags.add(loc.toLanguageTag());
      }
      locals.getItems().addAll(languageTags);
      locals.getSelectionModel().select(gui.getPrefs().get(DEFAULT_LOCALE, Locale.getDefault().toLanguageTag()));
      grid.add(locals, 3, 1);


      Label gradleHomeLabel = new Label("GRADLE_HOME");
      gradleHomeLabel.setTooltip(new Tooltip("The location of your gradle installation directory"));
      //mavenHomeLabel.setPadding(new Insets(0, 27, 0, 0));
      grid.add(gradleHomeLabel, 0,2);

      HBox gradleHomePane = new HBox();
      gradleHomePane.setAlignment(Pos.CENTER_LEFT);
      gradleHome = new TextField();
      HBox.setHgrow(gradleHome, Priority.ALWAYS);
      var defaultGradleHome = gui.getPrefs().get(GRADLE_HOME, System.getProperty("GRADLE_HOME", System.getenv("GRADLE_HOME")));
      gradleHome.setText(defaultGradleHome);
      gradleHomePane.getChildren().add(gradleHome);
      Button browseGradleHomeButton = new Button("...");
      browseGradleHomeButton.setOnAction(a -> {
        DirectoryChooser chooser = new DirectoryChooser();
        String initial = defaultGradleHome == null || "".equals(defaultGradleHome) ? "." : defaultGradleHome;
        chooser.setInitialDirectory(new File(initial));
        chooser.setTitle("Select Gradle home dir");
        File dir = chooser.showDialog(gui.getStage());
        if (dir != null) {
          gradleHome.setText(dir.getAbsolutePath());
        }
      });
      gradleHomePane.getChildren().add(browseGradleHomeButton);
      grid.add(gradleHomePane, 1,2,3, 1);

      FlowPane useCpPane = new FlowPane();
      grid.add(useCpPane, 0,3, 4, 1);

      Label useGradleFileClasspathLabel = new Label("Use build.gradle classpath");
      useGradleFileClasspathLabel.setTooltip(new Tooltip("Use classpath from build.gradle (if available) when running Groovy code"));
      useGradleFileClasspathLabel.setPadding(new Insets(0, 26, 0, 0));
      useCpPane.getChildren().add(useGradleFileClasspathLabel);
      useGradleFileClasspath = new CheckBox();
      useGradleFileClasspath.setSelected(gui.getPrefs().getBoolean(USE_GRADLE_CLASSLOADER, false));
      useCpPane.getChildren().add(useGradleFileClasspath);

      Label addBuildDirToClasspathLabel = new Label("Add build dir to classpath");
      addBuildDirToClasspathLabel.setPadding(new Insets(0, 27, 0, 20));
      addBuildDirToClasspathLabel.setTooltip(new Tooltip("Add target/classes and target/test-classes to classpath"));
      useCpPane.getChildren().add(addBuildDirToClasspathLabel);
      addBuildDirToClasspath = new CheckBox();
      addBuildDirToClasspath.setSelected(gui.getPrefs().getBoolean(ADD_BUILDDIR_TO_CLASSPATH, true));
      useCpPane.getChildren().add(addBuildDirToClasspath);

      // When developing packages we need to reload the session after mvn has been run
      // so that new definitions can be picked up from target/classes.
      FlowPane restartPane = new FlowPane();
      grid.add(restartPane, 0,4, 4, 1);
      Label restartSessionAfterGradleRunLabel = new Label("Restart session after build");
      restartSessionAfterGradleRunLabel.setPadding(new Insets(0, 27, 0, 0));
      restartSessionAfterGradleRunLabel.setTooltip(new Tooltip("When developing packages we need to reload the session after gradle has been run\nso that new definitions can be picked up from the build dir"));
      restartPane.getChildren().add(restartSessionAfterGradleRunLabel);
      restartSessionAfterGradleRun = new CheckBox();
      restartSessionAfterGradleRun.setSelected(gui.getPrefs().getBoolean(RESTART_SESSION_AFTER_GRADLE_RUN, true));
      restartPane.getChildren().add(restartSessionAfterGradleRun);

      FlowPane gitOptionPane = new FlowPane();
      Label enableGitLabel = new Label("Enable git integration");
      enableGitLabel.setPadding(new Insets(0, 20, 0, 0));
      enableGitLabel.setTooltip(new Tooltip("note: git must be initialized in the project dir for integration to work"));
      gitOptionPane.getChildren().add(enableGitLabel);
      enableGit = new CheckBox();
      enableGit.setSelected(gui.getPrefs().getBoolean(ENABLE_GIT, true));
      gitOptionPane.getChildren().add(enableGit);
      grid.add(gitOptionPane, 0, 5, 2, 1);

      HBox autoRunPane = new HBox();
      Label autoRunGlobalLabel = new Label("Run global autorun.groovy on init");
      autoRunGlobalLabel.setTooltip(new Tooltip("Run autorun.groovy from Grade install dir each time a session (re)starts."));
      autoRunGlobalLabel.setPadding(new Insets(0, 20, 0, 0));
      autoRunGlobal = new CheckBox();
      autoRunGlobal.setSelected(gui.getPrefs().getBoolean(AUTORUN_GLOBAL, false));
      autoRunPane.getChildren().addAll(autoRunGlobalLabel, autoRunGlobal);

      Label autoRunProjectLabel = new Label("Run project autorun.groovy on init");
      autoRunProjectLabel.setTooltip(new Tooltip("Run autorun.groovy from the project dir (working dir) each time a session (re)starts"));
      autoRunProjectLabel.setPadding(new Insets(0, 20, 0, 20));
      autoRunProject = new CheckBox();
      autoRunProject.setSelected(gui.getPrefs().getBoolean(AUTORUN_PROJECT, false));
      autoRunPane.getChildren().addAll(autoRunProjectLabel, autoRunProject);

      grid.add(autoRunPane, 0,6, 4, 1);

      FlowPane executionPane = new FlowPane();
      Label addImportsLabel = new Label("Add imports when running Groovy snippets");
      addImportsLabel.setPadding(new Insets(0, 20, 0, 0));
      executionPane.getChildren().add(addImportsLabel);
      addImports = new CheckBox();
      addImports.setSelected(gui.getPrefs().getBoolean(ADD_IMPORTS, gui.getPrefs().getBoolean(ADD_IMPORTS, true)));
      executionPane.getChildren().add(addImports);
      grid.add(executionPane, 0, 7,4, 1);

      getDialogPane().setPrefSize(760, 350);
      getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
      setResizable(true);

      GuiUtils.addStyle(gui, this);

      setResultConverter(button -> button == ButtonType.OK ? createResult() : null);
    } catch (Throwable t) {
      ExceptionAlert.showAlert(t.getMessage(), t);
    }
  }

  private GlobalOptions createResult() {
    GlobalOptions result = new GlobalOptions();
    result.put(CONSOLE_MAX_LENGTH_PREF, intField.getValue());
    result.put(THEME, themes.getValue());
    result.put(DEFAULT_LOCALE, locals.getValue());
    result.put(USE_GRADLE_CLASSLOADER, useGradleFileClasspath.isSelected());
    result.put(GRADLE_HOME, gradleHome.getText());
    result.put(ADD_BUILDDIR_TO_CLASSPATH, addBuildDirToClasspath.isSelected());
    result.put(RESTART_SESSION_AFTER_GRADLE_RUN, restartSessionAfterGradleRun.isSelected());
    result.put(ENABLE_GIT, enableGit.isSelected());
    result.put(AUTORUN_GLOBAL, autoRunGlobal.isSelected());
    result.put(AUTORUN_PROJECT, autoRunProject.isSelected());
    result.put(ADD_IMPORTS, addImports.isSelected());
    return result;
  }


}
