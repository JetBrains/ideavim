package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.group.RegisterGroup;
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
}
