/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
