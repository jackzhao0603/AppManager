package com.jackzhao.appmanager

import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.jackzhao.appmanager.const.jackContext
import com.jackzhao.appmanager.utils.VersionUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.experimental.or

object AppManager {
    private const val TAG = "AppManager"
    const val SHA1 = "SHA1"
    const val EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text"
    const val EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar"
    const val EXTRA_SHOW_FRAGMENT_AS_SUBSETTING = ":settings:show_fragment_as_subsetting"

    @Synchronized
    fun init(context: Context) {
        if (jackContext != null) {
            return
        }
        jackContext = context
    }

    fun isAppHideIcon(pkg: String): Boolean {
        if (VersionUtils.isAndroidL()) {
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(pkg)
            val resolveinfoList = jackContext!!.packageManager
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

    fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val pm: PackageManager = jackContext!!.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName ?: "com.android.launcher3"
    }

    fun isSystemApp(pkg: String): Boolean {
        val pm = jackContext!!.packageManager
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
        intent: Intent,
        bSystemOnly: Boolean,
    ): List<String>? {
        val list: MutableList<String> = ArrayList()
        val pm: PackageManager = jackContext!!.packageManager
        val infoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val r0 = infoList[0]
        for (info in infoList) {
            if (r0.priority == info.priority
                && r0.isDefault == info.isDefault
            ) {
                if (bSystemOnly && !isSystemApp(info.activityInfo.packageName)) {
                    continue
                }
                list.add(info.activityInfo.packageName)
            }
        }
        return list
    }

    fun getBrowserList(bSystemOnly: Boolean): List<String?>? {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.data = Uri.parse("http://www.trendmicro.com")
        return getIntentPkgList(intent, bSystemOnly)
    }

    fun getInstallerPkg(): String {
        val packageManager: PackageManager = jackContext!!.packageManager
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


    fun getSettingPkgname(): String {
        val packageManager: PackageManager = jackContext!!.packageManager
        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        val defaultResolveInfo = packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return defaultResolveInfo?.activityInfo?.packageName ?: "com.android.settings"
    }

    fun getAllAppsInOS(): List<String> {
        try {
            val packageManager = jackContext!!.packageManager
            val packageInfos = packageManager.getInstalledPackages(0)
            val packageNames: MutableList<String> = ArrayList()
            if (packageInfos != null) {
                for (i in packageInfos.indices) {
                    val packName = packageInfos[i].packageName
                    if (TextUtils.equals(jackContext!!.packageName, packName)) continue
                    packageNames.add(packName)
                }
            }
            return packageNames
        } catch (e: Exception) {
            Log.w(TAG, "getAllAppsInOS: ", e)
            return ArrayList()
        }

    }


    fun isInputMethodApp(packageName: String): Boolean {
        val pm = jackContext!!.packageManager
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

    fun gotoSystemSetting(settingAction: String?): Boolean {
        return try {
            val intent = Intent(settingAction)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            jackContext!!.startActivity(intent)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }


    fun getSingInfo(packageName: String, type: String): List<String>? {
        val list: MutableList<String> = java.util.ArrayList()
        val signs: Array<Signature>? = getSignatures(packageName)
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
    fun getSignatures(packageName: String): Array<Signature>? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = jackContext!!.packageManager.getPackageInfo(
                    packageName!!,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo.signingCertificateHistory
            } else {
                val packageInfo = jackContext!!.packageManager.getPackageInfo(
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


    fun getVersionCode(pkg: String): String {
        val pm = jackContext!!.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return ""
    }


    fun getApkPathByPkg(pkg: String): String? {
        val pm = jackContext!!.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.applicationInfo.sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun getApplicationInfo(pkg: String): ApplicationInfo? {
        val pm = jackContext!!.packageManager
        try {
            val packageInfo = pm.getPackageInfo(pkg, 0)
            return packageInfo.applicationInfo
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }


    fun findActivitiesForPackage(packageName: String): List<ResolveInfo>? {
        val packageManager = jackContext!!.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.setPackage(packageName)
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        return apps ?: java.util.ArrayList()
    }

    fun getMarketApps(): ArrayList<String> {
        val pkgs: ArrayList<String> = ArrayList()
        if (jackContext == null) return pkgs
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("market://details?id=")
        val pm = jackContext!!.packageManager
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


    fun showAppInfo(packageName: String) {
        val packageURI = Uri.parse("package:$packageName")
        val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS", packageURI)
        intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true)
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true)
        intent.putExtra(EXTRA_PREFS_SET_BACK_TEXT, "BACK")
        try {
            jackContext!!.startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

