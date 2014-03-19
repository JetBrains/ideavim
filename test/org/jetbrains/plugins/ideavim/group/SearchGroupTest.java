package org.jetbrains.plugins.ideavim.group;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.group.SearchGroup;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.option.Option;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.option.ToggleOption;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class SearchGroupTest extends VimTestCase {
  // |/|
  public void testOneLetter() {
    final int pos = search("w",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(5, pos);
  }

  public void testEOL() {
    final int pos = search("$",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(3, pos);
  }

  // VIM-146 |/|
  public void testEOLWithHighlightSearch() {
    setHighlightSearch();
    final int pos = search("$",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(3, pos);
  }

  public void testAndWithoutBranches() {
    final int pos = search("\\&",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(1, pos);
  }

  // VIM-226 |/|
  public void testAndWithoutBranchesWithHighlightSearch() {
    setHighlightSearch();
    final int pos = search("\\&",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(1, pos);
  }

  // VIM-528 |/|
  public void testSearchNotFound() {
    final int pos = search("(one)",
                           "<caret>one\n" +
                           "two\n");
    assertEquals(-1, pos);
  }
  
  // VIM-528 |/|
  public void testSearchGrouping() {
    final int pos = search("\\(one\\)",
                           "<caret>01234one\n" +
                           "two\n");
    assertEquals(5, pos);
  }


  // |/|
  public void testSearchMotion() {
    final List<KeyStroke> keys = stringToKeys("/two");
    keys.add(KeyStroke.getKeyStroke("ENTER"));
    typeTextInFile(keys, "<caret>one two\n");
    assertOffset(4);
  }

  // |i_CTRL-K|
  public void testSearchDigraph() {
    final List<KeyStroke> keys = stringToKeys("/");
    keys.add(KeyStroke.getKeyStroke("control K"));
    keys.addAll(stringToKeys("O:"));
    keys.add(KeyStroke.getKeyStroke("ENTER"));
    typeTextInFile(keys, "<caret>Hello, Österreich!\n");
    assertOffset(7);
  }
  
  private void setHighlightSearch() {
    final Options options = Options.getInstance();
    options.resetAllOptions();
    final Option option = options.getOption("hlsearch");
    assertInstanceOf(option, ToggleOption.class);
    final ToggleOption highlightSearch = (ToggleOption)option;
    highlightSearch.set();
  }

  private int search(final String pattern, String input) {
    myFixture.configureByText("a.java", input);
    final Editor editor = myFixture.getEditor();
    final Project project = myFixture.getProject();
    final SearchGroup searchGroup = VimPlugin.getSearch();
    final Ref<Integer> ref = Ref.create(-1);
    RunnableHelper.runReadCommand(project, new Runnable() {
      @Override
      public void run() {
        final int n = searchGroup.search(editor, pattern, 1, Command.FLAG_SEARCH_FWD, false);
        ref.set(n);
      }
    }, null, null);
    return ref.get();
  }
}
