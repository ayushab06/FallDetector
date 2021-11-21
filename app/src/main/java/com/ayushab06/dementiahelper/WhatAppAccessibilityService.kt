package com.ayushab06.dementiahelper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.lang.Exception
import java.net.URLEncoder

 public class WhatAppAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNodeInfo: AccessibilityNodeInfoCompat =
            AccessibilityNodeInfoCompat.wrap(rootInActiveWindow)
        val messageNodelist: List<AccessibilityNodeInfoCompat> =
            rootNodeInfo.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        val messageField = messageNodelist[0]
        if (messageField == null || messageField.text.length == 0 || !messageField.text.toString()
                .endsWith("   ")
        )
            return
        val sendMessageNodelist: List<AccessibilityNodeInfoCompat> =
            rootNodeInfo.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        val sendMessageField = sendMessageNodelist[0]
        //if (!sendMessageField.isVisibleToUser)
          //  return
        sendMessageField.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        /*try {
            Thread.sleep(2000)
            performGlobalAction(GLOBAL_ACTION_BACK)
            Thread.sleep(2000)
        } catch (ignore: InterruptedException) {
        }
        performGlobalAction(GLOBAL_ACTION_BACK)*/
        Thread.sleep(50)

    }

    override fun onInterrupt() {
        //
    }


}