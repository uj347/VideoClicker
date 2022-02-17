import kotlinx.coroutines.*
import java.awt.Point
import java.awt.Robot
import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.math.round

data class Frame private constructor (val frameData:Array<Array<Int>>) {

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
                val hexCode=Integer.toHexString(frameData[xInd][yInd])
                append(
                    when(hexCode.lowercase()){
                        "ffffffff"->"."
                        else->"#"
                    }


                    )
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
            val newFrameData:Array<Array<Int>> = Array(frameSize,{_-> arrayOf() })
            CoroutineScope(coroutineContext).launch {
                for(xOffset:Int in 0 until frameSize){
                   this.launch(Dispatchers.IO){
                        for (yOffset: Int in 0 until frameSize) {
                            ensureActive()
                            newFrameData[xOffset][yOffset] =
                                robot
                                    .getPixelColor(vStartX + xOffset, vStartY + yOffset)
                                    .rgb
                        }
                    }
                }

            }.join()
                return Frame(newFrameData)
            }



        suspend fun createDownScaledFromPosition(initialFrameSize: Int,scaleToSize:Int,realPoint: Point,robot:Robot):Frame{
            val scope= CoroutineScope(coroutineContext)

            val halfFrame=initialFrameSize/2
            val virtualStartPoint= produceVirtualPoint(realPoint,halfFrame)
            val vStartX=virtualStartPoint.x
            val vStartY=virtualStartPoint.y
            val virtualFullFrame= Array<Array<Point>>(initialFrameSize){xInd->
                Array<Point>(initialFrameSize){yInd->
                    Point(vStartX+xInd,vStartY+yInd)
                }
            }

            //TODO
           val deferrs = scope.performDownScaledArrayMapping<Array<Point>,Array<Int>>(
                virtualFullFrame,scaleToSize,Dispatchers.Default, Array<Int>(scaleToSize){0}){pointAr->
                    scope.performDownScaledArrayMapping<Point,Int>(pointAr,scaleToSize,Dispatchers.Default,0){
                        robot.getPixelColor(it.x,it.y).rgb
                    }.await()
               }
                deferrs.join()
          return Frame(deferrs.getCompleted())
        }
        
        
        inline fun <reified T,reified R> CoroutineScope.performDownScaledArrayMapping(input:Array<T>,
                                                                                        scaleToSize:Int,
                                                                                      dispatcher: CoroutineDispatcher,
                                                                                      defValue:R,

                                                                     crossinline operationForEach:suspend (T)->R):Deferred<Array<R>>{
            val initialSize=input.size
            val positionsToSkip:Int=when{
                (scaleToSize>initialSize) ->throw IllegalArgumentException("scaleToSize must be less or equal then initial size")
                (scaleToSize==initialSize||initialSize/scaleToSize==1)->0
                (scaleToSize==0||initialSize==0)->throw IllegalArgumentException("sizes most be greater then 0")
                else->initialSize/scaleToSize
            }.also { println("Points to skip: $it") }

           return async(dispatcher) {
               val newData:Array<R> = Array(scaleToSize){_->defValue }
                val resultXIndex=AtomicInteger(0)
               println("Scale to size is : $scaleToSize")
                for(offset:Int in 0 until initialSize){
                    //todo problem is here
                   if(positionsToSkip!=0) {
                        if (offset % positionsToSkip == 0 && offset != 0) {
                            println("Current offsset is $offset")
                            val newDataIndex = (initialSize / offset) - 1
                            println("Asigning terget frame#$newDataIndex")
                            newData[newDataIndex] = operationForEach(input[offset])

                        }
                        else if (resultXIndex.get() == scaleToSize - 1) {
                            println("Side branch")
                            newData[scaleToSize - 1] = operationForEach(input[input.lastIndex])
                        }
                    }else{
                        if (offset==scaleToSize)break
                        newData[offset]=operationForEach(input[offset])
                    }

                }
               newData
            }

        }


        private  fun produceVirtualPoint(realPoint: Point,halfFrame:Int):Point{
            val vX:Int= maxOf(0,realPoint.x-halfFrame)
            val vY:Int= maxOf(0,realPoint.y-halfFrame)
            val result=Point(vX,vY)
            return result
        }


    }
}