/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class CreateConstructorTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testSimpleConstructor() {
    configureByJavaText(
      """
      class MyClass {
          private int ${c}value;
      }
      """.trimIndent(),
    )
    enterCommand("action GenerateConstructor")
    assertState(
      """
      class MyClass {
          public MyClass(int value) ${c}{
              this.value = value;
          }

          private int value;
      }
      """.trimIndent(),
    )
  }
}
