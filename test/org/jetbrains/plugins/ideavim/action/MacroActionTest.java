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

package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MacroActionTest extends VimTestCase {
  // |q|
  public void testRecordMacro() {
    final Editor editor = typeTextInFile(parseKeys("qa", "3l", "q"), "on<caret>e two three\n");
    final CommandState commandState = CommandState.getInstance(editor);
    assertFalse(commandState.isRecording());
    final RegisterGroup registerGroup = VimPlugin.getRegister();
    final Register register = registerGroup.getRegister('a');
    assertNotNull(register);
    assertEquals("3l", register.getText());
  }

  public void testRecordMacroDoesNotExpandMap() {
    configureByText("");
    enterCommand("imap pp hello");
    typeText(parseKeys("qa", "i", "pp<Esc>", "q"));
    final Register register = VimPlugin.getRegister().getRegister('a');
    assertNotNull(register);
    assertEquals("ipp<Esc>", StringHelper.toKeyNotation(register.getKeys()));
  }

  public void testRecordMacroWithDigraph() {
    typeTextInFile(parseKeys("qa", "i", "<C-K>OK<Esc>", "q"), "");
    final Register register = VimPlugin.getRegister().getRegister('a');
    assertNotNull(register);
    assertEquals("i<C-K>OK<Esc>", StringHelper.toKeyNotation(register.getKeys()));
  }
}
