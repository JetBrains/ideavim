plugins {
  kotlin("jvm")
}

val kotlinVersion: String by project

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  implementation(project(":vim-engine"))
}

tasks.test {
  useJUnitPlatform()
}