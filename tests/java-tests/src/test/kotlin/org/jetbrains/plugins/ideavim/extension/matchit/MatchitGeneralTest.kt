/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.extension.matchit

import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchitJavaTest : VimJavaTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  /*
   * Tests to make sure we didn't break the default % motion
   */

  @Test
  fun `test jump from Java comment start to end`() {
    doTest(
      "%",
      """
        /$c**
         *
         */
      """.trimIndent(),
      """
        /**
         *
         *$c/
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from Java comment end to start`() {
    doTest(
      "%",
      """
        /**
         *
         *$c/
      """.trimIndent(),
      """
        $c/**
         *
         */
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }
}
