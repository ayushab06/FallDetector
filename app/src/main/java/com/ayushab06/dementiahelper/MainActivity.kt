package com.ayushab06.dementiahelper

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URLEncoder
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var high = 0.0
    private val MY_PERMISSIONS_REQUEST_SEND_SMS=1
    private var low = Double.MAX_VALUE
    private var i=0
    private val acc= mutableListOf<Double>()
    private var chk=false
    private val lowThreshold=6.0
    private val CHANNEL_ID="channel id"
    private val highThreshold=13.0
    private val LOCATION_PERMISSION_REQ_CODE = 1000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    lateinit var context:Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        context=this@MainActivity
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager.registerListener(this@MainActivity, sensor, 50000)
    }

    override fun onSensorChanged(event: SensorEvent?) {


        if (event != null) {

            val x = event.values[0]
            val y = event.values[1]
            var z = event.values[2]
            tvX.text = "xValue is : $x"
            tvY.text = "yValue is : $y"
            tvZ.text = "zValue is : $z"
            z-=9.8f
            val avg = sqrt((x * x + y * y + z * z).toDouble())
            tvAvg.text = "the avg accerlation is $avg"
            if (chk && i<40){
                acc.add(avg)
                i++
                return
            }
            var res= fallDetection()
            acc.removeAll(acc)
            i=0
            chk= avg<lowThreshold
            low=if(chk) avg else low
            if (res) {
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                tvLow.text = "the low value is $low"
                //Toast.makeText(this@MainActivity, "fall can be detected ", Toast.LENGTH_LONG).show()
                showNotification()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.v("TAG", "the changed values are ${sensor} and ${accuracy}")
    }
    fun fallDetection():Boolean {
        var n = acc.size
        for(a in 0 until n){
            if(acc[a]>highThreshold) {
                tvHigh.text="the high is ${acc[a]}"
                return true
            }
        }
        return false
    }
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE
            );
            return
        }
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    latitude = location.latitude
                    longitude = location.longitude
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this, "Failed on getting current location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQ_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                } else {
                    Toast.makeText(
                        this, "You need to grant permission to access location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val name="App Notification"
            val descriptionText="this is your notification"
            val importance=NotificationManager.IMPORTANCE_HIGH
            val channel=NotificationChannel(CHANNEL_ID, name, importance).apply {
                description=descriptionText
            }
            val notificationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun showNotification(){
        createNotificationChannel()
        val notificationLayout= RemoteViews(packageName, R.layout.lay_notificationbar)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("this is the title")
                .setSmallIcon(R.drawable.ic_baseline_emergency_24)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        getCurrentLocation()
        if (!isAccessibilityOn(context, WhatAppAccessibilityService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }else{
            val nos=  ArrayList<String>()
            nos.add("+919149367824")
            handleActionWhatsApp("i've fallen in a pit", nos)
        }
        with(NotificationManagerCompat.from(this)){
            notify(0, builder.build())
        }
    }
    fun handleActionWhatsApp(message: String, mobile_number: List<String>) {
        try {
            val packageManager = applicationContext.packageManager
            if (mobile_number.isNotEmpty()) {
                for (i in mobile_number.indices) {
                    val url =
                        "https://api.whatsapp.com/send?phone=" + mobile_number.get(i) + "&text=" + URLEncoder.encode(
                            message,
                            "UTF-8"
                        )
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setPackage("com.whatsapp")
                    intent.setData(Uri.parse(url))
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (intent.resolveActivity(packageManager) != null) {
                        applicationContext.startActivity(intent)
                        Thread.sleep(10000)
                    } else
                        Toast.makeText(
                            applicationContext,
                            "Whatsapp is not installed",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(
                applicationContext,
                "Something is not right +${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun isAccessibilityOn(
        context: Context,
        clazz: Class<out AccessibilityService?>
    ): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + clazz.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (ignored: Settings.SettingNotFoundException) {
        }
        val colonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue: String = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val accessibilityService = colonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }


}