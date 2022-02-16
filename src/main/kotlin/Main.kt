import kotlinx.coroutines.*
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent

fun main(){
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