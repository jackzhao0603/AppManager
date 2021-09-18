package com.jackzhao.appmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.jackzhao.appmanager.callback.IPermissionResult
import com.jackzhao.appmanager.const.PermissionConsts.ACCESSIBILITY
import com.jackzhao.appmanager.const.PermissionConsts.BIND_NOTIFICATION_LISTENER_SERVICE
import com.jackzhao.appmanager.const.PermissionConsts.CHECKPERMISSIONGROUP_LIST
import com.jackzhao.appmanager.const.PermissionConsts.DEVICE_ADMIN
import com.jackzhao.appmanager.const.jackContext
import com.jackzhao.appmanager.data.AppWithPermission
import com.jackzhao.appmanager.utils.VersionUtils
import java.lang.reflect.Field
import java.util.*


object PermissionManager {

    private const val TAG = "PermissionManager"
    private var mPermissionResult: IPermissionResult? = null

    fun isActiveAdmin(packageName: String): Boolean {
        val dpm =
            jackContext!!.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminList = dpm.activeAdmins
        if (adminList != null && adminList.isNotEmpty()) {
            for (componentName in adminList) {
                if (componentName.packageName == packageName) return true
            }
        }
        return false
    }


    fun getAllNotificationAccessApps(): Set<String?>? {
        return NotificationManagerCompat.getEnabledListenerPackages(jackContext!!)
    }

    fun checkNotificationAccessByPkg(pkg: String): Boolean {
        return getAllNotificationAccessApps()?.contains(pkg) == true
    }


    fun getAllAccessbilityApps(): List<String>? {
        var accessibilityEnabled = 0
        val result: MutableList<String> = ArrayList()
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                jackContext!!.applicationContext.contentResolver,
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
        if (accessibilityEnabled == 1 || VersionUtils.isAndroidR()) {
            val settingValue = Settings.Secure.getString(
                jackContext!!.applicationContext.contentResolver,
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

    fun checkAccessbilityByPkg(pkg: String): Boolean {
        return getAllAccessbilityApps()?.contains(pkg) == true
    }


    fun checkReadSdPermission(): Boolean {
        if (VersionUtils.isAndroidR() && Environment.isExternalStorageManager())
            return true
        if (!VersionUtils.isAndroidR() &&
            ActivityCompat.checkSelfPermission(
                jackContext!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            return true
        }

        return false
    }

    fun hasPermission(pkg: String, permission: String): Boolean {
        val pm: PackageManager = jackContext!!.packageManager
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission(permission, pkg)
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun checkSelfUsageAccess(): Boolean {
        return try {
            val packageManager = jackContext!!.packageManager
            var applicationInfo: ApplicationInfo? = null
            applicationInfo = packageManager.getApplicationInfo(jackContext!!.packageName, 0)
            @SuppressLint("WrongConstant") val appOpsManager = jackContext!!
                .getSystemService("appops") as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid, applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun checkSelfAccessbility(): Boolean {
        return checkAccessbilityByPkg(jackContext!!.packageName)
    }

    fun checkSelfNotificationAccess(): Boolean {
        return checkNotificationAccessByPkg(jackContext!!.packageName)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkSelfOverlay(): Boolean {
        return Settings.canDrawOverlays(jackContext!!)
    }

    fun getAllPermissionsByPkg(packageName: String): List<String> {
        val set = HashSet<String>()
        val clazz = Class.forName("android.Manifest")
        val classList = clazz.declaredClasses
        for (classObj in classList) {
            if (classObj == null) {
                continue
            }
            if (classObj.simpleName != "permission") {
                continue
            }
            val fields: Array<Field> = classObj.declaredFields
            for (field in fields) {
                val value = field.get(null) as String
                Log.e(ContentValues.TAG, "getAllPermissionsByPkg: $value")
                if (hasPermission(packageName, value)) {
                    CHECKPERMISSIONGROUP_LIST.forEach {
                        if (value.contains(it)) {
                            set.add(it)
                        }
                    }
                }
            }
        }
        return set.toList()
    }


    fun getAppPermissionsByPkg(pkg: String): AppWithPermission {
        var result = AppWithPermission(pkg)
        var normalPermissions = getAllPermissionsByPkg(pkg)
        result.normalPermissions = normalPermissions
        var specialPermissions = ArrayList<String>()

        if (isActiveAdmin(pkg)) {
            specialPermissions.add(DEVICE_ADMIN)
        }
        if (checkAccessbilityByPkg(pkg)) {
            specialPermissions.add(ACCESSIBILITY)
        }
        if (checkNotificationAccessByPkg(pkg)) {
            specialPermissions.add(BIND_NOTIFICATION_LISTENER_SERVICE)
        }

        result.specialPermissions = specialPermissions
        return result
    }


    fun gotoUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "go to setting exception: " + Settings.ACTION_USAGE_ACCESS_SETTINGS,
                e
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    fun gotoStorageSetting() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + jackContext!!.packageName)
            )
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun gotoAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "go to setting exception: " + Settings.ACTION_ACCESSIBILITY_SETTINGS,
                e
            )
        }
    }

    fun gotoOverlaySetting() {
        try {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse("package:" + jackContext!!.packageName)
            (jackContext!! as Activity).startActivityForResult(intent, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun gotoNotificationListenSettings() {
        try {
            val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            if (jackContext!! !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun gotoAppSettingsConfigActivity() {
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:" + jackContext!!.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        jackContext!!.startActivity(i)
    }

    fun gotoBatteryOptimization(activity: Activity): Boolean {
        val powerUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.packageManager.resolveActivity(powerUsageIntent, 0)?.let {
            activity.startActivity(powerUsageIntent)
            return true
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestPermissionResult(
        activity: Activity,
        requestCode: Int,
        permissionList: Array<String>,
        permissionResult: IPermissionResult,
    ) {
        if (!VersionUtils.isAndroidM()) {
            return
        }
        mPermissionResult = permissionResult
        val applyPermissions = ArrayList<String>()
        permissionList.forEach {
            if (ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            ) {
                applyPermissions.add(it)
            }
        }
        val permissions = applyPermissions.toTypedArray()
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        } else {
            permissionResult.onPermissionSuccess(activity, requestCode)
        }
    }

    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        requestResult(activity, requestCode, permissions, grantResults)
    }

    private fun requestResult(
        activity: Activity,
        code: Int,
        permissions: Array<String>,
        results: IntArray,
    ) {
        val deniedPermissions: MutableList<String> = ArrayList()
        for (i in results.indices) {
            if (results[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
            }
        }
        if (deniedPermissions.size > 0) {
            mPermissionResult?.onPermissionFailed(activity, code, deniedPermissions.toTypedArray())
        } else {
            mPermissionResult?.onPermissionSuccess(activity, code)
        }
    }

}