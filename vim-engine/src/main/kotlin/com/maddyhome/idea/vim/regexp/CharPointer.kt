/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.regexp

import java.nio.CharBuffer
import java.util.*

@Deprecated("Remove once old regex engine is removed")
public class CharPointer {
  private var seq: CharSequence
  public var pointer: Int = 0
  private var readonly: Boolean

  public constructor(text: String) {
    seq = text
    readonly = true
  }

  public constructor(text: CharBuffer) {
    seq = text
    readonly = true
  }

  public constructor(text: StringBuffer) {
    seq = text
    readonly = false
  }

  private constructor(ptr: CharPointer, offset: Int) {
    seq = ptr.seq
    readonly = ptr.readonly
    pointer = ptr.pointer + offset
  }

  public fun pointer(): Int {
    return pointer
  }

  @JvmOverloads
  public fun set(ch: Char, offset: Int = 0): CharPointer {
    check(!readonly) { "readonly string" }
    val data = seq as StringBuffer
    while (pointer + offset >= data.length) {
      data.append('\u0000')
    }
    data.setCharAt(pointer + offset, ch)
    return this
  }

  public fun charAtInc(): Char {
    val res = charAt(0)
    inc()
    return res
  }

  @JvmOverloads
  public fun charAt(offset: Int = 0): Char {
    return if (end(offset)) {
      '\u0000'
    } else {
      seq[pointer + offset]
    }
  }

  @JvmOverloads
  public operator fun inc(cnt: Int = 1): CharPointer {
    pointer += cnt
    return this
  }

  @JvmOverloads
  public operator fun dec(cnt: Int = 1): CharPointer {
    pointer -= cnt
    return this
  }

  public fun assign(ptr: CharPointer): CharPointer {
    seq = ptr.seq
    pointer = ptr.pointer
    readonly = ptr.readonly
    return this
  }

  public fun ref(offset: Int): CharPointer {
    return CharPointer(this, offset)
  }

  public fun substring(len: Int): String {
    if (end()) return ""
    val start = pointer
    val end = normalize(pointer + len)
    return CharBuffer.wrap(seq, start, end).toString()
  }

  public fun strlen(): Int {
    if (end()) return 0
    for (i in pointer until seq.length) {
      if (seq[i] == '\u0000') {
        return i - pointer
      }
    }
    return seq.length - pointer
  }

  public fun strncmp(str: String, len: Int): Int {
    var len = len
    if (end()) return -1
    val s = CharBuffer.wrap(seq, pointer, normalize(pointer + len)).toString()
    if (len > str.length) {
      len = str.length
    }
    return s.compareTo(str.substring(0, len))
  }

  public fun strncmp(str: CharPointer, len: Int, ignoreCase: Boolean): Int {
    if (end()) return -1
    val cs1: CharSequence = CharBuffer.wrap(seq, pointer, normalize(pointer + len))
    val cs2: CharSequence = CharBuffer.wrap(str.seq, str.pointer, str.normalize(str.pointer + len))
    val l = cs1.length
    if (l != cs2.length) {
      return 1
    }
    for (i in 0 until l) {
      val c1 = cs1[i]
      val c2 = cs2[i]
      val notEqual = if (ignoreCase) {
        c1.lowercaseChar() != c2.lowercaseChar() &&
          c1.uppercaseChar() != c2.uppercaseChar()
      } else {
        c1 != c2
      }
      if (notEqual) return 1
    }
    return 0
  }

  public fun strchr(c: Char): CharPointer? {
    if (end()) {
      return null
    }
    val len = seq.length
    for (i in pointer until len) {
      val ch = seq[i]
      if (ch == '\u0000') {
        return null
      }
      if (ch == c) {
        return ref(i - pointer)
      }
    }
    return null
  }

  public fun istrchr(c: Char): CharPointer? {
    var c = c
    if (end()) {
      return null
    }
    val len = seq.length
    val cc = c.uppercaseChar()
    c = c.lowercaseChar()
    for (i in pointer until len) {
      val ch = seq[i]
      if (ch == '\u0000') {
        return null
      }
      if (ch == c || ch == cc) {
        return ref(i - pointer)
      }
    }
    return null
  }

  public val isNul: Boolean
    get() = charAt() == '\u0000'

  @JvmOverloads
  public fun end(offset: Int = 0): Boolean {
    return pointer + offset >= seq.length
  }

  public fun OP(): Int {
    return charAt().code
  }

  public fun OPERAND(): CharPointer {
    return ref(3)
  }

  public fun NEXT(): Int {
    return (seq[pointer + 1].code and 0xff shl 8) + (seq[pointer + 2].code and 0xff)
  }

  public fun OPERAND_MIN(): Int {
    return (seq[pointer + 3].code shl 24) +
      (seq[pointer + 4].code shl 16) +
      (seq[pointer + 5].code shl 8) +
      seq[pointer + 6].code
  }

  public fun OPERAND_MAX(): Int {
    return (seq[pointer + 7].code shl 24) +
      (seq[pointer + 8].code shl 16) +
      (seq[pointer + 9].code shl 8) +
      seq[pointer + 10].code
  }

  public fun OPERAND_CMP(): Char {
    return seq[pointer + 7]
  }

  public override fun equals(obj: Any?): Boolean {
    if (obj is CharPointer) {
      val ptr = obj
      return ptr.seq === seq && ptr.pointer == pointer
    }
    return false
  }

  public override fun hashCode(): Int {
    return Objects.hash(seq, pointer)
  }

  public fun skipWhitespaces() {
    while (CharacterClasses.isWhite(charAt())) inc()
  }

  public val digits: Int
    get() {
      var res = 0
      while (Character.isDigit(charAt())) {
        res = res * 10 + (charAt() - '0')
        inc()
      }
      return res
    }

  private fun normalize(pos: Int): Int {
    return Math.min(seq.length, pos)
  }

  public override fun toString(): String {
    return substring(strlen())
  }
}
