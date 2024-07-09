/*
 * Copyright 2003-2023 The IdeaVim authors
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
internal class VimStandalonePluginUpdateChecker : StandalonePluginUpdateChecker(
  VimPlugin.getPluginId(),
  updateTimestampProperty = PROPERTY_NAME,
  NotificationService.IDEAVIM_STICKY_GROUP,
  VimIcons.IDEAVIM,
) {

  override fun skipUpdateCheck(): Boolean = VimPlugin.isNotEnabled() || "dev" in VimPlugin.getVersion()

  companion object {
    private const val PROPERTY_NAME = "ideavim.statistics.timestamp"
    fun getInstance(): VimStandalonePluginUpdateChecker = service()
  }
}
