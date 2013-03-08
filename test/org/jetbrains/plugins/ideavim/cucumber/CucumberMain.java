package org.jetbrains.plugins.ideavim.cucumber;

import com.intellij.idea.IdeaTestApplication;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.PlatformTestCase;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;

/**
 * User: zolotov
 * Date: 3/8/13
 */
public class CucumberMain {
  //private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";

  static {
    // Radar #5755208: Command line Java applications need a way to launch without a Dock icon.
    System.setProperty("apple.awt.UIElement", "true");
  }

  public static void main(final String[] args) {
    // Only in IntelliJ IDEA Ultimate Edition
    PlatformTestCase.initPlatformLangPrefix();
    // XXX: IntelliJ IDEA Community and Ultimate 12+
    //PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "PlatformLangXml");

    IdeaTestApplication.getInstance(null);
    final Ref<Throwable> errorRef = new Ref<Throwable>();
    final Ref<cucumber.runtime.Runtime> runtimeRef = new Ref<Runtime>();
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        try {
          final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          RuntimeOptions runtimeOptions = new RuntimeOptions(System.getProperties(), args);
          cucumber.runtime.Runtime runtime = new Runtime(new MultiLoader(classLoader), classLoader, runtimeOptions);
          runtimeRef.set(runtime);
          runtime.writeStepdefsJson();
          runtime.run();
        }
        catch (Throwable throwable) {
          errorRef.set(throwable);
          Logger.getInstance(CucumberMain.class).error(throwable);
        }
      }
    }, ApplicationManager.getApplication().getDefaultModalityState());
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    final Throwable throwable = errorRef.get();
    if (throwable != null) {
      throwable.printStackTrace();
    }
    System.err.println("Failed tests :");
    for (Throwable error : runtimeRef.get().getErrors()) {
      error.printStackTrace();
      System.err.println("=============================");
    }
    System.exit(throwable != null ? 1 : 0);
  }
}
