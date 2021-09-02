package com.jackzhao.specialpermission_app

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.jackzhao.appmanager.PermissionManager
import com.jackzhao.appmanager.callback.IPermissionResult
import com.jackzhao.appmanager.const.PermissionConsts
import com.jackzhao.appmanager.utils.ProcessUtils
import com.jackzhao.specialpermission_app.ui.theme.MyApplicationTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Greeting()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionManager.requestPermissionResult(this,
                100,
                PermissionConsts.CAMERA,
                object : IPermissionResult {
                    override fun onPermissionSuccess(activity: Activity, requestCode: Int) {
                        Log.e(TAG, "onPermissionSuccess: $requestCode")
                    }

                    override fun onPermissionFailed(
                        activity: Activity,
                        requestCode: Int,
                        deinedPermissions: Array<String>
                    ) {
                        Log.e(TAG, "onPermissionFailed: $requestCode --> $deinedPermissions")
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                deinedPermissions[0]
                            )
                        ) {
                            PermissionManager.gotoAppSettingsConfigActivity(activity)
                        }
                    }
                })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        PermissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
}

@Composable
fun Greeting() {
    Text(text = "Hello!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Greeting()
    }
}