/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface NativeAction {
  val action: Any
}

interface NativeActionManager {
  val enterAction: NativeAction?
  val createLineAboveCaret: NativeAction?
  val joinLines: NativeAction?
  val indentLines: NativeAction?
  val saveAll: NativeAction?
  val saveCurrent: NativeAction?
  val deleteAction: NativeAction?
}
