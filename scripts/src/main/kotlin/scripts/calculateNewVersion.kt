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
  val releaseType = args[1]
  println("Working directory: $projectDir")
  println("Release type: $releaseType")
  val lastVersion = getVersion(projectDir)

  val nextVersion = when (releaseType) {
    "major" -> TODO()
    "minor" -> lastVersion.nextMinor()
    "patch" -> TODO()
    else -> error("Only major, minor, and patch versions are supported")
  }
  println("Next $releaseType version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}

private fun getVersion(projectDir: String): Semver {
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)
  println(git.log().call().first())
  println(git.tagList().call().first())

  val version = git.tagList().call().mapNotNull { ref ->
    runCatching {
      Semver(ref.name.removePrefix("refs/tags/"))
    }.getOrNull()
  }
    .sortedBy { it }
    .last { it.isStable }
  return version
}