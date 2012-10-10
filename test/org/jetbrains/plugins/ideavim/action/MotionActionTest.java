package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

/**
 * @author vlan
 */
public class MotionActionTest extends VimTestCase {
  // VIM-198
  public void testVisualMotionInnerWordNewLineAtEOF() {
    final Editor editor = typeTextInFile("viw",
                                         "one tw<caret>o\n");
    final String selected = editor.getSelectionModel().getSelectedText();
    assertEquals("two", selected);
  }

  @NotNull
  private Editor typeTextInFile(@NotNull String input, @NotNull String fileContents) {
    myFixture.configureByText("a.java", fileContents);
    final Editor editor = myFixture.getEditor();
    final KeyHandler keyHandler = KeyHandler.getInstance();
    final EditorDataContext dataContext = new EditorDataContext(editor);
    final Project project = myFixture.getProject();
    final List<KeyStroke> keys = toKeyStrokes(input);
    RunnableHelper.runWriteCommand(project, new Runnable() {
      @Override
      public void run() {
        for (KeyStroke key : keys) {
          keyHandler.handleKey(editor, key, dataContext);
        }
      }
    }, null, null);
    return editor;
  }
}
