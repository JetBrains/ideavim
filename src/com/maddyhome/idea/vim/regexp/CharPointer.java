/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.regexp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.CharBuffer;
import java.util.Objects;


public class CharPointer {
  private @NotNull CharSequence seq;
  private int pointer;
  private boolean readonly;

  public CharPointer(@NotNull String text) {
    seq = text;
    readonly = true;
  }

  public CharPointer(@NotNull CharBuffer text) {
    seq = text;
    readonly = true;
  }

  public CharPointer(@NotNull StringBuffer text) {
    seq = text;
    readonly = false;
  }

  private CharPointer(@NotNull CharPointer ptr, int offset) {
    seq = ptr.seq;
    readonly = ptr.readonly;
    pointer = ptr.pointer + offset;
  }

  public int pointer() {
    return pointer;
  }

  public @NotNull CharPointer set(char ch) {
    return set(ch, 0);
  }

  public @NotNull CharPointer set(char ch, int offset) {
    if (readonly) {
      throw new IllegalStateException("readonly string");
    }

    StringBuffer data = (StringBuffer)seq;
    while (pointer + offset >= data.length()) {
      data.append('\u0000');
    }
    data.setCharAt(pointer + offset, ch);

    return this;
  }

  public char charAtInc() {
    char res = charAt(0);
    inc();

    return res;
  }

  public char charAt() {
    return charAt(0);
  }

  public char charAt(int offset) {
    if (end(offset)) {
      return '\u0000';
    }

    return seq.charAt(pointer + offset);
  }

  public @NotNull CharPointer inc() {
    return inc(1);
  }

  public @NotNull CharPointer inc(int cnt) {
    pointer += cnt;

    return this;
  }

  public @NotNull CharPointer dec() {
    return dec(1);
  }

  public @NotNull CharPointer dec(int cnt) {
    pointer -= cnt;

    return this;
  }

  public @NotNull CharPointer assign(@NotNull CharPointer ptr) {
    seq = ptr.seq;
    pointer = ptr.pointer;
    readonly = ptr.readonly;

    return this;
  }

  public @NotNull CharPointer ref(int offset) {
    return new CharPointer(this, offset);
  }

  public @NotNull String substring(int len) {
    if (end()) return "";

    int start = pointer;
    int end = normalize(pointer + len);
    return CharBuffer.wrap(seq, start, end).toString();
  }

  public int strlen() {
    if (end()) return 0;

    for (int i = pointer; i < seq.length(); i++) {
      if (seq.charAt(i) == '\u0000') {
        return i - pointer;
      }
    }

    return seq.length() - pointer;
  }

  public int strncmp(@NotNull String str, int len) {
    if (end()) return -1;

    String s = CharBuffer.wrap(seq, pointer, normalize(pointer + len)).toString();

    if (len > str.length()) {
      len = str.length();
    }

    return s.compareTo(str.substring(0, len));
  }

  public int strncmp(@NotNull CharPointer str, int len, boolean ignoreCase) {
    if (end()) return -1;

    CharSequence cs1 = CharBuffer.wrap(seq, pointer, normalize(pointer + len));
    CharSequence cs2 = CharBuffer.wrap(str.seq, str.pointer, str.normalize(str.pointer + len));

    int l = cs1.length();
    if (l != cs2.length()) {
      return 1;
    }

    for (int i = 0; i < l; i++) {
      char c1 = cs1.charAt(i);
      char c2 = cs2.charAt(i);

      final boolean notEqual = ignoreCase ? Character.toLowerCase(c1) != Character.toLowerCase(c2) &&
                                            Character.toUpperCase(c1) != Character.toUpperCase(c2) : c1 != c2;
      if (notEqual) return 1;
    }

    return 0;
  }

  public @Nullable CharPointer strchr(char c) {
    if (end()) {
      return null;
    }

    final int len = seq.length();
    for (int i = pointer; i < len; i++) {
      final char ch = seq.charAt(i);
      if (ch == '\u0000') {
        return null;
      }
      if (ch == c) {
        return ref(i - pointer);
      }
    }

    return null;
  }

  public @Nullable CharPointer istrchr(char c) {
    if (end()) {
      return null;
    }

    final int len = seq.length();
    final char cc = Character.toUpperCase(c);
    c = Character.toLowerCase(c);

    for (int i = pointer; i < len; i++) {
      final char ch = seq.charAt(i);
      if (ch == '\u0000') {
        return null;
      }
      if (ch == c || ch == cc) {
        return ref(i - pointer);
      }
    }

    return null;
  }

  public boolean isNul() {
    return charAt() == '\u0000';
  }

  public boolean end() {
    return end(0);
  }

  public boolean end(int offset) {
    return pointer + offset >= seq.length();
  }

  public int OP() {
    return charAt();
  }

  public @NotNull CharPointer OPERAND() {
    return ref(3);
  }

  public int NEXT() {
    return ((((int)seq.charAt(pointer + 1) & 0xff) << 8) + ((int)seq.charAt(pointer + 2) & 0xff));
  }

  public int OPERAND_MIN() {
    return (((int)seq.charAt(pointer + 3) << 24) +
            ((int)seq.charAt(pointer + 4) << 16) +
            ((int)seq.charAt(pointer + 5) << 8) +
            ((int)seq.charAt(pointer + 6)));
  }

  public int OPERAND_MAX() {
    return (((int)seq.charAt(pointer + 7) << 24) +
            ((int)seq.charAt(pointer + 8) << 16) +
            ((int)seq.charAt(pointer + 9) << 8) +
            ((int)seq.charAt(pointer + 10)));
  }

  public char OPERAND_CMP() {
    return seq.charAt(pointer + 7);
  }

  public boolean equals(Object obj) {
    if (obj instanceof CharPointer) {
      CharPointer ptr = (CharPointer)obj;
      return ptr.seq == seq && ptr.pointer == pointer;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(seq, pointer);
  }

  public void skipWhitespaces() {
    while (CharacterClasses.isWhite(charAt())) inc();
  }

  public int getDigits() {
    int res = 0;
    while (Character.isDigit(charAt())) {
      res = res * 10 + (charAt() - '0');
      inc();
    }

    return res;
  }

  private int normalize(int pos) {
    return Math.min(seq.length(), pos);
  }

  public @NotNull String toString() {
    return substring(strlen());
  }
}
