package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.listProperty
import de.qualersoft.jmeter.gradleplugin.property
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
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
@DisableCachingByDefault(because = "Would love to execute jmeter tests more than once;)")
open class JMeterRunTask : JMeterExecBaseTask() {

  init {
    outputs.upToDateWhen { false }
  }

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

  // <editor-fold desc="Proxy configuration">
  /**
   * proxy scheme to use - optional - for non-http
   *
   * E.g. `https`
   */
  @Input
  @Optional
  val proxyScheme: Property<String> = objectFactory.property<String>()
    .value(jmExt.proxyScheme)

  @Option(
    option = "E",
    description = """proxy scheme to use - optional - for non-http
      Example: --E=https
    """
  )
  fun setProxyScheme(scheme: String) {
    proxyScheme.value(scheme)
  }

  /**
   * proxy server hostname or ip address
   */
  @Input
  @Optional
  val proxyHost: Property<String> = objectFactory.property<String>()
    .value(jmExt.proxyHost)

  @Option(
    option = "H",
    description = "proxy scheme to use - optional - for non-http"
  )
  fun setProxyHost(host: String) {
    proxyHost.value(host)
  }

  /**
   * proxy server port
   */
  @Input
  @Optional
  val proxyPort: Property<Int> = objectFactory.property<Int>()
    .value(jmExt.proxyPort)

  @Option(option = "PP", description = "proxy server port")
  fun setProxyPort(port: String) {
    port.toIntOrNull()?.let {
      proxyPort.value(it)
    } ?: throw IllegalArgumentException("Port must be a valid number! Got >$port<.")
  }

  /**
   * nonproxy hosts (e.g. *.apache.org, localhost)
   */
  @Input
  @Optional
  val nonProxyHosts: ListProperty<String> = objectFactory.listProperty<String>()
    .value(jmExt.nonProxyHosts)

  @Option(
    option = "N", description = """nonproxy hosts
        Usage:
        1) --N=*.apache.org --N=localhost
        2) --N *.apache.org --N localhost"""
  )
  fun setNonProxyHosts(hosts: List<String>) {
    nonProxyHosts.value(hosts)
  }

  @Input
  @Optional
  val username = objectFactory.property<String>()

  @Option(option = "u", description = "username for proxy authentication - if required")
  fun setUsername(name: String) {
    username.value(name)
  }

  @Input
  @Optional
  val password = objectFactory.property<String>()

  @Option(option = "pwd", description = "password for proxy authentication - if required")
  fun setPassword(pwd: String) {
    password.value(pwd)
  }

  @Input
  @Optional
  val enableRemoteExecution: Property<Boolean> = objectFactory.property<Boolean>()
    .value(jmExt.enableRemoteExecution)

  @Input
  @Optional
  val exitRemoteServers: Property<Boolean> = objectFactory.property<Boolean>()
    .value(jmExt.exitRemoteServers)
  // </editor-fold>

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

    if (proxyScheme.isPresent) {
      add("-E")
      add(proxyScheme.get())
    }

    if (proxyHost.isPresent) {
      add("-H")
      add(proxyHost.get())
    }

    if (proxyPort.isPresent) {
      add("-P")
      add(proxyPort.get().toString())
    }

    if (username.isPresent) {
      add("-u")
      val usr = username.get()
      maskOutput.add(usr)
      add(usr)
    }

    if (password.isPresent) {
      add("-a")
      val pwd = password.get()
      maskOutput.add(pwd)
      add(pwd)
    }

    if (nonProxyHosts.isPresent) {
      val nph = nonProxyHosts.get().joinToString("|") {
        it.trim()
      }
      if (nph.isNotEmpty()) {
        add("-N")
        add(nph)
      }
    }

    addJmxFile(this)

    addResultFile(this, false)

    if (generateReport) {
      add("-e")
      addReport(this)
    }

    addDelete(this)

    addRemoteArgs(this)
  }

  private fun addRemoteArgs(args: MutableList<String>) {
    if (enableRemoteExecution.get()) {
      args.add("-r")
      if (exitRemoteServers.get()) {
        args.add("-X")
      }
    } else if (exitRemoteServers.get()) {
      logger.warn(
        "The Flag `exitRemoteServer` is enabled, but `enableRemoteExecution` isn't! Check your configuration."
      )
    }
  }
}
