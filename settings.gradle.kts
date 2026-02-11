// Set repository for snapshot versions of gradle plugin
pluginManagement {
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// Automatically download JDKs from Foojay API when required toolchain is not installed locally
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "IdeaVIM"


include("scripts")
include("annotation-processors")
include("tests:java-tests")
include("tests:property-tests")
include("tests:long-running-tests")
include("tests:ui-ij-tests")
include("tests:ui-py-tests")
include("tests:ui-fixtures")
include("api")
include("tests:ui-rd-tests")
include("vim-engine")
