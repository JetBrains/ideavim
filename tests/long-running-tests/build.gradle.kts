import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform
import org.jetbrains.intellij.platform.gradle.tasks.CustomTestIdeTask

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project

  intellijPlatform {
    create(ideaType, ideaVersion)
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
    instrumentationTools()
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

tasks {
  // This task is disabled because it should be excluded from `gradle test` run (because it's slow)
  // I didn't find a better way to exclude except disabling and defining a new task with a different name
  // Note that useJUnitTestPlatform() is required to prevent red code
  test {
    enabled = false
    useJUnitPlatform()
  }

  register<CustomTestIdeTask>("testLongRunning") {
    group = "verification"
    useJUnitPlatform()
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
