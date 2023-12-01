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
  val releaseType = args[1]
  println("Working directory: $projectDir")
  println("Release type: $releaseType")
  val (lastVersion, _) = getVersion(projectDir, ReleaseType.ONLY_STABLE)

  val nextVersion = when (releaseType) {
    "major" -> lastVersion.nextMajor()
    "minor" -> lastVersion.nextMinor()
    "patch" -> lastVersion.nextPatch()
    else -> error("Only major, minor, and patch versions are supported. Got '$releaseType'")
  }
  println("Next $releaseType version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}
