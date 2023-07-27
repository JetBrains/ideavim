/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

fun main(args: Array<String>) {
  println("HI!")
  val projectDir = args[0]
  println("Working directory: $projectDir")
  val (lastVersion, objectId) = getVersion(projectDir, onlyStable = true)

  val git = getGit(projectDir)
  val logDiff = git.log().setMaxCount(500).call().takeWhile { it.id.name != objectId.name }
  val numCommits = logDiff.size
  check(numCommits < 450) {
    "More than 450 commits detected since the last release. This is suspicious."
  }

  val nextVersion = lastVersion.nextMinor().withSuffix("dev.$numCommits")

  println("Next dev version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}
