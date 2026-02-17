/*
 * Copyright 2003-2026 The IdeaVim authors
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
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

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
  Path("build/reports").createDirectories().resolve("$name.png").writeBytes(bytes)
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
  if (Path("build/reports/styles.css").exists().not()) {
    saveFile("$url/styles.css", "build/reports", "styles.css")
  }
  println("Hierarchy snapshot: ${hierarchySnapshot.absolutePathString()}")
}

private fun saveFile(url: String, folder: String, name: String): Path {
  val response = client.newCall(Request.Builder().url(url).build()).execute()
  return Path(folder).createDirectories().resolve(name).also {
    it.writeText(response.body?.string() ?: "")
  }
}
