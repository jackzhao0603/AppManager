package com.jackzhao.appmanager

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.jackzhao.appmanager.AppManager.gotoSystemSetting
import com.jackzhao.appmanager.const.jackContext
import com.jackzhao.appmanager.utils.ReflectionUtils
import com.jackzhao.appmanager.utils.VersionUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object SecurityManager {
    fun checkNonMarketAppEnabled(): Boolean {
        val result = Settings.Secure.getInt(
            jackContext!!.contentResolver,
            Settings.Secure.INSTALL_NON_MARKET_APPS, 0
        )
        return result != 0
    }

    fun checkAdbDebugEnabled(): Boolean {
        if (VersionUtils.isAndroidJBMR1()) {
            val result = Settings.Global.getInt(
                jackContext!!.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            )
            return result != 0
        }
        val result = Settings.Secure.getInt(
            jackContext!!.contentResolver,
            Settings.Secure.ADB_ENABLED, 0
        )
        return result != 0
    }

    fun checkEncryptDeviceEnabled(): Boolean {
        val dpm =
            jackContext!!.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (VersionUtils.isAndroidJBMR1()) {
            val result = Settings.Secure.getInt(
                jackContext!!.contentResolver,
                "require_password_to_decrypt", 0
            )
        }
        val status = dpm.storageEncryptionStatus
        return status >= DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING
    }

    fun isSecured(): Boolean {
        var isSecured = false
        val classPath = "com.android.internal.widget.LockPatternUtils"
        try {
            val lockPatternClass = Class.forName(classPath)
            val lockPatternObject = lockPatternClass.getConstructor(Context::class.java)
                .newInstance(jackContext!!.applicationContext)
            val method = lockPatternClass.getMethod("isSecure")
            isSecured = method.invoke(lockPatternObject) as Boolean
            ReflectionUtils.getField(classPath, "isSecure", lockPatternObject)
        } catch (e: Exception) {
            isSecured = false
        }
        return isSecured
    }

    fun isDeviceSecure(): Boolean {
        return if (!VersionUtils.isAndroidM()) {
            val result = Settings.System.getInt(
                jackContext!!.contentResolver,
                Settings.Secure.LOCK_PATTERN_ENABLED,
                0
            )
            result != 0 || isSecured()
        } else {
            val km =
                jackContext!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.isDeviceSecure
        }
    }

    val isDeviceRooted: Boolean
        get() = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val buffer = BufferedReader(InputStreamReader(process.inputStream))
            buffer.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }


    fun gotoSystemDeveloperSettings(): Boolean {
        return gotoSystemSetting(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    }

    fun gotoSystemSecuritySettings(): Boolean {
        return gotoSystemSetting(Settings.ACTION_SECURITY_SETTINGS)
    }

    fun gotoSystemLockScreenSettings(): Boolean {
        return try {
            val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}