package org.jetbrains.plugins.ideavim;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author vlan
 */
public abstract class VimTestCase extends UsefulTestCase {
  private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";
  protected CodeInsightTestFixture myFixture;

  public VimTestCase() {
    // Only in IntelliJ IDEA Ultimate Edition
    PlatformTestCase.initPlatformLangPrefix();
    // XXX: IntelliJ IDEA Community and Ultimate 12+
    //PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "PlatformLangXml");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
    final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
    final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor);
    final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
                                                                                    new LightTempDirTestFixtureImpl(true));
    myFixture.setUp();
    myFixture.setTestDataPath(getTestDataPath());
    KeyHandler.getInstance().fullReset(myFixture.getEditor());
    Options.getInstance().resetAllOptions();
  }

  protected String getTestDataPath() {
    return PathManager.getHomePath() + "/community/plugins/ideavim/testData";
  }

  @Override
  protected void tearDown() throws Exception {
    myFixture.tearDown();
    myFixture = null;
    ExEntryPanel.getInstance().deactivate();
    super.tearDown();
  }

  @NotNull
  protected Editor typeTextInFile(@NotNull final List<KeyStroke> keys, @NotNull String fileContents) {
    myFixture.configureByText("a.txt", fileContents);
    return typeText(keys);
  }

  @NotNull
  protected Editor typeText(@NotNull final List<KeyStroke> keys) {
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        for (KeyStroke key : keys) {
          final ExEntryPanel exEntryPanel = ExEntryPanel.getInstance();
          if (exEntryPanel.isActive()) {
            exEntryPanel.handleKey(key);
          }
          else {
            keyHandler.handleKey(editor, key, dataContext);
          }
        }
      }
    }, null, null);
    return editor;
  }

  @NotNull
  protected Editor runExCommand(@NotNull final String command) {
    final Editor editor = myFixture.getEditor();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    final CommandParser commandParser = CommandParser.getInstance();
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        try {
          commandParser.processCommand(editor, dataContext, command, 1);
        }
        catch (ExException e) {
          throw new RuntimeException(e);
        }
      }
    }, null, null);
    return editor;
  }

  public void assertOffset(int expectedOffset) {
    final int offset = myFixture.getEditor().getCaretModel().getOffset();
    assertEquals(expectedOffset, offset);
  }

  public void assertMode(@NotNull CommandState.Mode expectedMode) {
    final CommandState.Mode mode = CommandState.getInstance(myFixture.getEditor()).getMode();
    assertEquals(expectedMode, mode);
  }

  public void assertSelection(@NotNull String expected) {
    final String selected = myFixture.getEditor().getSelectionModel().getSelectedText();
    assertEquals(expected, selected);
  }

  public void assertPluginError(boolean isError) {
    final VimPlugin plugin = VimPlugin.getInstance();
    assertEquals(isError, plugin.isError());
  }
}
