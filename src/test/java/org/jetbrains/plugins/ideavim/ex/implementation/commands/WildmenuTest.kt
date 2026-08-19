/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile

class WildmenuTest : VimExTestCase() {

  @TempDir
  lateinit var tempDir: Path

  private lateinit var tempPath: String

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    tempDir.resolve("beta.txt").createFile()
    tempDir.resolve("bravo.kt").createFile()
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tempDir)
    tempPath = tempDir.absolutePathString()
  }

  @Test
  fun `test wildmenu is on by default`() {
    assertCommandOutput("set wildmenu?", "  wildmenu")
  }

  @Test
  fun `test wildmenu can be disabled`() {
    enterCommand("set nowildmenu")

    assertCommandOutput("set wildmenu?", "nowildmenu")
  }

  @Test
  fun `test wildmenu has wmnu abbreviation`() {
    assertCommandOutput("set wmnu?", "  wildmenu")
  }

  @Test
  fun `test setting wildmenu does not report an unknown option`() {
    enterCommand("set nowildmenu")

    assertPluginError(false)
  }

  @Test
  fun `test tab still cycles matches with nowildmenu`() {
    enterCommand("set nowildmenu")

    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Tab>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test shift tab still cycles matches backwards with nowildmenu`() {
    enterCommand("set nowildmenu")

    typeText(":edit $tempPath/b<S-Tab>")
    assertExText("edit $tempPath/bravo.kt")
  }

  @Test
  fun `test left arrow moves caret instead of cycling matches with nowildmenu`() {
    enterCommand("set nowildmenu")

    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.tx${c}t")
  }

  @Test
  fun `test right arrow does not cycle matches with nowildmenu`() {
    enterCommand("set nowildmenu")

    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    // The caret is already at the end of the command line, so <Right> has nothing to do
    typeText("<Right>")
    assertExText("edit $tempPath/beta.txt")
  }

  @Test
  fun `test arrow keys cycle matches with wildmenu`() {
    enterCommand("set wildmenu")

    typeText(":edit $tempPath/b<Tab>")
    assertExText("edit $tempPath/beta.txt")

    typeText("<Right>")
    assertExText("edit $tempPath/bravo.kt")

    typeText("<Left>")
    assertExText("edit $tempPath/beta.txt")
  }
}
