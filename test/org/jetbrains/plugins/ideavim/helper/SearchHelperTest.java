package org.jetbrains.plugins.ideavim.helper;

import com.intellij.openapi.util.Pair;
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
    Pair<Integer, Integer> range = new Pair(3, 4);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 1, 1, true));
  }

  @Test
  public void testFindWithinBlockNested() {
    String text = "<a><b>c</b></a>";
    Pair<Integer, Integer> range = new Pair(3, 11);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 1, 1, true));
  }

  @Test
  public void testFindWithinBlockNestedInner() {
    String text = "<a><b>c</b></a>";
    Pair<Integer, Integer> range = new Pair(6, 7);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 3, 1, true));
  }

  @Test
  public void testFindWholeBlockNestedInner() {
    String text = "<a><b>c</b></a>";
    Pair<Integer, Integer> range = new Pair(3, 11);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 3, 1, false));
  }

  @Test
  public void testFindWholeBlockNestedOuter() {
    String text = "<a><b>c</b></a>";
    Pair<Integer, Integer> range = new Pair(0, 15);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 0, 1, false));
  }

  @Test
  public void testFindInnerBlockNestedTwice() {
    String text = "<a><b>c</b></a>";
    Pair<Integer, Integer> range = new Pair(3, 11);

    assertEquals(range, SearchHelper.findTagBlockLocation(text, 6, 2, true));
  }
}
