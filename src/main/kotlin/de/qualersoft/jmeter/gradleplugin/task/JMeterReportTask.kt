package de.qualersoft.jmeter.gradleplugin.task

import java.io.File

open class JMeterReportTask : JMeterBaseTask() {

  override fun processResources(jmBinDir: File) {
    super.processResources(jmBinDir)
    copyRespectProperty(jmExt.tool.reportGeneratorPropertyFile, "reportgenerator.properties", jmBinDir)
    // copy report-template dir
    if(jmExt.tool.reportTemplateFolder.isPresent) {
      jmExt.tool.reportTemplateFolder.asFile.get().copyRecursively(jmBinDir, true)
    } else {
      // TODO copy form jar
    }
  }

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply { 
    val src = sourceFile.asFile.get()
    // protocol/log file
    add("-g")
    add(resultDirectory.file("${src.nameWithoutExtension}.jtl").get().asFile.absolutePath)
    // output dir
    add("-o")
    add(reportDir.file(src.nameWithoutExtension).get().asFile.absolutePath)

    // log file
    add("-j")
    add(resultDirectory.file("${src.nameWithoutExtension}.log").get().asFile.absolutePath)

    // user properties file goes first to allow override by dedicated user properties
    if (userPropertiesFile.isPresent) {
      add("-G${userPropertiesFile.get().asFile.absolutePath}")
    }

    userProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }

    if (deleteResults) {
      add("-f")
    }
  }
}