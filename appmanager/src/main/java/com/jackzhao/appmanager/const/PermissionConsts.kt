package com.jackzhao.appmanager.const

import android.Manifest
import android.content.Context

var jackContext: Context? = null

object PermissionConsts {
    val CONTACTS = arrayOf<String>(
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.READ_CONTACTS
    )

    val PHONE = arrayOf<String>(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.USE_SIP,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.ADD_VOICEMAIL
    )

    val CALENDAR = arrayOf<String>(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    val CAMERA = arrayOf<String>(
        Manifest.permission.CAMERA
    )

    val SENSORS = arrayOf<String>(
        Manifest.permission.BODY_SENSORS
    )

    val LOCATION = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    val STORAGE = arrayOf<String>(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val MICROPHONE = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO
    )

    val SMS = arrayOf<String>(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS
    )


    val CHECKPERMISSIONGROUP_LIST: Array<String> = arrayOf(
        "ACTIVITY_RECOGNITION",
        "CALENDAR",
        "CALL_LOG",
        "CAMERA",
        "CONTACTS",
        "LOCATION",
        "RECORD",
        "PHONE",
        "SENSORS",
        "SMS",
        "STORAGE"
    )


    val DEVICE_ADMIN = "device_admin"
    val ACCESSIBILITY = "accessibility"
    val BIND_NOTIFICATION_LISTENER_SERVICE = "bind_notification_listener_service"

}