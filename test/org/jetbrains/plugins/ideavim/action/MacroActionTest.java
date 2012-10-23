package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.RegisterGroup;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

/**
 * @author vlan
 */
public class MacroActionTest extends VimTestCase {
  public void testRecordMacro() {
    final List<KeyStroke> keyStrokes = StringHelper.stringToKeys("qa3lq");
    final Editor editor = typeTextInFile(keyStrokes, "on<caret>e two three\n");
    final CommandState commandState = CommandState.getInstance(editor);
    assertFalse(commandState.isRecording());
    final RegisterGroup registerGroup = CommandGroups.getInstance().getRegister();
    final Register register = registerGroup.getRegister('a');
    assertEquals("3l", register.getText());
  }
}
