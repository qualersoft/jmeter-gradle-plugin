package de.qualersoft.jmeter.gradleplugin

import io.kotest.fp.firstOption
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

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

  @Test
  fun `plugin should register clean task by default`() {
    val prj = applyPlugin()
    val cleanTask = prj.getTasksByName("clean", false).firstOption().orNull()
    assertAll(
      { cleanTask shouldNot beNull() },
      { cleanTask?.description shouldContain "jmeter" }
    )
  }

  @Test
  fun `clean task will not be applied if already present`() {
    val prj = ProjectBuilder.builder().build().also { 
      it.plugins.apply("java")
      it.plugins.apply(PluginTestBase.PLUGIN_ID)
    }

    val cleanTask = prj.getTasksByName("clean", false).firstOption().orNull()
    assertAll(
      { cleanTask shouldNot beNull() },
      { cleanTask?.description shouldNot contain("jmeter") }
    )
  }

  private fun applyPlugin() = ProjectBuilder.builder().build().also {
    it.plugins.apply(PluginTestBase.PLUGIN_ID)
  }
}
