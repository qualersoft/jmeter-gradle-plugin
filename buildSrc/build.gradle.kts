plugins {
  kotlin("jvm") version "1.7.10"
}

repositories {
  mavenCentral()
}

val javaVersion = JavaVersion.VERSION_11
java {
  targetCompatibility = javaVersion
  withSourcesJar()
  withJavadocJar()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = javaVersion.toString()
  }
}
