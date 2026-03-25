/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import com.intellij.ide.starter.ide.IDEStartConfig
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.InstalledIde
import com.intellij.ide.starter.models.VMOptions
import com.intellij.util.system.OS
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Workaround for macOS: the native `.app` binary (`Contents/MacOS/idea`) does not support
 * the `serverMode` starter command. The `remote-dev-server.sh` script does.
 *
 * This patches only the backend IDE to use `remote-dev-server.sh`.
 * The frontend (thin client) uses the native binary which handles `thinClient` correctly.
 */
fun IDETestContext.patchForMacOsSplitMode() {
  if (OS.CURRENT != OS.macOS) return

  // Only patch this context (the backend). The frontend context uses the native launcher.
  // Reflection into IDETestContext.ide — targets ide-starter from IntelliJ 2025.1+.
  // Will throw NoSuchFieldException if the SDK renames this field.
  val ideField = IDETestContext::class.java.getDeclaredField("ide")
  ideField.isAccessible = true
  val originalIde = ideField.get(this) as InstalledIde

  val script = findRemoteDevScript(originalIde.installationPath) ?: return
  ideField.set(this, ShellLauncherIde(originalIde, script))
}

private fun findRemoteDevScript(installationPath: Path): Path? {
  // Non-installer layout: bin/remote-dev-server.sh
  val direct = installationPath.resolve("bin/remote-dev-server.sh")
  if (direct.exists()) return direct

  // macOS .app bundle layout: Contents/bin/remote-dev-server.sh
  val appBundle = installationPath.resolve("Contents/bin/remote-dev-server.sh")
  if (appBundle.exists()) return appBundle

  return null
}

/**
 * Delegates to the original [InstalledIde] but overrides [startConfig] to produce a command line
 * using `remote-dev-server.sh` instead of the native binary.
 */
private class ShellLauncherIde(
  private val delegate: InstalledIde,
  private val shellScript: Path,
) : InstalledIde by delegate {

  override fun startConfig(vmOptions: VMOptions, logFolder: Path): IDEStartConfig {
    val originalConfig = delegate.startConfig(vmOptions, logFolder)
    return object : IDEStartConfig by originalConfig {
      override val commandLine: List<String>
        get() = listOf(shellScript.toString())
    }
  }
}
