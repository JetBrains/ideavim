/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchit

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexException
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Port of matchit.vim (https://github.com/chrisbra/matchit)
 * @author Martin Yzeiri (@myzeiri)
 */
internal class Matchit : VimExtension {

  override fun getName(): String = "matchit"

  override fun init() {
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchitMotion)"),
      owner,
      MatchHandler(false),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(MatchitMotion)"),
      owner,
      MatchHandler(false),
      false
    )
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys("%"),
      owner,
      injector.parser.parseKeys("<Plug>(MatchitMotion)"),
      true
    )

    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"),
      owner,
      MatchHandler(true),
      false
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"),
      owner,
      MatchHandler(true),
      false
    )
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys("g%"),
      owner,
      injector.parser.parseKeys("<Plug>(ReverseMatchitMotion)"),
      true
    )
  }
}
