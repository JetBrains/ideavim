package com.maddyhome.idea.vim.api

interface VimExOutputPanelService {
  fun getPanel(editor: VimEditor): VimExOutputPanel
}

interface VimExOutputPanel {
  val text: String?

  fun output(text: String)
  fun clear()
}
