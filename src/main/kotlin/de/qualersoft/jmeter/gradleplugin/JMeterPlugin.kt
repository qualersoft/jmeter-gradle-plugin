package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete
import org.gradle.internal.execution.BuildOutputCleanupRegistry
import org.gradle.language.base.internal.plugins.CleanRule
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val EXTENSION_NAME = "jmeter"

const val JMETER_RUNNER = "jmeterRunner"
const val JMETER_PLUGIN_DEPENDENCY = "jmeterPlugin"
const val JMETER_LIB_DEPENDENCY = "jmeterLibrary"

class JMeterPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val jmExt = project.extensions.create(
      EXTENSION_NAME,
      JMeterExtension::class.java,
      project
    )

    val javaPlg = JavaPlugin::class.java
    if (!project.plugins.hasPlugin(javaPlg)) {
      project.plugins.apply(javaPlg)
    }

    registerConfiguration(project)
    project.afterEvaluate {
      registerTasks(it)

      // register the jmeter tool with it's desired version
      val jmRunner = project.configurations.named(JMETER_RUNNER).get()
      jmExt.tool.applyTo(jmRunner)

      // apply jmeter extensions
      val jmComp = project.configurations.named(JMETER_PLUGIN_DEPENDENCY).get()
      jmExt.tool.applyApacheComponents(jmComp)
    }
  }

  private fun registerConfiguration(project: Project) {
    val runnerConf = project.configurations.maybeCreate(JMETER_RUNNER)
    runnerConf.isVisible = false
    runnerConf.description = "The jmeter runner to use. Only for internal purposes!"

    val jmComp = project.configurations.maybeCreate(JMETER_PLUGIN_DEPENDENCY)
    jmComp.description = "JMeter extensions like 3rd party plugins. See `jmCoreExt` for easy adding jmeter extensions."
    jmComp.isVisible = true
    jmComp.isCanBeConsumed = false
    jmComp.isCanBeResolved = true

    val tools = project.configurations.maybeCreate(JMETER_LIB_DEPENDENCY)
    tools.description = "Additional tool libraries that can be used within jmeter scripts. E.g. apache-commons"
  }

  private fun registerTasks(project: Project) {
    registerClean(project as ProjectInternal)
  }

  private fun registerClean(project: ProjectInternal) {
    if (null == project.tasks.findByName(LifecycleBasePlugin.CLEAN_TASK_NAME)) {
      // Register clean task (Taken from LifecycleBasePlugin.addClean)
      val buildDir = project.layout.buildDirectory
      val buildOutputCleanupRegistry = project.services.get(BuildOutputCleanupRegistry::class.java)
      buildOutputCleanupRegistry.registerOutputs(buildDir)

      val clean = project.tasks.register(LifecycleBasePlugin.CLEAN_TASK_NAME, Delete::class.java) {
        it.description = "Deletes the build directory"
        it.group = LifecycleBasePlugin.BUILD_GROUP
        it.delete(buildDir)
      }
      buildOutputCleanupRegistry.registerOutputs(clean.map {
        it.targetFiles
      })

      // Register clean rule (Taken from LifecycleBasePlugin.addCleanRule)
      project.tasks.addRule(CleanRule(project.tasks))
    }
  }
}

internal fun ObjectFactory.propertyMap() = this.mapProperty(String::class.java, String::class.java)

/**
 * Main
 */
internal fun Project.jmeter(): JMeterExtension = extensions.getByName(EXTENSION_NAME) as? JMeterExtension
  ?: throw IllegalStateException("$EXTENSION_NAME is not of the correct type")
