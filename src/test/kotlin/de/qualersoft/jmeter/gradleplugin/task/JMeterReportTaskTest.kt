package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.entryStartsWith
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

class JMeterReportTaskTest : JMeterTaskTestBase() {

  @Test
  fun argumentGeneration() {
    val task = createTaskWithConfig<JMeterReportTask>({}, {
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    val sep = File.separatorChar
    assertAll(
      { withClue("result file flag") { args should contain("-g") } },
      { args shouldHave entryEndsWith("Report.jtl") },
      { withClue("report dir flag") { args should contain("-o") } },
      { args shouldHave entryEndsWith("reports${sep}jmeter${sep}Report") },
      { withClue("log file flag") { args should contain("-j") } },
      { args shouldHave entryEndsWith("Report.log") },
      { withClue("No global properies") { args shouldNotHave entryStartsWith("-G") } },
      { withClue("No delete flag") { args shouldNot contain("-f") } }
    )
  }

  @Test
  fun argsWithDelete() {
    val task = createTaskWithConfig<JMeterReportTask>({}, {
      deleteResults = true
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-f")
  }

  @Test
  fun argsWithGlobalPropertyFile() {
    val propFile = "GlobPropFile.properties"
    val task = createTaskWithConfig<JMeterReportTask>({}, {
      globalPropertiesFile.set(File(propFile))
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldHave matchingEntry("-G[^=]+$propFile".toRegex())
  }

  @Test
  fun argsWithGlobalProperties() {
    val task = createTaskWithConfig<JMeterReportTask>({}, {
      globalProperties.put("Global", "property")
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-GGlobal=property")
  }

  @Test
  fun argsWithJMeterPropertiesFromExtension() {
    val task = createTaskWithConfig<JMeterReportTask>({
      jmeterProperties.put("ABC", "xyz")
    }, {
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-JABC=xyz")
  }

  @Test
  fun argsWithJMeterPropertiesFromTask() {
    val task = createTaskWithConfig<JMeterReportTask>({}, {
      jmeterProperties.put("ASD", "roxx")
      jmxFile.set("Report.jmx")
    }).get()

    val result = task.createRunArguments()
    result should contain("-JASD=roxx")
  }
}
