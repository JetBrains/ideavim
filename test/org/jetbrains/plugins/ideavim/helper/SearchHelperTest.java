package org.jetbrains.plugins.ideavim.helper;

import com.intellij.openapi.util.Pair;
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
  public void testFindClosingTagSimple(){
    String text = "abc</de>fg";
    int position = 1;
    TextRange tagRange = new TextRange(3,8);
    final Pair<TextRange, String> closingTag = SearchHelper.findClosingTag(text, position);
    assertEquals(closingTag.getSecond(),"de");
   // assertEquals(closingTag.getFirst(),tagRange);  //TODO: realise equals in TextRange
    final TextRange closingTagRange = closingTag.getFirst();
    assertEquals(closingTagRange.getStartOffset(), tagRange.getStartOffset());
    assertEquals(closingTagRange.getEndOffset(), tagRange.getEndOffset());
  }

  @Test
  public void testFindClosingTagInsideClosing(){
    String text = "abc</de>fg";
    int position = 5;
    TextRange tagRange = new TextRange(3,8);
    final Pair<TextRange, String> closingTag = SearchHelper.findClosingTag(text, position);
    assertEquals(closingTag.getSecond(),"de");
    // assertEquals(closingTag.getFirst(),tagRange);  //TODO: realise equals in TextRange
    final TextRange closingTagRange = closingTag.getFirst();
    assertEquals(closingTagRange.getStartOffset(), tagRange.getStartOffset());
    assertEquals(closingTagRange.getEndOffset(), tagRange.getEndOffset());
  }

}
