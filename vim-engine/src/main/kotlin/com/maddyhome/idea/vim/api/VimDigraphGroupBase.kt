package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.EngineStringHelper
import java.util.*
import javax.swing.KeyStroke

abstract class VimDigraphGroupBase : VimDigraphGroup {
  val keys = TreeMap<Char, String>()

  override fun displayAsciiInfo(editor: VimEditor) {
    val offset = editor.currentCaret().offset.point
    val charsSequence = editor.text()
    if (charsSequence.isEmpty() || offset >= charsSequence.length) return
    val ch = charsSequence[offset]

    val digraph = keys[ch]
    val digraphText = if (digraph == null) "" else ", Digr $digraph"

    if (ch.code < 0x100) {
      injector.messages.showStatusBarMessage(
        String.format(
          "<%s>  %d,  Hex %02x,  Oct %03o%s",
          EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke(ch)),
          ch.code,
          ch.code,
          ch.code,
          digraphText
        )
      )
    } else {
      injector.messages.showStatusBarMessage(
        String.format(
          "<%s> %d, Hex %04x, Oct %o%s",
          EngineStringHelper.toPrintableCharacter(KeyStroke.getKeyStroke(ch)),
          ch.code,
          ch.code,
          ch.code,
          digraphText
        )
      )
    }
  }
}