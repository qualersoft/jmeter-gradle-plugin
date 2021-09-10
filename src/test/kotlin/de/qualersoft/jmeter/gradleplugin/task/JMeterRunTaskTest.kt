package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.matchingEntry
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.contain
import io.kotest.matchers.should
import io.kotest.matchers.shouldHave
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotHave
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File

class JMeterRunTaskTest : JMeterTaskTestBase() {

  @Test
  fun withReportFlagEnabled() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      jmxFile.set("Test.jmx")
      generateReport = true
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { withClue("No gui flag") { args should contain("-n") } },
      { withClue("Enable report flag") { args should contain("-e") } },
      { withClue("Output param") { args should contain("-o") } },
      { args shouldHave entryEndsWith("reports${File.separatorChar}jmeter${File.separatorChar}Test") }
    )
  }

  @Test
  fun withReportFlagDisabled() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      jmxFile.set("Test.jmx")
      generateReport = false
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { withClue("Enable report flag") { args shouldNot contain("-e") } },
      { withClue("Output param") { args shouldNot contain("-o") } },
      { args shouldNotHave entryEndsWith("reports${File.separatorChar}jmeter") }
    )
  }

  @Test
  fun argsWithGlobalPropertyFile() {
    val propFile = "GlobPropFile.properties"
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      globalPropertiesFile.set(File(propFile))
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldHave matchingEntry("-G[^=]+$propFile".toRegex())
  }

  @Test
  fun argsWithGlobalProperties() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      globalProperties.put("Global", "property")
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-GGlobal=property")
  }

  @Test
  fun withDeleteFlag() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      deleteResults = true
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-f")
  }

  @Test
  fun withoutDeleteFlag() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      deleteResults = false
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldNot contain("-f")
  }
}
