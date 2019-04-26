package org.jetbrains.plugins.ideavim.ex;

import com.intellij.openapi.actionSystem.ActionManager;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.Arrays;

/**
 * @author Naoto Ikeno
 */
public class ActionListCommandTest extends VimTestCase {
  public void testListAllActions() {
    configureByText("\n");
    typeText(commandToKeys("actionlist"));

    String output = ExOutputModel.getInstance(myFixture.getEditor()).getText();
    assertNotNull(output);

    // Header line
    String[] displayedLines = output.split("\n");
    assertEquals(displayedLines[0], "--- Actions ---");

    // Action lines
    int displayedActionNum = displayedLines.length - 1;
    String[] actionIds = ActionManager.getInstance().getActionIds("");
    assertEquals(displayedActionNum, actionIds.length);
  }

  public void testSearchByActionName() {
    configureByText("\n");
    typeText(commandToKeys("actionlist quickimpl"));

    String[] displayedLines = parseActionListOutput();
    for (int i = 0; i < displayedLines.length; i++) {
      String line = displayedLines[i];
      if (i == 0) {
        assertEquals(line, "--- Actions ---");
      }else {
        assertTrue(line.toLowerCase().contains("quickimpl"));
      }
    }
  }

  public void testSearchByAssignedShortcutKey() {
    configureByText("\n");
    typeText(commandToKeys("actionlist <M-S-"));

    String[] displayedLines = parseActionListOutput();
    for (int i = 0; i < displayedLines.length; i++) {
      String line = displayedLines[i];
      if (i == 0) {
        assertEquals(line, "--- Actions ---");
      }else {
        assertTrue(line.toLowerCase().contains("<m-s-"));
      }
    }
  }

  private String[] parseActionListOutput() {
    String output = ExOutputModel.getInstance(myFixture.getEditor()).getText();
    return output == null ? new String[]{} : output.split("\n");
  }
}
