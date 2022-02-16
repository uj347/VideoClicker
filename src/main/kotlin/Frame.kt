import kotlinx.coroutines.*
import java.awt.GraphicsDevice
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.lang.StringBuilder
import kotlin.coroutines.coroutineContext

data class Frame private constructor (val frameData:Array<IntArray>) {

    val size=frameData.size


    fun mostlyEquals(other: Frame,threshHold:Float):Boolean{
        if(size!=other.size) return false
        val totalElements=size*size
        var matchCount:Int=0
        val thisData=frameData
        val otherData=other.frameData
        thisData.forEachIndexed { xIndex,yValues->
            yValues.forEachIndexed { yIndex,rgb->
                if (rgb==otherData[xIndex][yIndex]) {
                    matchCount++
                }
            }
        }
        return matchCount.toFloat()/totalElements.toFloat()>=threshHold
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Frame

        if (!frameData.contentDeepEquals(other.frameData)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameData.contentDeepHashCode()
        result = 31 * result + size
        return result
    }

    override fun toString(): String {
       return StringBuilder().apply{

        for(yInd in 0 until size) {
            append("[ ")
            for (xInd in 0 until size) {
                append(frameData[xInd][yInd])
                append(" ")
            }
            append("]\n")
        }
        }.toString()
    }
    companion object {
       suspend fun createFromPosition(frameSize: Int,realPoint: Point,robot:Robot):Frame{
            val halfFrame=frameSize/2
            val virtualStartPoint= produceVirtualPoint(realPoint,halfFrame)
            val vStartX=virtualStartPoint.x
            val vStartY=virtualStartPoint.y
            val newFrameData:Array<IntArray> = Array(frameSize,{_->IntArray(frameSize)})
            CoroutineScope(coroutineContext).launch {
                for(xOffset:Int in 0 until frameSize){
                   this.launch(Dispatchers.IO){
                        for (yOffset: Int in 0 until frameSize) {
                            ensureActive()
                            newFrameData[xOffset][yOffset] = robot.getPixelColor(vStartX + xOffset, vStartY + yOffset).rgb
                        }
                    }
                }

            }.join()
                return Frame(newFrameData)
            }


        private  fun produceVirtualPoint(realPoint: Point,halfFrame:Int):Point{
            val vX:Int= maxOf(0,realPoint.x-halfFrame)
            val vY:Int= maxOf(0,realPoint.y-halfFrame)
            val result=Point(vX,vY)
            return result
        }


    }
}