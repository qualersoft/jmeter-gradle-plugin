package de.qualersoft.jmeter.gradleplugin

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
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

  @Test
  fun `can add additional libraries`() {
    val runner = setupTest("withAdditionalLib").withArguments("setupJMeter")
    val jmLibDir = runner.projectDir.resolve("build/jmeter/lib")
    runner.build()

    val libJars = jmLibDir.listFiles { file: File ->
      "jar" == file.extension &&
        file.name.contains("javax.annotation-api-1.3.2")
    }?.toList() ?: listOf()

    libJars should haveSize(1)
  }

  @Test
  fun `run two different setup tasks twice must execute both`() {
    val runner = setupTest("twoSetupTasks").withArguments("setupJMeter", "setup2")
    val result = runner.build()

    val output = result.output

    assertAll(
      { output should contain("> Task :setupJMeter") },
      { output should contain("> Task :setup2") },
      { output should contain("2 actionable tasks: 2 executed") }
    )
  }

  @Test
  fun `run same setup tasks twice must cache second run`() {
    val runner = setupTest("twoSetupTasks").withArguments(SETUP_TASK_NAME)
    var result = runner.build()
    val task1 = result.task(":$SETUP_TASK_NAME")

    result = runner.withArguments(SETUP_TASK_NAME).build()
    val task2 = result.task(":$SETUP_TASK_NAME")

    assertAll(
      { withClue("Outcome first run") { task1?.outcome shouldBe TaskOutcome.SUCCESS } },
      { withClue("Outcome second run") { task2?.outcome shouldBe TaskOutcome.UP_TO_DATE } }
    )
  }

  @Test
  fun `run same setup tasks twice with modification of tool must not cache second call`() {
    val runner = setupTest("withToolVersionFromCli").withArguments(SETUP_TASK_NAME)
    var result = runner.build()
    val task1 = result.task(":$SETUP_TASK_NAME")

    result = runner.withArguments(SETUP_TASK_NAME, "-PtoolVersion=5.4.2").build()
    val task2 = result.task(":$SETUP_TASK_NAME")
    // atm we do not clean
    val jars = testProjectDir.root.resolve("build/jmeter/bin").listFiles { _, name ->
      name.startsWith("ApacheJMeter")
    }!!

    assertAll(
      { withClue("Outcome first run") { task1?.outcome shouldBe TaskOutcome.SUCCESS } },
      { withClue("Outcome second run") { task2?.outcome shouldBe TaskOutcome.SUCCESS } },
      { jars shouldHaveSize 2 }
    )
  }

  /**
   * Defaults defined in [JMeterConfig][de.qualersoft.jmeter.gradleplugin.JMeterConfig]
   */
  companion object {
    const val DEFAULT_GROUP = "org.apache.jmeter"
    const val DEFAULT_NAME = "ApacheJMeter"
    const val DEFAULT_VERSION = "5.4.1"

    const val SETUP_TASK_NAME = "setupJMeter"
  }
}
