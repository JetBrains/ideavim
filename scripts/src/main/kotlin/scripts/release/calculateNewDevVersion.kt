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
  println("Last version: $lastVersion, hash: ${objectId.name}")

  val branch = getRepo(projectDir).branch
  check(branch == "master") {
    "We should be on master branch"
  }
  val git = getGit(projectDir)
  val log = git.log().setMaxCount(500).call()
  println("First commit hash in log: " + log.first().name)
  val logDiff = log.takeWhile { it.id.name != objectId.name }
  val numCommits = logDiff.size
  println("Log diff size is $numCommits")
  check(numCommits < 450) {
    "More than 450 commits detected since the last release. This is suspicious."
  }

  val nextVersion = lastVersion.nextMinor().withSuffix("dev.$numCommits")

  println("Next dev version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}
