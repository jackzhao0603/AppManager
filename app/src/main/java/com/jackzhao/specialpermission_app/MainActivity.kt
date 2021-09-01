package com.jackzhao.specialpermission_app

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jackzhao.appmanager.AppManager
import com.jackzhao.appmanager.PermissionManager
import com.jackzhao.appmanager.utils.ProcessUtils
import com.jackzhao.specialpermission_app.ui.theme.MyApplicationTheme

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
        AppManager.gotoSystemSetting(this,Settings.ACTION_HOME_SETTINGS)
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