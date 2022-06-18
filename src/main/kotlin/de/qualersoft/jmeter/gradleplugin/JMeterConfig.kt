package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Class to configure the jMeter tool.
 *
 * Provides settings for the used dependency of the main runner ([group], [name], [version], [main class][mainClass]).
 *
 * *Remark:*
 * Be careful when changing this properties.
 */
class JMeterConfig(private val project: Project) {

  private val logger: Logger = Logging.getLogger(JMeterConfig::class.java)

  private val objects = project.objects

  // <editor-fold desc="dependency settings">
  /**
   * The group id of the main library.
   * Used to resolve the `jmeter library` and `configuration library` within a repository.
   *
   * Default: `org.apache.jmeter`
   */
  @Input
  var group: String = "org.apache.jmeter"

  /**
   * The module name (artifact-id) of the main library.
   * Used to resolve the `jmeter library` and `configuration library` within a repository.
   *
   * Default: `ApacheJMeter`
   */
  @Input
  var name: String = "ApacheJMeter"

  /**
   * Overall version of jmeter.
   * Used to resolve the `jmeter library` and `configuration library` within a repository.
   *
   * Remark: If change make sure that additional provided resources like *.properties match!
   * To compare against the default properties, execute the `setupJMeter`-task and use the files in
   * the build/jmeter folder.
   *
   * Defaults to '5.4.1'
   */
  @Input
  var version: String = "5.4.1"

  /**
   * Closure that will be applied to the `dependency` declaration
   * for the main library.
   * Default: `null`
   */
  @Internal
  var mainConfigureClosure: (ExternalModuleDependency.() -> Unit)? = null

  /**
   * The fully qualified name of the Main class to be executed.
   *
   * Defaults to `org.apache.jmeter.NewDriver`
   */
  @Input
  val mainClass: Property<String> = objects.property(String::class.java)
    .convention("org.apache.jmeter.NewDriver")
  // </editor-fold>

  /**
   * Convenience method to add the jmeter tool dependency with the current setting to the project.
   */
  fun applyTo(config: Configuration) {
    config.dependencies.add(createJMeterLibDependency())
    val toolConfigDepNot = createToolConfigDependencyNotion()
    val toolConfigDep = project.dependencies.create(toolConfigDepNot)
    config.dependencies.add(applyBomWorkaround(toolConfigDep))
  }

  fun applyApacheComponents(config: Configuration) {
    listOf(
      "bolt", "components", "core", "ftp", "functions", "http", "java", "jdbc", "jms", "junit", "ldap",
      "mail", "mongodb", "native", "tcp"
    ).forEach {
      val depNot = jmeterDependency(it)
      val dep = project.dependencies.create(depNot)
      logger.debug("Adding dependency for {}", dep)
      config.dependencies.add(applyBomWorkaround(dep))
    }
  }

  private fun createJMeterLibDependency(): Dependency {
    val depNot = createToolDependencyNotation()
    val result = project.dependencies.create(depNot)
    mainConfigureClosure?.let {
      it(result as ExternalModuleDependency)
    }
    return applyBomWorkaround(result)
  }

  /**
   * Workaround for invalid bom reference in jmeter-module-descriptor.
   *
   * Details see [Issue 64465](https://bz.apache.org/bugzilla/show_bug.cgi?id=64465)
   */
  fun applyBomWorkaround(dependency: Dependency): Dependency {
    if (dependency is ExternalModuleDependency) {
      dependency.exclude(mapOf("group" to group, "module" to "bom"))
    }
    return dependency
  }

  private fun createToolDependencyNotation(): Map<String, String> = mutableMapOf<String, String>().also { res ->
    res["group"] = group
    res["name"] = name
    res["version"] = version
  }

  fun createToolConfigDependencyNotion(): Map<String, String> = jmeterDependency("config")

  /**
   * Creates a dependency notation for a jmeter core extension where group is [group].
   *
   * @param name The name of the extension. The `ApacheJMeter_` will be prepended.
   * @param version The version to use, defaults to [version]
   */
  fun jmeterDependency(name: String, version: String? = null) = mutableMapOf<String, String>().also {
    it["group"] = group
    it["name"] = "ApacheJMeter_$name"
    it["version"] = version ?: this.version
  }
}
