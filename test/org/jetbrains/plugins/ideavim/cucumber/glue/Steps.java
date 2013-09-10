package org.jetbrains.plugins.ideavim.cucumber.glue;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import javax.swing.*;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.jetbrains.plugins.ideavim.cucumber.glue.IdeaVimWorld.myFixture;

/**
 * User: zolotov
 * Date: 3/8/13
 */
public class Steps {
  @Given("^file with text$")
  public void file_with_text(String fileContent) {
    file_with_text("text.txt", fileContent);
  }

  @Given("^file \"([^\"]+)\" with text$")
  public void file_with_text(String fileName, String fileContent) {
    myFixture.configureByText(fileName, fileContent);
  }

  @When("^I type \"([^\"]*)\"$")
  public void type(String keysString) {
    final List<KeyStroke> keys = StringHelper.stringToKeys(keysString);
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        RunnableHelper.runWriteCommand(project, new Runnable() {
          @Override
          public void run() {
            for (KeyStroke key : keys) {
              keyHandler.handleKey(editor, key, dataContext);
            }
          }
        }, null, null);
      }
    }, ModalityState.any());
  }

  @Then("^text should be$")
  public void text_should_be(String expectedText) {
    myFixture.checkResult(expectedText);
  }

  @Then("^caret should be placed after \"([^\"]*)\" in (insert|visual|replace|command|normal) mode")
  public void caret_placed_after_in_given_mode(String textBeforeCaret, String expectedMode) {
    Editor editor = myFixture.getEditor();
    int position = getPositionBySignature(editor, textBeforeCaret, true);
    int actualOffset = editor.getCaretModel().getOffset();
    assertEquals("caret placed after '" + myFixture.getFile().getText().substring(0, actualOffset) + "':",
                 position, actualOffset);
    editorShouldBeInGivenMode(expectedMode);
  }

  @Then("^editor should be in (insert|visual|replace|command|normal) mode")
  public void editorShouldBeInGivenMode(String expectedMode) {
    expectedMode = "normal".equalsIgnoreCase(expectedMode) ? "command" : expectedMode;
    final CommandState.Mode mode = CommandState.getInstance(IdeaVimWorld.myFixture.getEditor()).getMode();
    assertEquals(CommandState.Mode.valueOf(expectedMode.toUpperCase()), mode);
  }

  private static int getPositionBySignature(Editor editor, String marker, boolean after) {
    final String text = editor.getDocument().getText();
    final int pos = text.indexOf(marker);
    assertTrue(pos >= 0);
    return after ? pos + marker.length() : pos;
  }
}

