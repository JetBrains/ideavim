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

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

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

  // VIM-855 |/|
  public void testCharacterClassRegression() {
    final int pos = search("[^c]b",
                           "<caret>bb\n");
    assertEquals(0, pos);
  }

  // VIM-855 |/|
  public void testCharacterClassRegressionCaseInsensitive() {
    final int pos = search("\\c[ABC]D",
                           "<caret>dd\n");
    assertEquals(-1, pos);
  }

  // VIM-856 |/|
  public void testNegativeLookbehindRegression() {
    final int pos = search("a\\@<!b",
                           "<caret>ab\n");
    assertEquals(-1, pos);
  }

  public void testSmartCaseSearchCaseInsensitive() {
    setIgnoreCaseAndSmartCase();
    final int pos = search("tostring",
                           "obj.toString();\n");
    assertEquals(4, pos);
  }

  public void testSmartCaseSearchCaseSensitive() {
    setIgnoreCaseAndSmartCase();
    final int pos = search("toString",
                           "obj.tostring();\nobj.toString();\n");
    assertEquals(20, pos);
  }

  // |/|
  public void testSearchMotion() {
    typeTextInFile(parseKeys("/", "two", "<Enter>"),
                   "<caret>one two\n");
    assertOffset(4);
  }

  // |i_CTRL-K|
  public void testSearchDigraph() {
    typeTextInFile(parseKeys("/", "<C-K>O:", "<Enter>"),
                   "<caret>Hello, Österreich!\n");
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

  private void setIgnoreCaseAndSmartCase() {
    final Options options = Options.getInstance();
    options.resetAllOptions();
    final Option ignoreCaseOption = options.getOption("ignorecase");
    final Option smartCaseOption = options.getOption("smartcase");
    assertInstanceOf(ignoreCaseOption, ToggleOption.class);
    assertInstanceOf(smartCaseOption, ToggleOption.class);
    final ToggleOption ignoreCase = (ToggleOption)ignoreCaseOption;
    final ToggleOption smartCase = (ToggleOption)smartCaseOption;
    ignoreCase.set();
    smartCase.set();
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
