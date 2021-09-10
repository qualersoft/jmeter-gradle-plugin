package de.qualersoft.jmeter.gradleplugin

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property

/**
 * Class to configure the jMeter tool.
 * 
 * Provides settings for the used dependency of the main runner ([group], [name], [version], main class[mainClass]).
 * As well as configuration settings for:
 * - [report-template folder][reportTemplateDirectory],
 * - [reportgenerator.properties][reportGeneratorPropertyFile],
 * - [saveservice.properties][saveServicePropertyFile],
 * - [upgrade.properties][upgradePropertyFile]
 */
class JMeterConfig(private val project: Project) {

  private val logger: Logger = Logging.getLogger(JMeterConfig::class.java)

  private val objects = project.objects

  //<editor-fold desc="dependency settings">
  /**
   * The group id of the main library.
   * Used to resolve the library within a repository.
   * 
   * Default: `org.apache.jmeter`
   */
  val group: String = "org.apache.jmeter"

  /**
   * The module name (artifact-id) of the main library.
   * Used to resolve the library within a repository.
   * 
   * Default: `ApacheJMeter`
   */
  val name: String = "ApacheJMeter"

  /**
   * Overall version of jmeter.
   * 
   * Remark: If change make sure that the additional resources like *.properties match!
   * 
   * Defaults to '5.4.1'
   */
  val version: String = "5.4.1"

  /**
   * Closure that will be applied to the `dependency` declaration
   * for the main library.
   * Default: `null`
   */
  val mainConfigureClosure: Closure<ExternalModuleDependency>? = null

  /**
   * The fully qualified name of the Main class to be executed.
   *
   * Defaults to `org.apache.jmeter.NewDriver`
   */
  val mainClass: Property<String> = objects.property(String::class.java)
    .convention("org.apache.jmeter.NewDriver")
  //</editor-fold>

  //<editor-fold desc="Configuration files">

  /**
   * Path to the report-template folder required by the report generator.
   * Will be copied to jmeters bin directory.
   *
   * Remarks:
   * - Probably needs be changed if [version] has been changed.
   * - Will be copied as is!
   *
   * Defaults to the folder bundled with the plugin.
   */
  // TASK: Move to extension & task(s) -> in task has to be mapped as 
  //  additional jmeterproperty (-J) 'jmeter.reportgenerator.exporter.html.property.template_dir'
  val reportTemplateDirectory: DirectoryProperty = objects.directoryProperty()

  /**
   * Path to the reportgenerator.properties file required by the report generator.
   * Will be copied to jmeters bin directory.
   *
   * Remarks:
   * - Probably needs be changed if [version] has been changed.
   * - Will be copied as is!
   *
   * Defaults to the file bundled with the plugin.
   */
  val reportGeneratorPropertyFile: RegularFileProperty = objects.fileProperty()

  /**
   * Path to the saveservice.properties file required by jmeter.
   * Will be copied to jmeters bin directory.
   *
   * Remarks:
   * - Probably needs be changed if [version] has been changed.
   * - Will be copied as is!
   *
   * Defaults to the file bundled with the plugin.
   */
  val saveServicePropertyFile: RegularFileProperty = objects.fileProperty()

  /**
   * Path to the upgrade.properties file required by jmeter.
   * Will be copied to jmeters bin directory.
   *
   * Remarks:
   * - Probably needs be changed if [version] has been changed.
   * - Will be copied as is!
   *
   * Defaults to the file bundled with the plugin.
   */
  val upgradePropertyFile: RegularFileProperty = objects.fileProperty()
  //</editor-fold>

  /**
   * Convenience method to add the jmeter tool dependency with the current setting to the project.
   */
  fun applyTo(config: Configuration) {
    config.dependencies.add(createJMeterLibDependency())
  }

  fun applyApacheComponents(config: Configuration) {
    listOf("bolt", "components", "core", "ftp", "functions", "http", "java", "jdbc", "jms", "junit", "ldap",
      "mail", "mongodb", "native", "tcp").forEach { 
      val depNot = jmeterDependency(it)
      val dep = project.dependencies.create(depNot)
      logger.debug("Adding dependency for {}", dep)
      config.dependencies.add(applyBomWorkaround(dep))
    }
  }

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

  private fun createJMeterLibDependency(): Dependency {
    val depNot = createDependencyNotation()
    val cc = mainConfigureClosure
    val result = if (null != cc) {
      project.dependencies.create(depNot, cc)
    } else {
      project.dependencies.create(depNot)
    }
    return applyBomWorkaround(result)
  }

  /**
   * Workaround for invalid bom reference in jmeter-module-descriptor.
   * 
   * Details see [https://bz.apache.org/bugzilla/show_bug.cgi?id=64465]
   */
  fun applyBomWorkaround(dependency: Dependency): Dependency {
    if (dependency is ExternalModuleDependency) {
      dependency.exclude(mapOf("group" to group, "module" to "bom"))
    }
    return dependency
  }

  private fun createDependencyNotation(): Map<String, String> = mutableMapOf<String, String>().also { res ->
    res["group"] = group
    res["name"] = name
    res["version"] = version
  }
}
