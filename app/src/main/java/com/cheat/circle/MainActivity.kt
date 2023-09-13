package com.cheat.circle

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val circleView = findViewById<com.cheat.lib_ui_circle.CircleView>(R.id.circle)
        circleView.setOnClickListener {
            circleView.setResource(com.cheat.lib_ui_circle.R.drawable.dog)
//            circleView.setBorderLength(10f)
        }

        val pauseBtn = findViewById<Button>(R.id.btn_pause)
        pauseBtn.setOnClickListener {
            circleView.pauseRotationAnimation()
        }

        val goOnBtn = findViewById<Button>(R.id.btn_go_on)
        goOnBtn.setOnClickListener {
            circleView.goOnRotationAnimation()
        }

        val goBtn = findViewById<Button>(R.id.btn_start)
        goBtn.setOnClickListener {
            circleView.startRotationAnimation()
        }
    }

}