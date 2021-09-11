package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.logging.Logging
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarFile

object CopyResource {

  val logger = Logging.getLogger(CopyResource::class.java)

  fun extractJarToDir(jarFile: JarFile, targetDir: File) {
    jarFile.entries().toList().forEach {
      val filename = it.name
      logger.info("Going to copy: {}", filename)
      val destFile = File(targetDir, filename)
      if (it.isDirectory) {
        logger.info("{} is directory -> creating it", filename)
        destFile.mkdirs()
      } else {
        copyStream(jarFile.getInputStream(it), destFile.outputStream())
      }
    }
  }

  fun copyStream(srcStream: InputStream, destStream: OutputStream) {
    srcStream.use { src ->
      destStream.use { dest -> src.copyTo(dest) }
    }
  }
}

/**
 * Copies this file to a target directory
 */
fun File.copyToDir(destDir: File): File {
  val destFile = destDir.resolve(this.name)
  if (!destFile.exists()) {
    this.copyTo(destFile)
  }
  return destFile
}
