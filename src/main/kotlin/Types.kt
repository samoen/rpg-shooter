import java.awt.Color
import java.awt.Graphics

data class EntCommon(
    var toBeRemoved: Boolean = false,
    var speed: Int = 0,
    var dimensions:EntDimens = EntDimens(0.0,0.0,50.0),
    var isSolid:Boolean = false
)

interface Entity {
    var commonStuff : EntCommon
//    var toBeRemoved: Boolean
//    var speed: Int
//    var dimensions:EntDimens
//    var isSolid:Boolean
//    var spriteu: Image
    fun updateEntity() {}

    fun drawEntity(g: Graphics) {
        drawAsSprite(this,gateClosedImage,g)
    }
}
class HealthStats{
    var didHeal :Boolean = false
    var currentHp :Double = 10.0
    var maxHP :Double = 10.0
    var ouchNoise = "ouch"
    var dieNoise = "die"
    val DAMAGED_ANIMATION_FRAMES = 3
    var didGetShot:Boolean = false
    var armorIsBroken:Boolean = false
    var armorBrokenFrames = 0
    var gotShotFrames = DAMAGED_ANIMATION_FRAMES
    var stopped = false
    var shieldSkill:Int = 1
    fun getArmored():Boolean{
        return stopped && !armorIsBroken
    }
    var shootySound:String = "die"
    var angy :Double = 0.0
    var wep:Weapon=Weapon()
    var turnSpeed:Float = 0.05f
    var bulColor:Color=Color.RED
    var teamNumber:Int=0
}
interface HasHealth{
    var healthStats:HealthStats
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

data class EntDimens(var xpos:Double,var ypos:Double,var drawSize:Double){
    fun getMidY():Double{
        return ypos+(drawSize/2)
    }
    fun getMidX():Double{
        return xpos+(drawSize/2)
    }
    fun overlapsOther(other: EntDimens):Boolean{
        return ypos+drawSize > other.ypos &&
                ypos<other.ypos+other.drawSize &&
                xpos+drawSize > other.xpos &&
                xpos<other.xpos+other.drawSize
    }
}

class playControls(
    var up:OneShotChannel=OneShotChannel(),
    var dwm:OneShotChannel=OneShotChannel(),
    var sht:OneShotChannel=OneShotChannel(),
    var Swp:OneShotChannel=OneShotChannel(),
    var riri:OneShotChannel=OneShotChannel(),
    var leflef:OneShotChannel=OneShotChannel(),
    var spinri:OneShotChannel=OneShotChannel(),
    var spenlef:OneShotChannel=OneShotChannel()
)

data class Weapon(
    var mobility:Float = 0.3f,
    var atkSpd:Int = 4,
    var bulLifetime:Int = 15,
    var bulspd:Int = 2,
    var recoil:Double = 5.0,
    var bulSize:Double = 9.0,
    var buldmg:Int = 3,
    var projectiles:Int = 1,
    var framesSinceShottah:Int = 999
)