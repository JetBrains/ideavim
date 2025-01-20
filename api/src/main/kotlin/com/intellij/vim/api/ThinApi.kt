/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.put.ProcessedTextData
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * - Locking / Transactions / Threading
 *   - In Fleet there are transactions. How to implement it?
 *     - In IJ there are write locks. Do they work the same as transactions?
 *     - How do we inject different implementations for Fleet & IJ.
 *   - In IdeaVim, are we thread safe? If no, can we improve that?
 * - Coroutines
 * - Do we do any kind of namespacing, or just a bunch of top-level functions?
 * - Make sure API is properly visible from IJ plugin dev kit
 * - How to obtain and keep the context?`
 */


interface Scope {
  val editor: VimEditor
  val context: ExecutionContext

  fun read(block: Transaction.() -> Unit)
  fun change(block: Transaction.() -> Unit)
}
interface CaretScope: Scope {
  val caretId: CaretId
  
  fun with(read: Read, block: CaretRead.() -> Unit)
  fun with(read: Transaction, block: CaretTransaction.() -> Unit)
}

interface Read: Scope
interface CaretRead: CaretScope

fun Read.getReg(caretId: CaretId, name: Char): String? {
  return injector.registerGroup.getRegister(apiEditor, apiContext, name)?.text
}

fun Read.getRegType(caretId: CaretId, name: Char): SelectionType? {
  return injector.registerGroup.getRegister(apiEditor, apiContext, name)?.type
}

fun Read.getRegContent(caretId: CaretId, name: Char): RegisterContent? {
  TODO()
}

enum class RegType {
  LINE,
  CHAR,
  BLOCK,
}

val RegType.isLine: Boolean
  get() = this == RegType.LINE

val register: Char
  get() {
    return if (apiEditor.carets().size == 1) injector.registerGroup.currentRegister else injector.registerGroup.getCurrentRegisterForMulticaret()
  }


interface Transaction : Read
interface CaretTransaction : Transaction, CaretRead


lateinit var apiEditor: VimEditor // TODO Put this as argument for transaction
lateinit var apiContext: ExecutionContext


inline fun <T> read(editor: VimEditor, context: ExecutionContext, block: Read.() -> T): T {
  val read = object : Read {
    override val editor: VimEditor = editor
    override val context: ExecutionContext = context
  }
  return read.block()
}

fun change(editor: VimEditor, context: ExecutionContext, block: Transaction.() -> Unit) {
  injector.application.invokeAndWait {
    injector.application.runWriteAction {
      val transaction = object : Transaction {
        override val editor: VimEditor = editor
        override val context: ExecutionContext = context
      }
      transaction.block()
    }
  }
}

fun Read.carets(): Set<CaretId> {
  return apiEditor.carets().mapTo(HashSet()) { CaretId(it.id) }
}

fun forEachCaretSorted(action: Transaction.(CaretId, CaretInfo) -> Unit) {
  val info = read {
    apiEditor.sortedCarets().map { getInfo(it) to CaretId(it.id) }
  }

  change {
    info.forEach { (caretInfo, caretId) ->
      action(caretId, caretInfo)
    }
  }
}

fun Transaction.updateCaret(caretId: CaretId, info: CaretInfo) {
  apiEditor.carets().firstOrNull { it.id == caretId.id }
    ?.let {
      it.moveToOffset(info.offset)
      if (info.selection != null) {
        it.setSelection(info.selection.first, info.selection.second)
      } else {
        it.removeSelection()
      }
    }
}

fun Read.getInfo(caretId: CaretId): CaretInfo? {
  // TODO How to process start & end direction?
  // Note: Another option is to get selection information by marks
  return apiEditor.carets()
    .firstOrNull { it.id == caretId.id }
    ?.let {
      val hasSelection = it.hasSelection()
      CaretInfo(
        it.offset,
        if (hasSelection) it.selectionStart to it.selectionEnd else null,
      )
    }
}

private fun Read.getInfo(caret: VimCaret): CaretInfo {
  val hasSelection = caret.hasSelection()
  return CaretInfo(
    caret.offset,
    if (hasSelection) caret.selectionStart to caret.selectionEnd else null,
  )
}

fun getpos(): Int {
  TODO()
}


//--- Functions.ks
fun getSeleciton(): Pair<Int, Int> {
  return getpos() to getpos()
}

// TODO Handle selection direction

fun Transaction.setMap(mode: String, lhs: String, rhs: String) {
  val mode = MappingMode.parseModeChar(mode.single())
  val left = injector.parser.parseKeys(lhs)
  val right = injector.parser.parseKeys(rhs)
  injector.keyGroup.putKeyMapping(setOf(mode), left, MappingOwner.Plugin.get("XXX"), right, true)
}

fun Transaction.setMap(mode: String, lhs: String, rhs: ExtensionHandler) {
  val mode = MappingMode.parseModeChar(mode.single())
  val left = injector.parser.parseKeys(lhs)
  injector.keyGroup.putKeyMapping(setOf(mode), left, MappingOwner.Plugin.get("XXX"), rhs, false)
}


fun Transaction.replaceText(startOffset: Int, endOffset: Int, text: String) {
  (apiEditor as MutableVimEditor).replaceString(startOffset, endOffset, text)
}

fun Transaction.replaceText(startOffset: Int, endOffset: Int, registerContent: RegisterContent) {
  apiEditor.deleteString(TextRange(startOffset, endOffset))
  injector.put.smartPutText(apiEditor, apiContext, startOffset, registerContent.text, registerContent.transferableData, registerContent.type)
}

