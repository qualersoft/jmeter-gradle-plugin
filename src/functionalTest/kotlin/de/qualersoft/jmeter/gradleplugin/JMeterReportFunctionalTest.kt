package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class JMeterReportFunctionalTest : JMeterPluginFunctionalTestBase() {
  
  init {
    rootFolder = { "reportTest" }
  }

  @Test
  fun `report with default template`() {
    val runner = setupTest("default_build").withArguments("runTest", "reportTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val expectedReportFile = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    assertAll(
      { runShouldSucceed(result) },
      { expectedReportFile should exist() }
    )
  }

  @Test
  fun `report with custom template`() {
    val runner = setupTest("ownReport_build").withArguments("runTest", "reportTest")
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
  fun `report with custom template from extension`() {
    val runner = setupTest("ownReportFromExtension_build").withArguments("runTest", "reportTest")
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
