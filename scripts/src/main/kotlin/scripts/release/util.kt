/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import com.vdurmont.semver4j.Semver
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevWalk
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

internal inline fun <T> withGit(rootDir: String, action: (Git) -> T): T {
  withRepo(rootDir) { repository ->
    Git(repository).use { git ->
      return action(git)
    }
  }
}

internal inline fun <T> withRepo(rootDir: String, action: (Repository) -> T): T {
  return RepositoryBuilder().setGitDir(File("$rootDir/.git")).build().use {
    return@use action(it)
  }
}

internal fun checkBranch(rootDir: String, releaseType: String) {
  val branch = withRepo(rootDir) { it.branch }
  check(
    releaseType in setOf("major", "minor") && branch == "master"
      || releaseType == "patch" && branch == "release"
  ) {
    "Incorrect branch for the release type. Release type: '$releaseType', branch '$branch'"
  }
}

enum class ReleaseType {
  ANY,
  ONLY_STABLE,
  STABLE_NO_PATCH, // Version that ends on 0. Like 2.5.0
}

internal fun getVersionsExistingVersionsFor(
  majorVersion: Int,
  minorVersion: Int,
  projectDir: String,
): Map<Semver, ObjectId> {
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)
  println(git.log().call().first())
  println(git.tagList().call().first())

  return git.tagList().call().mapNotNull { ref ->
    runCatching {
      // Git has two types of tags: light and annotated. This code detect hash of the commit for both types of tags
      val revWalk = RevWalk(repository)
      val tag = revWalk.parseAny(ref.objectId)
      val commitHash = revWalk.peel(tag).id
      val semver = Semver(ref.name.removePrefix("refs/tags/"))
      if (semver.major == majorVersion && semver.minor == minorVersion) {
        semver to commitHash
      } else null
    }.getOrNull()
  }
    .toMap()
}

internal fun getVersion(projectDir: String, releaseType: ReleaseType): Pair<Semver, ObjectId> {
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)
  println(git.log().call().first())
  println(git.tagList().call().first())

  val versions = git.tagList().call().mapNotNull { ref ->
    runCatching {
      // Git has two types of tags: light and annotated. This code detect hash of the commit for both types of tags
      val revWalk = RevWalk(repository)
      val tag = revWalk.parseAny(ref.objectId)
      val commitHash = revWalk.peel(tag).id
      Semver(ref.name.removePrefix("refs/tags/")) to commitHash
    }.getOrNull()
  }
    .sortedBy { it.first }

  val version = when (releaseType) {
    ReleaseType.ANY -> versions.last()
    ReleaseType.ONLY_STABLE -> versions.last { it.first.isStable }
    ReleaseType.STABLE_NO_PATCH -> versions.last { it.first.isStable && it.first.patch == 0 }
  }

  return version
}

internal fun Git.checkoutBranch(name: String) {
  println("Checking out $name")
  val shouldCreateBranch = this.branchList().call().any { it.name == "refs/heads/$name" }.not()
  val checkoutCommand = checkout()
    .setCreateBranch(shouldCreateBranch)
    .setName(name)
  if (shouldCreateBranch) {
    // Without starting point the branch will be created on HEAD.
    checkoutCommand.setStartPoint("origin/$name").setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
  }
  checkoutCommand.call()
}
