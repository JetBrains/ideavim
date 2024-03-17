/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.processors

import kotlinx.serialization.Serializable

@Serializable
data class CommandBean(val keys: String, val `class`: String, val modes: String)