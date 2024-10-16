import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.0.10"
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

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion.majorVersion))
  }
}
