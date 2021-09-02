package com.jackzhao.appmanager.callback

import android.app.Activity

interface IPermissionResult {
    fun onPermissionSuccess(activity: Activity, requestCode: Int)

    fun onPermissionFailed(activity: Activity, requestCode: Int, deinedPermissions: Array<String>)
}