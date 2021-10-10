package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser

object ScriptVisitor : VimscriptBaseVisitor<Script>() {

  override fun visitScript(ctx: VimscriptParser.ScriptContext): Script {
    return if (ctx.children != null) {
      val statements = ctx.children.mapNotNull { ExecutableVisitor.visit(it) }
      Script(statements)
    } else {
      Script(emptyList())
    }
  }
}
