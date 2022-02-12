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

  init {
    rootFolder = { "runTest" }
  }
  
  @Test
  fun `register a run task in kotlin dsl`() {
    val runner = setupTest("default_build")
      .withArguments("tasks")
    copyJmxToDefaultLocation()

    val result = runner.build()

    assertTrue(result.output.contains("runTest"))
  }

  @Test
  fun `execute run task with minimum config`() {
    val runner = setupTest("default_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val resultFolder = runner.projectDir.resolve("build/test-results/jmeter")
    assertAll(
      { runShouldSucceed(result) },
      { resultFolder should exist() },
      { resultFolder.resolve("Test.jtl") should exist() }
    )
  }

  @Test
  fun `jmx-File from command line`() {
    val runner = setupTest("noJmxFileGiven_build").withArguments("runTest", "--test=Test.jmx")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val resultFolder = runner.projectDir.resolve("jmeter-results")
    assertAll(
      { runShouldSucceed(result) },
      { resultFolder should exist() },
      { resultFolder.resolve("Test.jtl") should exist() }
    )
  }

  @Test
  fun `use jmeter extension to configure global defaults`() {
    val runner = setupTest("useExtension_build").withArguments("runTest")
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
  fun `overriding maxHeap`() {
    val runner = setupTest("maxHeap_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    out shouldContain "Using maximum heap size of 16m."
  }

  @Test
  fun `set maxHeap by commandline`() {
    val runner = setupTest("default_build").withArguments("runTest", "--maxHeap=32m")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    out shouldContain "Using maximum heap size of 32m."
  }

  @Test
  fun `set jmeter property by commandline`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--J=aKey01=aValue01", "--J=aKey02=aValue02")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    out shouldContain "-JaKey01=aValue01, -JaKey02=aValue02,"
  }

  @Test
  fun `set non existing additional jmeter property file by commandline should fail`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--addprop=ImNotThere.properties")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    val expectedFile = runner.projectDir.resolve("ImNotThere.properties").normalize().absolutePath
    out shouldContain "-q, $expectedFile,"
  }

  @Test
  fun `set non existing jmeter property file by commandline should fail`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--propfile=ImNotThere.properties")
    copyJmxToDefaultLocation()

    val result = runner.buildAndFail()

    val out = result.output
    val expectedFile = runner.projectDir.resolve("ImNotThere.properties").normalize().absolutePath
    out shouldContain "mainPropertyFile' specifies file '$expectedFile' which doesn't exist."
  }

  @Test
  fun `set sys property by commandline`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--sysProp=aKey01=aValue01", "--sysProp=aKey02=aValue02")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    out shouldContain "-DaKey01=aValue01, -DaKey02=aValue02,"
  }

  @Test
  fun `set non existing system property file by commandline should fail`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--sysPropFile=ImNotThere1.properties", "--sysPropFile=ImNotThere2.properties")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    val expectedFile1 = runner.projectDir.resolve("ImNotThere1.properties").normalize().absolutePath
    val expectedFile2 = runner.projectDir.resolve("ImNotThere2.properties").normalize().absolutePath
    assertAll(
      { out shouldContain "-S, $expectedFile1," },
      { out shouldContain "-S, $expectedFile2," }
    )
  }

  @Test
  fun `set global properties by commandline should fail`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--G=aGlobalKey1=aGlobalValue1", "--G=aGlobalKey2=aGlobalValue2")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val out = result.output
    out shouldContain "-GaGlobalKey1=aGlobalValue1, -GaGlobalKey2=aGlobalValue2,"
  }

  @Test
  fun `set non existing global property file by commandline should fail`() {
    val runner = setupTest("default_build")
      .withArguments("runTest", "--GF=ImNotThere.properties")
    copyJmxToDefaultLocation()

    val result = runner.buildAndFail()

    val out = result.output
    val expectedFile = runner.projectDir.resolve("ImNotThere.properties").normalize().absolutePath
    out shouldContain "'globalPropertiesFile' specifies file '$expectedFile' which doesn't exist."
  }

  @Test
  fun `no jmx files specified and no present should fail`() {
    val runner = setupTest("noJmxFileGiven_build").withArguments("runTest")

    val result = runner.buildAndFail()

    runShouldFail(result, "Build failed with an exception")
  }

  @Test
  fun `run with report shall generate a report`() {
    val runner = setupTest("runWithReport_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val result = runner.build()

    val report = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    val out = result.output
    val fileNotFoundMessage =
      "java\\.io\\.FileNotFoundException:.*\\\\build\\\\jmeter\\\\bin\\\\reportgenerator\\.properties".toRegex()
    assertAll(
      { out shouldNot contain(fileNotFoundMessage) },
      { report should exist() }
    )
  }

  @Test
  fun `run with custom report`() {
    val runner = setupTest("runWithCustomReport_build").withArguments("runTest")
    copyJmxToDefaultLocation()

    val reportDir = runner.projectDir.resolve("custom-template")
    copyZipResourceTo("report-template.zip", reportDir)

    val result = runner.build()

    val report = runner.projectDir.resolve("build/reports/jmeter/Test/index.html")
    val out = result.output
    val fileNotFoundMessage =
      "java\\.io\\.FileNotFoundException:.*\\\\build\\\\jmeter\\\\bin\\\\reportgenerator\\.properties".toRegex()
    assertAll(
      { out shouldNot contain(fileNotFoundMessage) },
      { report should exist() },
      { report.readText() should contain("<title>My own") }
    )
  }
}
