package com.jackzhao.appmanager

import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.jackzhao.appmanager.utils.VersionUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.indices
import kotlin.experimental.and
import kotlin.experimental.or

object AppManager {
    private const val TAG = "AppManager"
    const val SHA1 = "SHA1"

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

    fun gotoSystemSetting(context: Context, settingAction: String?): Boolean {
        return try {
            val intent = Intent(settingAction)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }


    fun getSingInfo(context: Context, packageName: String, type: String): List<String>? {
        val list: MutableList<String> = java.util.ArrayList()
        val signs: Array<Signature>? = getSignatures(context, packageName)
        signs?.let {
            for (sig in signs) {
                if (SHA1 == type) {
                    getSignatureString(sig, SHA1)?.let {
                        list.add(it)
                    }
                }
            }
        }
        return list
    }

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成16进制）
     *
     * @param sig
     * @param type
     * @return
     */
    fun getSignatureString(sig: Signature, type: String): String? {
        val hexBytes = sig.toCharsString().toByteArray()
        var fingerprint = "error!"
        try {
            val digest = MessageDigest.getInstance(type)
            if (digest != null) {
                val digestBytes = digest.digest(hexBytes)
                val sb = StringBuilder()
                for (digestByte in digestBytes) {
                    sb.append(
                        Integer.toHexString((digestByte and 0xFF.toByte() or 0x100.toByte()).toInt())
                            .substring(1, 3)
                    )
                }
                fingerprint = sb.toString().toUpperCase()
            }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return fingerprint
    }


    /**
     * 返回对应包的签名信息
     *
     * @param context
     * @param packageName
     * @return
     */
    fun getSignatures(context: Context, packageName: String): Array<Signature>? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName!!,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo.signingCertificateHistory
            } else {
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName!!,
                    PackageManager.GET_SIGNATURES
                )
                packageInfo.signatures
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }


    fun getVersionCode(context: Context, pkg: String): String {
        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return ""
    }


    fun getApkPathByPkg(context: Context, pkg: String): String? {
        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.applicationInfo.sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun getApplicationInfo(context: Context, pkg: String): ApplicationInfo? {
        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.applicationInfo
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }


    fun findActivitiesForPackage(context: Context, packageName: String): List<ResolveInfo>? {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.setPackage(packageName)
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        return apps ?: java.util.ArrayList()
    }

    fun getMarketApps(context: Context?): ArrayList<String> {
        val pkgs: ArrayList<String> = ArrayList()
        if (context == null) return pkgs
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("market://details?id=")
        val pm = context.packageManager
        val infos = pm.queryIntentActivities(
            intent,
            0
        )
        if (infos == null || infos.size == 0) return pkgs
        val size = infos.size
        for (i in 0 until size) {
            var pkgName = ""
            try {
                val activityInfo = infos[i].activityInfo
                pkgName = activityInfo.packageName
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            if (!TextUtils.isEmpty(pkgName)) pkgs.add(pkgName)
        }
        return pkgs
    }
}

