import java.awt.Color
import java.awt.Graphics

interface Entity {
    var toBeRemoved: Boolean
    var entityTag: String
    var speed: Int
    var color: Color
    var dimensions:EntDimens

    fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){}
    fun updateEntity() {}
    fun drawComponents(g: Graphics) {}
    fun overlapsOther(other: Entity):Boolean{
        return this.dimensions.ypos+this.dimensions.drawSize > other.dimensions.ypos &&
                this.dimensions.ypos<other.dimensions.ypos+other.dimensions.drawSize &&
                this.dimensions.xpos+this.dimensions.drawSize > other.dimensions.xpos &&
                this.dimensions.xpos<other.dimensions.xpos+other.dimensions.drawSize
    }
    fun getMidY():Double{
        return dimensions.ypos+(dimensions.drawSize/2)
    }
    fun getMidX():Double{
        return dimensions.xpos+(dimensions.drawSize/2)
    }
    fun drawEntity(g: Graphics) {
        drawAsSprite(this,gateClosedImage,g)
    }
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

class EntDimens(var xpos:Double,var ypos:Double,var drawSize:Double){
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