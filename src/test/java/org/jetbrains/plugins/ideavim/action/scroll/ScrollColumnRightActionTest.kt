/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/*
z<Left>      or                                        *zh* *z<Left>*
zh                      Move the view on the text [count] characters to the
                        left, thus scroll the text [count] characters to the
                        right.  This only works when 'wrap' is off.
 */
class ScrollColumnRightActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scrolls column to right`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "zh"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 58, 137)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scrolls column to right with zLeft`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "z<Left>"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 58, 137)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of line. IdeaVim will scroll up to length of longest line")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll last column to right moves cursor 1`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("$"))
    // Assert we got initial scroll correct
    // We'd need virtual space to scroll this. We're over 200 due to editor.settings.additionalColumnsCount
    assertVisibleLineBounds(0, 123, 202)

    typeText(injector.parser.parseKeys("zh"))
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 122, 201)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of line. IdeaVim will scroll up to length of longest line")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll last column to right moves cursor 2`() {
    configureByText(
      buildString {
        repeat(300) { append("0") }
        appendln()
        repeat(200) { append("0") }
      }
    )
    typeText(injector.parser.parseKeys("j$"))
    // Assert we got initial scroll correct
    // Note, this matches Vim - we've scrolled to centre (but only because the line above allows us to scroll without
    // virtual space)
    assertVisibleLineBounds(1, 159, 238)

    typeText(injector.parser.parseKeys("zh"))
    assertPosition(1, 199)
    assertVisibleLineBounds(1, 158, 237)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scrolls count columns to right`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "10zh"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 49, 128)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scrolls count columns to right with zLeft`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "10z<Left>"))
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 49, 128)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scrolls column to right with sidescrolloff moves cursor`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloff, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "ze" + "zh"))
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 29, 108)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll column to right ignores sidescroll`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescroll, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|"))
    // Assert we got initial scroll correct
    // sidescroll=10 means we don't get the sidescroll jump of half a screen and the cursor is positioned at the right edge
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 20, 99)

    typeText(injector.parser.parseKeys("zh")) // Moves cursor, but not by sidescroll jump
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 19, 98)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll column to right on first page does nothing`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("10|" + "zh"))
    assertPosition(0, 9)
    assertVisibleLineBounds(0, 0, 79)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll column to right correctly scrolls inline inlay associated with preceding text`() {
    configureByColumns(200)
    addInlay(130, true, 5)
    typeText(injector.parser.parseKeys("100|"))
    // Text at end of line is:              89:inlay0123
    assertVisibleLineBounds(0, 59, 133) // 75 characters wide
    typeText(injector.parser.parseKeys("3zh")) //    89:inlay0
    assertVisibleLineBounds(0, 56, 130) // 75 characters
    typeText(injector.parser.parseKeys("zh")) //     89:inlay
    assertVisibleLineBounds(0, 55, 129) // 75 characters
    typeText(injector.parser.parseKeys("zh")) //            8
    assertVisibleLineBounds(0, 49, 128) // 80 characters
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll column to right correctly scrolls inline inlay associated with following text`() {
    configureByColumns(200)
    addInlay(130, false, 5)
    typeText(injector.parser.parseKeys("100|"))
    // Text at end of line is:              89inlay:0123
    assertVisibleLineBounds(0, 59, 133) // 75 characters wide
    typeText(injector.parser.parseKeys("3zh")) //    89inlay:0
    assertVisibleLineBounds(0, 56, 130) // 75 characters
    typeText(injector.parser.parseKeys("zh")) //           89
    assertVisibleLineBounds(0, 50, 129) // 80 characters
    typeText(injector.parser.parseKeys("zh")) //            9
    assertVisibleLineBounds(0, 49, 128) // 80 characters
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll column to right with preceding inline inlay moves cursor at end of screen`() {
    configureByColumns(200)
    addInlay(90, false, 5)
    typeText(injector.parser.parseKeys("100|" + "ze" + "zh"))
    assertPosition(0, 98)
    assertVisibleLineBounds(0, 24, 98)
    typeText(injector.parser.parseKeys("zh"))
    assertPosition(0, 97)
    assertVisibleLineBounds(0, 23, 97)
    typeText(injector.parser.parseKeys("zh"))
    assertPosition(0, 96)
    assertVisibleLineBounds(0, 22, 96)
    typeText(injector.parser.parseKeys("zh"))
    assertPosition(0, 95)
    assertVisibleLineBounds(0, 21, 95)
  }
}
