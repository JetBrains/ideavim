import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

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
    bundledPlugins("com.intellij.java")
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
  test {
    useJUnitPlatform()
    enabled = false
  }

  // The `test` task is automatically set up with IntelliJ goodness. A custom test task needs to be configured for it
  val testPropertyBased by intellijPlatformTesting.testIde.registering {
    task {
      group = "verification"
      useJUnitPlatform()
    }
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
