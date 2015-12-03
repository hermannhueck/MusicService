package musicsvc.util

import java.io.File
import java.nio.file.{Path, StandardCopyOption}
import java.util.UUID

import scala.util.Random

object Utils {

  def copyToTmpFile(srcFilename: String): String = {
    val tmpDir = "TempFiles"
    createDirIfNotExists(tmpDir)
    val tmpFilename = tmpDir + "/" + UUID.randomUUID().toString
    copyFromTo(srcFilename, tmpFilename)
    tmpFilename
  }

  def copyFromTo(srcFilename: String, destFilename: String): Path = {
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
    val filenames: Array[String] = new File(dir).list()
    val randomIndex = Random.nextInt(filenames.length)
    dir + "/" + filenames(randomIndex)
  }
}
