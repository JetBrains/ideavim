/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.group

import com.intellij.vim.api.Api
import com.intellij.vim.api.Scope

private lateinit var api: Api

fun exportOperatorFunction(name: String, function: Scope.() -> Unit) = api.exportOperatorFunction(name, function)
