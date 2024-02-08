/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui

import com.automation.remarks.junit5.Video
import com.intellij.remoterobot.steps.CommonSteps
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PyCharmTest {
  init {
//    StepsLogger.init()
  }

  private lateinit var commonSteps: CommonSteps

  companion object {
    private var ideaProcess: Process? = null
    private var tmpDir: Path = Files.createTempDirectory("launcher")

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
//      val client = OkHttpClient()
//      val ideDownloader = IdeDownloader(client)
//      val downloadedIdePath = ideDownloader.downloadAndExtractLatestEap(Ide.PYCHARM, tmpDir)
//      // This hack doesn't work because it breaks .app. Waiting for the new version of robot with the fix.
////      if (Os.hostOS() == Os.MAC) {
////        // Hack for the problem of double vmoptions file
////        // The ides now have two vmoptions files: one for ide itself and one for jetbrains client
////        // Because of some reason, when ide detects more than one vmoptions file, it tries to find one that ends on
////        //  "64". And since it doesn't find one, the robot fails.
////        val dataPath = downloadedIdePath.resolve("Contents/bin")
////        Files.list(dataPath).filter {
////          it.fileName.toString().endsWith("jetbrains_client.vmoptions")
////        }.forEach { Files.delete(it) }
////      }
//      ideaProcess = IdeLauncher.launchIde(
//        downloadedIdePath,
//        mapOf("robot-server.port" to 8083),
//        emptyList(),
//        listOf(ideDownloader.downloadRobotPlugin(tmpDir)),
//        tmpDir
//      )
    }

    @AfterAll
    @JvmStatic
    fun cleanUp() {
//      ideaProcess?.destroy()
//      tmpDir.toFile().deleteRecursively()
    }
  }

  @Test
  @Video
  @Disabled("Waiting for the new version of the robot with fixes")
  fun run() {
    println("Hey")
  }
}