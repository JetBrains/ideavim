/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@Suppress("SpellCheckingInspection")
class KeymapOptionTest : VimTestCase() {
  @TempDir
  var homeDir: Path? = null
  private var originalHome: String? = null

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    // Point config discovery at a clean temp home so the user keymap dir (~/.config/ideavim/keymap)
    // is test-controlled and isolated from the developer's real config.
    originalHome = System.getProperty("user.home")
    System.setProperty("user.home", homeDir!!.toString())
    configureByText("\n")
  }

  @AfterEach
  fun restoreHome() {
    val home = originalHome
    if (home != null) System.setProperty("user.home", home) else System.clearProperty("user.home")
  }

  private fun writeUserKeymap(name: String, content: String) {
    val dir = homeDir!!.resolve(".config/ideavim/keymap")
    dir.createDirectories()
    dir.resolve("$name.vim").writeText(content)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test keymap option defaults to empty`() {
    assertCommandOutput("set keymap?", "  keymap=")
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test setting keymap to an unknown name reports E544`() {
    enterCommand("set keymap=nosuchkeymap")
    assertPluginError(true)
    assertPluginErrorMessage("E544: Keymap file not found")
  }

  // --- User config dir (search root #1) ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test keymap is loaded from the user config directory`() {
    writeUserKeymap("mykeymap", "loadkeymap\na b\n")
    enterCommand("set keymap=mykeymap")
    assertPluginError(false)
    assertMappingExists("a", "b", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test user keymap shadows a bundled keymap of the same name`() {
    // A user file named like a bundled one must win (user dir is searched first).
    writeUserKeymap("russian-jcukenwin", "loadkeymap\nq Z\n")
    enterCommand("set keymap=russian-jcukenwin")
    assertMappingExists("q", "Z", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test keymap falls back to the bundled keymap`() {
    // No user file of this name -> resolves to the bundled russian-jcukenwin.vim.
    enterCommand("set keymap=russian-jcukenwin")
    assertPluginError(false)
    assertMappingExists("q", "й", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test keymap lets you type the mapped glyphs when iminsert is on`() {
    configureByText("${c}\n")
    enterCommand("set keymap=russian-jcukenwin")
    enterCommand("set iminsert=1")
    typeText("i")
    typeText("q")
    assertState("й\n")
  }

  // --- Unload ---

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test clearing keymap unloads its language mappings`() {
    writeUserKeymap("mykeymap", "loadkeymap\na b\n")
    enterCommand("set keymap=mykeymap")
    enterCommand("set keymap=")
    assertNoMapping("a", MappingMode.L)
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test clearing keymap keeps other mappings from the ideavimrc`() {
    // A mapping defined in the ideavimrc is owned by InitScript, the same owner keymap mappings use.
    // Clearing the keymap must not remove it.
    val rc = homeDir!!.resolve(".ideavimrc")
    rc.writeText("nmap x y\n")
    injector.vimscriptExecutor.executeFile(rc, fixture.editor.vim, true)

    writeUserKeymap("mykeymap", "loadkeymap\na b\n")
    enterCommand("set keymap=mykeymap")
    enterCommand("set keymap=")

    assertNoMapping("a", MappingMode.L)              // the keymap's mapping is gone
    assertMappingExists("x", "y", MappingMode.N)     // but the user's nmap survives
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test switching keymap unloads the previous keymap`() {
    writeUserKeymap("first", "loadkeymap\na b\n")
    writeUserKeymap("second", "loadkeymap\nc d\n")
    enterCommand("set keymap=first")
    enterCommand("set keymap=second")

    assertNoMapping("a", MappingMode.L)              // first keymap's mapping is unloaded
    assertMappingExists("c", "d", MappingMode.L)     // second keymap's mapping is active
  }
}
