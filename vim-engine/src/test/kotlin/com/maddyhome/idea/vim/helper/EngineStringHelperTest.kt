/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineStringHelperTest {

  // --- toPrintableCharacter(codepoint) ---------------------------------------------------------

  @Test
  fun `test toPrintableCharacter encodes control characters as caret notation`() {
    assertEquals("^@", EngineStringHelper.toPrintableCharacter(0x00)) // NUL
    assertEquals("^A", EngineStringHelper.toPrintableCharacter(0x01))
    assertEquals("^I", EngineStringHelper.toPrintableCharacter(0x09)) // Tab
    assertEquals("^J", EngineStringHelper.toPrintableCharacter(0x0A)) // LF
    assertEquals("^M", EngineStringHelper.toPrintableCharacter(0x0D)) // CR
    assertEquals("^[", EngineStringHelper.toPrintableCharacter(0x1B)) // Esc
    assertEquals("^_", EngineStringHelper.toPrintableCharacter(0x1F))
    assertEquals("^?", EngineStringHelper.toPrintableCharacter(0x7F)) // DEL
  }

  @Test
  fun `test toPrintableCharacter leaves printable characters unchanged`() {
    assertEquals(" ", EngineStringHelper.toPrintableCharacter(0x20)) // space
    assertEquals("a", EngineStringHelper.toPrintableCharacter('a'.code))
    assertEquals("~", EngineStringHelper.toPrintableCharacter('~'.code))
    assertEquals("é", EngineStringHelper.toPrintableCharacter('é'.code))
  }

  @Test
  fun `test toPrintableCharacter Char delegates to codepoint`() {
    assertEquals("^M", EngineStringHelper.toPrintableCharacter(Char(0x0D)))
    assertEquals("a", EngineStringHelper.toPrintableCharacter('a'))
  }

  // --- toPrintableCharacter(KeyStroke) ---------------------------------------------------------

  @Test
  fun `test toPrintableCharacter KeyStroke`() {
    // Pressed Enter (no keyChar) is <CR> -> ^M
    assertEquals("^M", EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)))
    // Pressed Escape -> ^[
    assertEquals("^[", EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)))
    // A typed character is shown as itself
    assertEquals("a", EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke('a')))
    // Ctrl-A is an ASCII control character -> ^A
    assertEquals("^A", EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke('A'.code, InputEvent.CTRL_DOWN_MASK)))
    // Ctrl + a non-control key code falls back to ^<keyChar>
    assertEquals("^1", EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke('1'.code, InputEvent.CTRL_DOWN_MASK)))
  }

  // --- toPrintableCharacters(List<KeyStroke>) --------------------------------------------------

  @Test
  fun `test toPrintableCharacters from key strokes`() {
    assertEquals("", EngineStringHelper.toPrintableCharacters(emptyList<KeyStroke>()))
    assertEquals(
      "a^M",
      EngineStringHelper.toPrintableCharacters(
        listOf(KeyStroke.getKeyStroke('a'), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
      ),
    )
  }

  @Test
  fun `test toPrintableCharacters and fromPrintableCharacters round-trip`() {
    val raw = "iX" + Char(0x0D) + "Y" + Char(0x1B) + Char(0x09) + Char(0x00)
    val printable = raw.map { EngineStringHelper.toPrintableCharacter(it) }.joinToString(separator = "")
    assertEquals(raw, EngineStringHelper.fromPrintableCharacters(printable))
  }

  // --- isPrintableCharacter --------------------------------------------------------------------

  @Test
  fun `test isPrintableCharacter`() {
    assertTrue(EngineStringHelper.isPrintableCharacter('a'))
    assertTrue(EngineStringHelper.isPrintableCharacter(' '))
    assertTrue(EngineStringHelper.isPrintableCharacter('~'))
    assertFalse(EngineStringHelper.isPrintableCharacter(Char(0x01))) // control character
    assertFalse(EngineStringHelper.isPrintableCharacter(Char(0x1B))) // Esc
    assertFalse(EngineStringHelper.isPrintableCharacter(Char(0x7F))) // DEL
  }

  @Test
  fun `test containsControlCharacters`() {
    assertFalse(EngineStringHelper.containsControlCharacters(""))
    assertFalse(EngineStringHelper.containsControlCharacters("hello world"))
    // Tabs and newlines are ordinary text, not "weird" control characters
    assertFalse(EngineStringHelper.containsControlCharacters("line1" + Char(0x0A) + "line2" + Char(0x09) + "x"))
    assertTrue(EngineStringHelper.containsControlCharacters("a" + Char(0x1B) + "b")) // Esc
    assertTrue(EngineStringHelper.containsControlCharacters("a" + Char(0x0D))) // CR
    assertTrue(EngineStringHelper.containsControlCharacters(Char(0x00).toString())) // NUL
  }

  @Test
  fun `test controlTokenRanges`() {
    assertEquals(emptyList<IntRange>(), EngineStringHelper.controlTokenRanges("hello"))
    assertEquals(listOf(2..3), EngineStringHelper.controlTokenRanges("ab^Mcd")) // ^M spans index 2..3
    assertEquals(listOf(0..1, 3..4), EngineStringHelper.controlTokenRanges("^MX^[")) // ^M and ^[
    assertEquals(listOf(1..6), EngineStringHelper.controlTokenRanges("a<200b>")) // <200b> spans 1..6
    assertEquals(emptyList<IntRange>(), EngineStringHelper.controlTokenRanges("^1 <nothex>")) // no valid tokens
  }

  // --- fromPrintableCharacters -----------------------------------------------------------------

  @Test
  fun `test fromPrintableCharacters decodes caret notation`() {
    assertEquals(Char(0x00).toString(), EngineStringHelper.fromPrintableCharacters("^@")) // NUL
    assertEquals(Char(0x01).toString(), EngineStringHelper.fromPrintableCharacters("^A")) // Ctrl-A
    assertEquals(Char(0x09).toString(), EngineStringHelper.fromPrintableCharacters("^I")) // Tab
    assertEquals(Char(0x0A).toString(), EngineStringHelper.fromPrintableCharacters("^J")) // LF
    assertEquals(Char(0x0D).toString(), EngineStringHelper.fromPrintableCharacters("^M")) // CR / Enter
    assertEquals(Char(0x1B).toString(), EngineStringHelper.fromPrintableCharacters("^[")) // Esc
    assertEquals(Char(0x1F).toString(), EngineStringHelper.fromPrintableCharacters("^_"))
    assertEquals(Char(0x7F).toString(), EngineStringHelper.fromPrintableCharacters("^?")) // DEL
  }

  @Test
  fun `test fromPrintableCharacters decodes hex escapes`() {
    assertEquals(Char(0x200B).toString(), EngineStringHelper.fromPrintableCharacters("<200b>")) // zero-width space
    assertEquals(Char(0x85).toString(), EngineStringHelper.fromPrintableCharacters("<85>"))
  }

  @Test
  fun `test fromPrintableCharacters leaves printable text unchanged`() {
    assertEquals("hello world", EngineStringHelper.fromPrintableCharacters("hello world"))
  }

  @Test
  fun `test fromPrintableCharacters decodes a mixed macro`() {
    assertEquals("iX" + Char(0x0D) + "Y" + Char(0x1B), EngineStringHelper.fromPrintableCharacters("iX^MY^["))
    assertEquals(":d" + Char(0x0D), EngineStringHelper.fromPrintableCharacters(":d^M"))
  }

  @Test
  fun `test fromPrintableCharacters keeps ambiguous caret and angle brackets literal`() {
    assertEquals("^1", EngineStringHelper.fromPrintableCharacters("^1")) // '1' is not a caret token
    assertEquals("a^", EngineStringHelper.fromPrintableCharacters("a^")) // trailing caret
    assertEquals("<nothex>", EngineStringHelper.fromPrintableCharacters("<nothex>"))
    assertEquals("a < b", EngineStringHelper.fromPrintableCharacters("a < b"))
  }

  @Test
  fun `test fromPrintableCharacters is the inverse of toPrintableCharacter for control characters`() {
    for (codepoint in (0..31) + 127) {
      val printable = EngineStringHelper.toPrintableCharacter(codepoint)
      assertEquals(
        codepoint.toChar().toString(),
        EngineStringHelper.fromPrintableCharacters(printable),
        "round-trip failed for codepoint $codepoint (printable=$printable)",
      )
    }
  }

  // --- String extensions -----------------------------------------------------------------------

  @Test
  fun `test removeAsciiColorCodes`() {
    val esc = Char(0x1B)
    assertEquals("red", (esc + "[31m" + "red" + esc + "[0m").removeAsciiColorCodes())
    assertEquals("plain text", "plain text".removeAsciiColorCodes())
    assertEquals("ab", (esc + "[1;32m" + "a" + esc + "[0m" + "b").removeAsciiColorCodes())
  }

  @Test
  fun `test indexOfOrNull`() {
    assertEquals(0, "abc".indexOfOrNull('a'))
    assertEquals(1, "abc".indexOfOrNull('b'))
    assertNull("abc".indexOfOrNull('z'))
    assertEquals(3, "abcab".indexOfOrNull('a', 1)) // search starts at index 1
    assertNull("abc".indexOfOrNull('a', 1)) // no 'a' at or after index 1
  }

  @Test
  fun `test lastIndexOfOrNull`() {
    assertEquals(3, "abcab".lastIndexOfOrNull('a'))
    assertEquals(4, "abcab".lastIndexOfOrNull('b'))
    assertNull("abc".lastIndexOfOrNull('z'))
    assertEquals(0, "abcab".lastIndexOfOrNull('a', endIndex = 3)) // only search [0, 3)
    assertNull("abc".lastIndexOfOrNull('a', startIndex = 1)) // 'a' is before startIndex
    assertNull("abc".lastIndexOfOrNull('a', endIndex = 10)) // out-of-bounds endIndex
  }
}
