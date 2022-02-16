import kotlinx.coroutines.*
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Robot
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class VideoClicker(
    context: CoroutineContext,
    movementSteelTimeMillis: Long=5000,
    frameSizePixs: Int=50,
    frameStaticTime: Long=4000,
    frameComparisonThreshold:Float=0.6f,
    performActionOnEveryStatic:Boolean=true,
    onStaticFrameDetectionCallback: (Point,Frame,Robot) -> Unit
) {
    private val scope = CoroutineScope(context)
    private val isMovementStill = AtomicBoolean()
    private val isFrameStatic = AtomicBoolean()
    private val robot = Robot()

    private val onMovementDetectionCallBack: (Point) -> Unit = {
        isFrameStatic.set(false)
        isMovementStill.set(false)
//        println("New location: ${it.x}, ${it.y}")
    }
    private val onSteelDetectionCallback: (Point) -> Unit = {
        println("Still mouse detected")
    }



    suspend fun run() {
        movementDetectionJob.apply {
            start()
        }
        movementStillDetectionJob.apply {
            start()
        }

        staticFrameDetectionJob.apply {
            start()
        }

        //tickerJob.start()
    }

    private val movementDetectionJob = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
        while (isActive) {
            val scopeLastPos = MouseInfo.getPointerInfo().location
            delay(500)
            val scopeNewPos = MouseInfo.getPointerInfo().location
            if (scopeLastPos != scopeNewPos) {
                onMovementDetectionCallBack(scopeNewPos)
            }
        }
    }

    private val movementStillDetectionJob = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY,) {
        while (isActive) {
            val scopeLastPos = MouseInfo.getPointerInfo().location
            delay(movementSteelTimeMillis)
            val scopeNewPos = MouseInfo.getPointerInfo().location
            if (scopeLastPos == scopeNewPos) {
                if(isMovementStill.compareAndSet(false,true)) {
                    onSteelDetectionCallback(scopeNewPos)
                }
            }else{
                isMovementStill.set(false)
            }
        }
    }

    private val staticFrameDetectionJob = scope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
        while (isActive) {
            delay(250)
            if(isMovementStill.get()) {
                val point= MouseInfo.getPointerInfo().location
                val scopeLastFrame = Frame.createFromPosition(frameSizePixs, point, robot)
                println("Started still frame asessment for point: ${point}")
                delay(frameStaticTime)
                val scopeNewFrame =  Frame.createFromPosition(frameSizePixs, point, robot)
                if (scopeLastFrame.mostlyEquals(scopeNewFrame, frameComparisonThreshold)
                    &&isMovementStill.get()) {
                    if (performActionOnEveryStatic)onStaticFrameDetectionCallback(point, scopeNewFrame,robot)
                    if (isFrameStatic.compareAndSet(false, true)) {
                        if (!performActionOnEveryStatic)onStaticFrameDetectionCallback(point, scopeNewFrame,robot)
                    }
                } else {
                    println("setting frame nonstatic")
                    isFrameStatic.set(false)
                }
            }
        }
    }


}

