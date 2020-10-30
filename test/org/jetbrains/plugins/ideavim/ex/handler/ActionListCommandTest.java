/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.handler;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.util.ArrayUtil;
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
    assertNotNull(output);

    // Header line
    String[] displayedLines = output.split("\n");
    assertEquals("--- Actions ---", displayedLines[0]);

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
        assertEquals("--- Actions ---", line);
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
        assertEquals("--- Actions ---", line);
      }else {
        assertTrue(line.toLowerCase().contains("<m-s-"));
      }
    }
  }

  private String[] parseActionListOutput() {
    String output = ExOutputModel.getInstance(myFixture.getEditor()).getText();
    return output == null ? ArrayUtil.EMPTY_STRING_ARRAY : output.split("\n");
  }
}
