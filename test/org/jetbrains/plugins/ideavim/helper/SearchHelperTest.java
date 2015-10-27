package org.jetbrains.plugins.ideavim.helper;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.SearchHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;
import org.junit.Test;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class SearchHelperTest extends VimTestCase {
  @Test
  public void testFindNextWord() throws Exception {
    String text = "first second";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 1, true, false);

    assertEquals(nextWordPosition, text.indexOf("second"));
  }

  @Test
  public void testFindSecondNextWord() throws Exception {
    String text = "first second third";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 2, true, false);

    assertEquals(nextWordPosition, text.indexOf("third"));
  }

  @Test
  public void testFindAfterLastWord() throws Exception {
    String text = "first second";
    int nextWordPosition = SearchHelper.findNextWord(text, 0, text.length(), 3, true, false);

    assertEquals(nextWordPosition, text.length());
  }

  @Test
  public void testFindPreviousWord() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("second"), text.length(), -1, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  @Test
  public void testFindSecondPreviousWord() {
    String text = "first second third";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("third"), text.length(), -2, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  @Test
  public void testFindBeforeFirstWord() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.indexOf("second"), text.length(), -3, true, false);

    assertEquals(previousWordPosition, text.indexOf("first"));
  }

  @Test
  public void testFindPreviousWordWhenCursorOutOfBound() {
    String text = "first second";
    int previousWordPosition = SearchHelper.findNextWord(text, text.length(), text.length(), -1, true, false);

    assertEquals(previousWordPosition, text.indexOf("second"));
  }

  /**
   * Tests for SearchHelper.inHtmlTagPosition
   */
  //There is no difference between open tag and non-pair
  @Test
  public void testInsideOpenTag() {
    String text = "blabla <tag>";
    assertTrue(SearchHelper.inHtmlTagPosition(text, false, 9));
  }

  @Test
  public void testOutsideOpenTag() {
    String text = "blabla <tag>";
    assertFalse(SearchHelper.inHtmlTagPosition(text, false, 3));
  }

  @Test
  public void testInsideCloseTag() {
    String text = "blabla </tag>";
    assertTrue(SearchHelper.inHtmlTagPosition(text, true, 10));
  }

  @Test
  public void testOutsideCloseTag() {
    String text = "blabla </tag>";
    assertFalse(SearchHelper.inHtmlTagPosition(text, true, 3));
  }

  /**
   * Tests for SearchHelper.findBlockTagRange
   */

  public void testFindBlockTagInnerRangeInside() {
    String text = "foo<tag>abc<caret>de</tag>bar";
    Editor e = typeTextInFile(parseKeys(""), text);
    TextRange textRange = SearchHelper.findBlockTagRange(e, false);
    assertEquals("abcde",
                 text.replaceAll("<caret>", "").substring(textRange.getStartOffset(), textRange.getEndOffset() + 1));
  }

  public void testFindBlockTagRangeOutside() {
    String text = "fo<caret>o<tag>abcde</tag>bar";
    Editor e = typeTextInFile(parseKeys(""), text);
    TextRange textRange = SearchHelper.findBlockTagRange(e, false);
    assertNull(textRange);
  }
  public void testFindBlockTagOuterRangeInside() {
    String text = "foo<tag>abc<caret>de</tag>bar";
    Editor e = typeTextInFile(parseKeys(""), text);
    TextRange textRange = SearchHelper.findBlockTagRange(e, true);
    assertEquals("<tag>abcde</tag>",
                 text.replaceAll("<caret>", "").substring(textRange.getStartOffset(), textRange.getEndOffset() + 1));
  }

}
