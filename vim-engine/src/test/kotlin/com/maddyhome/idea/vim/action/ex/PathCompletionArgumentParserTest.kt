/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.api.VimEditor
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

/**
 * Covers the pure half of `%` expansion: splitting an argument into modifiers plus a literal suffix, and the
 * modifiers that work on a plain string. Resolving `%` itself and `:p` needs a real buffer, so those are covered by
 * the IDE-level tests instead (see `CmdFilterCommandTest` and `CommandLineCompletionTest`).
 */
class PathCompletionArgumentParserTest {

  // Parsing: which modifiers, and what is left over as a literal suffix

  @Test
  fun `bare percent has no modifiers and no suffix`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%")
    assertEquals(emptyList(), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent p parses a single p modifier`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:p")
    assertEquals(listOf("p"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent h parses a single h modifier`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:h")
    assertEquals(listOf("h"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent h slash separates modifier from suffix`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:h/")
    assertEquals(listOf("h"), modifiers.map { it.modifier() })
    assertEquals("/", suffix)
  }

  @Test
  fun `percent h slash prefix separates modifier from suffix with prefix`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:h/new_")
    assertEquals(listOf("h"), modifiers.map { it.modifier() })
    assertEquals("/new_", suffix)
  }

  @Test
  fun `percent t parses a single t modifier`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:t")
    assertEquals(listOf("t"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent r parses a single r modifier`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:r")
    assertEquals(listOf("r"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent e parses a single e modifier`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:e")
    assertEquals(listOf("e"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `unknown modifier is left as a literal suffix`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:zz")
    assertEquals(emptyList(), modifiers.map { it.modifier() })
    assertEquals(":zz", suffix)
  }

  // Chaining

  @Test
  fun `percent p h chains correctly`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:p:h")
    assertEquals(listOf("p", "h"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent t r chains tail then root`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:t:r")
    assertEquals(listOf("t", "r"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  @Test
  fun `percent p h h chains two head steps`() {
    val (modifiers, suffix) = PathCompletionArgumentParser.parse("%:p:h:h")
    assertEquals(listOf("p", "h", "h"), modifiers.map { it.modifier() })
    assertEquals("", suffix)
  }

  // PathCompletion.complete behaviour

  @Test
  fun `TailFilename extracts filename from absolute path`() {
    assertEquals("foo.py", TailFilename().complete("/home/user/project/src/foo.py", mockEditor()))
  }

  @Test
  fun `TailFilename returns full string when no slash present`() {
    assertEquals("foo.py", TailFilename().complete("foo.py", mockEditor()))
  }

  @Test
  fun `HeadDirectory extracts directory from absolute path`() {
    assertEquals("/home/user/project/src", HeadDirectory().complete("/home/user/project/src/foo.py", mockEditor()))
  }

  @Test
  fun `HeadDirectory returns dot for a relative name with no directory`() {
    // Vim turns an empty head into "." so that "%:h/other.txt" still names a sibling file
    assertEquals(".", HeadDirectory().complete("foo.py", mockEditor()))
  }

  @Test
  fun `HeadDirectory keeps the root separator for a file at the filesystem root`() {
    assertEquals("/", HeadDirectory().complete("/foo.py", mockEditor()))
  }

  @Test
  fun `HeadDirectory keeps a relative directory relative`() {
    assertEquals("src/main", HeadDirectory().complete("src/main/foo.py", mockEditor()))
  }

  @Test
  fun `RootFilename strips last extension from absolute path`() {
    assertEquals("/home/user/src/foo", RootFilename().complete("/home/user/src/foo.py", mockEditor()))
  }

  @Test
  fun `RootFilename strips only last extension when multiple dots present`() {
    assertEquals("/path/to/archive.tar", RootFilename().complete("/path/to/archive.tar.gz", mockEditor()))
  }

  @Test
  fun `RootFilename returns path unchanged when no extension`() {
    assertEquals("/path/to/Makefile", RootFilename().complete("/path/to/Makefile", mockEditor()))
  }

  @Test
  fun `ExtensionOnly returns extension without dot`() {
    assertEquals("py", ExtensionOnly().complete("/home/user/src/foo.py", mockEditor()))
  }

  @Test
  fun `ExtensionOnly returns last extension when multiple dots`() {
    assertEquals("gz", ExtensionOnly().complete("/path/to/archive.tar.gz", mockEditor()))
  }

  @Test
  fun `ExtensionOnly returns empty string when no extension`() {
    assertEquals("", ExtensionOnly().complete("/path/to/Makefile", mockEditor()))
  }

  @Test
  fun `percent t r chained complete gives filename without extension`() {
    val editor = mockEditor()
    val tail = TailFilename().complete("/home/user/src/foo.py", editor) // "foo.py"
    assertEquals("foo", RootFilename().complete(tail, editor))
  }

  @Test
  fun `percent p h h chained complete gives grandparent directory`() {
    val editor = mockEditor()
    val head1 = HeadDirectory().complete("/home/user/project/src/foo.py", editor) // "/home/user/project/src"
    assertEquals("/home/user/project", HeadDirectory().complete(head1, editor))
  }

  private fun mockEditor(): VimEditor = mock()
}
