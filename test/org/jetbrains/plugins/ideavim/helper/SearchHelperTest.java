package org.jetbrains.plugins.ideavim.helper;

import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.SearchHelper;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SearchHelperTest {
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

  @Test
  public void testFindWithinBlock() {
    String text = "<a>b</a>";
    TextRange range = new TextRange(3, 3);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 1, 1, false));
  }

  @Test
  public void testFindWithinBlockFromLastTag() {
    String text = "<a>b</a>";
    TextRange range = new TextRange(3, 3);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 7, 1, false));
  }

  @Test
  public void testFindWithinBlockNested() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(3, 10);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 1, 1, false));
  }

  @Test
  public void testFindWithinBlockNestedInner() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(6, 6);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 3, 1, false));
  }

  @Test
  public void testFindWholeBlockNestedInner() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(3, 10);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 3, 1, true));
  }

  @Test
  public void testFindWholeBlockNestedOuter() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(0, 14);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 0, 1, true));
  }

  @Test
  public void testFindInnerBlockNestedTwice() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(3, 10);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 6, 2, false));
  }

  @Test
  public void testFindOuterBlockFromWithinLastTag() {
    String text = "<a><b>c</b></a>";
    TextRange range = new TextRange(0, 14);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 13, 1, true));
  }

  @Test
  public void testFindInnerBlockWithEmptyTags() {
    String text = "<a><b/>c<b/></a>";
    TextRange range = new TextRange(3, 11);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 13, 1, false));
  }

  @Test
  public void testFindInnerBlockWithEmptyTagsFromWithinEmptyTag() {
    String text = "<a><b/>c<b/></a>";
    TextRange range = new TextRange(3, 11);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 4, 1, false));
  }

  @Test
  public void testNonXmlAngleBracket() {
    String text = "<script>if(x>1||x<0)x=0;</script>";
    TextRange range = new TextRange(8, 23);

    assertEquals(range, SearchHelper.findTagBlockRange(text, 1, 1, false));
  }
}