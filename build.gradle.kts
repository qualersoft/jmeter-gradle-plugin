import de.qualersoft.parseSemVer
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.internal.artifacts.configurations.DefaultUnlockedConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.owasp.dependencycheck.gradle.extension.AnalyzerExtension
import org.owasp.dependencycheck.gradle.extension.RetireJSExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format

plugins {
  // implementation
  kotlin("jvm") version "2.0.10"

  // quality
  `jacoco-report-aggregation`
  id("pl.droidsonroids.jacoco.testkit") version "1.0.12"
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
  id("org.owasp.dependencycheck") version "10.0.3"

  // documentation
  id("org.jetbrains.dokka") version "1.9.20"
  id("org.asciidoctor.jvm.convert") version "4.0.3"

  // publishing
  signing
  id("com.gradle.plugin-publish") version "1.2.1"
  id("org.jetbrains.changelog") version "2.2.1"
}

group = "de.qualersoft"

repositories {
  mavenCentral()
}

@Suppress("UnstableApiUsage")
testing {
  val junitVersion = "5.10.2"
  val kotestVersion = "5.9.1"
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(junitVersion)

      dependencies {
        implementation("io.kotest:kotest-assertions-core:$kotestVersion")
      }

      gradlePlugin.testSourceSet(sources)
    }

    register<JvmTestSuite>("functionalTest") {
      useJUnitJupiter(junitVersion)

      dependencies {
        implementation(project())
        implementation("io.kotest:kotest-assertions-core:$kotestVersion")
        implementation(gradleTestKit())

        implementation(platform("org.junit:junit-bom:5.11.0"))
        implementation("org.junit.platform:junit-platform-commons") {
          because(
            """we need to implement custom strategy
              |see https://github.com/junit-team/junit5/issues/1858""".trimMargin()
          )
        }
        implementation("org.junit.platform:junit-platform-engine") {
          because(
            """we need to implement custom strategy
              |see https://github.com/junit-team/junit5/issues/1858""".trimMargin()
          )
        }
      }

      targets.all {
        testTask.configure {
          dependsOn(tasks.jar)

          shouldRunAfter(test)
          applyJacocoWorkaround()
        }
        tasks.check {
          dependsOn(testTask)
        }

        jacocoTestKit {
          @Suppress("UNCHECKED_CAST")
          applyTo("functionalTestRuntimeOnly", testTask as TaskProvider<Task>)
        }
      }

      gradlePlugin.testSourceSet(sources)
    }
  }
}

dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  testRuntimeOnly(kotlin("script-runtime"))

  // quality
  detektPlugins(group = "io.gitlab.arturbosch.detekt", name = "detekt-formatting", version = detekt.toolVersion) {
    because("We also want to check formatting issues.")
  }
  detektPlugins(group = "io.gitlab.arturbosch.detekt", name = "detekt-rules-libraries", version = detekt.toolVersion)
}

@Suppress("UnstableApiUsage")
reporting {
  reports {
    register<JacocoCoverageReport>("jacocoAggregatedCoverageReport") {
      reportTask {
        val allJacocoTasksData = tasks.withType<Test>()
          .mapNotNull { it.extensions.findByType<JacocoTaskExtension>()?.destinationFile }
        executionData.from(allJacocoTasksData)
        testType = "aggregated"
      }
    }
  }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website.set("https://github.com/qualersoft/jmeter-gradle-plugin")
  vcsUrl.set("https://github.com/qualersoft/jmeter-gradle-plugin")
  // Define the plugin
  plugins.create("jmeter") {
    id = "de.qualersoft.jmeter"
    implementationClass = "de.qualersoft.jmeter.gradleplugin.JMeterPlugin"

    displayName = "jmeter gradle plugin"
    description = "Plugin to execute JMeter tests."
    tags.addAll("jmeter", "test", "performance")
  }
}

detekt {
  allRules = false
  source.from(files("src"))
  config.from(files("detekt.yml"))
  basePath = project.projectDir.path
}

dependencyCheck {
  suppressionFile = file("config/dependencyCheck/suppressions.xml").path
  formats = listOf(
    Format.HTML.name,
    Format.SARIF.name
  )

  scanConfigurations = configurations.filterIsInstance<DefaultUnlockedConfiguration>()
    .map { it.name }
    .filterNot {
      it.contains("Plugin") ||
          it.contains("dokka") ||
          it.contains("detekt")
    }

  analyzers(closureOf<AnalyzerExtension> {
    assemblyEnabled = false // requires 'dotnet' executable which is not present everywhere
    retirejs(closureOf<RetireJSExtension> {
      enabled = false // because there seams to be an issue with RetireJS
    })
    System.getenv().getOrDefault("NVD_API_KEY", findProperty("NVD_API_KEY"))?.also {
      if ((it as String).isNotBlank()) {
        nvd.apply {
          apiKey = it
        }
      }
    }
  })
}

if (project.version.toString().endsWith("-SNAPSHOT", true)) {
  status = "snapshot"
}

val javaVersion = JavaVersion.VERSION_1_8

java {
  targetCompatibility = javaVersion
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(project.version.toString())
  groups.set(emptyList())
}

tasks {

  withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
    }
  }

  validatePlugins {
    enableStricterValidation.set(true)
  }

  withType<Detekt>().configureEach {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = javaVersion.toString()
    reports {
      html.required.set(true)
      xml.required.set(true)
      txt.required.set(false)
      md.required.set(false)
      sarif.required.set(true)
    }
  }

  pluginUnderTestMetadata {
    pluginClasspath.from(files(jar))
    mustRunAfter(jar)
  }

  withType<JacocoReport> {
    reports {
      csv.required.set(false)
      html.required.set(true)
      xml.required.set(true)
    }
  }

  dokkaJavadoc.configure {
    outputDirectory.set(javadoc.get().destinationDir)
  }

  javadoc {
    dependsOn(dokkaJavadoc)
  }
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/qualersoft/jmeter-gradle-plugin")
      credentials {
        username = project.findProperty("gh.qualersoft.publish.gpr.usr") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gh.qualersoft.publish.gpr.key") as String? ?: System.getenv("TOKEN")
      }
    }
  }
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
}

@Suppress("PropertyName", "VariableNaming")
val KINDS = listOf("major", "minor", "patch", "snapshot")
tasks.register("nextVersion") {
  doLast {
    val kind = getKind(this)
    val semVer = parseSemVer(project.version.toString())
    semVer.updateByKind(kind)
    logger.lifecycle("NewVersion=$semVer")
  }
}

tasks.register("updateVersion") {
  description = """ONLY FOR CI/CD purposes!
    |
    |This task is meant to be used by CI/CD to generate new release versions.
    |Prerequists: a `gradle.properties` next to this build-script must exist.
    |   version must follow semver-schema: <number>'.'<number>'.'<number>('-'.*)?
    |Usage:
    |  > ./gradlew updateVersion -Pkind=${KINDS.joinToString("|", "[", "]")}
  """.trimMargin()

  doLast {
    val kind = getKind(this)

    val semVersion = parseSemVer(project.version.toString())
    semVersion.updateByKind(kind)
    semVersion.persist(getGradlePropsFile())
  }
}

fun getKind(task: Task) = (project.findProperty("kind") as String?)?.let { kind ->
  val cleanKind = kind.trim()
  KINDS.firstOrNull { it.equals(cleanKind, true) }
    ?: throw IllegalArgumentException("Given kind '$kind' is none of ${KINDS.joinToString("|", "[", "]")}")
} ?: throw IllegalArgumentException(
  "No `kind` specified! Usage: ./gradlew ${task.name} -Pkind=${KINDS.joinToString("|", "[", "]")}"
)

fun getGradlePropsFile(): File {
  val propsFile = files("./gradle.properties").singleFile
  if (!propsFile.exists()) {
    val msg = "This task requires version to be stored in gradle.properties file, which does not exist!"
    throw UnsupportedOperationException(msg)
  }
  return propsFile
}

object LOCK {
  const val MILLIS_TO_WAIT = 200L
  const val MAX_TRIES = 100
}

// https://github.com/koral--/jacoco-gradle-testkit-plugin/issues/9
fun Test.applyJacocoWorkaround() {
  // Workaround on gradle/jacoco keeping *.exec file locked
  if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
    this.doLast("JacocoLockWorkaround") {
      logger.lifecycle("Running on Windows -> using jacoco-lock workaround")
      fun File.isLocked() = !renameTo(this)
      val jacocoTestExec = checkNotNull(extensions.getByType(JacocoTaskExtension::class)).destinationFile
      if (null == jacocoTestExec) {
        logger.lifecycle("No exec file ô.Ô?")
        return@doLast
      }
      logger.lifecycle("Waiting for $jacocoTestExec to become unlocked")
      var tries = 0
      while ((!jacocoTestExec.exists() || jacocoTestExec.isLocked()) && (tries++ < LOCK.MAX_TRIES)) {
        logger.lifecycle("Waiting ${LOCK.MILLIS_TO_WAIT} ms (${jacocoTestExec.name} is locked) the ${tries}th time...")
        Thread.sleep(LOCK.MILLIS_TO_WAIT)
      }
      logger.lifecycle("Done waiting (${jacocoTestExec.name} is unlocked).")
    }
  }
}
