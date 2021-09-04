package de.qualersoft.jmeter.gradleplugin

import de.qualersoft.jmeter.gradleplugin.task.JMeterGuiTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

open class JMeterExtension(private val project: Project) {

  private val objects = project.objects
  private val layout = project.layout

  /**
   * Stores the settings of jMeter tool.
   * Use [tool] for configuration.
   *
   * @see [tool]
   */
  @get:Nested
  val tool: JMeterConfig = JMeterConfig(project)

  /**
   * Configure the jmeter tool.
   *
   * See [JMeterConfig] for details.
   */
  fun tool(action: Action<JMeterConfig>) {
    action.execute(tool)
  }

  /**
   * Root directory used by tasks to resolve its jmxFile.
   *
   * Defaults to `src/test/jmeter`
   */
  val jmxRootDir: DirectoryProperty = objects.directoryProperty().convention(
    layout.projectDirectory.dir("src/test/jmeter")
  )

  /**
   * Directory where to write the results of a jmeter run.
   *
   * Defaults to <buildDir>/test-results/jmeter
   */
  val resultDir: DirectoryProperty = objects.directoryProperty().convention(
    layout.buildDirectory.dir("test-results/jmeter")
  )

  /**
   * Directory where to put the reports.
   *
   * Defaults to `<buildDir>/reports/jmeter`
   */
  val reportDir: DirectoryProperty = objects.directoryProperty().convention(
    layout.buildDirectory.dir("reports/jmeter")
  )

  /**
   * Dedicated properties send to local JMeter only.
   */
  val jmeterProperties: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

  /**
   * Path to a JMeter property file which will be sent to all remote server.
   */
  val globalPropertiesFile: RegularFileProperty = objects.fileProperty()

  /**
   * Dedicated user properties send to all remote server.
   */
  val globalProperties: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

  /**
   * Declares the maximum heap size of the JVM process.
   */
  val maxHeap: Property<String> = objects.property(String::class.java)

  /**
   * JVM-arguments that will be passed to the java process which executes jMeter.
   */
  val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)

  /**
   * Creates task which starts the jMeter GUI.
   *
   * @param name Name under which the task will be registered.
   * @param configurationAction Optional additional configuration that will be applied to the task.
   */
  fun withGuiTask(name: String, configurationAction: ((JMeterGuiTask) -> Unit)? = null) {
    if (null != configurationAction) {
      project.tasks.register(name, JMeterGuiTask::class.java, configurationAction)
    } else {
      project.tasks.register(name, JMeterGuiTask::class.java)
    }
  }
}
