package _Self

object Constants {
  const val DEFAULT_CHANNEL = "default"
  const val EAP_CHANNEL = "eap"
  const val DEV_CHANNEL = "Dev"

  const val VERSION = "1.10.3"
  const val DEV_VERSION = "1.11.0"

  const val GITHUB_TESTS = "LATEST-EAP-SNAPSHOT"
  const val NVIM_TESTS = "LATEST-EAP-SNAPSHOT"
  const val PROPERTY_TESTS = "LATEST-EAP-SNAPSHOT"
  const val LONG_RUNNING_TESTS = "LATEST-EAP-SNAPSHOT"
  const val QODANA_TESTS = "LATEST-EAP-SNAPSHOT"
  const val RELEASE = "2022.1.3"


  // Use LATEST-EAP-SNAPSHOT only when we'll update the minimum version of IJ to 222+
  // Because of some API inconcistincies, IdeaVim built on 2022+ won't run on older versions of IJ
  const val RELEASE_DEV = "2022.1.3"
  const val RELEASE_EAP = "2022.1.3"
}
