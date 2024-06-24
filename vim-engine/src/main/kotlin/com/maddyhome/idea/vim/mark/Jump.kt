/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

// Todo maybe we should use something like IDE's RangeMarker here?
public data class Jump(var line: Int, val col: Int, var filepath: String, val protocol: String)