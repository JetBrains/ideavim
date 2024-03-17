/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter


fun main(args: Array<String>) {
  println("HI!")
  val projectDir = args[0]
  println("Working directory: $projectDir")
  val (lastVersion, objectId) = getVersion(projectDir, ReleaseType.STABLE_NO_PATCH)
  println("Last version: $lastVersion, hash: ${objectId.name}")

  val branch = withRepo(projectDir) { it.branch }
  check(branch == "master") {
    "We should be on master branch"
  }
  val mergeBaseCommit = getMergeBaseWithMaster(projectDir, objectId)
  println("Base commit $mergeBaseCommit")
  withGit(projectDir) { git ->
    val log = git.log().setMaxCount(500).call().toList()
    println("First commit hash in log: " + log.first().name + " log size: ${log.size}")
    val logDiff = log.takeWhile { it.id.name != mergeBaseCommit }
    val numCommits = logDiff.size
    println("Log diff size is $numCommits")
    check(numCommits < 450) {
      "More than 450 commits detected since the last release. This is suspicious."
    }

    val nextVersion = lastVersion.nextMinor().withSuffix("dev.$numCommits")

    println("Next dev version: $nextVersion")
    println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
  }
}

private fun getMergeBaseWithMaster(projectDir: String, tag: ObjectId): String {
  withRepo(projectDir) { repo ->
    val master = repo.resolve("master")
    RevWalk(repo).use { walk ->
      val tagRevCommit = walk.parseCommit(tag)
      val masterRevCommit = walk.parseCommit(master)
      walk.setRevFilter(RevFilter.MERGE_BASE)
      walk.markStart(tagRevCommit)
      walk.markStart(masterRevCommit)
      val mergeBase: RevCommit = walk.next()
      return mergeBase.name
    }
  }
}
