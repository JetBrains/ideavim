package org.jetbrains.plugins.ideavim.ex;

import com.intellij.openapi.actionSystem.ActionManager;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author Naoto Ikeno
 */
public class ActionListCommandTest extends VimTestCase {
  public void testListAllActions() {
    configureByText("\n");
    typeText(commandToKeys("actionlist"));

    String output = ExOutputModel.getInstance(myFixture.getEditor()).getText();
    assert output != null;

    // Header line
    String[] displayedLines = output.split("\n");
    assertEquals(displayedLines[0], "--- Actions ---");

    // Action lines
    int displayedActionNum = displayedLines.length - 1;
    String[] actionIds = ActionManager.getInstance().getActionIds("");
    assertEquals(displayedActionNum, actionIds.length);
  }

  // This test depends on default IntelliJ IDEA keymap
  public void testSearchByActionName() {
    configureByText("\n");
    typeText(commandToKeys("actionlist quickimpl"));
    assertExOutput("--- Actions ---\n" +
                   "QuickImplementations                               <M-S-I>");
  }

  // This test depends on default IntelliJ IDEA keymap
  public void testSearchByAssignedShortcutKey() {
    configureByText("\n");
    typeText(commandToKeys("actionlist <M-S-I>"));
    assertExOutput("--- Actions ---\n" +
                   "QuickImplementations                               <M-S-I>");
  }
}
