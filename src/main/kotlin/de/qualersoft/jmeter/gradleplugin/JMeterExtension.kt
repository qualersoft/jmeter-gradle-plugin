package de.qualersoft.jmeter.gradleplugin

import de.qualersoft.jmeter.gradleplugin.task.JMeterGuiTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Nested

open class JMeterExtension(private val project: Project) {

  @get:Nested
  val tool: JMeterConfig = JMeterConfig(project)
  fun tool(action: Action<JMeterConfig>) {
    action.execute(tool)
  }

  /**
   * Root directory used by tasks to resolve its jmxFile.
   *
   * Defaults to `<projectDirectory>/test/jmeter`
   */
  val jmxRootDir: DirectoryProperty = project.objects.directoryProperty().convention(
    project.layout.projectDirectory.dir("src/test/jmeter")
  )

  /**
   * Directory where to write the results of a jmeter run.
   * 
   * Defaults to <buildDir>/jmeter-result
   */
  val resultDir: DirectoryProperty = project.objects.directoryProperty().convention(
    project.layout.buildDirectory.dir("jmeter-result")
  )

  /**
   * Directory where to put the reports.
   *
   * Defaults to `<buildDir>/jmeter-report`
   */
  val reportDir: DirectoryProperty = project.objects.directoryProperty().convention(
    project.layout.buildDirectory.dir("jmeter-report")
  )

  val userPropertiesFile: RegularFileProperty = project.objects.fileProperty()
  val userProperties: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)

  /**
   * JMV-args that will be passed to the jmeter process
   */
  val jvmArgs: ListProperty<String> = project.objects.listProperty(String::class.java)

  fun withGuiTask(name: String, configurationAction: ((JMeterGuiTask) -> Unit)? = null) {
    if (null != configurationAction) {
      project.tasks.register(name, JMeterGuiTask::class.java, configurationAction)
    } else {
      project.tasks.register(name, JMeterGuiTask::class.java)
    }
  }
}
