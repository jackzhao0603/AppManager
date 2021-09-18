package com.jackzhao.appmanager.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.jackzhao.appmanager.const.jackContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object ProcessUtils {
    private const val TAG = "ProcessUtils"


    @Volatile
    private var processName: String? = null
    fun getProcessName(): String? {
        if (!TextUtils.isEmpty(processName)) return processName
        processName = doGetProcessName()
        return processName
    }

    private fun doGetProcessName(): String? {
        val am = jackContext!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (proInfo in runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName
                }
            }
        }
        return jackContext!!.packageName
    }

    fun isMainProcess(): Boolean {
        val processName = getProcessName()
        val pkgName = jackContext!!.packageName
        return !(!TextUtils.isEmpty(processName) && !TextUtils.equals(processName, pkgName))
    }

    @Throws(IOException::class, InterruptedException::class)
    fun execute(
        cmds: Array<String?>?,
        envs: Array<String?>?,
        outputBuilder: StringBuilder?,
        errorBuilder: StringBuilder?,
    ): Int {
        val p = Runtime.getRuntime().exec(cmds, envs)
        StreamConsumer(p.inputStream, outputBuilder).start()
        StreamConsumer(p.errorStream, errorBuilder).start()
        return p.waitFor()
    }

    @Throws(IOException::class)
    fun execute(
        cmds: Array<String?>?,
        envs: Array<String?>?,
        outputCallback: StreamCallback?,
        errorCallback: StreamCallback?,
    ): Process {
        val p = Runtime.getRuntime().exec(cmds, envs)
        StreamConsumer(p.inputStream, outputCallback).start()
        StreamConsumer(p.errorStream, errorCallback).start()
        return p
    }

    private class StreamConsumer : Thread {
        private var inputStream: InputStream
        private var stringBuilder: StringBuilder? = null
        private var streamCallback: StreamCallback? = null

        constructor(inputStream: InputStream, stringBuilder: StringBuilder?) {
            this.inputStream = inputStream
            this.stringBuilder = stringBuilder
        }

        constructor(inputStream: InputStream, streamCallback: StreamCallback?) {
            this.inputStream = inputStream
            this.streamCallback = streamCallback
        }

        override fun run() {
            val br = BufferedReader(InputStreamReader(inputStream))
            var line: String
            try {
                while (br.readLine().also { line = it } != null) {
                    if (streamCallback != null) {
                        streamCallback!!.onReadLine(line)
                    } else if (stringBuilder != null) {
                        stringBuilder!!.append(line)
                        stringBuilder!!.append("\n")
                    } else {
                        Log.v(TAG, "StreamConsumer: $line")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "StreamConsumer error", e)
            }
        }
    }

    interface StreamCallback {
        fun onReadLine(line: String?)
    }
}