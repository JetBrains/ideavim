/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.regexp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.CharBuffer;

/**
 *
 */
public class CharPointer {
  public static final CharPointer INIT = new CharPointer();

  public CharPointer(String text) {
    seq = text;
    readonly = true;
  }

  public CharPointer(CharBuffer text) {
    seq = text;
    readonly = true;
  }

  public CharPointer(StringBuffer text) {
    seq = text;
    readonly = false;
  }

  private CharPointer(@NotNull CharPointer ptr, int offset) {
    seq = ptr.seq;
    readonly = ptr.readonly;
    pointer = ptr.pointer + offset;
  }

  private CharPointer() {
    seq = null;
    pointer = -1;
    readonly = true;
  }

  public int pointer() {
    return pointer;
  }

  public boolean isInit() {
    return seq == null && pointer == -1;
  }

  @NotNull
  public CharPointer set(char ch) {
    return set(ch, 0);
  }

  @NotNull
  public CharPointer set(char ch, int offset) {
    if (seq == null) {
      return this;
    }

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

  @NotNull
  public CharPointer inc() {
    return inc(1);
  }

  @NotNull
  public CharPointer inc(int cnt) {
    pointer += cnt;

    return this;
  }

  @NotNull
  public CharPointer dec() {
    return dec(1);
  }

  @NotNull
  public CharPointer dec(int cnt) {
    pointer -= cnt;

    return this;
  }

  @NotNull
  public CharPointer assign(@NotNull CharPointer ptr) {
    seq = ptr.seq;
    pointer = ptr.pointer;
    readonly = ptr.readonly;

    return this;
  }

  @NotNull
  public CharPointer ref(int offset) {
    if (this.equals(INIT)) {
      return INIT;
    }

    return new CharPointer(this, offset);
  }

  @NotNull
  public String substring(int len) {
    if (end()) {
      return "";
    }
    else {
      int start = pointer;
      int end = normalize(pointer + len);
      int slen = seq.length();
      //return seq.subSequence(start, end - start).toString();
      return CharBuffer.wrap(seq, start, end).toString();
    }
  }

  public int strlen() {
    if (end()) {
      return 0;
    }

    for (int i = pointer; i < seq.length(); i++) {
      if (seq.charAt(i) == '\u0000') {
        return i - pointer;
      }
    }

    return seq.length() - pointer;
  }

  public int strncmp(@NotNull String str, int len) {
    if (end()) {
      return -1;
    }

    //String s = seq.subSequence(pointer, normalize(pointer + len)).toString();
    String s = CharBuffer.wrap(seq, pointer, normalize(pointer + len)).toString();

    if (len > str.length()) {
      len = str.length();
    }

    return s.compareTo(str.substring(0, len));
  }

  public int strncmp(@NotNull CharPointer str, int len) {
    if (end()) {
      return -1;
    }

    //CharSequence cs1 = seq.subSequence(pointer, normalize(pointer + len));
    //CharSequence cs2 = str.seq.subSequence(str.pointer, str.normalize(str.pointer + len));
    CharSequence cs1 = CharBuffer.wrap(seq, pointer, normalize(pointer + len));
    CharSequence cs2 = CharBuffer.wrap(str.seq, str.pointer, str.normalize(str.pointer + len));

    int l = cs1.length();
    if (l != cs2.length()) {
      return 1;
    }

    for (int i = 0; i < l; i++) {
      char c1 = cs1.charAt(i);
      char c2 = cs2.charAt(i);

      if (c1 != c2) {
        return 1;
      }
    }

    return 0;

    /*
    int slen = str.strlen();
    if (len > slen)
    {
        len = slen;
    }

    String s = seq.subSequence(pointer, normalize(pointer + len)).toString();

    return s.compareTo(str.substring(len));
    */
  }

  public int strnicmp(@NotNull CharPointer str, int len) {
    if (end()) {
      return -1;
    }

    //CharSequence cs1 = seq.subSequence(pointer, normalize(pointer + len));
    //CharSequence cs2 = str.seq.subSequence(str.pointer, str.normalize(str.pointer + len));
    CharSequence cs1 = CharBuffer.wrap(seq, pointer, normalize(pointer + len));
    CharSequence cs2 = CharBuffer.wrap(str.seq, str.pointer, str.normalize(str.pointer + len));

    int l = cs1.length();
    if (l != cs2.length()) {
      return 1;
    }

    for (int i = 0; i < l; i++) {
      char c1 = cs1.charAt(i);
      char c2 = cs2.charAt(i);

      if (Character.toLowerCase(c1) != Character.toLowerCase(c2) &&
          Character.toUpperCase(c1) != Character.toUpperCase(c2)) {
        return 1;
      }
    }

    return 0;

    /* was 2,407ms
    int slen = str.strlen();
    if (len > slen)
    {
        len = slen;
    }

    String s = seq.subSequence(pointer, normalize(pointer + len)).toString();

    return s.compareToIgnoreCase(str.substring(len));
    */
  }

  @Nullable
  public CharPointer strchr(char c) {
    if (end()) {
      return null;
    }

    int len = seq.length();
    for (int i = pointer; i < len; i++) {
      if (seq.charAt(i) == c) {
        return ref(i - pointer);
      }
    }

    return null;

    /*
    String str = seq.subSequence(pointer, pointer + strlen()).toString();
    int pos = str.indexOf(c);
    if (pos != -1)
    {
        return ref(pos);
    }
    else
    {
        return null;
    }
    */
  }

  @Nullable
  public CharPointer istrchr(char c) {
    if (end()) {
      return null;
    }

    int len = seq.length();
    char cc = Character.toUpperCase(c);
    c = Character.toLowerCase(c);

    for (int i = pointer; i < len; i++) {
      char ch = seq.charAt(i);
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
    if (seq == null) {
      return true;
    }

    return pointer + offset >= seq.length();
  }

  public int OP() {
    return (int)charAt();
  }

  @NotNull
  public CharPointer OPERAND() {
    return ref(3);
  }

  public int NEXT() {
//    #define NEXT(p)         (((*((p) + 1) & 0377) << 8) + (*((p) + 2) & 0377))
    return ((((int)seq.charAt(pointer + 1) & 0xff) << 8) + ((int)seq.charAt(pointer + 2) & 0xff));
  }

  public int OPERAND_MIN() {
//    #define OPERAND_MIN(p)  (((long)(p)[3] << 24) + ((long)(p)[4] << 16) \
//    + ((long)(p)[5] << 8) + (long)(p)[6])
    return (((int)seq.charAt(pointer + 3) << 24) + ((int)seq.charAt(pointer + 4) << 16) +
            ((int)seq.charAt(pointer + 5) << 8) + ((int)seq.charAt(pointer + 6)));
  }

  public int OPERAND_MAX() {
    return (((int)seq.charAt(pointer + 7) << 24) + ((int)seq.charAt(pointer + 8) << 16) +
            ((int)seq.charAt(pointer + 9) << 8) + ((int)seq.charAt(pointer + 10)));
  }

  public char OPERAND_CMP() {
//    #define OPERAND_CMP(p)  (p)[7]
    return seq.charAt(pointer + 7);
  }

  public boolean equals(Object obj) {
    if (obj instanceof CharPointer) {
      CharPointer ptr = (CharPointer)obj;
      if (ptr.seq == seq && ptr.pointer == pointer) {
        return true;
      }
    }

    return false;
  }

  private int normalize(int pos) {
    return Math.min(seq.length(), pos);
  }

  @NotNull
  public String toString() {
    return substring(strlen());
  }

  @Nullable private CharSequence seq;
  private int pointer;
  private boolean readonly = true;
}
