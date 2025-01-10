/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.model.Script

object ScriptVisitor : VimscriptBaseVisitor<Script>() {

  override fun visitScript(ctx: VimscriptParser.ScriptContext): Script {
    val script = if (ctx.children != null) {
      val statements = ctx.children.mapNotNull { ExecutableVisitor.visit(it) }
      Script(statements)
    } else {
      Script(emptyList())
    }
    script.rangeInScript = ctx.getTextRange()
    return script
  }
}
