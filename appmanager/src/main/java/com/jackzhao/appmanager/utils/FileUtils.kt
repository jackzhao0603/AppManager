package com.jackzhao.appmanager.utils

import android.annotation.SuppressLint
import android.util.Log
import com.jackzhao.appmanager.const.jackContext
import java.io.*
import java.util.*

object FileUtils {
    private val TAG = FileUtils::class.java.name
    fun chmod(file: File, mod: String): Boolean {
        return try {
            Runtime.getRuntime().exec(String.format("chmod %s %s", mod, file.absolutePath))
                .waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "chmod error: [$file] $mod")
            false
        }
    }

    fun rmdirs(file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    rmdirs(f)
                }
            }
        }
        file.delete()
    }

    fun copyFilesFromAssets(assetsPath: String, savePath: String) {
        try {
            val fileNames = jackContext!!.assets.list(assetsPath)
            if (fileNames!!.isNotEmpty()) {
                val file = File(savePath)
                file.mkdirs()
                for (fileName in fileNames) {
                    copyFilesFromAssets("$assetsPath/$fileName", "$savePath/$fileName")
                }
            } else {
                val inputStream = jackContext!!.assets.open(assetsPath)
                val fos = FileOutputStream(File(savePath))
                val buffer = ByteArray(1024)
                var byteCount = 0
                while (inputStream.read(buffer).also { byteCount = it } != -1) {
                    fos.write(buffer, 0, byteCount)
                }
                fos.flush()
                inputStream.close()
                fos.close()
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            Log.d(TAG, "copyFilesFromAssets: " + e.message)
        }
    }

    fun replace(path: String?, key: String?, newValue: String?) {
        var temp = ""
        try {
            val file = File(path)
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis)
            val br = BufferedReader(isr)
            var buf = StringBuffer()
            while (br.readLine().also { temp = it } != null) {
                buf = buf.append(temp.replace(key!!, newValue!!))
                buf = buf.append(System.getProperty("line.separator"))
            }
            br.close()
            val fos = FileOutputStream(file)
            val pw = PrintWriter(fos)
            pw.write(buf.toString().toCharArray())
            pw.flush()
            pw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copyfile(fromFile: File, toFile: File, deleteOld: Boolean): Boolean {
        if (!fromFile.exists()) {
            Log.e(TAG, "file not exist")
            return false
        }
        if (!fromFile.isFile) {
            Log.e(TAG, "file is not file")
            return false
        }
        if (!fromFile.canRead()) {
            Log.e(TAG, "file can not read")
            return false
        }
        if (!toFile.parentFile.exists()) {
            toFile.parentFile.mkdirs()
        }
        if (toFile.exists()) {
            toFile.delete()
        }
        var fosfrom: FileInputStream? = null
        var fosto: FileOutputStream? = null
        try {
            fosfrom = FileInputStream(fromFile)
            fosto = FileOutputStream(toFile)
            val bt = ByteArray(1024)
            var c: Int
            while (fosfrom.read(bt).also { c = it } > 0) {
                fosto.write(bt, 0, c)
            }
            if (deleteOld) {
                fromFile.delete()
            }
            return true
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            //关闭输入、输出流
            if (fosfrom != null) {
                try {
                    fosfrom.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (fosto != null) {
                try {
                    fosto.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        Log.e(TAG, "file copy failed")
        return false
    }

    fun readTxtFile(strFilePath: String): String {
        val content = StringBuffer()
        val file = File(strFilePath)
        if (file.isDirectory) {
            Log.d("TestFile", "The File doesn't not exist.")
        } else {
            try {
                val instream: InputStream = FileInputStream(file)
                if (instream != null) {
                    val inputreader = InputStreamReader(instream)
                    val buffreader = BufferedReader(inputreader)
                    var line: String
                    while (buffreader.readLine().also { line = it } != null) {
                        content.append(line + System.getProperty("line.separator"))
                    }
                    instream.close()
                }
            } catch (e: FileNotFoundException) {
                Log.d("TestFile", "The File doesn't not exist.")
            } catch (e: IOException) {
                Log.d("TestFile", e.message!!)
            }
        }
        return content.toString()
    }

    fun writeFileData(filename: String?, content: String) {
        try {
            val file = File(filename)
            val fos = FileOutputStream(file)
            val bytes = content.toByteArray()
            fos.write(bytes)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isFileExist(filename: String?): Boolean {
        val file = File(filename)
        return file.exists()
    }

    @Throws(FileNotFoundException::class)
    fun getFileLastLine(path: String?): String? {
        val fileScanner = Scanner(FileReader(path))
        var lastLine: String? = null
        while (fileScanner.hasNextLine() && fileScanner.nextLine().also { lastLine = it } != null) {
            if (!fileScanner.hasNextLine()) Log.i(TAG, lastLine!!)
        }
        return lastLine
    }

    fun getAppDir(): String? {
        var dir: String? = null
        dir = try {
            jackContext!!.packageManager.getPackageInfo(jackContext!!.packageName,
                0).applicationInfo.dataDir
        } catch (e: Exception) {
            jackContext!!.filesDir.parent
        }
        if (!dir!!.endsWith("/")) {
            dir += "/"
        }
        return dir
    }
}