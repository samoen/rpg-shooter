import java.awt.Color
import java.awt.Graphics
import javax.swing.ImageIcon

interface Entity {
    var xpos: Double
    var ypos: Double
    var isDead: Boolean
    var entityTag: String
    var speed: Int
    var drawSize: Double
    var color: Color

    fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){

    }
    fun updateEntity() {}
    fun drawComponents(g: Graphics) {}
    fun overlapsOther(other: Entity):Boolean{
        return this.ypos+this.drawSize > other.ypos &&
                this.ypos<other.ypos+other.drawSize &&
                this.xpos+this.drawSize > other.xpos &&
                this.xpos<other.xpos+other.drawSize
    }
    fun getMidY():Double{
        return ypos+(drawSize/2)
    }
    fun getMidX():Double{
        return xpos+(drawSize/2)
    }
    fun drawEntity(g: Graphics) {
        g.color = color
        g.fillRect(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), getWindowAdjustedPos(drawSize).toInt(), getWindowAdjustedPos(drawSize).toInt())
    }
}
fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}


class ButtonSet(val up:Int,val down:Int,val left:Int,val right:Int,val swapgun:Int,val shoot:Int,val spinleft:Int,val spinright:Int)

class OneShotChannel(var locked:Boolean=false, var booly:Boolean=false){
    fun tryConsume():Boolean{
        if(booly){
            booly = false
            locked = true
            return true
        }else return false
    }

    fun tryProduce(){
        if(!locked){
            booly=true
        }
    }
    fun release(){
        locked = false
        booly = false
    }
}

class EntDimens(val xpos:Double,val ypos:Double,val drawSize:Double){
    fun getMidpoint():Pair<Double,Double>{
        return Pair((xpos+(drawSize/2)),ypos+(drawSize/2))
    }
    fun getMidY():Double{
        return ypos+(drawSize/2)
    }
    fun getMidX():Double{
        return xpos+(drawSize/2)
    }
}

class playControls(var up:OneShotChannel=OneShotChannel(), var dwm:OneShotChannel=OneShotChannel(), var sht:OneShotChannel=OneShotChannel(), var Swp:OneShotChannel=OneShotChannel(), var riri:OneShotChannel=OneShotChannel(), var leflef:OneShotChannel=OneShotChannel(), var spinri:OneShotChannel=OneShotChannel(), var spenlef:OneShotChannel=OneShotChannel())

class Weapon(
    var atkSpd:Int = 4,
    var bulspd:Int = 2,
    var recoil:Double = 5.0,
    var bulSize:Double = 9.0,
    var buldmg:Int = 3,
    var framesSinceShottah:Int = 999
)