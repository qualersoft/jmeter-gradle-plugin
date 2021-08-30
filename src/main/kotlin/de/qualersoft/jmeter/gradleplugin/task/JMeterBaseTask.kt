package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource.copyFromResourceFile
import de.qualersoft.jmeter.gradleplugin.JMETER_EXEC
import de.qualersoft.jmeter.gradleplugin.JMETER_EXTENSION
import de.qualersoft.jmeter.gradleplugin.JMETER_TOOL
import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.copyToDir
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Abstract base class")
abstract class JMeterBaseTask : JavaExec() {

  private val log: Logger = Logging.getLogger(javaClass)

  @Internal
  protected val jmExt = project.extensions.getByType(JMeterExtension::class.java)

  @Input
  val jmxFile: Property<String> = objectFactory.property(String::class.java)

  @Input
  @Optional
  val jmeterProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.jmeterProperties)

  @InputFile
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @Optional
  val globalPropertiesFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.globalPropertiesFile)

  @Input
  val globalProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.globalProperties)

  @InputFile
  @PathSensitive(PathSensitivity.ABSOLUTE)
  protected val sourceFile: RegularFileProperty = objectFactory.fileProperty()

  @OutputDirectory
  val resultDirectory: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.resultDir)

  /**
   * Whether to delete results if exists or not.
   * If `false` but results exists, jmeter fails!
   *
   * Defaults to `false`
   */
  @Input
  var deleteResults: Boolean = false

  /**
   * Directory where to create the report.
   *
   * Defaults to jmeter.reportDir
   */
  @OutputDirectory
  val reportDir: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.reportDir)

  @Input
  val maxHeap: Property<String> = objectFactory.property(String::class.java)
    .value(jmExt.maxHeap)

  init {
    group = "jmeter"

    sourceFile.convention {
      if (jmxFile.isPresent) {
        jmExt.jmxRootDir.file(jmxFile).get().asFile
      } else {
        log.lifecycle("No jmx file specified! Taking any from '${jmExt.jmxRootDir.get()}'.")
        jmExt.jmxRootDir.asFileTree.matching {
          it.include("*.jmx")
        }.singleFile
      }
    }

    mainClass.convention(jmExt.tool.mainClass)
  }

  /**
   * Copies default resources to jmeters bin dir.
   * Can be overridden if a task needs to copy additional resources.
   * Default resources are:
   * - log4j.xml
   * - jmeter.properties
   * - upgrade.properties
   * - saveservice.properties
   *
   * Function respects tool-configurations.
   * Remarks: Don't forget to call super!
   */
  protected open fun processResources(jmBinDir: File) {
    val tool = jmExt.tool
    copyRespectProperty(tool.logConfig, "log4j2.xml", jmBinDir)
    copyRespectProperty(tool.jmeterPropertyFile, "jmeter.properties", jmBinDir)
    copyRespectProperty(tool.upgradePropertyFile, "upgrade.properties", jmBinDir)
    copyRespectProperty(tool.saveServicePropertyFile, "saveservice.properties", jmBinDir)
  }

  protected fun copyRespectProperty(property: RegularFileProperty, resource: String, toDir: File) {
    if (property.isPresent) {
      property.asFile.get().copyToDir(toDir)
    } else {
      toDir.copyFromResourceFile(resource)
    }
  }

  @TaskAction
  override fun exec() {
    val tmpJmJar = getJMeterLib()

    // create the folder structure required by jmeter
    val jmDir = project.buildDir.resolve("jmeter")
    jmDir.mkdir()

    val jmBin = jmDir.resolve("bin")
    val jmJar = tmpJmJar.copyToDir(jmBin)
    processResources(jmBin)

    classpath(project.files(jmJar))

    val libDir = jmDir.resolve("lib")
    val extDir = libDir.resolve("ext")
    val junitDir = libDir.resolve("junit")

    extDir.mkdirs()
    junitDir.mkdirs()

    resolveExtensionLibs(JMETER_EXTENSION, extDir, libDir)
    resolveToolLibs(JMETER_TOOL, libDir)

    if (maxHeap.isPresent) {
      maxHeapSize = maxHeap.get()
    }
    jvmArgs(jmExt.jvmArgs.get())
    args(createRunArguments())
    log.lifecycle("Running jmeter with jvmArgs: {} and cmdArgs: {}", jvmArgs, args)
    super.exec()
  }

  private fun resolveExtensionLibs(confName: String, extDir: File, toolDir: File) {
    val resolvedExtenstions = mutableListOf<ResolvedDependency>()
    project.configurations.getByName(confName)
      .resolvedConfiguration.firstLevelModuleDependencies.flatMap {
        resolvedExtenstions.add(it)
        it.moduleArtifacts
      }.map {
        it.file
      }.forEach {
        it.copyToDir(extDir)
      }
    resolvedExtenstions.flatMap {
      it.children
    }.filterNot {
      // only take dependencies that were not already copied earlier
      resolvedExtenstions.contains(it)
    }.flatMap {
      it.allModuleArtifacts
    }.map {
      it.file
    }.forEach {
      it.copyToDir(toolDir)
    }
  }

  private fun resolveToolLibs(confName: String, toolDir: File) {
    project.configurations.getByName(confName)
      .resolvedConfiguration.resolvedArtifacts.map {
        it.file
      }.forEach {
        it.copyToDir(toolDir)
      }
  }

  private fun getJMeterLib(): File {
    val jmTool = jmExt.tool
    val artifacts: Set<ResolvedArtifact> = project.configurations
      .getByName(JMETER_EXEC)
      .resolvedConfiguration.resolvedArtifacts
    return artifacts.find {
      val id = it.moduleVersion.id
      id.group == jmTool.group &&
        id.name == jmTool.name
    }?.file!!
  }

  internal open fun createRunArguments() = mutableListOf<String>().apply {
    add("-t")
    val src = sourceFile.get().asFile
    add(src.absolutePath) // test file

    // result file
    add("-l")
    add(resultDirectory.file("${src.nameWithoutExtension}.jtl").get().asFile.absolutePath)

    // log file
    add("-j")
    add(resultDirectory.file("${src.nameWithoutExtension}.log").get().asFile.absolutePath)

    jmeterProperties.get().forEach { (k, v) ->
      add("-J$k=$v")
    }

    // global properties file goes first to allow override by dedicated global properties
    if (globalPropertiesFile.isPresent) {
      add("-G${globalPropertiesFile.get().asFile.absolutePath}")
    }

    globalProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }

    if (deleteResults) {
      add("-f")
    }
  }
}
