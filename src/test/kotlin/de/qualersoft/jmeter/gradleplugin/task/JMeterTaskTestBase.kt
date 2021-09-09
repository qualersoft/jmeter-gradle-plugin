package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.PluginTestBase
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder

open class JMeterTaskTestBase {

  lateinit var project: Project

  protected inline fun <reified T : Task> createTaskWithConfig(
    extConfig: JMeterExtension.() -> Unit,
    noinline taskConfig: T.() -> Unit
  ): TaskProvider<T> {
    return ProjectBuilder.builder().build().also {
      project = it
      it.plugins.apply(PluginTestBase.PLUGIN_ID)
      extConfig(it.extensions.getByType(JMeterExtension::class.java))
    }.tasks.register(T::class.simpleName!!, T::class.java, taskConfig)
  }

  protected inline fun <reified T : Task> createTask(config: JMeterExtension.() -> Unit): TaskProvider<T> {
    return ProjectBuilder.builder().build().also {
      project = it
      it.plugins.apply(PluginTestBase.PLUGIN_ID)
      config(it.extensions.getByType(JMeterExtension::class.java))
    }.tasks.register(T::class.simpleName!!, T::class.java)
  }
}
