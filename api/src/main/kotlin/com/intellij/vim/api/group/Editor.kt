/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.group

import com.intellij.vim.api.Api
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.CaretRead
import com.intellij.vim.api.CaretScope
import com.intellij.vim.api.CaretTransaction
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Read
import com.intellij.vim.api.Scope
import com.intellij.vim.api.Transaction

private lateinit var api: Api

val Scope.mode: Mode get() = api.getMode(this)

fun Scope.forEachCaret(action: CaretScope.(CaretInfo) -> Unit) = api.forEachCaret(this, action)
//fun Read.forEachCaret(scope: Scope, action: CaretRead.(CaretInfo) -> Unit) = api.forEachCaretRead(scope, action)
//fun Transaction.forEachCaret(scope: Scope, action: CaretTransaction.(CaretInfo) -> Unit) = api.forEachCaretTransaction(scope, action)
