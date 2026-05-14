/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.functextobj

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
class VimFuncTextObjExtensionTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("functextobj")
  }

  @Test
  fun `test dam deletes method without javadoc and annotations`() {
    doTest(
      "dam",
      """
        public class Foo {
            /**
             * doc
             */
            @Override
            public void bar() {
                int x = 1${c};
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            /**
             * doc
             */
            @Override
            $c
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test daM deletes method including javadoc and annotations`() {
    doTest(
      "daM",
      """
        public class Foo {
            /**
             * doc
             */
            @Override
            public void bar() {
                int x = 1${c};
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            $c
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test dim deletes only method body content`() {
    doTest(
      "dim",
      """
        public class Foo {
            public void bar() {
                int x = ${c}1;
                int y = 2;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            public void bar() {$c}
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test vam selects whole method definition`() {
    doTest(
      "vam",
      """
        public class Foo {
            public void ${c}bar() {
                int x = 1;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            ${s}public void bar() {
                int x = 1;
            $c}$se
        }
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test cam puts caret in insert mode after deleting method`() {
    doTest(
      "cam",
      """
        public class Foo {
            public void bar() {
                int ${c}x = 1;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            $c
        }
      """.trimIndent(),
      Mode.INSERT,
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test dam from within signature deletes the method`() {
    doTest(
      "dam",
      """
        public class Foo {
            public void ba${c}r() {
                int x = 1;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            $c
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test dim deletes only inner body keeping signature and braces`() {
    doTest(
      "dim",
      """
        public class Foo {
            public void bar() {
                int x = ${c}1;
                int y = 2;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            public void bar() {$c}
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test daM on annotation-only method deletes everything including annotation`() {
    doTest(
      "daM",
      """
        public class Foo {
            @Override
            public void ${c}bar() {
                int x = 1;
            }
        }
      """.trimIndent(),
      """
        public class Foo {
            $c
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }

  @Test
  fun `test am on caret outside any method does nothing`() {
    doTest(
      "dam",
      """
        ${c}public class Foo {
            public void bar() {
                int x = 1;
            }
        }
      """.trimIndent(),
      """
        ${c}public class Foo {
            public void bar() {
                int x = 1;
            }
        }
      """.trimIndent(),
      fileType = JavaFileType.INSTANCE,
    )
  }
}
