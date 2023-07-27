/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File


fun checkReleaseType(releaseType: String) {
  check(releaseType in setOf("major", "minor", "patch")) {
    "This function accepts only major, minor, or path as release type. Current value: '$releaseType'"
  }
}

internal fun readArgs(args: Array<String>): Triple<String, String, String> {
  val newVersion = args[0]
  val rootDir = args[1]
  val releaseType = args[2]
  println("New version: $newVersion")
  println("root dir: $rootDir")
  println("Release Type: $releaseType")
  return Triple(newVersion, rootDir, releaseType)
}

internal fun getGit(rootDir: String): Git {
  val repository = getRepo(rootDir)
  return Git(repository)
}

internal fun getRepo(rootDir: String): Repository {
  return RepositoryBuilder().setGitDir(File("$rootDir/.git")).build()
}

internal fun checkBranch(rootDir: String, releaseType: String) {
  val repo = getRepo(rootDir)
  val branch = repo.branch
  check(
    releaseType in setOf("major", "minor") && branch == "master"
      || releaseType == "patch" && branch == "release"
  ) {
    "Incorrect branch for the release type. Release type: '$releaseType', branch '$branch'"
  }
}
