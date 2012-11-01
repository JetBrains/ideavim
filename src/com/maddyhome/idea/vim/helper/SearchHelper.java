package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.Options;

import java.util.List;

/**
 * Helper methods for searching text
 */
public class SearchHelper {
  public static boolean anyNonWhitespace(Editor editor, int offset, int dir) {
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

  public static int findSection(Editor editor, char type, int dir, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int line = editor.getCaretModel().getLogicalPosition().line + dir;
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

  public static int findUnmatchedBlock(Editor editor, char type, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
    int loc = blockChars.indexOf(type);
    // What direction should we go now (-1 is backward, 1 is forward)
    int dir = loc % 2 == 0 ? -1 : 1;
    // Which character did we find and which should we now search for
    char match = blockChars.charAt(loc);
    char found = blockChars.charAt(loc - dir);

    return findBlockLocation(chars, found, match, dir, pos, count);
  }

  public static TextRange findBlockRange(Editor editor, char type, int count, boolean isOuter) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
    int start = editor.getSelectionModel().getSelectionStart();
    int end = editor.getSelectionModel().getSelectionEnd();
    if (start != end) {
      pos = Math.min(start, end);
    }

    int loc = blockChars.indexOf(type);
    char close = blockChars.charAt(loc + 1);

    int bstart = findBlockLocation(chars, close, type, -1, pos, count);
    if (bstart == -1) {
      return null;
    }

    int bend = findBlockLocation(chars, type, close, 1, bstart + 1, 1);

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

    return new TextRange(bstart, bend);
  }

  /**
   * This looks on the current line, starting at the cursor postion for one of {, }, (, ), [, or ]. It then searches
   * forward or backward, as appropriate for the associated match pair. String in double quotes are skipped over.
   * Single characters in single quotes are skipped too.
   *
   * @param editor The editor to search in
   * @return The offset within the editor of the found character or -1 if no match was found or none of the characters
   *         were found on the remainder of the current line.
   */
  public static int findMatchingPairOnCurrentLine(Editor editor) {
    int line = editor.getCaretModel().getLogicalPosition().line;
    int end = EditorHelper.getLineEndOffset(editor, line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
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
      res = findBlockLocation(chars, found, match, dir, pos, 1);
    }

    return res;
  }

  private static int findBlockLocation(CharSequence chars, char found, char match, int dir, int pos, int cnt) {
    int res = -1;
    boolean inString = checkInString(chars, pos, true);
    boolean inChar = checkInString(chars, pos, false);
    int stack = 0;
    pos += dir;
    // Search to start or end of file, as appropriate
    while (pos >= 0 && pos < chars.length() && cnt > 0) {
      // If we found a match and we're not in a string...
      if (chars.charAt(pos) == match && !inString && !inChar) {
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
      // We found another character like our original - belongs to another pair
      else if (chars.charAt(pos) == found && !inString && !inChar) {
        stack++;
      }
      // We found the start/end of a string
      else if (!inChar && chars.charAt(pos) == '"' && (pos == 0 || chars.charAt(pos - 1) != '\\')) {
        inString = !inString;
      }
      else if (!inString && chars.charAt(pos) == '\'' && (pos == 0 || chars.charAt(pos - 1) != '\\')) {
        inChar = !inChar;
      }
      // End of line - mark not in a string any more (in case we started in the middle of one
      else if (chars.charAt(pos) == '\n') {
        inString = false;
        inChar = false;
      }

      pos += dir;
    }

    return res;
  }

  private static int findNextQuoteInLine(CharSequence chars, int pos) {
    return findQuoteInLine(chars, pos, (byte) 1);
  }

  private static int findPreviousQuoteInLine(CharSequence chars, int pos) {
    return findQuoteInLine(chars, pos, (byte) -1);
  }
  
  private static int findCurrentLineStart(CharSequence chars, int pos) {
    return findLineBorder(chars, pos, (byte) -1);
  }

  private static int findCurrentLineEnd(CharSequence chars, int pos) {
    return findLineBorder(chars, pos, (byte) 1);
  }
  
  private static int findFirstQuoteInLine(CharSequence chars, int pos) {
    int offset = SearchHelper.findCurrentLineStart(chars, pos);
    return SearchHelper.findNextQuoteInLine(chars, offset);
  }

  private static int findQuoteInLine(CharSequence chars, int pos, final byte direction) {
      return findCharacterPosition(chars, pos, '"', true, false, direction);
  }

  private static int countCharactersInLine(CharSequence chars, int pos, char ch, boolean searchEscaped, final byte direction) {
    int cnt = 0;
    int cPos = pos;
    while ((cPos!= -1) && (chars.charAt(cPos + direction) != '\n' )) {
      cPos = SearchHelper.findCharacterPosition(chars, cPos + direction, ch, searchEscaped, true, direction);
      if (cPos != -1) {
        cnt++;
      }
    }
    return cnt;
  }

  private static int findLineBorder(CharSequence chars, int pos, final byte direction) {
    if (chars.charAt(pos) == '\n') {
      if (direction == 1) {
        return pos;
      }
      else {
        pos--;
      }
    }

    int newlinePosition = findCharacterPosition(chars, pos, '\n', false, false, direction);

    if (newlinePosition == -1) {
      if (direction == -1) {
        return 0;
      }
      else {
        return chars.length() > 0 ? chars.length() - 1 : 0;
      }
    }

    if (direction == -1) {
      return newlinePosition + 1;
    }
    else {
      return newlinePosition;
    }
  }

  private static int findCharacterPosition(CharSequence chars, int pos, final char ch, final boolean searchInsideCurrentLine, final boolean searchEscaped, final byte direction) {
    int offset = pos;

    while(offset >= 0 && offset < chars.length() && (!searchInsideCurrentLine || chars.charAt(offset) != '\n') ) {
      if (chars.charAt(offset) == ch && (offset == 0 || searchEscaped || chars.charAt(offset - 1) != '\\')) {
        return offset;
      }
      offset += direction;
    }

    return -1;
  }

  public static TextRange findBlockQuoteInLineRange(Editor editor, boolean isOuter) {

    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();

    if (chars.charAt(pos) == '\n') {
        return null;
    }

    int firstQuoteInBlock = SearchHelper.findPreviousQuoteInLine(chars, pos);

    if (firstQuoteInBlock == -1) {
      firstQuoteInBlock = SearchHelper.findFirstQuoteInLine(chars, pos);
    }

    if (firstQuoteInBlock == -1 ) {
      return null;
    }

    int currentPosition = (firstQuoteInBlock > pos)?firstQuoteInBlock : pos;
    int lastQuoteInBlock = currentPosition;

    if (chars.charAt(pos) == '"' && currentPosition == pos) {
      int quoteNumberFromCurrentPositionToLineStart = SearchHelper.countCharactersInLine(chars, pos, '"', false, (byte) -1) + 1;

      if (quoteNumberFromCurrentPositionToLineStart % 2 == 0) {
        firstQuoteInBlock = SearchHelper.findPreviousQuoteInLine(chars, currentPosition - 1);
      }
      else {
        lastQuoteInBlock = SearchHelper.findNextQuoteInLine(chars, currentPosition + 1);
      }
    }
    else {
      lastQuoteInBlock = SearchHelper.findNextQuoteInLine(chars, currentPosition + 1);
    }

    if (lastQuoteInBlock == -1) {
      return null;
    }

    if (isOuter) {
      return new TextRange(firstQuoteInBlock, lastQuoteInBlock);
    }
    else {
      return new TextRange(firstQuoteInBlock + 1, lastQuoteInBlock);
    }
  }

  private static boolean checkInString(CharSequence chars, int pos, boolean str) {
    int offset = pos;
    while (offset > 0 && chars.charAt(offset) != '\n') {
      offset--;
    }

    boolean inString = false;
    boolean inChar = false;
    for (int i = offset; i < pos; i++) {
      if (!inChar && chars.charAt(i) == '"' && (i == 0 || chars.charAt(i - 1) != '\\')) {
        inString = !inString;
      }
      else if (!inString && chars.charAt(i) == '\'' && (i == 0 || chars.charAt(i - 1) != '\\')) {
        inChar = !inChar;
      }
    }

    return str ? inString : inChar;
  }

  public static int findNextCamelStart(Editor editor, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
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

  public static int findNextCamelEnd(Editor editor, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
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
        if (pos == size - 1 || !Character.isLetter(chars.charAt(pos + 1)) ||
            (Character.isUpperCase(chars.charAt(pos + 1)) && pos <= size - 2 && Character.isLowerCase(chars.charAt(pos + 2)))) {
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
  public static CountPosition countWords(Editor editor) {
    int size = EditorHelper.getFileSize(editor);

    return countWords(editor, 0, size);
  }

  /**
   * This counts all the words in the file.
   */
  public static CountPosition countWords(Editor editor, int start, int end) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int offset = editor.getCaretModel().getOffset();

    return countWords(chars, start, end, offset);
  }

  public static CountPosition countWords(CharSequence chars, int start, int end, int offset) {
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
        if (count == 2 && res > offset) {
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

  /**
   * This finds the offset to the start of the next/previous word/WORD.
   *
   * @param editor   The editor to find the words in
   * @param count    The number of words to skip. Negative for backward searches
   * @param skipPunc If true then find WORD, if false then find word
   * @return The offset of the match
   */
  public static int findNextWord(Editor editor, int count, boolean skipPunc) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
    int size = EditorHelper.getFileSize(editor);

    return findNextWord(chars, pos, size, count, skipPunc, false);
  }

  public static int findNextWord(CharSequence chars, int pos, int size, int count, boolean skipPunc, boolean spaceWords) {
    int step = count >= 0 ? 1 : -1;
    count = Math.abs(count);

    int res = pos;
    for (int i = 0; i < count; i++) {
      res = findNextWordOne(chars, res, size, step, skipPunc, spaceWords);
      if (res == pos || res == 0 || res == size - 1) {
        break;
      }
    }

    return res;
  }

  private static int findNextWordOne(CharSequence chars, int pos, int size, int step, boolean skipPunc, boolean spaceWords) {
    boolean found = false;
    pos = pos < size ? pos : size - 1;
    // For back searches, skip any current whitespace so we start at the end of a word
    if (step < 0 && pos > 0) {
      if (CharacterHelper.charType(chars.charAt(pos - 1), skipPunc) == CharacterHelper.TYPE_SPACE && !spaceWords) {
        pos = skipSpace(chars, pos - 1, step, size) + 1;
      }
      if (CharacterHelper.charType(chars.charAt(pos), skipPunc) != CharacterHelper.charType(chars.charAt(pos - 1), skipPunc)) {
        pos += step;
      }
    }
    int res = pos;
    if (pos < 0 || pos >= size) {
      return pos;
    }

    int type = CharacterHelper.charType(chars.charAt(pos), skipPunc);
    if (type == CharacterHelper.TYPE_SPACE && step < 0 && pos > 0 && !spaceWords) {
      type = CharacterHelper.charType(chars.charAt(pos - 1), skipPunc);
    }

    pos += step;
    while (pos >= 0 && pos < size && !found) {
      int newType = CharacterHelper.charType(chars.charAt(pos), skipPunc);
      if (newType != type) {
        if (newType == CharacterHelper.TYPE_SPACE && step >= 0 && !spaceWords) {
          pos = skipSpace(chars, pos, step, size);
          res = pos;
        }
        else if (step < 0) {
          res = pos + 1;
        }
        else {
          res = pos;
        }

        type = CharacterHelper.charType(chars.charAt(res), skipPunc);
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

    return res;
  }

  public static TextRange findNumberUnderCursor(final Editor editor, final boolean alpha, final boolean hex, final boolean octal) {
    int lline = editor.getCaretModel().getLogicalPosition().line;
    String text = EditorHelper.getLineText(editor, lline).toLowerCase();
    int offset = EditorHelper.getLineStartOffset(editor, lline);
    int pos = editor.getCaretModel().getOffset() - offset;

    if (logger.isDebugEnabled()) {
      logger.debug("lline=" + lline);
      logger.debug("text=" + text);
      logger.debug("offset=" + offset);
      logger.debug("pos=" + pos);
    }

    while (true) {
      // Skip over current whitespace if any
      while (pos < text.length() && !isNumberChar(text.charAt(pos), alpha, hex, octal, true)) {
        pos++;
      }

      if (logger.isDebugEnabled()) logger.debug("pos=" + pos);
      if (pos >= text.length()) {
        logger.debug("no number char on line");
        return null;
      }

      boolean isHexChar = "abcdefABCDEF".indexOf(text.charAt(pos)) >= 0;

      if (hex) {
        // Ox and OX handling
        if (text.charAt(pos) == '0' && pos < text.length() - 1 && "xX".indexOf(text.charAt(pos + 1)) >= 0) {
          pos += 2;
        }
        else if ("xX".indexOf(text.charAt(pos)) >= 0 && pos > 0 && text.charAt(pos - 1) == '0') {
          pos++;
        }

        logger.debug("checking hex");
        final Pair<Integer, Integer> range = findRange(text, pos, false, true, false, false);
        int start = range.first;
        int end = range.second;

        // Ox and OX
        if (start >= 2 && text.substring(start - 2, start).toLowerCase().equals("0x")) {
          logger.debug("found hex");
          return new TextRange(start - 2 + offset, end + offset);
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
      final Pair<Integer, Integer> range = findRange(text, pos, false, false, true, false);
      int start = range.first;
      int end = range.second;

      if (text.charAt(start) == '0' && end > start && !(start > 0 && isNumberChar(text.charAt(start - 1), false, false, false, true))) {
        logger.debug("found octal");
        return new TextRange(start + offset, end + offset);
      }
    }

    if (alpha) {
      if (logger.isDebugEnabled()) logger.debug("checking alpha for " + text.charAt(pos));
      if (isNumberChar(text.charAt(pos), true, false, false, false)) {
        if (logger.isDebugEnabled()) logger.debug("found alpha at " + pos);
        return new TextRange(pos + offset, pos + 1 + offset);
      }
    }

    final Pair<Integer, Integer> range = findRange(text, pos, false, false, false, true);
    int start = range.first;
    int end = range.second;
    if (start > 0 && text.charAt(start - 1) == '-') {
      start--;
    }

    return new TextRange(start + offset, end + offset);
  }

  /**
   * Searches for digits block that matches parameters
   */
  private static Pair<Integer, Integer> findRange(final String text, final int pos,
                                                  final boolean alpha, final boolean hex, final boolean octal, final boolean decimal) {
    int end = pos;
    while (end < text.length() && isNumberChar(text.charAt(end), alpha, hex, octal, decimal)) {
      end++;
    }
    int start = pos;
    while (start >= 0 && isNumberChar(text.charAt(start), alpha, hex, octal, decimal)) {
      start--;
    }
    if (start < end &&
        (start == -1 || 0 <= start && start < text.length() && !isNumberChar(text.charAt(start), alpha, hex, octal, decimal))) {
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
    else if (decimal && (ch >= '0' && ch <= '9')) {
      return true;
    }

    return false;
  }

  /**
   * Find the word under the cursor or the next word to the right of the cursor on the current line.
   *
   * @param editor The editor to find the word in
   * @return The text range of the found word or null if there is no word under/after the cursor on the line
   */
  public static TextRange findWordUnderCursor(Editor editor) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int stop = EditorHelper.getLineEndOffset(editor, editor.getCaretModel().getLogicalPosition().line, true);

    int pos = editor.getCaretModel().getOffset();
    int start = pos;
    int[] types = new int[]{CharacterHelper.TYPE_CHAR, CharacterHelper.TYPE_PUNC};
    for (int i = 0; i < 2; i++) {
      start = pos;
      int type = CharacterHelper.charType(chars.charAt(start), false);
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
    if (start < stop && CharacterHelper.charType(chars.charAt(start + 1), false) != CharacterHelper.TYPE_CHAR) {
      end = start + 1;
    }
    else {
      end = findNextWordEnd(chars, start, stop, 1, false, false, false) + 1;
    }

    return new TextRange(start, end);
  }

  public static TextRange findWordUnderCursor(Editor editor, int count, int dir, boolean isOuter, boolean isBig, boolean hasSelection) {
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

    int pos = editor.getCaretModel().getOffset();
    boolean startSpace = CharacterHelper.charType(chars.charAt(pos), isBig) == CharacterHelper.TYPE_SPACE;
    // Find word start
    boolean onWordStart = pos == min ||
                          CharacterHelper.charType(chars.charAt(pos - 1), isBig) != CharacterHelper.charType(chars.charAt(pos), isBig);
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
                        CharacterHelper.charType(chars.charAt(pos + 1), isBig) != CharacterHelper.charType(chars.charAt(pos), isBig);

    if (logger.isDebugEnabled()) logger.debug("onWordEnd=" + onWordEnd);

    int end = pos;
    if (!onWordEnd || hasSelection || (count > 1 && dir == 1) || (startSpace && isOuter)) {
      if (dir == 1) {
        end = findNextWordEnd(chars, pos, max, count -
                                               (onWordEnd && !hasSelection && (!(startSpace && isOuter) || (startSpace && !isOuter))
                                                ? 1
                                                : 0),
                              isBig, true, !isOuter);
      }
      else {
        end = findNextWordEnd(chars, pos, max, 1, isBig, true, !isOuter);
      }
    }

    if (logger.isDebugEnabled()) logger.debug("end=" + end);

    boolean goBack = (startSpace && !hasSelection) || (!startSpace && hasSelection && !onWordStart);
    if (dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = findNextWordEnd(chars, pos, max, 1, isBig, true, false);
      }
      if (firstEnd < max) {
        if (CharacterHelper.charType(chars.charAt(firstEnd + 1), false) != CharacterHelper.TYPE_SPACE) {
          goBack = true;
        }
      }
    }
    if (dir == -1 && isOuter && startSpace) {
      if (pos > min) {
        if (CharacterHelper.charType(chars.charAt(pos - 1), false) != CharacterHelper.TYPE_SPACE) {
          goBack = true;
        }
      }
    }

    boolean goForward = (dir == 1 && isOuter && ((!startSpace && !onWordEnd) || (startSpace && onWordEnd && hasSelection)));
    if (!goForward && dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = findNextWordEnd(chars, pos, max, 1, isBig, true, false);
      }
      if (firstEnd < max) {
        if (CharacterHelper.charType(chars.charAt(firstEnd + 1), false) != CharacterHelper.TYPE_SPACE) {
          goForward = true;
        }
      }
    }
    if (!goForward && dir == 1 && isOuter && !startSpace && !hasSelection) {
      if (end < max) {
        if (CharacterHelper.charType(chars.charAt(end + 1), !isBig) != CharacterHelper.charType(chars.charAt(end), !isBig)) {
          goForward = true;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("goBack=" + goBack);
      logger.debug("goForward=" + goForward);
    }

    if (goForward) {
      while (end < max && CharacterHelper.charType(chars.charAt(end + 1), false) == CharacterHelper.TYPE_SPACE) {
        end++;
      }
    }
    if (goBack) {
      while (start > min && CharacterHelper.charType(chars.charAt(start - 1), false) == CharacterHelper.TYPE_SPACE) {
        start--;
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("end=" + end);
    }

    return new TextRange(start, end);
  }

  /**
   * This finds the offset to the end of the next/previous word/WORD.
   *
   * @param editor   The editor to search in
   * @param count    The number of words to skip. Negative for backward searches
   * @param skipPunc If true then find WORD, if false then find word
   * @return The offset of match
   */
  public static int findNextWordEnd(Editor editor, int count, boolean skipPunc, boolean stayEnd) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = editor.getCaretModel().getOffset();
    int size = EditorHelper.getFileSize(editor);

    return findNextWordEnd(chars, pos, size, count, skipPunc, stayEnd, false);
  }

  public static int findNextWordEnd(CharSequence chars, int pos, int size, int count, boolean skipPunc, boolean stayEnd,
                                    boolean spaceWords) {
    int step = count >= 0 ? 1 : -1;
    count = Math.abs(count);

    int res = pos;
    for (int i = 0; i < count; i++) {
      res = findNextWordEndOne(chars, res, size, step, skipPunc, stayEnd, spaceWords);
      if (res == pos || res == 0 || res == size - 1) {
        break;
      }
    }

    return res;
  }

  private static int findNextWordEndOne(CharSequence chars, int pos, int size, int step, boolean skipPunc, boolean stayEnd,
                                        boolean spaceWords) {
    boolean found = false;
    // For forward searches, skip any current whitespace so we start at the start of a word
    if (step > 0 && pos < size - 1) {
      /*
      if (CharacterHelper.charType(chars[pos + step], false) == CharacterHelper.TYPE_SPACE)
      {
          if (!stayEnd)
          {
              pos += step;
          }
          pos = skipSpace(chars, pos, step, size);
      }
      */
      if (CharacterHelper.charType(chars.charAt(pos + 1), skipPunc) == CharacterHelper.TYPE_SPACE && !spaceWords) {
        pos = skipSpace(chars, pos + 1, step, size) - 1;
      }
      if (pos < size - 1 && CharacterHelper.charType(chars.charAt(pos), skipPunc) != CharacterHelper.charType(chars.charAt(pos + 1), skipPunc)) {
        pos += step;
      }
    }
    int res = pos;
    if (pos < 0 || pos >= size) {
      return pos;
    }
    int type = CharacterHelper.charType(chars.charAt(pos), skipPunc);
    if (type == CharacterHelper.TYPE_SPACE && step >= 0 && pos < size - 1 && !spaceWords) {
      type = CharacterHelper.charType(chars.charAt(pos + 1), skipPunc);
    }

    pos += step;
    while (pos >= 0 && pos < size && !found) {
      int newType = CharacterHelper.charType(chars.charAt(pos), skipPunc);
      if (newType != type) {
        if (step >= 0) {
          res = pos - 1;
        }
        else if (newType == CharacterHelper.TYPE_SPACE && step < 0 && !spaceWords) {
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
      if (res < 0) //(pos <= 0)
      {
        res = 0;
      }
      else if (res >= size) //(pos >= size)
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
   * This skips whitespace starting with the supplied position.
   *
   * @param chars  The text as a character array
   * @param offset The starting position
   * @param step   The direction to move
   * @param size   The size of the document
   * @return The new position. This will be the first non-whitespace character found
   */
  public static int skipSpace(CharSequence chars, int offset, int step, int size) {
    while (offset >= 0 && offset < size) {
      if (CharacterHelper.charType(chars.charAt(offset), false) != CharacterHelper.TYPE_SPACE) {
        break;
      }

      offset += step;
    }

    return offset;
  }

  /**
   * This locates the position with the document of the count'th occurence of ch on the current line
   *
   * @param editor The editor to search in
   * @param count  The number of occurences of ch to locate. Negative for backward searches
   * @param ch     The character on the line to find
   * @return The document offset of the matching character match, -1
   */
  public static int findNextCharacterOnLine(Editor editor, int count, char ch) {
    int line = editor.getCaretModel().getLogicalPosition().line;
    int start = EditorHelper.getLineStartOffset(editor, line);
    int end = EditorHelper.getLineEndOffset(editor, line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int found = 0;
    int step = count >= 0 ? 1 : -1;
    int pos = editor.getCaretModel().getOffset() + step;
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

  public static int findNextSentenceStart(Editor editor, int count, boolean countCurrent, boolean requireAll) {
    int dir = count > 0 ? 1 : -1;
    count = Math.abs(count);
    int total = count;
    CharSequence chars = editor.getDocument().getCharsSequence();
    int start = editor.getCaretModel().getOffset();
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

  public static int findNextSentenceEnd(Editor editor, int count, boolean countCurrent, boolean requireAll) {
    int dir = count > 0 ? 1 : -1;
    count = Math.abs(count);
    int total = count;
    CharSequence chars = editor.getDocument().getCharsSequence();
    int start = editor.getCaretModel().getOffset();
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

  private static int findSentenceStart(Editor editor, CharSequence chars, int start, int max, int dir,
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

  private static int findSentenceEnd(Editor editor, CharSequence chars, int start, int max, int dir,
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
        int end = offset; // Save where we found the punctuation.
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
          offset--;
          while (offset >= 0) {
            ch = chars.charAt(offset);
            if (ch != '\n') {
              offset++;
              break;
            }

            offset--;
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

  private static int findSentenceRangeEnd(Editor editor, CharSequence chars, int start, int max, int count,
                                          boolean isOuter, boolean oneway) {
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
      res = dir > 0 ? max - 1 : 0;
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

  public static TextRange findSentenceRange(Editor editor, int count, boolean isOuter) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int max = EditorHelper.getFileSize(editor);
    int offset = editor.getCaretModel().getOffset();
    int ssel = editor.getSelectionModel().getSelectionStart();
    int esel = editor.getSelectionModel().getSelectionEnd();
    if (Math.abs(esel - ssel) > 1) {
      int start;
      int end;
      // Forward selection
      if (offset == esel - 1) {
        start = ssel;
        end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, true);

        return new TextRange(start, end);
      }
      // Backward selection
      else {
        end = esel - 1;
        start = findSentenceRangeEnd(editor, chars, offset, max, -count, isOuter, true);

        return new TextRange(end, start);
      }
    }
    else {
      int end = findSentenceRangeEnd(editor, chars, offset, max, count, isOuter, false);

      boolean space = isOuter;
      if (Character.isSpaceChar(chars.charAt(end))) {
        space = false;
      }

      int start = findSentenceRangeEnd(editor, chars, offset, max, -1, space, false);

      return new TextRange(start, end);
    }
  }

  public static int findNextParagraph(Editor editor, int count, boolean allowBlanks) {
    int line = findNextParagraphLine(editor, count, allowBlanks);

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

  private static int findNextParagraph(Editor editor, int lline, int dir, boolean allowBlanks, boolean skipLines) {
    int line = findNextParagraphLine(editor, lline, dir, allowBlanks, skipLines);

    if (line >= 0) {
      return EditorHelper.getLineStartOffset(editor, line);
    }
    else {
      return dir > 0 ? EditorHelper.getFileSize(editor) - 1 : 0;
    }
  }

  private static int findNextParagraphLine(Editor editor, int count, boolean allowBlanks) {
    int line = editor.getCaretModel().getLogicalPosition().line;
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

  private static int findNextParagraphLine(Editor editor, int line, int dir, boolean allowBlanks, boolean skipLines) {
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

  private static int skipEmptyLines(Editor editor, int line, int dir, boolean allowBlanks) {
    int maxline = EditorHelper.getLineCount(editor);
    while (line >= 0 && line < maxline) {
      if (!EditorHelper.isLineEmpty(editor, line, allowBlanks)) {
        return line;
      }

      line += dir;
    }

    return line;
  }

  public static TextRange findParagraphRange(Editor editor, int count, boolean isOuter) {
    int line = editor.getCaretModel().getLogicalPosition().line;
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
        sline = findNextParagraphLine(editor, -1, true);
      }

      eline = findNextParagraphLine(editor, count, true);
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
      else if (!EditorHelper.isLineEmpty(editor, eline, true) && !EditorHelper.isLineEmpty(editor, sline, true) &&
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
        sline = findNextParagraphLine(editor, -1, true);
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

    return new TextRange(start, end);
  }

  public static int findMethodStart(Editor editor, int count) {
    return PsiHelper.findMethodStart(editor, editor.getCaretModel().getOffset(), count);
  }

  public static int findMethodEnd(Editor editor, int count) {
    return PsiHelper.findMethodEnd(editor, editor.getCaretModel().getOffset(), count);
  }

  private static String getPairChars() {
    if (pairsChars == null) {
      ListOption lo = (ListOption)Options.getInstance().getOption("matchpairs");
      pairsChars = parseOption(lo);

      lo.addOptionChangeListener(new OptionChangeListener() {
        public void valueChange(OptionChangeEvent event) {
          pairsChars = parseOption((ListOption)event.getOption());
        }
      });
    }

    return pairsChars;
  }

  private static String parseOption(ListOption option) {
    List<String> vals = option.values();
    StringBuffer res = new StringBuffer();
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

    private int count;
    private int position;
  }

  private static String pairsChars = null;
  private static String blockChars = "{}()[]<>";

  private static Logger logger = Logger.getInstance(SearchHelper.class.getName());
}
