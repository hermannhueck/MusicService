package musicWebApp.util

import java.io.{FileNotFoundException, File}
import java.nio.file.{StandardCopyOption, Path}
import java.util.UUID

import scala.util.Random

object FileUtils {

  val tmpDir = "TempFiles"

  def copyToTmpFile(srcFilename: String): String = {
    createDirIfNotExists(tmpDir)
    val tmpFilename = tmpDir + "/" + UUID.randomUUID().toString
    copyFromTo(srcFilename, tmpFilename)
    tmpFilename
  }

  def copyFromTo(srcFilename: String, destFilename: String): Path = {
    val destDir = destFilename.substring(0, destFilename.lastIndexOf('/'))
    createDirIfNotExists(destDir)
    java.nio.file.Files.copy(new File(srcFilename).toPath,
      new File(destFilename).toPath,
      StandardCopyOption.REPLACE_EXISTING)
  }

  def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }

  def removeFileIfExists(filename: String): Unit = {
    java.nio.file.Files.deleteIfExists(new File(filename).toPath)
  }

  def randomDataFile(implicit dir: String): String = {
    val d = new File(dir)
    if (!d.exists()) throw new FileNotFoundException("Directory doesn't exist: " + dir)
    val filenames: Array[String] = new File(dir).list()
    if (filenames.isEmpty) throw new FileNotFoundException("No sound files available in: " + dir)
    val randomIndex = Random.nextInt(filenames.length)
    dir + "/" + filenames(randomIndex)
  }

  def dataPath(id: Long, dataDir: String): String = dataDir + "/" + "recording_" + id + ".mp3"
}
