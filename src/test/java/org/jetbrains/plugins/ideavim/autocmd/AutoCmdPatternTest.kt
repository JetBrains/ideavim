/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.autocmd

import com.maddyhome.idea.vim.autocmd.AutoCmdPattern
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoCmdPatternTest {

  @Test
  fun `star matches any file`() {
    assertTrue(AutoCmdPattern("*").matches("/path/to/file.txt"))
  }

  @Test
  fun `star matches null path`() {
    assertTrue(AutoCmdPattern("*").matches(null))
  }

  @Test
  fun `non-star pattern does not match null path`() {
    assertFalse(AutoCmdPattern("*.py").matches(null))
  }

  @Test
  fun `extension pattern matches correct extension`() {
    assertTrue(AutoCmdPattern("*.py").matches("/path/to/script.py"))
  }

  @Test
  fun `extension pattern does not match wrong extension`() {
    assertFalse(AutoCmdPattern("*.py").matches("/path/to/script.txt"))
  }

  @Test
  fun `extension pattern matches file name only`() {
    assertTrue(AutoCmdPattern("*.py").matches("/some/deep/path/test.py"))
  }

  @Test
  fun `brace alternation matches first option`() {
    assertTrue(AutoCmdPattern("*.{py,txt}").matches("/path/to/file.py"))
  }

  @Test
  fun `brace alternation matches second option`() {
    assertTrue(AutoCmdPattern("*.{py,txt}").matches("/path/to/file.txt"))
  }

  @Test
  fun `brace alternation does not match unlisted extension`() {
    assertFalse(AutoCmdPattern("*.{py,txt}").matches("/path/to/file.kt"))
  }

  @Test
  fun `question mark matches single character`() {
    assertTrue(AutoCmdPattern("?.txt").matches("/path/to/a.txt"))
  }

  @Test
  fun `question mark does not match multiple characters`() {
    assertFalse(AutoCmdPattern("?.txt").matches("/path/to/ab.txt"))
  }

  @Test
  fun `exact filename matches`() {
    assertTrue(AutoCmdPattern("Makefile").matches("/path/to/Makefile"))
  }

  @Test
  fun `exact filename does not match different name`() {
    assertFalse(AutoCmdPattern("Makefile").matches("/path/to/Rakefile"))
  }

  @Test
  fun `pattern with path matches full path`() {
    assertTrue(AutoCmdPattern("/home/user/*.py").matches("/home/user/script.py"))
  }

  @Test
  fun `pattern with path does not match different directory`() {
    assertFalse(AutoCmdPattern("/home/user/*.py").matches("/other/path/script.py"))
  }

  @Test
  fun `double star matches across directories`() {
    assertTrue(AutoCmdPattern("**/*.py").matches("/some/deep/path/script.py"))
  }

  @Test
  fun `star does not match path separators`() {
    assertFalse(AutoCmdPattern("src/*.py").matches("src/sub/script.py"))
  }

  @Test
  fun `double star matches path separators`() {
    assertTrue(AutoCmdPattern("src/**/*.py").matches("src/sub/script.py"))
  }

  @Test
  fun `bracket character class matches`() {
    assertTrue(AutoCmdPattern("*.[ch]").matches("/path/to/file.c"))
    assertTrue(AutoCmdPattern("*.[ch]").matches("/path/to/file.h"))
  }

  @Test
  fun `bracket character class does not match unlisted`() {
    assertFalse(AutoCmdPattern("*.[ch]").matches("/path/to/file.o"))
  }

  @Test
  fun `dot in extension is escaped properly`() {
    assertFalse(AutoCmdPattern("*.py").matches("/path/to/file_py"))
  }

  @Test
  fun `prefix pattern matches`() {
    assertTrue(AutoCmdPattern("test*").matches("/path/to/test_file.py"))
  }

  @Test
  fun `prefix pattern does not match different prefix`() {
    assertFalse(AutoCmdPattern("test*").matches("/path/to/prod_file.py"))
  }

  @Test
  fun `multiple extensions with brace`() {
    val pattern = AutoCmdPattern("*.{c,h,cpp,hpp}")
    assertTrue(pattern.matches("/path/to/main.cpp"))
    assertTrue(pattern.matches("/path/to/main.h"))
    assertFalse(pattern.matches("/path/to/main.py"))
  }

  @Test
  fun `simple filename without extension`() {
    assertTrue(AutoCmdPattern("*").matches("/path/to/Makefile"))
  }
}
