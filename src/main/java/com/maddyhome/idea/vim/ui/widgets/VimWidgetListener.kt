/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.VimPluginListener
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

public class VimWidgetListener(private val updateWidget: Runnable) : GlobalOptionChangeListener, VimPluginListener {
  init {
    injector.listenersNotifier.vimPluginListeners.add(this)
  }

  override fun onGlobalOptionChanged() {
    updateWidget.run()
  }

  override fun turnedOn() {
    updateWidget.run()
  }

  override fun turnedOff() {
    updateWidget.run()
  }
}