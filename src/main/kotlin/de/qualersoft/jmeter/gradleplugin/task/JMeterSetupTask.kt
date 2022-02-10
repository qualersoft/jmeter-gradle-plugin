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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.jar.JarFile

@CacheableTask
open class JMeterSetupTask : DefaultTask() {

  private val jmExt = project.jmeter()

  private val jmToolDir = project.buildDir.resolve("jmeter")
  private val jmBinDir = jmToolDir.resolve("bin")
  private val jmLibDir = jmToolDir.resolve("lib")
  private val jmExtDir = jmLibDir.resolve("ext")
  private val jmJUnitDir = jmLibDir.resolve("junit")

  @get:Internal
  internal val jmJar: RegularFileProperty = project.objects.fileProperty()

  @TaskAction
  fun run() {
    prepareDirectories()

    // copy jmeter-runner to bin dir
    jmJar.set(getJMeterLib().copyToDir(jmBinDir))

    val resourceJar = getJMeterResourceLib()
    CopyResource.extractJarToDir(JarFile(resourceJar), jmToolDir)

    resolveAndCopyExtensionLibs()
    resolveAndCopyToolLibs()
  }

  /**
   * Gets the jmeter-runner library jar file.
   */
  private fun getJMeterLib(): File {
    val jmTool = jmExt.tool
    val artifacts: Set<ResolvedArtifact> = project.configurations
      .getByName(JMETER_RUNNER)
      .resolvedConfiguration.resolvedArtifacts

    return artifacts.find {
      val id = it.moduleVersion.id
      id.group == jmTool.group &&
        id.name == jmTool.name
    }?.file!!
  }

  /**
   * Gets the jmeter-resource library jar file.
   */
  private fun getJMeterResourceLib(): File {
    val jmTool = jmExt.tool
    val artifacts: Set<ResolvedArtifact> = project.configurations
      .getByName(JMETER_RUNNER)
      .resolvedConfiguration.resolvedArtifacts

    val toolConfNot = jmTool.createToolConfigDependencyNotion()
    val toolConfName = toolConfNot["name"]
    return artifacts.find {
      val id = it.moduleVersion.id
      id.group == jmTool.group &&
        id.name == toolConfName
    }?.file!!
  }

  /**
   * Resolve the extension libraries (aka. plugins) and copy them to the [jmExtDir].
   * Any transitive library of an extension will be copied to the [jmLibDir].
   */
  private fun resolveAndCopyExtensionLibs() {
    val resolvedExtensions = mutableListOf<ResolvedDependency>()
    project.configurations.getByName(JMETER_PLUGIN_DEPENDENCY)
      .resolvedConfiguration.firstLevelModuleDependencies.flatMap {
        resolvedExtensions.add(it)
        it.moduleArtifacts
      }.map {
        it.file
      }.forEach {
        it.copyToDir(jmExtDir)
      }
    resolvedExtensions.flatMap {
      it.children
    }.filterNot {
      // only take dependencies that were not already copied earlier
      resolvedExtensions.contains(it)
    }.flatMap {
      it.allModuleArtifacts
    }.map {
      it.file
    }.forEach {
      it.copyToDir(jmLibDir)
    }
  }

  /**
   * Resolve the tool libraries and copy them to the [jmLibDir].
   */
  private fun resolveAndCopyToolLibs() {
    project.configurations.getByName(JMETER_LIB_DEPENDENCY)
      .resolvedConfiguration.resolvedArtifacts.map {
        it.file
      }.forEach {
        it.copyToDir(jmLibDir)
      }
  }

  /**
   * Creates the directories required by jmeter.
   * Only create the directories that aren't exist.
   */
  private fun prepareDirectories() {
    if (!jmToolDir.exists()) {
      jmToolDir.mkdirs()
    }
    if (!jmBinDir.exists()) {
      jmBinDir.mkdirs()
    }
    if (!jmLibDir.exists()) {
      jmLibDir.mkdirs()
    }
    if (!jmExtDir.exists()) {
      jmExtDir.mkdirs()
    }
    if (!jmJUnitDir.exists()) {
      jmJUnitDir.mkdirs()
    }
  }
}
