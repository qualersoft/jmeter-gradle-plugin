package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File

class JMeterSetupFunctionalTest : JMeterPluginFunctionalTestBase() {

  init {
    rootFolder = { "setupTest" }
  }

  @Test
  fun `use a different jmeter tool version`() {
    val runner = setupTest("version").withArguments("setupJMeter")
    val jmBinDir = runner.projectDir.resolve("build/jmeter/bin")
    runner.build()
    val jmJars = jmBinDir.listFiles { file: File ->
      "jar" == file.extension &&
        file.name.contains("ApacheJMeter")
    }?.toList() ?: listOf()

    assertAll(
      { jmJars shouldHaveSize 1 },
      { jmJars.first().name shouldContain "5.4.3" }
    )
  }

  @Test
  fun `tools group property can be changed`() {
    val runner = setupTest("group").withArguments("setupJMeter")
    val result = runner.buildAndFail()

    result.output shouldContain "com.example.dummy:$DEFAULT_NAME:$DEFAULT_VERSION"
  }

  @Test
  fun `tools name property can be changed`() {
    val runner = setupTest("name").withArguments("setupJMeter")
    val result = runner.buildAndFail()

    result.output shouldContain "$DEFAULT_GROUP:DummyName:$DEFAULT_VERSION"
  }

  @Test
  fun `tools dependency config can be changed`() {
    val runner = setupTest("config").withArguments("setupJMeter")
    val result = runner.build()

    result.output shouldContain "Reconfiguring tools dependencies"
  }

  @Test
  fun `tools dependency config can be changed in groovy dsl`() {
    val runner = setupTest("config", "gradle").withArguments("setupJMeter")
    val result = runner.build()

    result.output shouldContain "Reconfiguring tools dependencies in groovy"
  }

  /**
   * Defaults defined in [JMeterConfig]
   */
  companion object {
    const val DEFAULT_GROUP = "org.apache.jmeter"
    const val DEFAULT_NAME = "ApacheJMeter"
    const val DEFAULT_VERSION = "5.4.1"
  }
}
