/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.motion.mark

import com.intellij.ide.bookmarks.BookmarkManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.IdeaMarkskOptionsData
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class MotionMarkActionTest : VimOptionTestCase(IdeaMarkskOptionsData.name) {
  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test simple add mark`() {
    val keys = StringHelper.parseKeys("mA")
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    typeText(keys)
    checkMarks('A' to 2)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test simple add multiple marks`() {
    val keys = StringHelper.parseKeys("mAj", "mBj", "mC")
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    typeText(keys)
    checkMarks('A' to 2, 'B' to 3, 'C' to 4)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test simple add multiple marks on same line`() {
    val keys = StringHelper.parseKeys("mA", "mB", "mC")
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    typeText(keys)
    checkMarks('A' to 2, 'B' to 2, 'C' to 2)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test move to another line`() {
    val keys = StringHelper.parseKeys("mAjj", "mA")
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    typeText(keys)
    checkMarks('A' to 4)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test simple system mark`() {
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    val bookmarkManager = BookmarkManager.getInstance(myFixture.project)
    bookmarkManager.addEditorBookmark(myFixture.editor, 2)
    val bookmark = bookmarkManager.findEditorBookmark(myFixture.editor.document, 2) ?: kotlin.test.fail()
    bookmarkManager.setMnemonic(bookmark, 'A')
    val vimMarks = VimPlugin.getMark().getMarks(myFixture.editor)
    TestCase.assertEquals(1, vimMarks.size)
    TestCase.assertEquals('A', vimMarks[0].key)
  }

  @VimOptionTestConfiguration(VimTestOption(IdeaMarkskOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  fun `test system mark move to another line`() {
    val text = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(text)
    var bookmarkManager = BookmarkManager.getInstance(myFixture.project)
    bookmarkManager.addEditorBookmark(myFixture.editor, 2)
    var bookmark = bookmarkManager.findEditorBookmark(myFixture.editor.document, 2) ?: kotlin.test.fail()
    bookmarkManager.setMnemonic(bookmark, 'A')

    bookmarkManager = BookmarkManager.getInstance(myFixture.project)
    bookmarkManager.addEditorBookmark(myFixture.editor, 4)
    bookmark = bookmarkManager.findEditorBookmark(myFixture.editor.document, 4) ?: kotlin.test.fail()
    bookmarkManager.setMnemonic(bookmark, 'A')
    val vimMarks = VimPlugin.getMark().getMarks(myFixture.editor)
    TestCase.assertEquals(1, vimMarks.size)
    TestCase.assertEquals('A', vimMarks[0].key)
    TestCase.assertEquals(4, vimMarks[0].logicalLine)
  }

  private fun checkMarks(vararg marks: Pair<Char, Int>) {
    val validBookmarks = BookmarkManager.getInstance(myFixture.project).validBookmarks
    assertEquals(marks.size, validBookmarks.size)
    marks.forEachIndexed { index, (mn, line) ->
      assertEquals(mn, validBookmarks[marks.size - index - 1].mnemonic)
      assertEquals(line, validBookmarks[marks.size - index - 1].line)
    }
  }
}
