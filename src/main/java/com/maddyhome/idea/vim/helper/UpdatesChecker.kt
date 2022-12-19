/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.ide.plugins.StandalonePluginUpdateChecker
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.icons.VimIcons

@Service(Service.Level.APP)
class VimStandalonePluginUpdateChecker : StandalonePluginUpdateChecker(
  VimPlugin.getPluginId(),
  updateTimestampProperty = PROPERTY_NAME,
  NotificationService.IDEAVIM_STICKY_GROUP,
  VimIcons.IDEAVIM,
) {
  companion object {
    private const val PROPERTY_NAME = "ideavim.statistics.timestamp"
    val instance: VimStandalonePluginUpdateChecker = service()
  }
}
