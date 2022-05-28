package de.qualersoft.jmeter.gradleplugin

import de.qualersoft.jmeter.gradleplugin.task.JMeterGuiTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Nested

/**
 * Central configuration class.
 * Herein configured properties will be used as default for any
 * JMeter*Task.
 */
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

  // <editor-fold desc="JMeter-Property configurations">
  /**
   * Additional system property file(s).
   */
  val systemPropertyFiles: ConfigurableFileCollection = objects.fileCollection()

  /**
   * Define additional system properties.
   */
  val systemProperties = objects.mapProperty<String, String>()

  /**
   * The jmeter property file to use.
   */
  val mainPropertyFile: RegularFileProperty = objects.fileProperty()

  /**
   * Additional JMeter property file(s).
   */
  val additionalPropertyFiles: ConfigurableFileCollection = objects.fileCollection()

  /**
   * Define additional JMeter properties.
   */
  val jmeterProperties = objects.mapProperty<String, String>()

  /**
   * Path to a custom report-template folder used by report generator.
   */
  val customReportTemplateDirectory: DirectoryProperty = objects.directoryProperty()

  /**
   * Path to a JMeter property file which will be sent to all servers.
   */
  val globalPropertiesFile: RegularFileProperty = objects.fileProperty()

  /**
   * Properties which will be sent to remote servers.
   */
  val globalProperties = objects.mapProperty<String, String>()
  // </editor-fold>

  // <editor-fold desc="Proxy configuration">
  /**
   * Proxy scheme to use - optional - for non-http
   */
  val proxyScheme = objects.property<String>()

  /**
   * Proxy server hostname or ip address
   */
  val proxyHost = objects.property<String>()

  /**
   * Proxy server port
   */
  val proxyPort = objects.property<Int>()

  /**
   * Non-proxy hosts (e.g. *.apache.org, localhost)
   */
  val nonProxyHosts = objects.listProperty<String>()
  // </editor-fold>

  // <editor-fold desc="Logging configuration">
  /**
   * Path to the logger-configuration file (attow `log4j2.xml`) required by jmeter.
   *
   * Defaults to the file bundled with the plugin.
   */
  val logConfig: RegularFileProperty = objects.fileProperty()

  /**
   * File where jmeter log will be written to.
   *
   * Defaults to `<buildDir>/logs/jmeter.log`
   */
  val logOutputFile: RegularFileProperty = objects.fileProperty().convention(
    layout.buildDirectory.file("logs/jmeter.log")
  )
  // </editor-fold>

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
   * Declares the maximum heap size of the JVM process.
   */
  val maxHeap = objects.property<String>()

  /**
   * JVM-arguments that will be passed to the java process which executes jMeter.
   */
  val jvmArgs = objects.listProperty<String>()

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
