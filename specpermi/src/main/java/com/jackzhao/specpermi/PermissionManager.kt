package com.jackzhao.specpermi

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import com.jackzhao.specpermi.utils.VersionUtils
import java.util.ArrayList
import java.util.NoSuchElementException

object PermissionManager {

    private const val TAG = "PermissionManager"

    fun isActiveAdmin(context: Context, packageName: String): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminList = dpm.activeAdmins
        if (adminList != null && adminList.isNotEmpty()) {
            for (componentName in adminList) {
                if (componentName.packageName == packageName) return true
            }
        }
        return false
    }

    fun isAppHideIcon(context: Context, pkg: String): Boolean {
        if (VersionUtils.isAndroidL()) {
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(pkg)
            val resolveinfoList = context.packageManager
                .queryIntentActivities(resolveIntent, 0)
            try {
                resolveinfoList.iterator().next()
            } catch (e: NoSuchElementException) {
                return true
            }
            return false
        }
        return false
    }


    fun getAllNotificationAccessApps(context: Context): Set<String?>? {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
    }

    fun checkNotificationAccessByPkg(context: Context, pkg: String): Boolean {
        return getAllNotificationAccessApps(context)?.contains(pkg) == true
    }


    fun getAllAccessbilityApps(context: Context): List<String>? {
        var accessibilityEnabled = 0
        val result: MutableList<String> = ArrayList()
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            Log.v(TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(
                TAG, "Error finding setting, default accessibility to not found: "
                        + e.message
            )
        }
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessabilityService = mStringColonSplitter.next()
                    result.add(accessabilityService.split("/".toRegex()).toTypedArray()[0])
                }
            }
        }
        return result
    }

    fun checkAccessbilityByPkg(context: Context, pkg: String): Boolean {
        return getAllAccessbilityApps(context)?.contains(pkg) == true
    }


    fun checkReadSdPermission(context: Context): Boolean {
        if (VersionUtils.isAndroidR() && Environment.isExternalStorageManager())
            return true
        if (!VersionUtils.isAndroidR() &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            return true
        }

        return false
    }




}