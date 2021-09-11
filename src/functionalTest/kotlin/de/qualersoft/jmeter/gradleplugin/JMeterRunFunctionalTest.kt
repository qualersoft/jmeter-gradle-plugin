package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertTrue

/**
 * A simple functional test for the 'de.qualersoft.jmeter' plugin.
 */
class JMeterRunFunctionalTest : JMeterPluginFunctionalTestBase() {

  @Test
  @KotlinTag
  fun `register a run task in kotlin dsl`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("default_build")
      .withArguments("tasks")
    copyJmxToDefaultLocation()

    val result = runner.build()

    assertTrue(result.output.contains("runTest"))
  }

  @Test
  @GroovyTag
  fun `register a run task in groovy dsl`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("default_build")
      .withArguments("tasks")
    copyJmxToDefaultLocation()

    val result = runner.build()

    assertTrue(result.output.contains("runTest"))
  }

  @Test
  @KotlinTag
  fun `execute run task with minimum config`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("default_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.forwardOutput().build()

    val resultFolder = runner.projectDir.resolve("build/test-results/jmeter")
    assertAll(
      { runShouldSucceed(result) },
      { resultFolder should exist() },
      { resultFolder.resolve("Test.jtl") should exist() }
    )
  }

  @Test
  @KotlinTag
  fun `use jmeter extension to configure global defaults`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("useExtension_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val resultFolder = runner.projectDir.resolve("jmeter-results")
    val defaultResultFolder = runner.projectDir.resolve("build/test-results/jmeter")
    assertAll(
      { runShouldSucceed(result) },
      { resultFolder should exist() },
      { resultFolder.resolve("Test.jtl") should exist() },
      { defaultResultFolder shouldNot exist() }
    )
  }

  @Test
  @KotlinTag
  fun `overriding maxHeap`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("maxHeap_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    assertAll(
      out shouldContain "Using maximum heap size of 16m."
    )
  }

  @Test
  @KotlinTag
  fun `no jmx files specified and no present should fail`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("noJmxFileGiven_build").withArguments("runTest")

    val result = runner.buildAndFail()

    runShouldFail(result, "Build failed with an exception")
  }

  @Test
  @KotlinTag
  fun `run with report shall generate a report`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("runWithReport_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val report = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    val out = result.output
    assertAll(
      { out shouldNot contain("Problem loading properties\\. java\\.io\\.FileNotFoundException:.*\\\\build\\\\jmeter\\\\bin\\\\reportgenerator\\.properties".toRegex()) },
      { report should exist() }
    )
  }

  @Test
  @KotlinTag
  fun `run with custom report`() {
    rootFolder = { "runTest" }
    val runner = setupKotlinTest("runWithCustomReport_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val reportDir = runner.projectDir.resolve("custom-template")
    copyZipResourceTo("report-template.zip", reportDir)

    val result = runner.build()

    val report = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    val out = result.output
    assertAll(
      { out shouldNot contain("Problem loading properties\\. java\\.io\\.FileNotFoundException:.*\\\\build\\\\jmeter\\\\bin\\\\reportgenerator\\.properties".toRegex()) },
      { report should exist() },
      { report.readText() should contain("<title>My own") }
    )
  }
}
