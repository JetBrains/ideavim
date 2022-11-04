/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector

fun <T> Node<T>.addLeafs(keys: String, actionHolder: T) {
  addLeafs(injector.parser.parseKeys(keys), actionHolder)
}
