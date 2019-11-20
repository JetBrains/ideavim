package com.maddyhome.idea.vim.action

import javax.swing.KeyStroke

interface ComplicatedKeysAction {
  val keyStrokesSet: Set<List<KeyStroke>>
}
