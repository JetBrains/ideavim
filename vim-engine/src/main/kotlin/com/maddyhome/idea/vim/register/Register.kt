/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.annotations.NonNls
import javax.swing.KeyStroke

// TODO should we prefer keys over text, as they are more informative?
// TODO e.g.  could be both <Esc> and <C-[> after trying to restore original keys
data class Register(
  val name: Char,
  val copiedText: VimCopiedText,
  val type: SelectionType,
) {
  val text = copiedText.text
  val keys get() = injector.parser.stringToKeys(copiedText.text)
  val printableString: String =
    EngineStringHelper.toPrintableCharacters(keys) // should be the same as [text], but we can't render control notation properly

  constructor(name: Char, type: SelectionType, keys: MutableList<KeyStroke>) : this(
    name,
    injector.clipboardManager.dumbCopiedText(injector.parser.toPrintableString(keys)),
    type
  )
//  constructor(name: Char, type: SelectionType, text: String, transferableData: MutableList<out Any>) : this(name, text, type, transferableData)

  override fun toString(): String = "@$name = $printableString"

  object KeySorter : Comparator<Register> {
    @NonNls
    private const val ORDER = "\"0123456789abcdefghijklmnopqrstuvwxyz-*+.:%#/="

    override fun compare(o1: Register, o2: Register): Int {
      return ORDER.indexOf(o1.name.lowercaseChar()) - ORDER.indexOf(o2.name.lowercaseChar())
    }
  }
}

/**
 * Imagine you yanked two lines and have the following content in your register a - foo\nbar\n (register type is line-wise)
 * Now, there are three different ways to append content, each with a different outcome:
 * - If you append a macro qAbazq, you'll get foo\nbarbaz\n in register `a` and it stays line-wise
 * - If you use Vim script and execute let @A = "baz", the result will be foo\nbar\nbaz and the register becomes character-wise
 * - If you copy "baz" to register A, it becomes foo\nbar\nbaz\n and stays line-wise
 *
 * At the moment, we will stick to the third option to not overcomplicate the plugin
 * (until there is a user who notices the difference)
 */
fun Register.addText(text: String): Register {
  return when (this.type) {
    SelectionType.CHARACTER_WISE -> {
      Register(this.name, injector.clipboardManager.dumbCopiedText(this.text + text), SelectionType.CHARACTER_WISE) // todo it's empty for historical reasons, but should we really clear transferable data?
    }
    SelectionType.LINE_WISE -> {
      Register(this.name, injector.clipboardManager.dumbCopiedText(this.text + text + (if (text.endsWith('\n')) "" else "\n")), SelectionType.LINE_WISE) // todo it's empty for historical reasons, but should we really clear transferable data?
    }
    SelectionType.BLOCK_WISE -> {
      Register(this.name, injector.clipboardManager.dumbCopiedText(this.text + "\n" + text), SelectionType.BLOCK_WISE) // todo it's empty for historical reasons, but should we really clear transferable data?
    }
  }
}
