package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

/**
 * Task to start the jmeter gui.
 */
@DisableCachingByDefault(because = "Gui can be started always")
open class JMeterGuiTask : JMeterBaseTask() {

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

  /**
   * Dedicated user properties send to all remote server.
   *
   * Inherited from [JMeterExtension.globalProperties]
   */
  @Input
  val globalProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)
    .value(jmExt.globalProperties)

  init {
    outputs.upToDateWhen {
      false
    }
  }

  override fun createRunArguments() = mutableListOf<String>().apply {
    addAll(super.createRunArguments())

    // global properties file goes first to allow override by dedicated global properties
    if (globalPropertiesFile.isPresent) {
      add("-G${globalPropertiesFile.get().asFile.absolutePath}")
    }
    globalProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }
    if (sourceFile.isPresent) {
      addJmxFile(this)
    }
  }
}
