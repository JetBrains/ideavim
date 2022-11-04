/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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

object UpdatesChecker {

  private val logger = logger<UpdatesChecker>()
  private const val IDEAVIM_STATISTICS_TIMESTAMP_KEY = "ideavim.statistics.timestamp"
  private val DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1)

  /**
   * Check if we have plugin updates for IdeaVim.
   *
   * Also this code is necessary for JetStat, so do not remove this check.
   * See https://github.com/go-lang-plugin-org/go-lang-idea-plugin/commit/5182ab4a1d01ad37f6786268a2fe5e908575a217
   */
  fun check() {
    val lastUpdate = PropertiesComponent.getInstance().getLong(IDEAVIM_STATISTICS_TIMESTAMP_KEY, 0)
    val outOfDate = lastUpdate == 0L || System.currentTimeMillis() - lastUpdate > DAY_IN_MILLIS

    if (!outOfDate || !VimPlugin.isEnabled()) return

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val buildNumber = ApplicationInfo.getInstance().build.asString()
        val version = URLEncoder.encode(VimPlugin.getVersion(), CharsetToolkit.UTF8)
        val os = URLEncoder.encode("${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION}", CharsetToolkit.UTF8)
        val uid = PermanentInstallationID.get()

        val url = "https://plugins.jetbrains.com/plugins/list?" +
          "pluginId=${VimPlugin.getPluginId().idString}" +
          "&build=$buildNumber" +
          "&pluginVersion=$version" +
          "&os=$os" +
          "&uuid=$uid"
        PropertiesComponent.getInstance()
          .setValue(IDEAVIM_STATISTICS_TIMESTAMP_KEY, System.currentTimeMillis().toString())

        HttpRequests.request(url).connect { request: HttpRequests.Request ->
          logger.info("Check IdeaVim updates: $url")
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
