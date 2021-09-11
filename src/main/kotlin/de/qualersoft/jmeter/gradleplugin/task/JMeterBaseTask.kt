package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import de.qualersoft.jmeter.gradleplugin.JMETER_LIB_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_PLUGIN_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_RUNNER
import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.copyToDir
import de.qualersoft.jmeter.gradleplugin.propertyMap
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.jar.JarFile

/**
 * Base task for all JMeter*Tasks.
 * Take care of proper preparation of jmeter runtime.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Abstract base class")
abstract class JMeterBaseTask : JavaExec() {

  private val log: Logger = Logging.getLogger(javaClass)

  @Internal
  protected val jmExt: JMeterExtension = project.extensions.getByType(JMeterExtension::class.java)

  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val jmSystemPropertyFiles: ConfigurableFileCollection = objectFactory.fileCollection()
    .from(jmExt.systemPropertyFiles)

  @Input
  @Optional
  val jmSystemProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.systemProperties)

  /**
   * Main jmeter property file.
   *
   * Inherited from [JMeterExtension.mainPropertyFile].
   */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val mainPropertyFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.mainPropertyFile)

  /**
   * Additional property files.
   *
   * Inherited from [JMeterExtension.additionalPropertyFiles].
   * To override use `setFrom`, to extend use `from`.
   */
  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val additionalPropertyFiles: ConfigurableFileCollection = objectFactory.fileCollection()
    .from(jmExt.additionalPropertyFiles)

  /**
   * Dedicated properties send to local JMeter only.
   *
   * Inherited from [JMeterExtension.jmeterProperties].
   */
  @Input
  @Optional
  val jmeterProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.jmeterProperties)

  /**
   * File where jmeter log will be written to.
   *
   * Inherited from [JMeterExtension.logOutputFile].
   */
  @OutputFile
  @Optional
  val logOutputFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.logOutputFile)

  /**
   * The jmx-file to use. Absolute or relative file.
   * If relative it will be resolved against [JMeterExtension.jmxRootDir].
   * Attention: Even if optional, [Run][JMeterRunTask] and [Report][JMeterReportTask] tasks
   * require it!
   */
  @Input
  @Optional
  val jmxFile: Property<String> = objectFactory.property(String::class.java)

  /**
   * The source-jmx-file to use for execution. Will be computed based on [JMeterExtension.jmxRootDir] and [jmxFile].
   *
   * Just for internal usage
   */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  internal val sourceFile: RegularFileProperty = objectFactory.fileProperty().fileProvider(resolveJmxFile())

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
    mainClass.value(jmExt.tool.mainClass)
  }

  private fun resolveJmxFile() = jmxFile.map {
    val file = File(it)
    if (file.isAbsolute) {
      file
    } else {
      jmExt.jmxRootDir.file(it).get().asFile
    }
  }

  @TaskAction
  override fun exec() {

    // create the folder structure required by jmeter
    val jmDir = project.buildDir.resolve("jmeter")
    jmDir.mkdir()
    val jmBin = jmDir.resolve("bin")

    val tmpJmJar = getJMeterLib()
    val jmJar = tmpJmJar.copyToDir(jmBin)
    classpath(project.files(jmJar))

    val resourceJar = getJMeterResourceLib()
    CopyResource.extractJarToDir(JarFile(resourceJar), jmDir)

    val libDir = jmDir.resolve("lib")
    val extDir = libDir.resolve("ext")

    resolveExtensionLibs(JMETER_PLUGIN_DEPENDENCY, extDir, libDir)
    resolveToolLibs(JMETER_LIB_DEPENDENCY, libDir)

    // not quite sure if required, maybe remove
    extDir.mkdirs()
    val junitDir = libDir.resolve("junit")
    junitDir.mkdirs()

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

  internal open fun createRunArguments() = mutableListOf<String>().apply {
    // system prop file first
    jmSystemPropertyFiles.forEach {
      add("-S")
      add(it.absolutePath)
    }

    // now normal sys-props
    jmSystemProperties.get().forEach { (t, u) ->
      add("-D$t=$u")
    }

    // main jmeter property file next
    if (mainPropertyFile.isPresent) {
      add("-p")
      add(mainPropertyFile.get().asFile.absolutePath)
    }

    additionalPropertyFiles.forEach {
      add("-q")
      add(it.absolutePath)
    }

    // normal jmeter props
    jmeterProperties.get().forEach { (k, v) ->
      add("-J$k=$v")
    }

    // log config
    if (jmExt.logConfig.isPresent) {
      add("-i")
      add(jmExt.logConfig.get().asFile.absolutePath)
    }
    // log output file
    if (logOutputFile.isPresent) {
      add("-j")
      add(logOutputFile.get().asFile.absolutePath)
    }
  }

  protected fun addJmxFile(args: MutableList<String>) = args.apply {
    add("-t")
    add(sourceFile.get().asFile.absolutePath) // test file
  }
}
