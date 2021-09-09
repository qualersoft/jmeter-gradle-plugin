package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import de.qualersoft.jmeter.gradleplugin.CopyResource.copyFromResourceFile
import de.qualersoft.jmeter.gradleplugin.JMETER_LIB_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_PLUGIN_DEPENDENCY
import de.qualersoft.jmeter.gradleplugin.JMETER_RUNNER
import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.copyToDir
import de.qualersoft.jmeter.gradleplugin.propertyMap
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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
  protected val jmExt: JMeterExtension = project.extensions.getByType(JMeterExtension::class.java)

  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val jmSystemPropertyFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.sysPropertyFile)

  @Input
  @Optional
  val jmSystemProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.systemProperties)

  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val mainPropertyFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.mainPropertyFile)

  // TODO Not quite sure if this will work as expected
  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val additionalPropertyFiles: ConfigurableFileCollection = objectFactory.fileCollection()
    .from(jmExt.additionalPropertyFiles)

  /**
   * Dedicated properties send to local JMeter only.
   *
   * Inherited from [JMeterExtension.jmeterProperties]
   */
  @Input
  @Optional
  val jmeterProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.jmeterProperties)

  @Internal
  @Optional
  val logOutputFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.logOutputFile)

  /**
   * The jmx-file to use. If omitted, any jmx-file under [JMeterExtension.jmxRootDir] will be used.
   */
  @Input
  @Optional
  val jmxFile: Property<String> = objectFactory.property(String::class.java)

  /**
   * The source-jmx-file to use for execution. Will be computed based on [JMeterExtension.jmxRootDir] and [jmxFile].
   *
   * Just for internal usage
   */
  @InputFile
  @PathSensitive(PathSensitivity.ABSOLUTE)
  protected val sourceFile: RegularFileProperty = objectFactory.fileProperty().fileProvider(resolveJmxFile())

  /**
   * Directory where to store the results.
   *
   * Inherited from [JMeterExtension.resultDir]
   */
  @OutputDirectory
  // TODO: Maybe only for RunTask (maybe also GUI?)
  val resultDirectory: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.resultDir)


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
    mainClass.value(jmExt.tool.mainClass)
  }

  protected fun resolveJmxFile() = jmxFile.map {
      val file = File(it)
      if (file.isAbsolute) {
        file
      } else {
        jmExt.jmxRootDir.file(it).get().asFile
      }
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
    // TODO: Will be removed with #14
    copyRespectProperty(jmExt.logConfig, "log4j2.xml", jmBinDir)
    copyRespectProperty(jmExt.mainPropertyFile, "jmeter.properties", jmBinDir)
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

  /**
   * Copies the report-template either from given directory property if present or
   * from bundled to the jmeter-bin directory.
   *
   * @param reportTemplate The property pointing to the custom report template.
   * @param jmBinDir The target directory to which to copy the template.
   */
  protected fun copyReportTemplate(reportTemplate: DirectoryProperty, jmBinDir: File) {
    copyRespectProperty(jmExt.tool.reportGeneratorPropertyFile, "reportgenerator.properties", jmBinDir)
    // copy report-template dir
    val destReportTempDir = jmBinDir.resolve("report-template")
    if (reportTemplate.isPresent) {
      reportTemplate.asFile.get().copyRecursively(destReportTempDir, true)
    } else {
      // copy from jar
      // ensure directory exists
      if (destReportTempDir.exists()) {
        destReportTempDir.delete()
      }
      destReportTempDir.mkdirs()
      CopyResource.copyJarEntriesToFolder("report-template", destReportTempDir)
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

    processResources(jmBin)

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

  internal open fun createRunArguments() = mutableListOf<String>().apply {
    // system prop file first
    if (jmSystemPropertyFile.isPresent) {
      add("-S")
      add(jmSystemPropertyFile.get().asFile.absolutePath)
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
      add("-q${it.absolutePath}")
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

  protected fun addResultFile(args: MutableList<String>, forReport: Boolean) = args.apply {
    val file = sourceFile.get().asFile
    // result file
    if(forReport) {
      add("-g")
    } else {
      add("-l")
    }
    add(resultDirectory.file("${file.nameWithoutExtension}.jtl").get().asFile.absolutePath)
  }
  
  protected fun addReport(args: MutableList<String>) {
    val file = sourceFile.get().asFile
    args.add("-o")
    args.add(reportDir.file(file.nameWithoutExtension).get().asFile.absolutePath)
  }
  
  protected fun addDelete(args: MutableList<String>, deleteResults: Boolean) {
    if (deleteResults) {
      args.add("-f")
    }
  }
}
