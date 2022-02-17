import kotlinx.coroutines.*
import java.awt.Point
import java.awt.Robot
import java.awt.event.InputEvent

fun main111(){
runBlocking {

    VideoClicker(this.coroutineContext, frameSizePixs = 16){p,f,r->
        this.launch (Dispatchers.IO){
            println("Static frame, clickin!")
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            delay(250)
            r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
         }

    }.let { clicker ->
            clicker.run()
        }
    awaitCancellation()

}
}
fun main(){
    runBlocking {
        val robot=Robot()
        println("Starting frame creation at time: ${System.currentTimeMillis()}")
        val startTime=System.currentTimeMillis()
        val frame=Frame.createDownScaledFromPosition(1050,32,pointOf(347 to 347),robot)
        val endTime=System.currentTimeMillis()
        val elapsedTime=(endTime-startTime).toFloat()/10000f
        println("Elapsed time: $elapsedTime")
        println("Result frame is: $frame")







    }


}




 fun pointOf(intPair:Pair<Int,Int>):Point{
    return Point(intPair.first,intPair.second)
}