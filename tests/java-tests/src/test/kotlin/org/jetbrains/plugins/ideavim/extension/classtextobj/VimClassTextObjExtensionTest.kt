/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.classtextobj

import com.intellij.ide.highlighter.JavaFileType
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimClassTextObjExtensionTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("classtextobj")
  }

  @Test
  fun `test dac deletes whole class definition`() {
    doTest(
      "dac",
      """
        public class Foo {
            public void bar() {
                int ${c}x = 1;
            }
        }
      """.trimIndent(),
      "$c",
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test vac selects whole class definition`() {
    doTest(
      "vac",
      """
        public class ${c}Foo {
            public void bar() {
                int x = 1;
            }
        }
      """.trimIndent(),
      """
        ${s}public class Foo {
            public void bar() {
                int x = 1;
            }
        $c}$se
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test vac with nested classes selects innermost containing class`() {
    doTest(
      "vac",
      """
        public class Outer {
            public class Inner {
                public void ba${c}r() {
                    int x = 1;
                }
            }
        }
      """.trimIndent(),
      """
        public class Outer {
            ${s}public class Inner {
                public void bar() {
                    int x = 1;
                }
            $c}$se
        }
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test dac from cursor on class keyword deletes class`() {
    doTest(
      "dac",
      """
        cla${c}ss Foo {
            int x;
        }
      """.trimIndent(),
      "$c",
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test ac on caret outside any class does nothing`() {
    doTest(
      "dac",
      """
        $c// just a comment
      """.trimIndent(),
      """
        $c// just a comment
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }
}
