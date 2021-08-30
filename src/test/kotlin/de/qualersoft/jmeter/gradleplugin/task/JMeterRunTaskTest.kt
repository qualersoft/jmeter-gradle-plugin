package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.entryEndsWith
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
      { args shouldHave entryEndsWith("reports${File.separatorChar}jmeter") }
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
}
