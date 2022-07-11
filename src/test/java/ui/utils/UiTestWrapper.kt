/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package ui.utils

import com.intellij.remoterobot.RemoteRobot
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

fun uiTest(testName: String = "test_${System.currentTimeMillis()}", url: String = "http://127.0.0.1:8082", test: RemoteRobot.() -> Unit) {
  val remoteRobot = RemoteRobot(url)
  try {
    remoteRobot.test()
  } catch (e: Throwable) {
    try {
      saveScreenshot(testName, remoteRobot)
      saveHierarchy(testName, url)
    } catch (another: Throwable) {
      another.initCause(e)
      throw another
    }
    throw e
  }
}
private val client by lazy { OkHttpClient() }
private fun BufferedImage.save(name: String) {
  val bytes = ByteArrayOutputStream().use { b ->
    ImageIO.write(this, "png", b)
    b.toByteArray()
  }
  File("build/reports").apply { mkdirs() }.resolve("$name.png").writeBytes(bytes)
}

fun saveScreenshot(testName: String, remoteRobot: RemoteRobot) {
  fetchScreenShot(remoteRobot).save(testName)
}

private fun fetchScreenShot(remoteRobot: RemoteRobot): BufferedImage {
  return remoteRobot.getScreenshot()
}

private fun saveHierarchy(testName: String, url: String) {
  val hierarchySnapshot =
    saveFile(url, "build/reports", "hierarchy-$testName.html")
  if (File("build/reports/styles.css").exists().not()) {
    saveFile("$url/styles.css", "build/reports", "styles.css")
  }
  println("Hierarchy snapshot: ${hierarchySnapshot.absolutePath}")
}

private fun saveFile(url: String, folder: String, name: String): File {
  val response = client.newCall(Request.Builder().url(url).build()).execute()
  return File(folder).apply {
    mkdirs()
  }.resolve(name).apply {
    writeText(response.body?.string() ?: "")
  }
}
