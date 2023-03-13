/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface NativeAction {
  public val action: Any
}

public interface NativeActionManager {
  public val enterAction: NativeAction?
  public val createLineAboveCaret: NativeAction?
  public val joinLines: NativeAction?
  public val indentLines: NativeAction?
  public val saveAll: NativeAction?
  public val saveCurrent: NativeAction?
  public val deleteAction: NativeAction?
}
