package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource.copyFromResourceFile
import de.qualersoft.jmeter.gradleplugin.JMETER_RUNNER
import de.qualersoft.jmeter.gradleplugin.JMETER_PLUGIN_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_LIB_DEPENDENCY
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

/**
 * Base task for all JMeter*Tasks.
 * Take care of proper preparation of jmeter runtime.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Abstract base class")
abstract class JMeterBaseTask : JavaExec() {

  private val log: Logger = Logging.getLogger(javaClass)

  @Internal
  protected val jmExt = project.extensions.getByType(JMeterExtension::class.java)

  /**
   * The jmx-file to use. If omitted, any jmx-file under [JMeterExtension.jmxRootDir] will be used.
   */
  @Input
  @Optional
  val jmxFile: Property<String> = objectFactory.property(String::class.java)

  /**
   * Dedicated properties send to local JMeter only.
   * 
   * Inherited from [JMeterExtension.jmeterProperties]
   */
  @Input
  @Optional
  val jmeterProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.jmeterProperties)

  /**
   * Path to a JMeter property file which will be sent to all remote server.
   * 
   * Inherited from [JMeterExtension.globalPropertiesFile]
   */
  @InputFile
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @Optional
  val globalPropertiesFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.globalPropertiesFile)

  /**
   * Dedicated user properties send to all remote server.
   * 
   * Inherited from [JMeterExtension.globalProperties]
   */
  @Input
  val globalProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.globalProperties)

  /**
   * The source-jmx-file to use for execution. Will be computed based on [JMeterExtension.jmxRootDir] and [jmxFile].
   * 
   * Just for internal usage
   */
  @InputFile
  @PathSensitive(PathSensitivity.ABSOLUTE)
  protected val sourceFile: RegularFileProperty = objectFactory.fileProperty()

  /**
   * Directory where to store the results.
   * 
   * Inherited from [JMeterExtension.resultDir]
   */
  @OutputDirectory
  val resultDirectory: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.resultDir)

  /**
   * Force jmeter to delete/override any existing output.
   * If `false` but output exists, jmeter fails!
   *
   * Defaults to `false`
   */
  @Input
  var deleteResults: Boolean = false

  /**
   * Directory where to create the report.
   *
   * Inherited from [JMeterExtension.reportDir]
   */
  @OutputDirectory
  val reportDir: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.reportDir)

  /**
   * 
   * Inherited from [JMeterExtension.maxHeap]
   */
  @Input
  @Optional
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
        }.first()
      }
    }

    mainClass.value(jmExt.tool.mainClass)
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

    resolveExtensionLibs(JMETER_PLUGIN_DEPENDENCY, extDir, libDir)
    resolveToolLibs(JMETER_LIB_DEPENDENCY, libDir)

    if (maxHeap.isPresent) {
      maxHeapSize = maxHeap.get()
      log.lifecycle("Using maximum heap size of $maxHeapSize.")
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
      .getByName(JMETER_RUNNER)
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
