package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldNot
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class JMeterPluginTest {

  @Test
  fun `plugin should register jmeter extension`() {
    // Create a test project and apply the plugin
    val project = applyPlugin()

    project.extensions.findByName("jmeter") shouldNot beNull()
  }

  @Test
  fun `plugin should register configuration to tool jar`() {
    val project = applyPlugin()
    project.configurations.findByName(JMETER_LIB_DEPENDENCY) shouldNot beNull()
  }

  @Test
  fun `plugin should register extension configuration`() {
    val prj = applyPlugin()
    prj.configurations.findByName(JMETER_PLUGIN_DEPENDENCY) shouldNot beNull()
  }

  @Test
  fun `plugin should register tool configuration`() {
    val prj = applyPlugin()
    prj.configurations.findByName(JMETER_LIB_DEPENDENCY) shouldNot beNull()
  }

  private fun applyPlugin() = ProjectBuilder.builder().build().also {
    it.plugins.apply(PluginTestBase.PLUGIN_ID)
  }
}
