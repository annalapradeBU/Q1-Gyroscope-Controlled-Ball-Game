package com.example.q1gyroscope_controlledballgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.q1gyroscope_controlledballgame.ui.theme.Q1GyroscopeControlledBallGameTheme
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


class MainActivity : ComponentActivity(), SensorEventListener {

    // requirement: use gyroscope to detect phone tilt
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null



    // state for ball position, beginning in the middle
    private var ballX by mutableStateOf(100f)
    private var ballY by mutableStateOf(100f)

    // game constraints
    private val ballRadius = 40f

    private val acceleration = 2.5f
    private val friction = 0.92f

    // physics variables
    private var velocityX = 0f
    private var velocityY = 0f

    // define obstacles so sensor and UI "see" the same map and ball cannot pass through rect
    // requirement: Add walls and obstacles for a simple maze game
    private val obstacles = listOf(
        Rect(offset = Offset(0f, 500f), size = Size(700f, 60f)),
        Rect(offset = Offset(300f, 1200f), size = Size(1000f, 60f))
    )



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // initialize sensor manager for the Gyroscope
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent{

            Q1GyroscopeControlledBallGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // requirement: use canvas drawing
                    MazeGame(ballX, ballY, ballRadius, obstacles)
                }
            }
        }
    }

    // register sensor when app is active to save battery
    override fun onResume(){
        super.onResume()
        gyroscope?.let{
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    // unregister sensor when app is paused
    override fun onPause(){
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // requirement: mve a circle (ball) based on tilt direction
    // translates Gyroscope angular velocity into ball acceleration
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // define velocity based on sensor values
            // it.values[0] is X-axis (tilting forward/back) -> moves Ball Y
            // it.values[1] is Y-axis (tilting left/right) -> moves Ball X
            velocityX += it.values[1] * acceleration
            velocityY += it.values[0] * acceleration

            // apply friction for smoother control
            velocityX *= friction
            velocityY *= friction

            // sub step the movement, hopefully it won't move past the barrier this time
            val steps = 5
            val stepX = velocityX / steps
            val stepY = velocityY / steps



            // collision check (in steps!)
            repeat(steps) {
                // Try moving X a tiny bit
                val nextX = ballX + stepX
                if (!checkCollision(nextX, ballY)) {
                    ballX = nextX
                } else {
                    velocityX = 0f // hit something, stop X momentum
                }

                // try moving Y a tiny bit
                val nextY = ballY + stepY
                if (!checkCollision(ballX, nextY)) {
                    ballY = nextY
                } else {
                    velocityY = 0f // hit something, stop Y momentum
                }
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // helper function for collision checking
    private fun checkCollision(x: Float, y: Float): Boolean {
        val ballRect = Rect(x - ballRadius, y - ballRadius, x + ballRadius, y + ballRadius)
        for (rect in obstacles) {
            if (rect.overlaps(ballRect)) return true
        }
        return false
    }

}




@Composable
// requirement: use Canvas drawing for the game UI
fun MazeGame(x: Float, y: Float, radius: Float, obstacles: List<Rect>){
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height


        // draw the maze obstacles
        obstacles.forEach { rect ->
            drawRect(color = Color.DarkGray, topLeft = rect.topLeft, size = rect.size)
        }


        // draw the goal
        drawCircle(
            color = Color.Green,
            radius = 60f,
            center = Offset(canvasWidth - 100f, canvasHeight - 200f)
        )

        // draw the ball
        drawCircle(
            color = Color.Red,
            radius = radius,
            center = Offset(
                x.coerceIn(radius, canvasWidth - radius),
                y.coerceIn(radius, canvasHeight - radius)
            )
        )
    }

}
