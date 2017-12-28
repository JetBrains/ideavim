package org.jetbrains.plugins.ideavim.group;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.ArrayList;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;


public class ChangeGroupTest extends VimTestCase {
  private static final String BLOCK_INSERT_BASIC_TEST_DATA =
      "/*"                                   + "\n" +
      " * Something to fold away"            + "\n" +
      " * and some more text"                + "\n" +
      " */"                                  + "\n" +
                                               "\n" +
      "public class a {"                     + "\n" +
      "  final int <caret>FOO = 0;"          + "\n" +
      "  final int BAR = 1;"                 + "\n" +
      "  final int BAZ = 2;"                 + "\n" +
      "}";

  private static final String[] BLOCK_INSERT_BASIC_TEST_COMMAND = { "<C-v>", "j", "j", "<S-i>", "PREFIX_", "<Esc>" };

  // VIM-1110
  public void testBlockInsertCaretPositionWithoutFold() {
    assertCaretPositionStaysSameAfterCommandWithFold(JavaFileType.INSTANCE, BLOCK_INSERT_BASIC_TEST_DATA,
        new ArrayList<Integer>(), BLOCK_INSERT_BASIC_TEST_COMMAND);
  }

  // VIM-1110
  public void testBlockInsertCaretPositionWithFold() {
    List<Integer> collapse = new ArrayList<Integer>();
    collapse.add(0);

    assertCaretPositionStaysSameAfterCommandWithFold(JavaFileType.INSTANCE, BLOCK_INSERT_BASIC_TEST_DATA, collapse,
        BLOCK_INSERT_BASIC_TEST_COMMAND);
  }

  /**
   * Asserts that the caret position (offset) is not changed by the specified command.
   *
   * @param testFileType    the content on which the command should operate.
   * @param testFileContent the file type of the content string.
   * @param collapse        a list of indices specifying which folds to collapse.
   * @param command         the command to be executed/tested.
   */
  private void assertCaretPositionStaysSameAfterCommandWithFold(FileType testFileType, String testFileContent,
                                                                List<Integer> collapse, String[] command) {
    myFixture.configureByText(testFileType, testFileContent);

    final Editor editor = myFixture.getEditor();
    final FoldingModel foldingModel = editor.getFoldingModel();

    CodeFoldingManager.getInstance(myFixture.getProject()).updateFoldRegions(editor);

    final FoldRegion[] folds = foldingModel.getAllFoldRegions();
    final int offset = myFixture.getCaretOffset();

    for (int i = 0; i < folds.length; i++)
      foldRegionSetExpanded(foldingModel, folds[i], !collapse.contains(i));

    typeText(parseKeys(command));

    assertOffset(offset);
  }

  private static void foldRegionSetExpanded(FoldingModel model, final FoldRegion fold, final boolean expanded) {
    model.runBatchFoldingOperation(new Runnable() {
      @Override
      public void run() {
        fold.setExpanded(expanded);
      }
    });
  }
}
