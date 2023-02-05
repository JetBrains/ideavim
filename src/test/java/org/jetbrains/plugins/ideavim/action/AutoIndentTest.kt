/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class AutoIndentTest : VimTestCase() {
  // VIM-256 |==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testCaretPositionAfterAutoIndent() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
}
"""
    )
    typeText(injector.parser.parseKeys("=="))
    assertState(
      """class C {
   int a;
    <caret>int b;
   int c;
}
"""
    )
  }

  // |2==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testAutoIndentWithCount() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
   int d;
}
"""
    )
    typeText(injector.parser.parseKeys("2=="))
    assertState(
      """class C {
   int a;
    <caret>int b;
    int c;
   int d;
}
"""
    )
  }

  // |=k|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testAutoIndentWithUpMotion() {
    configureByJavaText(
      """class C {
   int a;
   int b;
   int <caret>c;
   int d;
}
"""
    )
    typeText(injector.parser.parseKeys("=k"))
    assertState(
      """class C {
   int a;
    <caret>int b;
    int c;
   int d;
}
"""
    )
  }

  // |=l|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testAutoIndentWithRightMotion() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
}
"""
    )
    typeText(injector.parser.parseKeys("=l"))
    assertState(
      """class C {
   int a;
    <caret>int b;
   int c;
}
"""
    )
  }

  // |2=j|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testAutoIndentWithCountsAndDownMotion() {
    configureByJavaText(
      """class C {
   int <caret>a;
   int b;
   int c;
   int d;
}
"""
    )
    typeText(injector.parser.parseKeys("2=j"))
    assertState(
      """class C {
    <caret>int a;
    int b;
    int c;
   int d;
}
"""
    )
  }

  // |v| |l| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testVisualAutoIndent() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
}
"""
    )
    typeText(injector.parser.parseKeys("v" + "l" + "="))
    assertState(
      """class C {
   int a;
    <caret>int b;
   int c;
}
"""
    )
  }

  // |v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testVisualMultilineAutoIndent() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
   int d;
}
"""
    )
    typeText(injector.parser.parseKeys("v" + "j" + "="))
    assertState(
      """class C {
   int a;
    <caret>int b;
    int c;
   int d;
}
"""
    )
  }

  // |C-v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testVisualBlockAutoIndent() {
    configureByJavaText(
      """class C {
   int a;
   int <caret>b;
   int c;
   int d;
}
"""
    )
    typeText(injector.parser.parseKeys("<C-V>" + "j" + "="))
    assertState(
      """class C {
   int a;
    <caret>int b;
    int c;
   int d;
}
"""
    )
  }
}
