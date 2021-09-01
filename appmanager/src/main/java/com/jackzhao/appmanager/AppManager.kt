package com.jackzhao.appmanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.jackzhao.appmanager.utils.VersionUtils
import java.util.NoSuchElementException

object AppManager {
    private const val TAG = "AppManager"

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

    fun getLauncherPackageName(context: Context): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val pm: PackageManager = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName ?: "com.android.launcher3"
    }

    fun isSystemApp(context: Context, pkg: String): Boolean {
        val pm = context.packageManager
        return if (pkg != null) {
            try {
                val info = pm.getPackageInfo(pkg, 0)
                info?.applicationInfo != null &&
                        info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        } else {
            false
        }
    }

    private fun getIntentPkgList(
        context: Context,
        intent: Intent,
        bSystemOnly: Boolean
    ): List<String>? {
        val list: MutableList<String> = ArrayList()
        val pm: PackageManager = context.packageManager
        val infoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val r0 = infoList[0]
        for (info in infoList) {
            if (r0.priority == info.priority
                && r0.isDefault == info.isDefault
            ) {
                if (bSystemOnly && !isSystemApp(context, info.activityInfo.packageName)) {
                    continue
                }
                list.add(info.activityInfo.packageName)
            }
        }
        return list
    }

    fun getBrowserList(context: Context, bSystemOnly: Boolean): List<String?>? {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.data = Uri.parse("http://www.trendmicro.com")
        return getIntentPkgList(context, intent, bSystemOnly)
    }

    fun getInstallerPkg(context: Context): String {
        val packageManager: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            Uri.parse("file://tmp.apk"),
            "application/vnd.android.package-archive"
        )
        val defaultResolveInfo = packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return defaultResolveInfo?.activityInfo?.packageName
            ?: "com.google.android.packageinstaller"
    }


    fun getSettingPkgname(context: Context): String {
        val packageManager: PackageManager = context.packageManager
        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        val defaultResolveInfo = packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return defaultResolveInfo?.activityInfo?.packageName ?: "com.android.settings"
    }

    fun getAllAppsInOS(context: Context): List<String> {
        try {
            val packageManager = context.packageManager
            val packageInfos = packageManager.getInstalledPackages(0)
            val packageNames: MutableList<String> = ArrayList()
            if (packageInfos != null) {
                for (i in packageInfos.indices) {
                    val packName = packageInfos[i].packageName
                    if (TextUtils.equals(context.packageName, packName)) continue
                    packageNames.add(packName)
                }
            }
            return packageNames
        } catch (e: Exception) {
            Log.w(TAG, "getAllAppsInOS: ", e)
            return ArrayList()
        }

    }


    fun isInputMethodApp(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        var isInputMethodApp = false
        try {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
            val sInfo = pkgInfo.services
            if (sInfo != null) {
                for (i in sInfo.indices) {
                    val serviceInfo = sInfo[i]
                    if (serviceInfo.permission != null && serviceInfo.permission == "android.permission.BIND_INPUT_METHOD") {
                        isInputMethodApp = true
                        break
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isInputMethodApp
    }
}