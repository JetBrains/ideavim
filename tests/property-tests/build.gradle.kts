plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

repositories {
  mavenCentral()
  maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

val kotlinVersion: String by project
val ideaVersion: String by project
val javaVersion: String by project

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project
}

// This task is disabled because it should be excluded from `gradle test` run (because it's slow)
// I didn't find a better way to exclude except disabling and defining a new task with a different name
tasks.test {
  enabled = false
}

tasks.register<Test>("testPropertyBased") {
  group = "verification"
  useJUnitPlatform()
}

intellij {
  version.set(ideaVersion)
  type.set("IC")
  plugins.set(listOf("java"))
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
