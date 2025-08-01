/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.VimListenersNotifier
import com.maddyhome.idea.vim.impl.state.VimStateMachineImpl
import com.maddyhome.idea.vim.state.VimStateMachine

abstract class VimInjectorBase : VimInjector {
  override val vimState: VimStateMachine = VimStateMachineImpl()
  override val listenersNotifier: VimListenersNotifier by lazy { VimListenersNotifier() }
}
