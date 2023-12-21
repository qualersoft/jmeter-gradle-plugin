package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import de.qualersoft.jmeter.gradleplugin.JMETER_LIB_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_PLUGIN_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_RUNNER
import de.qualersoft.jmeter.gradleplugin.copyToDir
import de.qualersoft.jmeter.gradleplugin.jmeter
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.jar.JarFile

@CacheableTask
open class JMeterSetupTask : DefaultTask() {

  @Nested
  protected val jmTool = project.jmeter().tool

  @OutputDirectory
  protected val jmToolDir: Provider<Directory> = project.layout.buildDirectory.dir("jmeter")

  private val jmBinDir = jmToolDir.map { it.dir("bin") }
  private val jmLibDir = jmToolDir.map { it.dir("lib") }
  private val jmExtDir = jmLibDir.map { it.dir("ext") }
  private val jmJUnitDir = jmLibDir.map { it.dir("junit") }

  private val sourceJmJar by lazy { getJMeterLib() }

  @get:OutputFile
  internal val jmJar: RegularFileProperty = project.objects.fileProperty()
    .value(jmBinDir.map { it.file("${jmTool.name}-${jmTool.version}.jar") })

  init {
    group = "jmeter"
  }

  @TaskAction
  fun run() {
    prepareDirectories()

    // copy jmeter-runner to bin dir
    sourceJmJar.copyTo(jmJar.asFile.get(), true)

    val resourceJar = getJMeterResourceLib()
    CopyResource.extractJarToDir(JarFile(resourceJar), jmToolDir.get().asFile)

    resolveAndCopyExtensionLibs()
    resolveAndCopyToolLibs()
  }

  /**
   * Gets the jmeter-runner library jar file.
   */
  private fun getJMeterLib(): File {
    val artifacts: Set<ResolvedArtifact> = project.configurations
      .getByName(JMETER_RUNNER)
      .resolvedConfiguration
      .resolvedArtifacts

    return findArtifactMatch(artifacts, jmTool.group, jmTool.name)
  }

  /**
   * Gets the jmeter-resource library jar file.
   */
  private fun getJMeterResourceLib(): File {
    val artifacts: Set<ResolvedArtifact> = project.configurations
      .getByName(JMETER_RUNNER)
      .resolvedConfiguration
      .resolvedArtifacts

    val toolConfNot = jmTool.createToolConfigDependencyNotion()
    val toolConfName = toolConfNot["name"]!!
    return findArtifactMatch(artifacts, jmTool.group, toolConfName)
  }

  private fun findArtifactMatch(artifacts: Set<ResolvedArtifact>, group: String, name: String): File = checkNotNull(
    artifacts.find {
      val id = it.moduleVersion.id
      id.group == group && id.name == name
    }
  ).file

  /**
   * Resolve the extension libraries (aka. plugins) and copy them to the [jmExtDir].
   * Any transitive library of an extension will be copied to the [jmLibDir].
   */
  private fun resolveAndCopyExtensionLibs() {
    val resolvedExtensions = mutableListOf<ResolvedDependency>()
    project.configurations.getByName(JMETER_PLUGIN_DEPENDENCY)
      .resolvedConfiguration
      .firstLevelModuleDependencies
      .flatMap {
        resolvedExtensions.add(it)
        it.moduleArtifacts
      }
      .map { it.file }
      .forEach { it.copyToDir(jmExtDir.get().asFile) }

    resolvedExtensions
      .flatMap { it.children }
      // only take dependencies that were not already copied earlier
      .filterNot { resolvedExtensions.contains(it) }
      .flatMap { it.allModuleArtifacts }
      .map { it.file }
      .forEach { it.copyToDir(jmLibDir.get().asFile) }
  }

  /**
   * Resolve the tool libraries and copy them to the [jmLibDir].
   */
  private fun resolveAndCopyToolLibs() {
    project.configurations.getByName(JMETER_LIB_DEPENDENCY)
      .resolvedConfiguration
      .resolvedArtifacts
      .map { it.file }
      .forEach { it.copyToDir(jmLibDir.get().asFile) }
  }

  /**
   * Creates the directories required by jmeter.
   * Only create the directories that aren't exist.
   */
  private fun prepareDirectories() {
    jmToolDir.createIfNotExists()
    jmBinDir.createIfNotExists()
    jmLibDir.createIfNotExists()
    jmExtDir.createIfNotExists()
    jmJUnitDir.createIfNotExists()
  }

  private fun Provider<Directory>.createIfNotExists() {
    this.get().asFile.apply {
      if (!exists()) {
        mkdirs()
      }
    }
  }
}
