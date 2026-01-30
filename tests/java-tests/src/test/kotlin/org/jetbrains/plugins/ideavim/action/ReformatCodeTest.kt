/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IdeaVim uses IDE code formatter instead of Vim's text formatting based on textwidth",
)
class ReformatCodeTest : VimJavaTestCase() {
  @Test
  fun testEmpty() {
    configureByJavaText(c)
    typeText(injector.parser.parseKeys("gqq"))
    assertState(c)
  }

  @Test
  fun testWithCount() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      	int d;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("2gqq"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
          int c;
      	int d;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testWithUpMotion() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int b;
      	int ${c}c;
      	int d;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("gqk"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
          int c;
      	int d;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testWithRightMotion() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("gql"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
      	int c;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testWithTextObject() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("gqi{"))
    assertState(
      """
      class C {
          ${c}int a;
          int b;
          int c;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testWithCountsAndDownMotion() {
    configureByJavaText(
      """
      class C {
      	int ${c}a;
      	int b;
      	int c;
      	int d;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("2gqj"))
    assertState(
      """
      class C {
          ${c}int a;
          int b;
          int c;
      	int d;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testVisual() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vlgq"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
      	int c;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testLinewiseVisual() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vlgq"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
      	int c;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualMultiline() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      	int d;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vjgq"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
          int c;
      	int d;
      }
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlock() {
    configureByJavaText(
      """
      class C {
      	int a;
      	int ${c}b;
      	int c;
      	int d;
      }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("<C-V>jgq"))
    assertState(
      """
      class C {
      	int a;
          ${c}int b;
          int c;
      	int d;
      }
      """.trimIndent(),
    )
  }
}
