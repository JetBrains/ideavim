package com.maddyhome.idea.vim.helper

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimStringParser
import com.maddyhome.idea.vim.api.VimStringParserBase
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

@Service
class IjVimStringParser : VimStringParserBase()
