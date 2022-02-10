package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMETER_SETUP_TASK_NAME
import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.propertyMap
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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Base task for all JMeter*Tasks.
 * Take care of proper preparation of jmeter runtime.
 */
@Suppress("UnstableApiUsage", "TooManyFunctions")
@DisableCachingByDefault(because = "Abstract base class")
abstract class JMeterBaseTask : JavaExec() {

  private val log: Logger = Logging.getLogger(javaClass)

  @Internal
  protected val jmExt: JMeterExtension = project.extensions.getByType(JMeterExtension::class.java)

  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val jmSystemPropertyFiles: ConfigurableFileCollection = objectFactory.fileCollection()
    .from(jmExt.systemPropertyFiles)

  @Option(option = "sysPropFile", description = "Additional system property file(s).")
  fun setJmSystemPropertyFiles(files: List<String>) {
    jmSystemPropertyFiles.setFrom(files)
  }

  @Input
  @Optional
  val jmSystemProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.systemProperties)

  @Option(
    option = "sysProp",
    description = """Define additional system properties.
        Usage:
        1) --sysProp=key1=value1 --sysProp=key2=value2
        2) --sysProp key1=value1 --sysProp key2=value2"""
  )
  fun setJmSystemProperties(keyValues: List<String>) {
    jmSystemProperties.putAll(parseCliListToMap(keyValues))
  }

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

  @Option(option = "propfile", description = "The jmeter property file to use.")
  fun setMainPropertyFile(path: String) {
    mainPropertyFile.set(project.file(path))
  }

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

  @Option(option = "addprop", description = "Additional JMeter property file(s).")
  fun setAdditionalPropertyFiles(files: List<String>) {
    additionalPropertyFiles.setFrom(files)
  }

  /**
   * Dedicated properties send to local JMeter only.
   *
   * Inherited from [JMeterExtension.jmeterProperties].
   */
  @Input
  @Optional
  val jmeterProperties: MapProperty<String, String> = objectFactory.propertyMap()
    .value(jmExt.jmeterProperties)

  @Option(
    option = "J",
    description = """Define additional JMeter properties.
        Usage:
        1) --J=Key1=Value1 --J=Key2=Value2
        2) --J Key1=Value1 --J Key2=Value2"""
  )
  fun setJmeterProperties(keyValues: List<String>) {
    jmeterProperties.putAll(parseCliListToMap(keyValues))
  }

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
  @Option(
    option = "test",
    description = "The jmeter test(.jmx) file to run. " +
      "If relative or just a file name, it will be resolved relative to the jmxRootDir."
  )
  val jmxFile: Property<String> = objectFactory.property(String::class.java)

  /**
   * The source-jmx-file to use for execution. Will be computed based on [JMeterExtension.jmxRootDir] and [jmxFile].
   *
   * Just for internal usage
   */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  @get:Optional
  internal val sourceFile: RegularFileProperty = objectFactory.fileProperty().fileProvider(resolveJmxFile())

  /**
   *
   * Inherited from [JMeterExtension.maxHeap]
   */
  @Input
  @Optional
  @Option(option = "maxHeap", description = "The maximum heap size of the JVM process.")
  val maxHeap: Property<String> = objectFactory.property(String::class.java)
    .value(jmExt.maxHeap)

  private val setupTask: TaskProvider<JMeterSetupTask> =
    project.tasks.named(JMETER_SETUP_TASK_NAME, JMeterSetupTask::class.java)

  private val jmToolJar: RegularFileProperty = objectFactory.fileProperty()
    .value(setupTask.map { it.jmJar.get() })

  init {
    group = "jmeter"
    mainClass.value(jmExt.tool.mainClass)
    super.dependsOn(setupTask)
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

    classpath(jmToolJar.get())

    if (maxHeap.isPresent) {
      maxHeapSize = maxHeap.get()
      log.lifecycle("Using maximum heap size of $maxHeapSize.")
    }

    jvmArgs(jmExt.jvmArgs.get())
    args(createRunArguments())
    log.lifecycle("Running jmeter with jvmArgs: {} and cmdArgs: {}", jvmArgs, args)
    super.exec()
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

  protected fun parseCliListToMap(keyValues: List<String>) = keyValues.associate {
    val (key, value) = it.split("=".toRegex(), 2)
    key to value
  }
}
