package de.qualersoft.jmeter.gradleplugin

import de.qualersoft.jmeter.gradleplugin.task.JMeterSetupTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.Delete
import org.gradle.internal.execution.BuildOutputCleanupRegistry
import org.gradle.language.base.internal.plugins.CleanRule
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val EXTENSION_NAME = "jmeter"

const val JMETER_RUNNER = "jmeterRunner"
const val JMETER_PLUGIN_DEPENDENCY = "jmeterPlugin"
const val JMETER_LIB_DEPENDENCY = "jmeterLibrary"

const val JMETER_SETUP_TASK_NAME = "setupJMeter"

@Suppress("unused")
class JMeterPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val jmExt = project.extensions.create(
      EXTENSION_NAME,
      JMeterExtension::class.java,
      project
    )

    registerConfiguration(project)
    registerToolSetup(project)

    project.afterEvaluate {
      // we register clean task in afterEvaluate to do not depend on plugin registration order
      registerClean(it as ProjectInternal)

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
    applyResolutionStrategyFor(runnerConf, project)

    val jmComp = project.configurations.maybeCreate(JMETER_PLUGIN_DEPENDENCY)
    jmComp.description = "JMeter extensions like 3rd party plugins. See `jmCoreExt` for easy adding jmeter extensions."
    jmComp.isVisible = true
    jmComp.isCanBeConsumed = false
    jmComp.isCanBeResolved = true
    applyResolutionStrategyFor(jmComp, project)

    val tools = project.configurations.maybeCreate(JMETER_LIB_DEPENDENCY)
    tools.description = "Additional tool libraries that can be used within jmeter scripts. E.g. apache-commons"
    applyResolutionStrategyFor(tools, project)
  }

  private fun applyResolutionStrategyFor(config: Configuration, project: Project) {
    config.attributes {
      val libCat = project.objects.named(Category::class.java, Category.LIBRARY)
      it.attribute(Category.CATEGORY_ATTRIBUTE, libCat)
      val libBundling = project.objects.named(Bundling::class.java, Bundling.EXTERNAL)
      it.attribute(Bundling.BUNDLING_ATTRIBUTE, libBundling)
    }
  }

  private fun registerClean(project: ProjectInternal) {
    if (null == project.tasks.findByName(LifecycleBasePlugin.CLEAN_TASK_NAME)) {
      // Register clean task (Taken from LifecycleBasePlugin.addClean)
      val buildDir = project.layout.buildDirectory
      val buildOutputCleanupRegistry = project.services.get(BuildOutputCleanupRegistry::class.java)
      buildOutputCleanupRegistry.registerOutputs(buildDir)

      val clean = project.tasks.register(LifecycleBasePlugin.CLEAN_TASK_NAME, Delete::class.java) {
        it.description = "Deletes the build directory (added by jmeter-task)"
        it.group = LifecycleBasePlugin.BUILD_GROUP
        it.delete(buildDir)
      }
      buildOutputCleanupRegistry.registerOutputs(clean.map { it.targetFiles })

      // Register clean rule (Taken from LifecycleBasePlugin.addCleanRule)
      project.tasks.addRule(CleanRule(project.tasks))
    }
  }

  private fun registerToolSetup(project: Project) {
    project.tasks.register(JMETER_SETUP_TASK_NAME, JMeterSetupTask::class.java)
  }
}

/**
 * Main
 */
internal fun Project.jmeter(): JMeterExtension = checkNotNull(extensions.getByName(EXTENSION_NAME) as? JMeterExtension
) { "$EXTENSION_NAME is not of the correct type" }
