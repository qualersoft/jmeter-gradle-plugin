package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class JMeterReportFunctionalTest : JMeterPluginFunctionalTestBase() {

  @Test
  @KotlinTag
  fun `report with default template`() {
    rootFolder = { "reportTest" }
    val runner = setupKotlinTest("default_build").withArguments("runTest", "reportTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val expectedReportFile = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    assertAll(
      { runShouldSucceed(result) },
      { expectedReportFile should exist() }
    )
  }

  @Test
  @KotlinTag
  fun `report with custom template`() {
    rootFolder = { "reportTest" }
    val runner = setupKotlinTest("ownReport_build").withArguments("runTest", "reportTest")
    copyJmxToDefaultLocation()
    val reportDir = runner.projectDir.resolve("custom-template")
    copyZipResourceTo("report-template.zip", reportDir)

    val result = runner.build()

    val expectedReportFile = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    assertAll(
      { runShouldSucceed(result) },
      { expectedReportFile should exist() },
      { expectedReportFile.readText() should contain("<title>My own") }
    )
  }

  @Test
  @KotlinTag
  fun `report always use fresh template dir`() {
    rootFolder = { "reportTest" }
    val runner = setupKotlinTest("freshReport_build").withArguments("runTest", "reportTest")
    copyJmxToDefaultLocation()
    runner.build()

    val reportDir = runner.projectDir.resolve("custom-template")
    copyZipResourceTo("report-template.zip", reportDir)
    val result = runner.withArguments("customReport").build()

    val expectedReportFile = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    assertAll(
      { runShouldSucceed(result) },
      { expectedReportFile should exist() },
      { expectedReportFile.readText() should contain("<title>My own") }
    )
  }

  @Test
  @KotlinTag
  fun `report with custom template from extension`() {
    rootFolder = { "reportTest" }
    val runner = setupKotlinTest("ownReportFromExtension_build").withArguments("runTest", "reportTest")
    copyJmxToDefaultLocation()
    val reportDir = runner.projectDir.resolve("custom-template")
    copyZipResourceTo("report-template.zip", reportDir)

    val result = runner.build()

    val expectedReportFile = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    assertAll(
      { runShouldSucceed(result) },
      { expectedReportFile should exist() },
      { expectedReportFile.readText() should contain("<title>My own") }
    )
  }
}
