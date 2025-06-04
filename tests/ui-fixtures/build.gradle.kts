plugins {
  java
  kotlin("jvm")
  id("java-test-fixtures")
}

repositories {
  mavenCentral()
  maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

val kotlinVersion: String by project
val ideaVersion: String by project
val javaVersion: String by project
val remoteRobotVersion: String by project

dependencies {
  testFixturesImplementation("org.junit.jupiter:junit-jupiter:5.13.0")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testFixturesImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testFixturesImplementation(testFixtures(project(":"))) // The root project

  testFixturesImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testFixturesImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testFixturesImplementation("com.intellij.remoterobot:ide-launcher:$remoteRobotVersion")
}

tasks {
  // This task is disabled because it should be excluded from `gradle test` run (because it's slow)
  // I didn't find a better way to exclude except disabling and defining a new task with a different name
  test {
    useJUnitPlatform()
    enabled = false
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
