package com.jackzhao.appmanager

import com.jackzhao.appmanager.utils.VersionUtils
import android.app.admin.DevicePolicyManager
import com.jackzhao.appmanager.utils.ReflectionUtils
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception

object SecurityManager {
    fun checkNonMarketAppEnabled(context: Context): Boolean {
        val result = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.INSTALL_NON_MARKET_APPS, 0
        )
        return result != 0
    }

    fun checkAdbDebugEnabled(context: Context): Boolean {
        if (VersionUtils.isAndroidJBMR1()) {
            val result = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            )
            return result != 0
        }
        val result = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ADB_ENABLED, 0
        )
        return result != 0
    }

    fun checkEncryptDeviceEnabled(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (VersionUtils.isAndroidJBMR1()) {
            val result = Settings.Secure.getInt(
                context.contentResolver,
                "require_password_to_decrypt", 0
            )
        }
        val status = dpm.storageEncryptionStatus
        return status >= DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING
    }

    fun isSecured(context: Context): Boolean {
        var isSecured = false
        val classPath = "com.android.internal.widget.LockPatternUtils"
        try {
            val lockPatternClass = Class.forName(classPath)
            val lockPatternObject = lockPatternClass.getConstructor(Context::class.java)
                .newInstance(context.applicationContext)
            val method = lockPatternClass.getMethod("isSecure")
            isSecured = method.invoke(lockPatternObject) as Boolean
            ReflectionUtils.getField(classPath, "isSecure", lockPatternObject)
        } catch (e: Exception) {
            isSecured = false
        }
        return isSecured
    }

    fun isDeviceSecure(context: Context): Boolean {
        return if (!VersionUtils.isAndroidM()) {
            val result = Settings.System.getInt(
                context.contentResolver,
                Settings.Secure.LOCK_PATTERN_ENABLED,
                0
            )
            result != 0 || isSecured(context)
        } else {
            val km =
                context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
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

    fun gotoSystemSetting(context: Context, settingAction: String?): Boolean {
        return try {
            val intent = Intent(settingAction)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun gotoSystemDeveloperSettings(context: Context): Boolean {
        return gotoSystemSetting(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    }

    fun gotoSystemSecuritySettings(context: Context): Boolean {
        return gotoSystemSetting(context, Settings.ACTION_SECURITY_SETTINGS)
    }

    fun gotoSystemLockScreenSettings(context: Context): Boolean {
        return try {
            val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}