/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import com.vdurmont.semver4j.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File

fun main(args: Array<String>) {
  println("HI!")
  val projectDir = args[0]
  println("Working directory: $projectDir")
  val lastVersion = getVersion(projectDir)

  println("Next minor version: ${lastVersion.nextMinor()}")
}

private fun getVersion(projectDir: String): Semver {
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)

  val version = git.tagList().call().mapNotNull { ref ->
    runCatching {
      Semver(ref.name.removePrefix("refs/tags/"))
    }.getOrNull()
  }
    .sortedBy { it }
    .last { it.isStable }
  return version
}