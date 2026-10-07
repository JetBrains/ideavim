/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.visualmulti.VimVisualMultiExtension
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Test
import java.beans.PropertyChangeEvent
import javax.swing.JPanel

/**
 * Changing the variables, e.g. by reloading the ideavimrc, should not require restarting the IDE
 */
class VisualMultiReloadMappingsTest : VisualMultiTestCase() {

  private val extension: VimVisualMultiExtension
    get() = VimExtension.EP_NAME.extensionList.single { it.name == "visual-multi" }.instance as VimVisualMultiExtension

  private fun reloadMappings() {
    ApplicationManager.getApplication().invokeAndWait { extension.reloadMappings() }
  }

  private fun focusOwnerChanged(newFocusOwner: Any) {
    ApplicationManager.getApplication().invokeAndWait {
      extension.focusListener.propertyChange(PropertyChangeEvent(this, "focusOwner", null, newFocusOwner))
    }
  }

  @Test
  fun `test reloading mappings applies changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText("<C-d>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings removes key replaced in VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText("<C-n>")

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test reloading mappings applies key changed again in VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")
    enterCommand("let g:VM_maps['Find Under'] = '<C-e>'")

    reloadMappings()
    typeText("<C-e>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings restores default key removed from VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Find Under'] = '<C-d>'")
    enterCommand("let g:VM_maps = {}")

    reloadMappings()
    typeText("<C-n><C-n>")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings applies changed VM_default_mappings`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_default_mappings = 0")

    reloadMappings()
    typeText("<Bslash><Bslash>A")

    // Without the mapping, `A` is the normal append command
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test reloading mappings keeps plug mappings`() {
    val before = """qwe
      |asd
      |q${c}we
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Select All'] = ''")

    reloadMappings()
    typeText("<Plug>(VM-Select-All)")

    val after = """${s}qwe$se
      |asd
      |${s}qwe$se
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test reloading mappings does not enable disabled extension`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("set novisual-multi")
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    reloadMappings()
    typeText("<C-d>")

    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test editor getting focus applies changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    focusOwnerChanged(fixture.editor.contentComponent)
    typeText("<C-d>")

    val after = """${s}qwe$se
      |asd
      |qwe
    """.trimMargin()
    assertState(after)
  }

  @Test
  fun `test other component getting focus does not apply changed VM_maps`() {
    val before = """q${c}we
      |asd
      |qwe
    """.trimMargin()
    configureByText(before)
    enterCommand("let g:VM_maps = {}")
    enterCommand("let g:VM_maps['Find Under'] = '<C-d>'")

    focusOwnerChanged(JPanel())
    typeText("<C-n>")

    // The old mapping is still there
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }
}
