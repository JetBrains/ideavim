/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for searching text
 */
public class SearchHelper {
  public static boolean anyNonWhitespace(@NotNull Editor editor, int offset, int dir) {
    int start;
    int end;
    if (dir > 0) {
      start = offset + 1;
      end = EditorHelper.getLineEndForOffset(editor, offset);
    }
    else {
      start = EditorHelper.getLineStartForOffset(editor, offset);
      end = offset - 1;
    }

    CharSequence chars = editor.getDocument().getCharsSequence();
    for (int i = start; i <= end; i++) {
      if (!Character.isWhitespace(chars.charAt(i))) {
        return true;
      }
    }

    return false;
  }

  public static int findSection(@NotNull Editor editor, @NotNull Caret caret, char type, int dir, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int line = caret.getLogicalPosition().line + dir;
    int maxline = EditorHelper.getLineCount(editor);
    int res = -1;

    while (line > 0 && line < maxline && count > 0) {
      int offset = EditorHelper.getLineStartOffset(editor, line);
      char ch = chars.charAt(offset);
      if (ch == type || ch == '\u000C') {
        res = offset;
        count--;
      }

      line += dir;
    }

    if (res == -1) {
      res = dir < 0 ? 0 : chars.length() - 1;
    }

    return res;
  }

  public static int findUnmatchedBlock(@NotNull Editor editor, @NotNull Caret caret, char type, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int loc = blockChars.indexOf(type);
    // What direction should we go now (-1 is backward, 1 is forward)
    int dir = loc % 2 == 0 ? -1 : 1;
    // Which character did we find and which should we now search for
    char match = blockChars.charAt(loc);
    char found = blockChars.charAt(loc - dir);

    return findBlockLocation(chars, found, match, dir, pos, count, false);
  }

  @Nullable
  public static TextRange findBlockRange(@NotNull Editor editor, @NotNull Caret caret, char type, int count,
                                         boolean isOuter) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int start = caret.getSelectionStart();
    int end = caret.getSelectionEnd();

    int loc = blockChars.indexOf(type);
    char close = blockChars.charAt(loc + 1);

    boolean rangeSelection = end - start > 1;
    if (rangeSelection && start == 0) // early return not only for optimization
      return null;                    // but also not to break the interval semantic on this edge case (see below)

    /* In case of successive inner selection. We want to break out of
     * the block delimiter of the current inner selection.
     * In other terms, for the rest of the algorithm, a previous inner selection of a block
     * if equivalent to an outer one. */
    if (!isOuter
        && (start - 1) >= 0 && type == chars.charAt(start - 1)
        && end < chars.length() && close == chars.charAt(end)) {
      start = start - 1;
      pos = start;
      rangeSelection = true;
    }

    /* when one char is selected, we want to find the enclosing block of (start,end]
     * although when a range of characters is selected, we want the enclosing block of [start, end]
     * shifting the position allow to express which kind of interval we work on */
    if (rangeSelection)
      pos = Math.max(0, start - 1);

    boolean initialPosIsInString = checkInString(chars, pos, true);

    int bstart = -1;
    int bend = -1;

    boolean startPosInStringFound = false;

    if (initialPosIsInString) {
      TextRange quoteRange = findBlockQuoteInLineRange(editor, caret, '"', false);
      if (quoteRange != null) {
        int startOffset = quoteRange.getStartOffset();
        int endOffset = quoteRange.getEndOffset();
        CharSequence subSequence = chars.subSequence(startOffset, endOffset);
        int inQuotePos = pos - startOffset;
        int inQuoteStart = findBlockLocation(subSequence, close, type, -1, inQuotePos, count, false);
        if (inQuoteStart != -1) {
          startPosInStringFound = true;
          int inQuoteEnd = findBlockLocation(subSequence, type, close, 1, inQuoteStart, 1, false);
          if (inQuoteEnd != -1) {
            bstart = inQuoteStart + startOffset;
            bend = inQuoteEnd + startOffset;
          }
        }
      }
    }

    if (!startPosInStringFound) {
      bstart = findBlockLocation(chars, close, type, -1, pos, count, false);
      if (bstart != -1) {
        bend = findBlockLocation(chars, type, close, 1, bstart, 1, false);
      }
    }

    if (bstart == -1 || bend == -1) {
      return null;
    }

    if (!isOuter) {
      bstart++;
      if (chars.charAt(bstart) == '\n') {
        bstart++;
      }

      int o = EditorHelper.getLineStartForOffset(editor, bend);
      boolean allWhite = true;
      for (int i = o; i < bend; i++) {
        if (!Character.isWhitespace(chars.charAt(i))) {
          allWhite = false;
          break;
        }
      }

      if (allWhite) {
        bend = o - 2;
      }
      else {
        bend--;
      }
    }

    // End offset exclusive
    return new TextRange(bstart, bend + 1);
  }

  private static int findMatchingBlockCommentPair(@NotNull PsiComment comment, int pos, @Nullable String prefix,
                                                  @Nullable String suffix) {
    if (prefix != null && suffix != null) {
      final String commentText = comment.getText();
      if (commentText.startsWith(prefix) && commentText.endsWith(suffix)) {
        final int endOffset = comment.getTextOffset() + comment.getTextLength();
        if (pos < comment.getTextOffset() + prefix.length()) {
          return endOffset;
        }
        else if (pos >= endOffset - suffix.length()) {
          return comment.getTextOffset();
        }
      }
    }
    return -1;
  }

  private static int findMatchingBlockCommentPair(@NotNull PsiElement element, int pos) {
    final Language language = element.getLanguage();
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
    final PsiComment comment = PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
    if (comment != null) {
      final int ret = findMatchingBlockCommentPair(comment, pos, commenter.getBlockCommentPrefix(),
                                                   commenter.getBlockCommentSuffix());
      if (ret >= 0) {
        return ret;
      }
      if (commenter instanceof CodeDocumentationAwareCommenter) {
        final CodeDocumentationAwareCommenter docCommenter = (CodeDocumentationAwareCommenter)commenter;
        return findMatchingBlockCommentPair(comment, pos, docCommenter.getDocumentationCommentPrefix(),
                                            docCommenter.getDocumentationCommentSuffix());
      }
    }
    return -1;
  }

  /**
   * This looks on the current line, starting at the cursor position for one of {, }, (, ), [, or ]. It then searches
   * forward or backward, as appropriate for the associated match pair. String in double quotes are skipped over.
   * Single characters in single quotes are skipped too.
   *
   * @param editor The editor to search in
   * @return The offset within the editor of the found character or -1 if no match was found or none of the characters
   * were found on the remainder of the current line.
   */
  public static int findMatchingPairOnCurrentLine(@NotNull Editor editor, @NotNull Caret caret) {
    int pos = caret.getOffset();

    final int commentPos = findMatchingComment(editor, pos);
    if (commentPos >= 0) {
      return commentPos;
    }

    int line = caret.getLogicalPosition().line;
    int end = EditorHelper.getLineEndOffset(editor, line, true);

    // To handle the case where visual mode allows the user to go past the end of the line,
    // which will prevent loc from finding a pairable character below
    if(pos > 0 && pos == end) {
      pos = end - 1;
    }

    CharSequence chars = editor.getDocument().getCharsSequence();
    int loc = -1;
    // Search the remainder of the current line for one of the candidate characters
    while (pos < end) {
      loc = getPairChars().indexOf(chars.charAt(pos));
      if (loc >= 0) {
        break;
      }

      pos++;
    }

    int res = -1;
    // If we found one ...
    if (loc >= 0) {
      // What direction should we go now (-1 is backward, 1 is forward)
      int dir = loc % 2 == 0 ? 1 : -1;
      // Which character did we find and which should we now search for
      char found = getPairChars().charAt(loc);
      char match = getPairChars().charAt(loc + dir);
      res = findBlockLocation(chars, found, match, dir, pos, 1, true);
    }

    return res;
  }

  /**
   * If on the start/end of a block comment, jump to the matching of that comment, or vice versa.
   */
  private static int findMatchingComment(@NotNull Editor editor, int pos) {
    final PsiFile psiFile = PsiHelper.getFile(editor);
    if (psiFile != null) {
      final PsiElement element = psiFile.findElementAt(pos);
      if (element != null) {
        return findMatchingBlockCommentPair(element, pos);
      }
    }
    return -1;
  }

  private static int findBlockLocation(@NotNull CharSequence chars,
                                       char found,
                                       char match,
                                       int dir,
                                       int pos,
                                       int cnt,
                                       boolean allowInString) {
    int res = -1;
    final int inCheckPos = dir < 0 && pos > 0 ? pos - 1 : pos;
    boolean inString = checkInString(chars, inCheckPos, true);
    boolean initialInString = inString;
    boolean inChar = checkInString(chars, inCheckPos, false);
    boolean initial = true;
    int stack = 0;
    // Search to start or end of file, as appropriate
    while (pos >= 0 && pos < chars.length() && cnt > 0) {
      // If we found a match and we're not in a string...
      if (chars.charAt(pos) == match && (allowInString ? initialInString == inString : !inString) && !inChar) {
        // We found our match
        if (stack == 0) {
          res = pos;
          cnt--;
        }
        // Found the character but it "closes" a different pair
        else {
          stack--;
        }
      }
      // End of line - mark not in a string any more (in case we started in the middle of one
      else if (chars.charAt(pos) == '\n') {
        inString = false;
        inChar = false;
      }
      else if (!initial) {
        // We found another character like our original - belongs to another pair
        if (!inString && !inChar && chars.charAt(pos) == found) {
          stack++;
        }
        // We found the start/end of a string
        else if (!inChar && isQuoteWithoutEscape(chars, pos, '"')) {
          inString = !inString;
        }
        else if (!inString && isQuoteWithoutEscape(chars, pos, '\'')) {
          inChar = !inChar;
        }
      }

      pos += dir;
      initial = false;
    }

    return res;
  }

  /**
   * Returns true if [quote] is at this [pos] and it's not escaped (like \")
   */
  private static boolean isQuoteWithoutEscape(@NotNull CharSequence chars, int pos, char quote) {
    if (chars.charAt(pos) != quote) return false;

    int backslashCounter = 0;
    while (pos-- > 0) {
      if (chars.charAt(pos) == '\\') {
        backslashCounter++;
      }
      else {
        break;
      }
    }
    return backslashCounter % 2 == 0;
  }

  private enum Direction {
    BACK(-1), FORWARD(1);

    private final int value;

    Direction(int i) {
      value = i;
    }

    private int toInt() {
      return value;
    }
  }

  private static int findNextQuoteInLine(@NotNull CharSequence chars, int pos, char quote) {
    return findQuoteInLine(chars, pos, quote, Direction.FORWARD);
  }

  private static int findPreviousQuoteInLine(@NotNull CharSequence chars, int pos, char quote) {
    return findQuoteInLine(chars, pos, quote, Direction.BACK);
  }

  private static int findFirstQuoteInLine(@NotNull Editor editor, int pos, char quote) {
    final int start = EditorHelper.getLineStartForOffset(editor, pos);
    return findNextQuoteInLine(editor.getDocument().getCharsSequence(), start, quote);
  }

  private static int findQuoteInLine(@NotNull CharSequence chars, int pos, char quote, @NotNull Direction direction) {
    return findCharacterPosition(chars, pos, quote, true, false, direction);
  }

  private static int countCharactersInLine(@NotNull CharSequence chars, int pos, char c) {
    int cnt = 0;
    while (pos > 0 && (chars.charAt(pos + Direction.BACK.toInt()) != '\n')) {
      pos = findCharacterPosition(chars, pos + Direction.BACK.toInt(), c, false, true, Direction.BACK);
      if (pos != -1) {
        cnt++;
      }
    }
    return cnt;
  }

  private static int findCharacterPosition(@NotNull CharSequence chars, int pos, final char c, boolean currentLineOnly,
                                           boolean searchEscaped, @NotNull Direction direction) {
    while (pos >= 0 && pos < chars.length() && (!currentLineOnly || chars.charAt(pos) != '\n')) {
      if (chars.charAt(pos) == c && (pos == 0 || searchEscaped || chars.charAt(pos - 1) != '\\')) {
        return pos;
      }
      pos += direction.toInt();
    }
    return -1;
  }

  /** returns new position which ignore whitespaces at beginning of the line*/
  private static int ignoreWhitespaceAtLineStart(CharSequence seq, int lineStart, int pos) {
    if (seq.subSequence(lineStart, pos).chars().allMatch(Character::isWhitespace)) {
      while (pos < seq.length() && seq.charAt(pos) != '\n' && Character.isWhitespace(seq.charAt(pos))) {
        pos++;
      }
    }
    return pos;
  }


  @Nullable
  public static TextRange findBlockTagRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    final int position = caret.getOffset();
    final CharSequence sequence = editor.getDocument().getCharsSequence();

    final int selectionStart = caret.getSelectionStart();
    final int selectionEnd = caret.getSelectionEnd();

    final boolean isRangeSelection = selectionEnd - selectionStart > 1;

    int searchStartPosition;
    if (!isRangeSelection) {
      final int line = caret.getLogicalPosition().line;
      final int lineBegin = editor.getDocument().getLineStartOffset(line);
      searchStartPosition = ignoreWhitespaceAtLineStart(sequence, lineBegin, position);
    } else {
      searchStartPosition = selectionEnd;
    }

    if (isInHTMLTag(sequence, searchStartPosition, false)) {
      // caret is inside opening tag. Move to closing '>'.
      while (searchStartPosition < sequence.length() && sequence.charAt(searchStartPosition) != '>') {
        searchStartPosition ++;
      }
    }
    else if (isInHTMLTag(sequence, searchStartPosition, true)) {
      // caret is inside closing tag. Move to starting '<'.
      while (searchStartPosition > 0 && sequence.charAt(searchStartPosition) != '<') {
        searchStartPosition --;
      }
    }

    while (true) {
      final Pair<TextRange, String> closingTag = findUnmatchedClosingTag(sequence, searchStartPosition, count);
      if (closingTag == null) {
        return null;
      }
      final TextRange closingTagTextRange = closingTag.getFirst();
      final String tagName = closingTag.getSecond();

      TextRange openingTag = findUnmatchedOpeningTag(sequence, closingTagTextRange.getStartOffset(), tagName);
      if (openingTag == null) {
        return null;
      }

      if (isRangeSelection && openingTag.getEndOffset() - 1 >= selectionStart) {
        // If there was already some text selected and the new selection would not extend further, we try again
        searchStartPosition = closingTagTextRange.getEndOffset();
        count = 1;
        continue;
      }

      int selectionEndWithoutNewline = selectionEnd;
      while (selectionEndWithoutNewline < sequence.length() && sequence.charAt(selectionEndWithoutNewline) == '\n') {
        selectionEndWithoutNewline ++;
      }

      if (closingTagTextRange.getStartOffset() == selectionEndWithoutNewline && openingTag.getEndOffset() == selectionStart) {
        // Special case: if the inner tag is already selected we should like isOuter is active
        // Note that we need to ignore newlines, because their selection is lost between multiple "it" invocations
        isOuter = true;
      } else
      if (openingTag.getEndOffset() == closingTagTextRange.getStartOffset() && selectionStart == openingTag.getEndOffset()) {
        // Special case: for an empty tag pair (e.g. <a></a>) the whole tag is selected if the caret is in the middle.
        isOuter = true;
      }

      if (isOuter) {
        return new TextRange(openingTag.getStartOffset(), closingTagTextRange.getEndOffset());
      } else {
        return new TextRange(openingTag.getEndOffset(), closingTagTextRange.getStartOffset());
      }
    }
  }

  /**
   * Returns true if there is a html at the given position. Ignores tags with a trailing slash like <aaa/>.
   */
  private static boolean isInHTMLTag(@NotNull final CharSequence sequence, final int position, final boolean isEndtag) {
    int openingBracket = -1;
    for (int i = position; i >= 0 && i < sequence.length(); i--) {
      if (sequence.charAt(i) == '<') {
        openingBracket = i;
        break;
      }
      if (sequence.charAt(i) == '>' && i != position) {
        return false;
      }
    }

    if (openingBracket == -1) {
      return false;
    }

    boolean hasSlashAfterOpening = openingBracket + 1 < sequence.length() && sequence.charAt(openingBracket + 1) == '/';
    if ((isEndtag && !hasSlashAfterOpening) || (!isEndtag && hasSlashAfterOpening)) {
      return false;
    }

    int closingBracket = -1;
    for (int i = openingBracket; i < sequence.length(); i++) {
      if (sequence.charAt(i) == '>') {
        closingBracket = i;
        break;
      }
    }

    return closingBracket != -1 && sequence.charAt(closingBracket - 1) != '/';
  }

  @Nullable
  private static Pair<TextRange,String> findUnmatchedClosingTag(@NotNull final CharSequence sequence, final int position, int count) {
    // The tag name may contain any characters except slashes, whitespace and '>'
    final String tagNamePattern = "([^/\\s>]+)";
    // An opening tag consists of '<' followed by a tag name, optionally some additional text after whitespace and a '>'
    final String openingTagPattern = String.format("<%s(?:\\s[^>]*)?>", tagNamePattern);
    final String closingTagPattern = String.format("</%s>", tagNamePattern);
    final Pattern tagPattern = Pattern.compile(String.format("(?:%s)|(?:%s)", openingTagPattern, closingTagPattern));
    final Matcher matcher = tagPattern.matcher(sequence.subSequence(position, sequence.length()));

    final Stack<String> openTags = new Stack<>();

    while (matcher.find()) {
      boolean isClosingTag = matcher.group(1) == null;
      if (isClosingTag) {
        final String tagName = matcher.group(2);
        // Ignore unmatched open tags. Either the file is malformed or it might be a tag like <br> that does not need to be closed.
        while (!openTags.isEmpty() && !openTags.peek().equalsIgnoreCase(tagName)) {
          openTags.pop();
        }
        if (openTags.isEmpty()) {
          if (count <= 1) {
            return Pair.create(new TextRange(position + matcher.start(), position + matcher.end()), tagName);
          } else {
            count--;
          }
        } else {
          openTags.pop();
        }
      } else {
        final String tagName = matcher.group(1);
        openTags.push(tagName);
      }
    }
    return null;
  }

  @Nullable
  private static TextRange findUnmatchedOpeningTag(@NotNull CharSequence sequence, int position, @NotNull String tagName) {
    final String quotedTagName = Pattern.quote(tagName);
    final String patternString = "(</%s>)"  // match closing tags
            + "|(<%s"      // or opening tags starting with tagName
            + "(\\s([^>]*"  // After at least one whitespace there might be additional text in the tag. E.g. <html lang="en">
            + "[^/])?)?>)";  // Slash is not allowed as last character (this would be a self closing tag).
    final Pattern tagPattern = Pattern.compile(String.format(patternString, quotedTagName, quotedTagName), Pattern.CASE_INSENSITIVE);
    final Matcher matcher = tagPattern.matcher(sequence.subSequence(0, position+1));
    final Stack<TextRange> openTags = new Stack<>();

    while (matcher.find()) {
      final TextRange match = new TextRange(matcher.start(), matcher.end());
      if (sequence.charAt(matcher.start() + 1) == '/') {
        if (!openTags.isEmpty()) {
          openTags.pop();
        }
      }
      else {
        openTags.push(match);
      }
    }

    if (openTags.isEmpty()) {
      return null;
    } else {
      return openTags.pop();
    }
  }


  @Nullable
  public static TextRange findBlockQuoteInLineRange(@NotNull Editor editor, @NotNull Caret caret, char quote,
                                                    boolean isOuter) {
    final CharSequence chars = editor.getDocument().getCharsSequence();
    final int pos = caret.getOffset();
    if (pos >= chars.length() || chars.charAt(pos) == '\n') {
      return null;
    }

    int start = findPreviousQuoteInLine(chars, pos, quote);
    if (start == -1) {
      start = findFirstQuoteInLine(editor, pos, quote);
      if (start == -1) {
        return null;
      }
    }
    final int current = Math.max(start, pos);
    int end = current;

    if (chars.charAt(pos) == quote && current == pos) {
      final int quotes = countCharactersInLine(chars, pos, quote) + 1;

      if (quotes % 2 == 0) {
        start = findPreviousQuoteInLine(chars, current - 1, quote);
      }
      else {
        end = findNextQuoteInLine(chars, current + 1, quote);
      }
    }
    else {
      end = findNextQuoteInLine(chars, current + 1, quote);
    }

    if (end == -1) {
      return null;
    }
    if (!isOuter) {
      start++;
      end--;
    }

    // End offset exclusive
    return new TextRange(start, end + 1);
  }

  private static boolean checkInString(@NotNull CharSequence chars, int pos, boolean str) {
    if (chars.length() == 0) return false;
    int offset = pos;
    while (offset > 0 && chars.charAt(offset) != '\n') {
      offset--;
    }

    boolean inString = false;
    boolean inChar = false;
    for (int i = offset; i <= pos; i++) {
      if (!inChar && isQuoteWithoutEscape(chars, i, '"')) {
        inString = !inString;
      }
      else if (!inString && isQuoteWithoutEscape(chars, i, '\'')) {
        inChar = !inChar;
      }
    }

    return str ? inString : inChar;
  }

  public static int findNextCamelStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int size = EditorHelper.getFileSize(editor);

    int found = 0;
    int step = count >= 0 ? 1 : -1;
    if (pos < 0 || pos >= size) {
      return pos;
    }

    int res = pos;
    pos += step;
    while (pos >= 0 && pos < size && found < Math.abs(count)) {
      if (Character.isUpperCase(chars.charAt(pos))) {
        if ((pos == 0 || !Character.isUpperCase(chars.charAt(pos - 1))) ||
            (pos == size - 1 || Character.isLowerCase(chars.charAt(pos + 1)))) {
          res = pos;
          found++;
        }
      }
      else if (Character.isLowerCase(chars.charAt(pos))) {
        if (pos == 0 || !Character.isLetter(chars.charAt(pos - 1))) {
          res = pos;
          found++;
        }
      }
      else if (Character.isDigit(chars.charAt(pos))) {
        if (pos == 0 || !Character.isDigit(chars.charAt(pos - 1))) {
          res = pos;
          found++;
        }
      }

      pos += step;
    }

    if (found < Math.abs(count)) {
      res = -1;
    }

    return res;
  }

  public static int findNextCamelEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int size = EditorHelper.getFileSize(editor);

    int found = 0;
    int step = count >= 0 ? 1 : -1;
    if (pos < 0 || pos >= size) {
      return pos;
    }

    int res = pos;
    pos += step;
    while (pos >= 0 && pos < size && found < Math.abs(count)) {
      if (Character.isUpperCase(chars.charAt(pos))) {
        if (pos == size - 1 ||
            !Character.isLetter(chars.charAt(pos + 1)) ||
            (Character.isUpperCase(chars.charAt(pos + 1)) &&
             pos <= size - 2 &&
             Character.isLowerCase(chars.charAt(pos + 2)))) {
          res = pos;
          found++;
        }
      }
      else if (Character.isLowerCase(chars.charAt(pos))) {
        if (pos == size - 1 || !Character.isLowerCase(chars.charAt(pos + 1))) {
          res = pos;
          found++;
        }
      }
      else if (Character.isDigit(chars.charAt(pos))) {
        if (pos == size - 1 || !Character.isDigit(chars.charAt(pos + 1))) {
          res = pos;
          found++;
        }
      }

      pos += step;
    }

    if (found < Math.abs(count)) {
      res = -1;
    }

    return res;
  }

  /**
   * This counts all the words in the file.
   */
  @NotNull
  public static CountPosition countWords(@NotNull Editor editor) {
    int size = EditorHelper.getFileSize(editor);

    return countWords(editor, 0, size);
  }

  /**
   * This counts all the words in the file.
   */
  @NotNull
  public static CountPosition countWords(@NotNull Editor editor, int start, int end) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int offset = editor.getCaretModel().getOffset();

    return countWords(chars, start, end, offset);
  }

  @NotNull
  public static CountPosition countWords(@NotNull CharSequence chars, int start, int end, int offset) {
    int count = 1;
    int position = 0;
    int last = -1;
    int res = start;
    while (true) {
      res = findNextWordOne(chars, res, end, 1, true, false);
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

  public static int findNextWord(@NotNull Editor editor, int searchFrom, int count, boolean bigWord) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    final int size = EditorHelper.getFileSize(editor);

    return findNextWord(chars, searchFrom, size, count, bigWord, false);
  }

  public static int findNextWord(@NotNull CharSequence chars, int pos, int size, int count, boolean bigWord,
                                 boolean spaceWords) {
    int step = count >= 0 ? 1 : -1;
    count = Math.abs(count);

    int res = pos;
    for (int i = 0; i < count; i++) {
      res = findNextWordOne(chars, res, size, step, bigWord, spaceWords);
      if (res == pos || res == 0 || res == size - 1) {
        break;
      }
    }

    return res;
  }

  private static int findNextWordOne(@NotNull CharSequence chars, int pos, int size, int step, boolean bigWord,
                                     boolean spaceWords) {
    boolean found = false;
    pos = pos < size ? pos : Math.min(size, chars.length() - 1);
    // For back searches, skip any current whitespace so we start at the end of a word
    if (step < 0 && pos > 0) {
      if (CharacterHelper.charType(chars.charAt(pos - 1), bigWord) == CharacterHelper.CharacterType.WHITESPACE &&
          !spaceWords) {
        pos = skipSpace(chars, pos - 1, step, size) + 1;
      }
      if (pos > 0 &&
          CharacterHelper.charType(chars.charAt(pos), bigWord) !=
          CharacterHelper.charType(chars.charAt(pos - 1), bigWord)) {
        pos += step;
      }
    }
    int res = pos;
    if (pos < 0 || pos >= size) {
      return pos;
    }

    CharacterHelper.CharacterType type = CharacterHelper.charType(chars.charAt(pos), bigWord);
    if (type == CharacterHelper.CharacterType.WHITESPACE && step < 0 && pos > 0 && !spaceWords) {
      type = CharacterHelper.charType(chars.charAt(pos - 1), bigWord);
    }

    pos += step;
    while (pos >= 0 && pos < size && !found) {
      CharacterHelper.CharacterType newType = CharacterHelper.charType(chars.charAt(pos), bigWord);
      if (newType != type) {
        if (newType == CharacterHelper.CharacterType.WHITESPACE && step >= 0 && !spaceWords) {
          pos = skipSpace(chars, pos, step, size);
          res = pos;
        }
        else if (step < 0) {
          res = pos + 1;
        }
        else {
          res = pos;
        }

        type = CharacterHelper.charType(chars.charAt(res), bigWord);
        found = true;
      }

      pos += step;
    }

    if (found) {
      if (res < 0) //(pos <= 0)
      {
        res = 0;
      }
      else if (res >= size) //(pos >= size)
      {
        res = size - 1;
      }
    }
    else if (pos <= 0) {
      res = 0;
    }
    else if (pos >= size) {
      res = size;
    }

    return res;
  }

  @NotNull
  public static List<TextRange> findNumbersInRange(@NotNull final Editor editor, @NotNull TextRange textRange,
                                                   final boolean alpha, final boolean hex, final boolean octal) {
    List<TextRange> result = new ArrayList<>();
    int firstLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
    int lastLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line;

    int startOffset = textRange.getStartOffset();
    for (int lineNumber = firstLine; lineNumber <= lastLine; lineNumber++) {
      int endOffset = EditorHelper.getLineEndOffset(editor, lineNumber, true);
      if (endOffset > textRange.getEndOffset()) endOffset = textRange.getEndOffset();

      String text = EditorHelper.getText(editor, startOffset, endOffset);

      TextRange numberRange = findNumberInText(text, 0, alpha, hex, octal);

      if (numberRange == null) continue;

      result.add(new TextRange(numberRange.getStartOffset() + startOffset, numberRange.getEndOffset() + startOffset));

      startOffset = endOffset + 1;
    }

    return result;
  }

  @Nullable
  public static TextRange findNumberUnderCursor(@NotNull final Editor editor, @NotNull Caret caret, final boolean alpha,
                                                final boolean hex, final boolean octal) {
    int lline = caret.getLogicalPosition().line;
    String text = EditorHelper.getLineText(editor, lline).toLowerCase();
    int startLineOffset = EditorHelper.getLineStartOffset(editor, lline);
    int posOnLine = caret.getOffset() - startLineOffset;

    TextRange numberTextRange = findNumberInText(text, posOnLine, alpha, hex, octal);

    if (numberTextRange == null) {
      return null;
    }
    return new TextRange(numberTextRange.getStartOffset() + startLineOffset,
            numberTextRange.getEndOffset() + startLineOffset);
  }

  /**
   * Search for number in given text from start position
   *
   * @param textInRange - text to search in
   * @param startPosOnLine - start offset to search
   * @return - text range with number
   */
  @Nullable
  public static TextRange findNumberInText(@NotNull final String textInRange, int startPosOnLine, final boolean alpha,
                                            final boolean hex, final boolean octal) {

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
        if (textInRange.charAt(pos) == '0' && pos < lineEndOffset - 1 && "xX".indexOf(textInRange.charAt(pos + 1)) >= 0) {
          pos += 2;
        }
        else if ("xX".indexOf(textInRange.charAt(pos)) >= 0 && pos > 0 && textInRange.charAt(pos - 1) == '0') {
          pos++;
        }

        logger.debug("checking hex");
        final Pair<Integer, Integer> range = findRange(textInRange, pos, false, true, false, false);
        int start = range.first;
        int end = range.second;

        // Ox and OX
        if (start >= 2 && textInRange.substring(start - 2, start).toLowerCase().equals("0x")) {
          logger.debug("found hex");
          return new TextRange(start - 2, end);
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
      int start = range.first;
      int end = range.second;

      if (textInRange.charAt(start) == '0' &&
          end > start &&
          !(start > 0 && isNumberChar(textInRange.charAt(start - 1), false, false, false, true))) {
        logger.debug("found octal");
        return new TextRange(start, end);
      }
    }

    if (alpha) {
      if (logger.isDebugEnabled()) logger.debug("checking alpha for " + textInRange.charAt(pos));
      if (isNumberChar(textInRange.charAt(pos), true, false, false, false)) {
        if (logger.isDebugEnabled()) logger.debug("found alpha at " + pos);
        return new TextRange(pos, pos + 1);
      }
    }

    final Pair<Integer, Integer> range = findRange(textInRange, pos, false, false, false, true);
    int start = range.first;
    int end = range.second;
    if (start > 0 && textInRange.charAt(start - 1) == '-') {
      start--;
    }

    return new TextRange(start, end);
  }

  /**
   * Searches for digits block that matches parameters
   */
  @NotNull
  private static Pair<Integer, Integer> findRange(@NotNull final String text, final int pos, final boolean alpha,
                                                  final boolean hex, final boolean octal, final boolean decimal) {
    int end = pos;
    while (end < text.length() && isNumberChar(text.charAt(end), alpha, hex, octal, decimal)) {
      end++;
    }
    int start = pos;
    while (start >= 0 && isNumberChar(text.charAt(start), alpha, hex, octal, decimal)) {
      start--;
    }
    if (start < end &&
        (start == -1 ||
         0 <= start && start < text.length() && !isNumberChar(text.charAt(start), alpha, hex, octal, decimal))) {
      start++;
    }
    return Pair.create(start, end);
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
   * @param caret The caret to find word under
   * @return The text range of the found word or null if there is no word under/after the cursor on the line
   */
  @Nullable
  public static TextRange findWordUnderCursor(@NotNull Editor editor, @NotNull Caret caret) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int stop = EditorHelper.getLineEndOffset(editor, caret.getLogicalPosition().line, true);

    int pos = caret.getOffset();
    // Technically the first condition is covered by the second one, but let it be
    if (chars.length() == 0 || chars.length() <= pos) return null;

    int start = pos;
    CharacterHelper.CharacterType[] types = new CharacterHelper.CharacterType[]{CharacterHelper.CharacterType.KEYWORD,
      CharacterHelper.CharacterType.PUNCTUATION};
    for (int i = 0; i < 2; i++) {
      start = pos;
      CharacterHelper.CharacterType type = CharacterHelper.charType(chars.charAt(start), false);
      if (type == types[i]) {
        // Search back for start of word
        while (start > 0 && CharacterHelper.charType(chars.charAt(start - 1), false) == types[i]) {
          start--;
        }
      }
      else {
        // Search forward for start of word
        while (start < stop && CharacterHelper.charType(chars.charAt(start), false) != types[i]) {
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
        CharacterHelper.charType(chars.charAt(start + 1), false) != CharacterHelper.CharacterType.KEYWORD) {
      end = start + 1;
    }
    else {
      end = findNextWordEnd(chars, start, stop, 1, false, false) + 1;
    }

    return new TextRange(start, end);
  }

  @Contract("_, _, _, _, _, _, _ -> new")
  @NotNull
  public static TextRange findWordUnderCursor(@NotNull Editor editor, @NotNull Caret caret, int count, int dir,
                                              boolean isOuter, boolean isBig, boolean hasSelection) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
      logger.debug("dir=" + dir);
      logger.debug("isOuter=" + isOuter);
      logger.debug("isBig=" + isBig);
      logger.debug("hasSelection=" + hasSelection);
    }

    CharSequence chars = editor.getDocument().getCharsSequence();
    //int min = EditorHelper.getLineStartOffset(editor, EditorHelper.getCurrentLogicalLine(editor));
    //int max = EditorHelper.getLineEndOffset(editor, EditorHelper.getCurrentLogicalLine(editor), true);
    int min = 0;
    int max = EditorHelper.getFileSize(editor);

    if (logger.isDebugEnabled()) {
      logger.debug("min=" + min);
      logger.debug("max=" + max);
    }

    int pos = caret.getOffset();
    if (chars.length() <= pos) return new TextRange(chars.length() - 1, chars.length() - 1);

    boolean startSpace = CharacterHelper.charType(chars.charAt(pos), isBig) == CharacterHelper.CharacterType.WHITESPACE;
    // Find word start
    boolean onWordStart = pos == min ||
                          CharacterHelper.charType(chars.charAt(pos - 1), isBig) !=
                          CharacterHelper.charType(chars.charAt(pos), isBig);
    int start = pos;

    if (logger.isDebugEnabled()) {
      logger.debug("pos=" + pos);
      logger.debug("onWordStart=" + onWordStart);
    }

    if ((!onWordStart && !(startSpace && isOuter)) || hasSelection || (count > 1 && dir == -1)) {
      if (dir == 1) {
        start = findNextWord(chars, pos, max, -1, isBig, !isOuter);
      }
      else {
        start = findNextWord(chars, pos, max, -(count - (onWordStart && !hasSelection ? 1 : 0)), isBig, !isOuter);
      }

      start = EditorHelper.normalizeOffset(editor, start, false);
    }

    if (logger.isDebugEnabled()) logger.debug("start=" + start);

    // Find word end
    boolean onWordEnd = pos == max ||
                        CharacterHelper.charType(chars.charAt(pos + 1), isBig) !=
                        CharacterHelper.charType(chars.charAt(pos), isBig);

    if (logger.isDebugEnabled()) logger.debug("onWordEnd=" + onWordEnd);

    int end = pos;
    if (!onWordEnd || hasSelection || (count > 1 && dir == 1) || (startSpace && isOuter)) {
      if (dir == 1) {
        end = findNextWordEnd(chars, pos, max, count -
                                               (onWordEnd &&
                                                !hasSelection &&
                                                (!(startSpace && isOuter) || (startSpace && !isOuter)) ? 1 : 0), isBig,
                              !isOuter);
      }
      else {
        end = findNextWordEnd(chars, pos, max, 1, isBig, !isOuter);
      }
    }

    if (logger.isDebugEnabled()) logger.debug("end=" + end);

    boolean goBack = (startSpace && !hasSelection) || (!startSpace && hasSelection && !onWordStart);
    if (dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = findNextWordEnd(chars, pos, max, 1, isBig, false);
      }
      if (firstEnd < max) {
        if (CharacterHelper.charType(chars.charAt(firstEnd + 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true;
        }
      }
    }
    if (dir == -1 && isOuter && startSpace) {
      if (pos > min) {
        if (CharacterHelper.charType(chars.charAt(pos - 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true;
        }
      }
    }

    boolean goForward =
      (dir == 1 && isOuter && ((!startSpace && !onWordEnd) || (startSpace && onWordEnd && hasSelection)));
    if (!goForward && dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = findNextWordEnd(chars, pos, max, 1, isBig, false);
      }
      if (firstEnd < max) {
        if (CharacterHelper.charType(chars.charAt(firstEnd + 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goForward = true;
        }
      }
    }
    if (!goForward && dir == 1 && isOuter && !startSpace && !hasSelection) {
      if (end < max) {
        if (CharacterHelper.charType(chars.charAt(end + 1), !isBig) !=
            CharacterHelper.charType(chars.charAt(end), !isBig)) {
          goForward = true;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("goBack=" + goBack);
      logger.debug("goForward=" + goForward);
    }

    if (goForward && anyNonWhitespace(editor, end, 1)) {
      while (end < max &&
             CharacterHelper.charType(chars.charAt(end + 1), false) == CharacterHelper.CharacterType.WHITESPACE) {
        end++;
      }
    }
    if (goBack && anyNonWhitespace(editor, start, -1)) {
      while (start > min &&
             CharacterHelper.charType(chars.charAt(start - 1), false) == CharacterHelper.CharacterType.WHITESPACE) {
        start--;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("end=" + end);
    }

    // End offset is exclusive
    return new TextRange(start, end + 1);
  }

  /**
   * This finds the offset to the end of the next/previous word/WORD.
   *
   * @param editor  The editor to search in
   * @param caret   The caret to start search from
   * @param count   The number of words to skip. Negative for backward searches
   * @param bigWord If true then find WORD, if false then find word
   * @return The offset of match
   */
  public static int findNextWordEnd(@NotNull Editor editor, @NotNull Caret caret, int count, boolean bigWord) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int size = EditorHelper.getFileSize(editor);

    return findNextWordEnd(chars, pos, size, count, bigWord, false);
  }

  public static int findNextWordEnd(@NotNull CharSequence chars, int pos, int size, int count, boolean bigWord,
                                    boolean spaceWords) {
    int step = count >= 0 ? 1 : -1;
    count = Math.abs(count);

    int res = pos;
    for (int i = 0; i < count; i++) {
      res = findNextWordEndOne(chars, res, size, step, bigWord, spaceWords);
      if (res == pos || res == 0 || res == size - 1) {
        break;
      }
    }

    return res;
  }

  private static int findNextWordEndOne(@NotNull CharSequence chars, int pos, int size, int step, boolean bigWord,
                                        boolean spaceWords) {
    boolean found = false;
    // For forward searches, skip any current whitespace so we start at the start of a word
    if (step > 0 && pos < size - 1) {
      if (CharacterHelper.charType(chars.charAt(pos + 1), bigWord) == CharacterHelper.CharacterType.WHITESPACE &&
          !spaceWords) {
        pos = skipSpace(chars, pos + 1, step, size) - 1;
      }
      if (pos < size - 1 &&
          CharacterHelper.charType(chars.charAt(pos), bigWord) !=
          CharacterHelper.charType(chars.charAt(pos + 1), bigWord)) {
        pos += step;
      }
    }
    int res = pos;
    if (pos < 0 || pos >= size) {
      return pos;
    }
    CharacterHelper.CharacterType type = CharacterHelper.charType(chars.charAt(pos), bigWord);
    if (type == CharacterHelper.CharacterType.WHITESPACE && step >= 0 && pos < size - 1 && !spaceWords) {
      type = CharacterHelper.charType(chars.charAt(pos + 1), bigWord);
    }

    pos += step;
    while (pos >= 0 && pos < size && !found) {
      CharacterHelper.CharacterType newType = CharacterHelper.charType(chars.charAt(pos), bigWord);
      if (newType != type) {
        if (step >= 0) {
          res = pos - 1;
        }
        else if (newType == CharacterHelper.CharacterType.WHITESPACE && step < 0 && !spaceWords) {
          pos = skipSpace(chars, pos, step, size);
          res = pos;
        }
        else {
          res = pos;
        }

        found = true;
      }

      pos += step;
    }

    if (found) {
      if (res < 0)
      {
        res = 0;
      }
      else if (res >= size)
      {
        res = size - 1;
      }
    }
    else if (pos == size) {
      res = size - 1;
    }

    return res;
  }

  /**
   * Skip whitespace starting with the supplied position.
   * <p>
   * An empty line is considered a whitespace break.
   *
   * @param chars  The text as a character array
   * @param offset The starting position
   * @param step   The direction to move
   * @param size   The size of the document
   * @return The new position. This will be the first non-whitespace character found or an empty line
   */
  private static int skipSpace(@NotNull CharSequence chars, int offset, int step, int size) {
    char prev = 0;
    while (offset >= 0 && offset < size) {
      final char c = chars.charAt(offset);
      if (c == '\n' && c == prev) {
        break;
      }
      if (CharacterHelper.charType(c, false) != CharacterHelper.CharacterType.WHITESPACE) {
        break;
      }
      prev = c;
      offset += step;
    }

    return offset < size ? offset : size - 1;
  }

  /**
   * This locates the position with the document of the count-th occurrence of ch on the current line
   *
   * @param editor The editor to search in
   * @param caret  The caret to be searched starting from
   * @param count  The number of occurrences of ch to locate. Negative for backward searches
   * @param ch     The character on the line to find
   * @return The document offset of the matching character match, -1
   */
  public static int findNextCharacterOnLine(@NotNull Editor editor, @NotNull Caret caret, int count, char ch) {
    int line = caret.getLogicalPosition().line;
    int start = EditorHelper.getLineStartOffset(editor, line);
    int end = EditorHelper.getLineEndOffset(editor, line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int found = 0;
    int step = count >= 0 ? 1 : -1;
    int pos = caret.getOffset() + step;
    while (pos >= start && pos < end && pos >= 0 && pos < chars.length()) {
      if (chars.charAt(pos) == ch) {
        found++;
        if (found == Math.abs(count)) {
          break;
        }
      }
      pos += step;
    }

    if (found == Math.abs(count)) {
      return pos;
    }
    else {
      return -1;
    }
  }

  public static int findNextSentenceStart(@NotNull Editor editor, @NotNull Caret caret, int count, boolean countCurrent,
                                          boolean requireAll) {
    int dir = count > 0 ? 1 : -1;
    count = Math.abs(count);
    int total = count;
    CharSequence chars = editor.getDocument().getCharsSequence();
    int start = caret.getOffset();
    int max = EditorHelper.getFileSize(editor);

    int res = start;
    for (; count > 0 && res >= 0 && res <= max - 1; count--) {
      res = findSentenceStart(editor, chars, res, max, dir, countCurrent, count > 1);
      if (res == 0 || res == max - 1) {
        count--;
        break;
      }
    }

    if (res < 0 && (!requireAll || total == 1)) {
      res = dir > 0 ? max - 1 : 0;
    }
    else if (count > 0 && total > 1 && !requireAll) {
      res = dir > 0 ? max - 1 : 0;
    }
    else if (count > 0 && total > 1 && requireAll) {
      res = -count;
    }

    return res;
  }

  /**
   * @deprecated Use {@link #findNextParagraphLine(Editor, Caret, int, boolean)}
   */
  public static int findNextSentenceStart(@NotNull Editor editor, int count, boolean countCurrent, boolean requireAll) {
    return findNextSentenceStart(editor, editor.getCaretModel().getPrimaryCaret(), count, countCurrent, requireAll);
  }

  public static int findNextSentenceEnd(@NotNull Editor editor, @NotNull Caret caret, int count, boolean countCurrent,
                                        boolean requireAll) {
    int dir = count > 0 ? 1 : -1;
    count = Math.abs(count);
    int total = count;
    CharSequence chars = editor.getDocument().getCharsSequence();
    int start = caret.getOffset();
    int max = EditorHelper.getFileSize(editor);

    int res = start;
    for (; count > 0 && res >= 0 && res <= max - 1; count--) {
      res = findSentenceEnd(editor, chars, res, max, dir, countCurrent && count == total, count > 1);
      if (res == 0 || res == max - 1) {
        count--;
        break;
      }
    }

    if (res < 0 && (!requireAll || total == 1)) {
      res = dir > 0 ? max - 1 : 0;
    }
    else if (count > 0 && total > 1 && !requireAll) {
      res = dir > 0 ? max - 1 : 0;
    }
    else if (count > 0 && total > 1 && requireAll) {
      res = -count;
    }

    return res;
  }

  private static int findSentenceStart(@NotNull Editor editor, @NotNull CharSequence chars, int start, int max, int dir,
                                       boolean countCurrent, boolean multiple) {
    // Save off the next paragraph since a paragraph is a valid sentence.
    int lline = editor.offsetToLogicalPosition(start).line;
    int np = findNextParagraph(editor, lline, dir, false, multiple);

    int end;
    if (chars.charAt(start) == '\n' && !countCurrent) {
      end = findSentenceEnd(editor, chars, start, max, -1, false, multiple);
    }
    else {
      end = findSentenceEnd(editor, chars, start, max, -1, true, multiple);
    }
    if (end == start && countCurrent && chars.charAt(end) == '\n') {
      return end;
    }

    int pos = end - 1;
    if (end >= 0) {
      int offset = end + 1;
      while (offset < max) {
        char ch = chars.charAt(offset);
        if (!Character.isWhitespace(ch)) {
          break;
        }
        offset++;
      }

      if (dir > 0) {
        if (offset == start && countCurrent) {
          return offset;
        }
        else if (offset > start) {
          return offset;
        }
      }
      else {
        if (offset == start && countCurrent) {
          return offset;
        }
        else if (offset < start) {
          return offset;
        }
      }
    }

    if (dir > 0) {
      end = findSentenceEnd(editor, chars, start, max, dir, true, multiple);
    }
    else {
      end = findSentenceEnd(editor, chars, pos, max, dir, countCurrent, multiple);
    }

    int res = end + 1;
    if (end != -1 && (chars.charAt(end) != '\n' || !countCurrent)) {
      while (res < max) {
        char ch = chars.charAt(res);
        if (!Character.isWhitespace(ch)) {
          break;
        }
        res++;
      }
    }

    // Now let's see which to return, the sentence we found or the paragraph we found.
    // This mess returns which ever is closer to our starting point (and in the right direction).
    if (res >= 0 && np >= 0) {
      if (dir > 0) {
        if (np < res || res < start) {
          res = np;
        }
      }
      else {
        if (np > res || (res >= start && !countCurrent)) {
          res = np;
        }
      }
    }
    else if (res == -1 && np >= 0) {
      res = np;
    }
    // else we found neither, res already -1

    return res;
  }

  private static int findSentenceEnd(@NotNull Editor editor, @NotNull CharSequence chars, int start, int max, int dir,
                                     boolean countCurrent, boolean multiple) {
    if (dir > 0 && start >= EditorHelper.getFileSize(editor) - 1) {
      return -1;
    }
    else if (dir < 0 && start <= 0) {
      return -1;
    }

    // Save off the next paragraph since a paragraph is a valid sentence.
    int lline = editor.offsetToLogicalPosition(start).line;
    int np = findNextParagraph(editor, lline, dir, false, multiple);

    // Sections are also end-of-sentence markers. However, { and } in column 1 don't count.
    // Since our section implementation only supports these and form-feed chars, we'll just
    // check for form-feeds below.

    int res = -1;

    int offset = start;
    boolean found = false;
    // Search forward looking for a candidate end-of-sentence character (., !, or ?)
    while (offset >= 0 && offset < max && !found) {
      char ch = chars.charAt(offset);
      if (".!?".indexOf(ch) >= 0) {
        int end = offset; // Save where we found the punctuation.
        offset++;
        // This can be followed by any number of ), ], ", or ' characters.
        while (offset < max) {
          ch = chars.charAt(offset);
          if (")]\"'".indexOf(ch) == -1) {
            break;
          }

          offset++;
        }

        // The next character must be whitespace for this to be a valid end-of-sentence.
        if (offset >= max || Character.isWhitespace(ch)) {
          // So we have found the end of the next sentence. Now let's see if we ended
          // where we started (or further) on a back search. This will happen if we happen
          // to start this whole search already on a sentence end.
          if (offset - 1 == start && !countCurrent) {
            // Skip back to the sentence end so we can search backward from there
            // for the real previous sentence.
            offset = end;
          }
          else {
            // Yeah - we found the real end-of-sentence. Save it off.
            res = offset - 1;
            found = true;
          }
        }
        else {
          // Turned out not to be an end-of-sentence so move back to where we were.
          offset = end;
        }
      }
      else if (ch == '\n') {
        int end = offset; // Save where we found the newline.
        if (dir > 0) {
          offset++;
          while (offset < max) {
            ch = chars.charAt(offset);
            if (ch != '\n') {
              offset--;
              break;
            }
            if (offset == np && (end - 1 != start || countCurrent)) {
              break;
            }
            offset++;
          }

          if (offset == np && (end - 1 != start || countCurrent)) {
            res = end - 1;
            found = true;
          }
          else if (offset > end) {
            res = offset;
            np = res;
            found = true;
          }
          else if (offset == end) {
            if (offset > 0 && chars.charAt(offset - 1) == '\n' && countCurrent) {
              res = end;
              np = res;
              found = true;
            }
          }
        }
        else {
          if (offset > 0) {
            offset--;
            while (offset >= 0) {
              ch = chars.charAt(offset);
              if (ch != '\n') {
                offset++;
                break;
              }

              offset--;
            }
          }

          if (offset < end) {
            if (end == start && countCurrent) {
              res = end;
            }
            else {
              res = offset - 1;
            }

            found = true;
          }
        }

        offset = end;
      }
      // Form-feeds are also end-of-sentence markers.
      else if (ch == '\u000C') {
        res = offset;
        found = true;
      }

      offset += dir;
    }

    // Now let's see which to return, the sentence we found or the paragraph we found.
    // This mess returns which ever is closer to our starting point (and in the right direction).
    if (res >= 0 && np >= 0) {
      if (dir > 0) {
        if (np < res || res < start) {
          res = np;
        }
      }
      else {
        if (np > res || (res >= start && !countCurrent)) {
          res = np;
        }
      }
    }
    /*
    else if (res == -1 && np >= 0)
    {
        res = np;
    }
    */

    return res;
  }

  private static int findSentenceRangeEnd(@NotNull Editor editor, @NotNull CharSequence chars, int start, int max,
                                          int count, boolean isOuter, boolean oneway) {
    int dir = count > 0 ? 1 : -1;
    count = Math.abs(count);
    int total = count;

    boolean toggle = !isOuter;
    boolean findend = dir < 1;
    // Even = start, odd = end
    int which;
    int eprev = findSentenceEnd(editor, chars, start, max, -1, true, false);
    int enext = findSentenceEnd(editor, chars, start, max, 1, true, false);
    int sprev = findSentenceStart(editor, chars, start, max, -1, true, false);
    int snext = findSentenceStart(editor, chars, start, max, 1, true, false);
    if (snext == eprev) // On blank line
    {
      if (dir < 0 && !oneway) {
        return start;
      }

      which = 0;
      if (oneway) {
        findend = dir > 0;
      }
      else if (dir > 0 && start < max - 1 && !Character.isSpaceChar(chars.charAt(start + 1))) {
        findend = true;
      }
    }
    else if (start == snext) // On sentence start
    {
      if (dir < 0 && !oneway) {
        return start;
      }

      which = dir > 0 ? 1 : 0;
      if (dir < 0 && oneway) {
        findend = false;
      }
    }
    else if (start == enext) // On sentence end
    {
      if (dir > 0 && !oneway) {
        return start;
      }

      which = 0;
      if (dir > 0 && oneway) {
        findend = true;
      }
    }
    else if (start >= sprev && start <= enext && enext < snext) // Middle of sentence
    {
      which = dir > 0 ? 1 : 0;
    }
    else // Between sentences
    {
      which = dir > 0 ? 0 : 1;
      if (dir > 0) {
        if (oneway) {
          if (start < snext - 1) {
            findend = true;
          }
          else if (start == snext - 1) {
            count++;
          }
        }
        else {
          findend = true;
        }
      }
      else {
        if (oneway) {
          if (start > eprev + 1) {
            findend = false;
          }
          else if (start == eprev + 1) {
            count++;
          }
        }
        else {
          findend = true;
        }
      }
    }

    int res = start;
    for (; count > 0 && res >= 0 && res <= max - 1; count--) {
      if ((toggle && which % 2 == 1) || (isOuter && findend)) {
        res = findSentenceEnd(editor, chars, res, max, dir, false, total > 1);
      }
      else {
        res = findSentenceStart(editor, chars, res, max, dir, false, total > 1);
      }
      if (res == 0 || res == max - 1) {
        count--;
        break;
      }
      if (toggle) {
        if (which % 2 == 1 && dir < 0) {
          res++;
        }
        else if (which % 2 == 0 && dir > 0) {
          res--;
        }
      }

      which++;
    }

    if (res < 0 || count > 0) {
      res = dir > 0 ? (max > 0 ? max - 1 : 0) : 0;
    }
    else if (isOuter && ((dir < 0 && findend) || (dir > 0 && !findend))) {
      if (res != 0 && res != max - 1) {
        res -= dir;
      }
    }

    if (chars.charAt(res) == '\n' && res > 0 && chars.charAt(res - 1) != '\n') {
      res--;
    }

    return res;
  }

  @Contract("_, _, _, _ -> new")
  @NotNull
  public static TextRange findSentenceRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    if (chars.length() == 0) return new TextRange(0, 0);
    int max = EditorHelper.getFileSize(editor);
    int offset = caret.getOffset();
    int ssel = caret.getSelectionStart();
    int esel = caret.getSelectionEnd();
    if (Math.abs(esel - ssel) > 1) {
      int start;
      int end;
      // Forward selection
      if (offset == esel - 1) {
        start = ssel;
        end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, true);

        return new TextRange(start, end + 1);
      }
      // Backward selection
      else {
        end = esel - 1;
        start = findSentenceRangeEnd(editor, chars, offset, max, -count, isOuter, true);

        return new TextRange(end, start + 1);
      }
    }
    else {
      int end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, false);

      boolean space = isOuter;
      if (Character.isSpaceChar(chars.charAt(end))) {
        space = false;
      }

      int start = findSentenceRangeEnd(editor, chars, offset, max, -1, space, false);

      return new TextRange(start, end + 1);
    }
  }

  public static int findNextParagraph(@NotNull Editor editor, @NotNull Caret caret, int count, boolean allowBlanks) {
    int line = findNextParagraphLine(editor, caret, count, allowBlanks);

    int maxline = EditorHelper.getLineCount(editor);
    if (line >= 0 && line < maxline) {
      return EditorHelper.getLineStartOffset(editor, line);
    }
    else if (line == maxline) {
      return count > 0 ? EditorHelper.getFileSize(editor) - 1 : 0;
    }
    else {
      return -1;
    }
  }

  /**
   * @deprecated To find the next paragraph, use {@link #findNextParagraph(Editor, Caret, int, boolean)}
   */
  public static int findNextParagraph(@NotNull Editor editor, int count, boolean allowBlanks) {
    return findNextParagraph(editor, editor.getCaretModel().getPrimaryCaret(), count, allowBlanks);
  }

  private static int findNextParagraph(@NotNull Editor editor, int lline, int dir, boolean allowBlanks,
                                       boolean skipLines) {
    int line = findNextParagraphLine(editor, lline, dir, allowBlanks, skipLines);

    if (line >= 0) {
      return EditorHelper.getLineStartOffset(editor, line);
    }
    else {
      return dir > 0 ? EditorHelper.getFileSize(editor) - 1 : 0;
    }
  }

  private static int findNextParagraphLine(@NotNull Editor editor, @NotNull Caret caret, int count,
                                           boolean allowBlanks) {
    int line = caret.getLogicalPosition().line;

    int maxline = EditorHelper.getLineCount(editor);
    int dir = count > 0 ? 1 : -1;
    boolean skipLines = count > 1;
    count = Math.abs(count);
    int total = count;

    for (; count > 0 && line >= 0; count--) {
      line = findNextParagraphLine(editor, line, dir, allowBlanks, skipLines);
    }

    if (total == 1 && line < 0) {
      line = dir > 0 ? maxline - 1 : 0;
    }
    else if (total > 1 && count == 0 && line < 0) {
      line = dir > 0 ? maxline - 1 : 0;
    }

    return line;
  }

  /**
   * @deprecated Use {@link #findNextParagraphLine(Editor, Caret, int, boolean)}
   */
  private static int findNextParagraphLine(@NotNull Editor editor, int count, boolean allowBlanks) {
    return findNextParagraphLine(editor, editor.getCaretModel().getPrimaryCaret(), count, allowBlanks);
  }

  private static int findNextParagraphLine(@NotNull Editor editor, int line, int dir, boolean allowBlanks,
                                           boolean skipLines) {
    int maxline = EditorHelper.getLineCount(editor);
    int res = -1;

    line = skipEmptyLines(editor, line, dir, allowBlanks);
    while (line >= 0 && line < maxline && res == -1) {
      if (EditorHelper.isLineEmpty(editor, line, allowBlanks)) {
        res = line;
        if (skipLines) {
          line = skipEmptyLines(editor, line, dir, allowBlanks);
        }
      }

      line += dir;
    }

    return res;
  }

  private static int skipEmptyLines(@NotNull Editor editor, int line, int dir, boolean allowBlanks) {
    int maxline = EditorHelper.getLineCount(editor);
    while (line >= 0 && line < maxline) {
      if (!EditorHelper.isLineEmpty(editor, line, allowBlanks)) {
        return line;
      }

      line += dir;
    }

    return line;
  }

  @Nullable
  public static TextRange findParagraphRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    int line = caret.getLogicalPosition().line;
    int maxline = EditorHelper.getLineCount(editor);
    if (logger.isDebugEnabled()) logger.debug("starting on line " + line);
    int sline;
    int eline;
    boolean fixstart = false;
    boolean fixend = false;
    if (isOuter) {
      if (EditorHelper.isLineEmpty(editor, line, true)) {
        sline = line;
      }
      else {
        sline = findNextParagraphLine(editor, caret, -1, true);
      }

      eline = findNextParagraphLine(editor, caret, count, true);
      if (eline < 0) {
        return null;
      }

      if (EditorHelper.isLineEmpty(editor, sline, true) && EditorHelper.isLineEmpty(editor, eline, true)) {
        if (sline == line) {
          eline--;
          fixstart = true;
        }
        else {
          sline++;
          fixend = true;
        }
      }
      else if (!EditorHelper.isLineEmpty(editor, eline, true) &&
               !EditorHelper.isLineEmpty(editor, sline, true) &&
               sline > 0) {
        sline--;
        fixstart = true;
      }
      else if (EditorHelper.isLineEmpty(editor, eline, true)) {
        fixend = true;
      }
      else if (EditorHelper.isLineEmpty(editor, sline, true)) {
        fixstart = true;
      }
    }
    else {
      sline = line;
      if (!EditorHelper.isLineEmpty(editor, sline, true)) {
        sline = findNextParagraphLine(editor, caret, -1, true);
        if (EditorHelper.isLineEmpty(editor, sline, true)) {
          sline++;
        }
        eline = line;
      }
      else {
        eline = line - 1;
      }

      int which = EditorHelper.isLineEmpty(editor, sline, true) ? 0 : 1;
      for (int i = 0; i < count; i++) {
        if (which % 2 == 1) {
          eline = findNextParagraphLine(editor, eline, 1, true, false) - 1;
          if (eline < 0) {
            if (i == count - 1) {
              eline = maxline - 1;
            }
            else {
              return null;
            }
          }
        }
        else {
          eline++;
        }
        which++;
      }
      fixstart = true;
      fixend = true;
    }

    if (fixstart && EditorHelper.isLineEmpty(editor, sline, true)) {
      while (sline > 0) {
        if (EditorHelper.isLineEmpty(editor, sline - 1, true)) {
          sline--;
        }
        else {
          break;
        }
      }
    }

    if (fixend && EditorHelper.isLineEmpty(editor, eline, true)) {
      while (eline < maxline - 1) {
        if (EditorHelper.isLineEmpty(editor, eline + 1, true)) {
          eline++;
        }
        else {
          break;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("final sline=" + sline);
      logger.debug("final eline=" + eline);
    }
    int start = EditorHelper.getLineStartOffset(editor, sline);
    int end = EditorHelper.getLineStartOffset(editor, eline);

    return new TextRange(start, end + 1);
  }

  public static int findMethodStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return PsiHelper.findMethodStart(editor, caret.getOffset(), count);
  }

  public static int findMethodEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return PsiHelper.findMethodEnd(editor, caret.getOffset(), count);
  }

  @NotNull
  private static String getPairChars() {
    if (pairsChars == null) {
      ListOption lo = OptionsManager.INSTANCE.getMatchpairs();
      pairsChars = parseOption(lo);

      lo.addOptionChangeListener(event -> pairsChars = parseOption((ListOption)event.getOption()));
    }

    return pairsChars;
  }

  @NotNull
  private static String parseOption(@NotNull ListOption option) {
    List<String> vals = option.values();
    if (vals == null) {
      return "";
    }
    StringBuilder res = new StringBuilder();
    for (String s : vals) {
      if (s.length() == 3) {
        res.append(s.charAt(0)).append(s.charAt(2));
      }
    }

    return res.toString();
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

  @Nullable private static String pairsChars = null;
  @NotNull private static final String blockChars = "{}()[]<>";

  private static final Logger logger = Logger.getInstance(SearchHelper.class.getName());
}
