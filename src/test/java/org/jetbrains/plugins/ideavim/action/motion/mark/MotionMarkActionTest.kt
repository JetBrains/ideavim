/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.mark

import com.intellij.ide.bookmark.BookmarkType
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.bookmark.LineBookmark
import com.intellij.ide.vfs.rpcId
import com.intellij.openapi.application.ApplicationManager
import com.intellij.platform.project.projectId
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.bookmark.BookmarkBackendService
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionMarkActionTest : VimTestCase() {
  @Test
  fun `test simple add mark`() {
    val keys = injector.parser.parseKeys("mA")
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")
    typeText(keys)
    checkMarks('A' to 2)
  }

  @Test
  fun `test simple add multiple marks`() {
    val keys = injector.parser.parseKeys("mAj" + "mBj" + "mC")
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")
    typeText(keys)
    checkMarks('A' to 2, 'B' to 3, 'C' to 4)
  }

  @Test
  fun `test simple add multiple marks on same line`() {
    val keys = injector.parser.parseKeys("mA" + "mB" + "mC")
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")
    typeText(keys)
    checkMarks('A' to 2)

    // Previously it was like this, but now it's impossible to set multiple bookmarks on the same line.
//    checkMarks('A' to 2, 'B' to 2, 'C' to 2)
  }

  @Test
  fun `test move to another line`() {
    val keys = injector.parser.parseKeys("mAjj" + "mA")
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")
    typeText(keys)
    checkMarks('A' to 4)
  }

  @Test
  fun `test simple system mark`() {
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")
    val bookmarkService = BookmarkBackendService.getInstance()
    bookmarkService.createOrGetSystemMark(
      'A',
      2,
      0,
      fixture.file.virtualFile.rpcId(),
      fixture.project.projectId(),
    )
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    val vimMarks = injector.markService.getAllGlobalMarks()
    kotlin.test.assertEquals(1, vimMarks.size)
    kotlin.test.assertEquals('A', vimMarks.first().key)
  }

  @Test
  fun `test system mark move to another line`() {
    val text = """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(text)
    enterCommand("set ideamarks")

    val bookmarkService = BookmarkBackendService.getInstance()
    bookmarkService.createOrGetSystemMark(
      'A',
      2,
      0,
      fixture.file.virtualFile.rpcId(),
      fixture.project.projectId(),
    )

    bookmarkService.removeBookmark('A')
    bookmarkService.createOrGetSystemMark(
      'A',
      4,
      0,
      fixture.file.virtualFile.rpcId(),
      fixture.project.projectId(),
    )
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    val vimMarks = injector.markService.getAllGlobalMarks()
    kotlin.test.assertEquals(1, vimMarks.size)
    val mark = vimMarks.first()
    kotlin.test.assertEquals('A', mark.key)
    kotlin.test.assertEquals(4, mark.line)
  }

  private fun checkMarks(vararg marks: Pair<Char, Int>) {
    val project = fixture.project
    val bookmarksManager = BookmarksManager.getInstance(project)!!
    val validBookmarks = bookmarksManager.bookmarks
      .filter { bookmarksManager.getType(it) != BookmarkType.DEFAULT }
      .sortedBy { bookmarksManager.getType(it)!!.mnemonic }
    kotlin.test.assertEquals(marks.size, validBookmarks.size)
    marks.sortedBy { it.first }.forEachIndexed { index, (mn, line) ->
      kotlin.test.assertEquals(mn, bookmarksManager.getType(validBookmarks[index])!!.mnemonic)
      kotlin.test.assertEquals(line, (validBookmarks[index] as LineBookmark).line)
    }
  }
}
