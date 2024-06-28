/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.SpellCheckerSeveritiesProvider;
import com.maddyhome.idea.vim.api.EngineEditorHelperKt;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.maddyhome.idea.vim.api.VimInjectorKt.*;

/**
 * Helper methods for searching text
 */
public class SearchHelper {

  /**
   * This counts all the words in the file.
   */
  public static @NotNull CountPosition countWords(@NotNull Editor editor) {
    int size = EditorHelperRt.getFileSize(editor);

    return countWords(editor, 0, size);
  }

  /**
   * This counts all the words in the file.
   */
  public static @NotNull CountPosition countWords(@NotNull Editor editor, int start, int end) {
    int offset = editor.getCaretModel().getOffset();
    final IjVimEditor vimEditor = new IjVimEditor(editor);

    int count = 1;
    int position = 0;
    int last = -1;
    int res = start;
    while (true) {
      res = injector.getSearchHelper().findNextWord(vimEditor, res, 1, true, false);
      if (res == start || res == 0 || res > end || res == last) {
        break;
      }

      count++;

      if (res == offset) {
        position = count;
      }
      else if (last < offset && res >= offset) {
        if (count == 2) {
          position = 1;
        }
        else {
          position = count - 1;
        }
      }

      last = res;
    }

    if (position == 0 && res == offset) {
      position = count;
    }

    return new CountPosition(count, position);
  }

  public static @NotNull List<Pair<TextRange, NumberType>> findNumbersInRange(final @NotNull Editor editor,
                                                                              @NotNull TextRange textRange,
                                                                              final boolean alpha,
                                                                              final boolean hex,
                                                                              final boolean octal) {
    List<Pair<TextRange, NumberType>> result = new ArrayList<>();


    for (int i = 0; i < textRange.size(); i++) {
      int startOffset = textRange.getStartOffsets()[i];
      final int end = textRange.getEndOffsets()[i];
      String text = EngineEditorHelperKt.getText(new IjVimEditor(editor), startOffset, end);
      String[] textChunks = text.split("\\n");
      int chunkStart = 0;
      for (String chunk : textChunks) {
        Pair<TextRange, NumberType> number = findNumberInText(chunk, 0, alpha, hex, octal);

        if (number != null) {
          result.add(new Pair<>(new TextRange(number.getFirst().getStartOffset() + startOffset + chunkStart,
                                              number.getFirst().getEndOffset() + startOffset + chunkStart),
                                number.getSecond()));
        }
        chunkStart += 1 + chunk.length();
      }
    }
    return result;
  }

  public static @Nullable Pair<TextRange, NumberType> findNumberUnderCursor(final @NotNull Editor editor,
                                                                            @NotNull Caret caret,
                                                                            final boolean alpha,
                                                                            final boolean hex,
                                                                            final boolean octal) {
    int lline = caret.getLogicalPosition().line;
    String text = new IjVimEditor(editor).getLineText(lline).toLowerCase();
    int startLineOffset = new IjVimEditor(editor).getLineStartOffset(lline);
    int posOnLine = caret.getOffset() - startLineOffset;

    Pair<TextRange, NumberType> numberTextRange = findNumberInText(text, posOnLine, alpha, hex, octal);

    if (numberTextRange == null) {
      return null;
    }
    return new Pair<>(new TextRange(numberTextRange.getFirst().getStartOffset() + startLineOffset,
                                    numberTextRange.getFirst().getEndOffset() + startLineOffset),
                      numberTextRange.getSecond());
  }

  /**
   * Search for number in given text from start position
   *
   * @param textInRange    - text to search in
   * @param startPosOnLine - start offset to search
   * @return - text range with number
   */
  public static @Nullable Pair<TextRange, NumberType> findNumberInText(final @NotNull String textInRange,
                                                                       int startPosOnLine,
                                                                       final boolean alpha,
                                                                       final boolean hex,
                                                                       final boolean octal) {

    if (logger.isDebugEnabled()) {
      logger.debug("text=" + textInRange);
    }

    int pos = startPosOnLine;
    int lineEndOffset = textInRange.length();

    while (true) {
      // Skip over current whitespace if any
      while (pos < lineEndOffset && !isNumberChar(textInRange.charAt(pos), alpha, hex, octal, true)) {
        pos++;
      }

      if (logger.isDebugEnabled()) logger.debug("pos=" + pos);
      if (pos >= lineEndOffset) {
        logger.debug("no number char on line");
        return null;
      }

      boolean isHexChar = "abcdefABCDEF".indexOf(textInRange.charAt(pos)) >= 0;

      if (hex) {
        // Ox and OX handling
        if (textInRange.charAt(pos) == '0' &&
            pos < lineEndOffset - 1 &&
            "xX".indexOf(textInRange.charAt(pos + 1)) >= 0) {
          pos += 2;
        }
        else if ("xX".indexOf(textInRange.charAt(pos)) >= 0 && pos > 0 && textInRange.charAt(pos - 1) == '0') {
          pos++;
        }

        logger.debug("checking hex");
        final Pair<Integer, Integer> range = findRange(textInRange, pos, false, true, false, false);
        int start = range.getFirst();
        int end = range.getSecond();

        // Ox and OX
        if (start >= 2 && textInRange.substring(start - 2, start).equalsIgnoreCase("0x")) {
          logger.debug("found hex");
          return new Pair<>(new TextRange(start - 2, end), NumberType.HEX);
        }

        if (!isHexChar || alpha) {
          break;
        }
        else {
          pos++;
        }
      }
      else {
        break;
      }
    }

    if (octal) {
      logger.debug("checking octal");
      final Pair<Integer, Integer> range = findRange(textInRange, pos, false, false, true, false);
      int start = range.getFirst();
      int end = range.getSecond();

      if (end - start == 1 && textInRange.charAt(start) == '0') {
        return new Pair<>(new TextRange(start, end), NumberType.DEC);
      }
      if (textInRange.charAt(start) == '0' &&
          end > start &&
          !(start > 0 && isNumberChar(textInRange.charAt(start - 1), false, false, false, true))) {
        logger.debug("found octal");
        return new Pair<>(new TextRange(start, end), NumberType.OCT);
      }
    }

    if (alpha) {
      if (logger.isDebugEnabled()) logger.debug("checking alpha for " + textInRange.charAt(pos));
      if (isNumberChar(textInRange.charAt(pos), true, false, false, false)) {
        if (logger.isDebugEnabled()) logger.debug("found alpha at " + pos);
        return new Pair<>(new TextRange(pos, pos + 1), NumberType.ALPHA);
      }
    }

    final Pair<Integer, Integer> range = findRange(textInRange, pos, false, false, false, true);
    int start = range.getFirst();
    int end = range.getSecond();
    if (start > 0 && textInRange.charAt(start - 1) == '-') {
      start--;
    }

    return new Pair<>(new TextRange(start, end), NumberType.DEC);
  }

  /**
   * Searches for digits block that matches parameters
   */
  private static @NotNull Pair<Integer, Integer> findRange(final @NotNull String text,
                                                           final int pos,
                                                           final boolean alpha,
                                                           final boolean hex,
                                                           final boolean octal,
                                                           final boolean decimal) {
    int end = pos;
    while (end < text.length() && isNumberChar(text.charAt(end), alpha, hex, octal, decimal || octal)) {
      end++;
    }
    int start = pos;
    while (start >= 0 && isNumberChar(text.charAt(start), alpha, hex, octal, decimal || octal)) {
      start--;
    }
    if (start < end &&
        (start == -1 ||
         0 <= start &&
         start < text.length() &&
         !isNumberChar(text.charAt(start), alpha, hex, octal, decimal || octal))) {
      start++;
    }
    if (octal) {
      for (int i = start; i < end; i++) {
        if (!isNumberChar(text.charAt(i), false, false, true, false)) return new Pair<>(0, 0);
      }
    }
    return new Pair<>(start, end);
  }

  private static boolean isNumberChar(char ch, boolean alpha, boolean hex, boolean octal, boolean decimal) {
    if (alpha && ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
      return true;
    }
    else if (octal && (ch >= '0' && ch <= '7')) {
      return true;
    }
    else if (hex && ((ch >= '0' && ch <= '9') || "abcdefABCDEF".indexOf(ch) >= 0)) {
      return true;
    }
    else {
      return decimal && (ch >= '0' && ch <= '9');
    }

  }

  /**
   * Find the word under the cursor or the next word to the right of the cursor on the current line.
   *
   * @param editor The editor to find the word in
   * @param caret  The caret to find word under
   * @return The text range of the found word or null if there is no word under/after the cursor on the line
   */
  public static @Nullable TextRange findWordUnderCursor(@NotNull Editor editor, @NotNull Caret caret) {
    final IjVimEditor vimEditor = new IjVimEditor(editor);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int stop = EngineEditorHelperKt.getLineEndOffset(vimEditor, caret.getLogicalPosition().line, true);

    int pos = caret.getOffset();
    // Technically the first condition is covered by the second one, but let it be
    if (chars.length() == 0 || chars.length() <= pos) return null;
    //if (pos == chars.length() - 1) return new TextRange(chars.length() - 1, chars.length());

    int start = pos;
    CharacterHelper.CharacterType[] types = new CharacterHelper.CharacterType[]{CharacterHelper.CharacterType.KEYWORD,
      CharacterHelper.CharacterType.PUNCTUATION};
    for (int i = 0; i < 2; i++) {
      start = pos;
      CharacterHelper.CharacterType type = CharacterHelper.charType(vimEditor, chars.charAt(start), false);
      if (type == types[i]) {
        // Search back for start of word
        while (start > 0 && CharacterHelper.charType(vimEditor, chars.charAt(start - 1), false) == types[i]) {
          start--;
        }
      }
      else {
        // Search forward for start of word
        while (start < stop && CharacterHelper.charType(vimEditor, chars.charAt(start), false) != types[i]) {
          start++;
        }
      }

      if (start != stop) {
        break;
      }
    }

    if (start == stop) {
      return null;
    }

    int end;
    // Special case 1 character words because 'findNextWordEnd' returns one to many chars
    if (start < stop &&
        (start >= chars.length() - 1 ||
         CharacterHelper.charType(vimEditor, chars.charAt(start + 1), false) != CharacterHelper.CharacterType.KEYWORD)) {
      end = start + 1;
    }
    else {
      end = injector.getSearchHelper().findNextWordEnd(vimEditor, start, 1, false, false) + 1;
    }

    return new TextRange(start, end);
  }

  public static int findMisspelledWords(@NotNull Editor editor,
                                       int startOffset,
                                       int endOffset,
                                       int skipCount,
                                       IntComparator offsetOrdering) {
    Project project = editor.getProject();
    if (project == null) {
      return -1;
    }

    IntSortedSet offsets = new IntRBTreeSet(offsetOrdering);
    DaemonCodeAnalyzerEx.processHighlights(editor.getDocument(), project, SpellCheckerSeveritiesProvider.TYPO,
                                           startOffset, endOffset, highlight -> {
        if (highlight.getSeverity() == SpellCheckerSeveritiesProvider.TYPO) {
          int offset = highlight.getStartOffset();
          if (offset >= startOffset && offset <= endOffset) {
            offsets.add(offset);
          }
        }
        return true;
      });

    if (offsets.isEmpty()) {
      return -1;
    }

    if (skipCount >= offsets.size()) {
      return offsets.lastInt();
    }
    else {
      IntIterator offsetIterator = offsets.iterator();
      skip(offsetIterator, skipCount);
      return offsetIterator.nextInt();
    }
  }

  private static void skip(IntIterator iterator, final int n) {
    if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
    int i = n;
    while (i-- != 0 && iterator.hasNext()) iterator.nextInt();
  }

  public static class CountPosition {
    public CountPosition(int count, int position) {
      this.count = count;
      this.position = position;
    }

    public int getCount() {
      return count;
    }

    public int getPosition() {
      return position;
    }

    private final int count;
    private final int position;
  }

  private static final Logger logger = Logger.getInstance(SearchHelper.class.getName());
}
