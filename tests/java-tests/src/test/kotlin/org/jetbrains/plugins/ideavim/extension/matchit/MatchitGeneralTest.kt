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

  @Test
  fun `jump from if to else`() {
    doTest(
      "%",
      """
         ${c}if (true) {
         } else {
         }
      """.trimIndent(),
      """
         if (true) {
         } ${c}else {
         }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `just from else to if`() {
    doTest(
      "%",
      """
         if (true) {
         } ${c}else {
         }
      """.trimIndent(),
      """
         ${c}if (true) {
         } else {
         }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `just from if to if else`() {
    doTest(
      "%",
      """
         ${c}if (true) {
         } else if(false) {
         }
      """.trimIndent(),
      """
         if (true) {
         } ${c}else if(false) {
         }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `just from if to if else to else`() {
    doTest(
      "%%",
      """
         ${c}if (true) {
         } else if(false) {
         } else {
         }
      """.trimIndent(),
      """
         if (true) {
         } else if(false) {
         } ${c}else {
         }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `just from do to while`() {
    doTest(
      "%",
      """
          ${c}do {
            System.out.println("Hey");
          } while (true);
      """.trimIndent(),
      """
          do {
            System.out.println("Hey");
          } ${c}while (true);
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `jump from while to do`() {
    doTest(
      "%",
      """
          do {
            System.out.println("Hey");
          } ${c}while (true);
      """.trimIndent(),
      """
          ${c}do {
            System.out.println("Hey");
          } while (true);
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `jump from try to catch`() {
    doTest(
      "%",
      """
          ${c}try {
            System.out.println("Hey");
          } catch (Exception ex) {
            System.out.println("Failed");
          }
      """.trimIndent(),
      """
          try {
            System.out.println("Hey");
          } ${c}catch (Exception ex) {
            System.out.println("Failed");
          }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `jump from try to finally`() {
    doTest(
      "%",
      """
          ${c}try {
            System.out.println("Hey");
          } finally {
            System.out.println("Failed");
          }
      """.trimIndent(),
      """
          try {
            System.out.println("Hey");
          } ${c}finally {
            System.out.println("Failed");
          }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `jump from try to catch then finally`() {
    doTest(
      "%%",
      """
          ${c}try {
            System.out.println("Hey");
          } catch (Exception ex) {
            System.out.println("SomeIssue");
          } finally {
            System.out.println("Failed");
          }
      """.trimIndent(),
      """
          try {
            System.out.println("Hey");
          } catch (Exception ex) {
            System.out.println("SomeIssue");
          } ${c}finally {
            System.out.println("Failed");
          }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }
}
