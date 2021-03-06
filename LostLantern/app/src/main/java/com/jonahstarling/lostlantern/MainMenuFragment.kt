package com.jonahstarling.lostlantern

import android.app.Fragment
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.hardware.SensorManager
import kotlinx.android.synthetic.main.fragment_main_menu.view.*
import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Handler


/**
 * Created by Jonah Starling on 11/11/17.
 *
 * Main Menu Fragment:
 *     -
 *
 * Relevant Files:
 *     -
 */

class MainMenuFragment : Fragment(), SensorEventListener {

    private lateinit var rootView: View
    private lateinit var sensorManager: SensorManager
    private var phoneLeveled: Boolean = false
    private var enteredGate: Boolean = false
    private var xSpeed: Float = 0.0f
    private var ySpeed: Float = 0.0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_main_menu, container, false)
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Update player
        val animator = TimeAnimator()
        animator.setTimeListener { _, _, _ ->
            rootView.player.x += xSpeed
            rootView.player.y += ySpeed
            if (xSpeed > 0.0f) {
                rootView.player.angle = Math.atan(ySpeed.toDouble() / xSpeed.toDouble())
            } else if (xSpeed < 0.0f) {
                rootView.player.angle = Math.atan(ySpeed.toDouble() / xSpeed.toDouble()) + Math.PI
            } else if (xSpeed == 0.0f) {
                if (ySpeed >= 0.0f) {
                    rootView.player.angle = Math.PI / 2
                } else {
                    rootView.player.angle = 3 * Math.PI / 2
                }
            }
            rootView.player.invalidate()
        }
        animator.start()

        // Wait to show hint text and then animate into view
        val handler = Handler()
        handler.postDelayed({
            if (!phoneLeveled) {
                val hintTextAnimator = ValueAnimator.ofInt(0, 1)
                hintTextAnimator.duration = 1000
                hintTextAnimator.addUpdateListener { rootView.hintText.alpha = hintTextAnimator.animatedFraction }
                hintTextAnimator.start()
            }
        }, 2000)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        // Register this class as a listener for the orientation and accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (phoneLeveled) {
            if (enteredGate) {
                xSpeed = 0.0f
                ySpeed = 0.0f
                sensorManager.unregisterListener(this)
                val intent = Intent(activity, GameActivity().javaClass)
                startActivity(intent)
                activity.finish()
            } else {
                xSpeed = PlayerMovement.calculateXSpeed(sensorEvent, 0.1f, PlayerMovement.SPEED_MULTIPLIER_NORMAL)
                ySpeed = PlayerMovement.calculateYSpeed(sensorEvent, 0.1f, PlayerMovement.SPEED_MULTIPLIER_NORMAL)
                enteredGate = checkEnter()
            }
        } else {
            phoneLeveled = checkPhoneLeveled(sensorEvent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO: Maybe do something here
    }

    private fun checkPhoneLeveled(sensorEvent: SensorEvent): Boolean {
        var checkX = false
        var checkY = false
        if (sensorEvent.values[0] < 1.0f && sensorEvent.values[0] > -1.0f) {
            checkX = true
        }
        if (sensorEvent.values[1] < 1.0f && sensorEvent.values[1] > -1.0f) {
            checkY = true
        }
        if (checkX && checkY) {
            if (rootView.hintText.alpha != 0f) {
                val hintTextAnimator = ValueAnimator.ofFloat(rootView.hintText.alpha, 0f)
                hintTextAnimator.duration = 1000
                hintTextAnimator.addUpdateListener { rootView.hintText.alpha = hintTextAnimator.animatedValue as Float }
                hintTextAnimator.start()
            }
        }
        return checkX && checkY
    }

    private fun checkEnter(): Boolean {
        // Enter Box
        val enterTop = rootView.enterGate.y
        val enterBottom = enterTop + rootView.enterGate.height
        val enterLeft = rootView.enterGate.x
        val enterRight = enterLeft + rootView.enterGate.width

        // Player Box
        val playerTop = rootView.player.y
        val playerBottom = playerTop + rootView.player.height
        val playerLeft = rootView.player.x
        val playerRight = playerLeft + rootView.player.width

        // Check if Player Box is inside Enter Box
        if (playerTop > enterTop && playerBottom < enterBottom) {
            if (playerLeft > enterLeft && playerRight < enterRight) {
                return true
            }
        }
        return false
    }
}