package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

/**
 * Task to execute jmeter through cli mode (no gui).
 *
 * This is the preferred way to run performance tests.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Would love to execute jmeter tests more than once;)")
open class JMeterRunTask : JMeterExecBaseTask() {

  /**
   * Path to a JMeter property file which will be sent to all remote server.
   *
   * Inherited from [JMeterExtension.globalPropertiesFile]
   */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val globalPropertiesFile: RegularFileProperty = objectFactory.fileProperty()
    .value(jmExt.globalPropertiesFile)

  @Option(option = "GF", description = "Define global properties file (sent to servers).")
  fun setGlobalPropertyFile(file: String) {
    globalPropertiesFile.set(project.file(file))
  }

  /**
   * Dedicated user properties send to all remote server.
   *
   * Inherited from [JMeterExtension.globalProperties]
   */
  @Input
  val globalProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.globalProperties)

  @Option(
    option = "G",
    description = """Define global properties (sent to servers).
        Attention to add a File use GF
        Usage:
        1) --G=Key1=Value1 --G=Key2=Value2
        2) --G Key1=Value1 --G Key2=Value2"""
  )
  fun setGlobalProperties(keyValues: List<String>) {
    globalProperties.putAll(parseCliListToMap(keyValues))
  }

  /**
   * If `true` the report will automatically be generated after executions.
   *
   * *Remark*: Consider to also enable [deleteResults] to avoid failures on rerun.
   *
   * Defaults to `false`
   */
  @Input
  var generateReport: Boolean = false

  override fun createRunArguments() = mutableListOf<String>().apply {
    add("-n") // no gui

    addAll(super.createRunArguments())

    // global properties file goes first to allow override by dedicated global properties
    if (globalPropertiesFile.isPresent) {
      add("-G${globalPropertiesFile.get().asFile.absolutePath}")
    }
    globalProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }

    addJmxFile(this)

    addResultFile(this, false)

    if (generateReport) {
      add("-e")
      addReport(this)
    }

    addDelete(this)
  }
}
