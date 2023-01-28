/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.util.ArrayUtil;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.List;

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
    assertEquals("--- Actions ---", displayedLines[0]);

    // Action lines
    int displayedActionNum = displayedLines.length - 1;
    List<@NonNls String> actionIds = ActionManager.getInstance().getActionIdList("");
    assertEquals(displayedActionNum, actionIds.size());
  }

  public void testSearchByActionName() {
    configureByText("\n");
    typeText(commandToKeys("actionlist quickimpl"));

    String[] displayedLines = parseActionListOutput();
    for (int i = 0; i < displayedLines.length; i++) {
      String line = displayedLines[i];
      if (i == 0) {
        assertEquals("--- Actions ---", line);
      }
      else {
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
        assertEquals("--- Actions ---", line);
      }
      else {
        assertTrue(line.toLowerCase().contains("<m-s-"));
      }
    }
  }

  private String[] parseActionListOutput() {
    String output = ExOutputModel.getInstance(myFixture.getEditor()).getText();
    return output == null ? ArrayUtil.EMPTY_STRING_ARRAY : output.split("\n");
  }
}
