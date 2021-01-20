/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.io.HttpRequests
import com.maddyhome.idea.vim.VimPlugin
import org.jdom.JDOMException
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object StatisticReporter {

  private val logger = logger<StatisticReporter>()
  private const val IDEAVIM_STATISTICS_TIMESTAMP_KEY = "ideavim.statistics.timestamp"
  private val DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1)

  /**
   * Reports statistics about installed IdeaVim and enabled Vim emulation.
   *
   * See https://github.com/go-lang-plugin-org/go-lang-idea-plugin/commit/5182ab4a1d01ad37f6786268a2fe5e908575a217
   */
  fun report() {
    val lastUpdate = PropertiesComponent.getInstance().getLong(IDEAVIM_STATISTICS_TIMESTAMP_KEY, 0)
    val outOfDate = lastUpdate == 0L || System.currentTimeMillis() - lastUpdate > DAY_IN_MILLIS

    if (!outOfDate || !VimPlugin.isEnabled()) return

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val buildNumber = ApplicationInfo.getInstance().build.asString()
        val version = URLEncoder.encode(VimPlugin.getVersion(), CharsetToolkit.UTF8)
        val os = URLEncoder.encode("${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION}", CharsetToolkit.UTF8)
        val uid = PermanentInstallationID.get()

        val url = "https://plugins.jetbrains.com/plugins/list?pluginId=${VimPlugin.getPluginId().idString}&build=$buildNumber&pluginVersion=$version&os=$os&uuid=$uid"
        PropertiesComponent.getInstance().setValue(IDEAVIM_STATISTICS_TIMESTAMP_KEY, System.currentTimeMillis().toString())

        HttpRequests.request(url).connect { request: HttpRequests.Request ->
          logger.info("Sending statistics: $url")
          try {
            JDOMUtil.load(request.inputStream)
          } catch (e: JDOMException) {
            logger.warn(e)
          }
        }
      } catch (e: IOException) {
        logger.warn(e)
      }
    }
  }
}
