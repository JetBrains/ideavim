/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class MotionPercentOrMatchActionJavaTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  @Test
  fun `test percent match java comment start`() {
    configureByJavaText("/$c* foo */")
    typeText("%")
    assertState("/* foo *$c/")
  }

  @Test
  fun `test percent doesnt match partial java comment`() {
    configureByJavaText("$c/* ")
    typeText("%")
    assertState("$c/* ")
  }

  @Test
  fun `test percent match java comment end`() {
    configureByJavaText("/* foo $c*/")
    typeText("%")
    assertState("$c/* foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test percent match java doc comment start`() {
    configureByJavaText("/*$c* foo */")
    typeText("%")
    assertState("/** foo *$c/")
  }

  @Test
  fun `test percent match java doc comment end`() {
    configureByJavaText("/** foo *$c/")
    typeText("%")
    assertState("$c/** foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  @Test
  fun `test percent doesnt match after comment start`() {
    configureByJavaText("/*$c foo */")
    typeText("%")
    assertState("/*$c foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  @Test
  fun `test percent doesnt match before comment end`() {
    configureByJavaText("/* foo $c */")
    typeText("%")
    assertState("/* foo $c */")
  }
}