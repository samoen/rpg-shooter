import java.awt.Graphics
import java.awt.Image

data class EntCommon(
    var toBeRemoved: Boolean = false,
    var speed: Int = 0,
    var dimensions:EntDimens = EntDimens(0.0,0.0,50.0),
    var isSolid:Boolean = false,
    var spriteu: Image = backgroundImage
)

interface Entity {
    var commonStuff : EntCommon
    fun updateEntity() {}
    fun drawEntity(g: Graphics) {
        drawAsSprite(this,commonStuff.spriteu,g,false)
    }
}
data class HealthStats(
    var didHeal :Boolean = false,
    var currentHp :Double = 10.0,
    var maxHP :Double = 10.0,
    var ouchNoise:soundType = soundType.OUCH,
    var dieNoise:soundType = soundType.DIE,
    var didGetShot:Boolean = false,
    var armorIsBroken:Boolean = false,
    var armorBrokenFrames :Int= 0,
    var gotShotFrames :Int= DAMAGED_ANIMATION_FRAMES,
    var stopped :Boolean= false,
    var shieldSkill:Int = 3,
    var shootySound:soundType = soundType.DIE,
    var angy :Double = 0.0,
    var wep:Weapon=Weapon(),
    var turnSpeed:Float = 0.05f,
    var teamNumber:Int=0
){
    fun getArmored():Boolean{
        return stopped && !armorIsBroken
    }
}

interface HasHealth:Entity{
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
//    var up:OneShotChannel=OneShotChannel(),
//    var dwm:OneShotChannel=OneShotChannel(),
    var sht:OneShotChannel=OneShotChannel(),
    var Swp:OneShotChannel=OneShotChannel(),
    var selDwn:OneShotChannel=OneShotChannel(),
    var selUp:OneShotChannel=OneShotChannel(),
//    var riri:OneShotChannel=OneShotChannel(),
    var leftStickAngle:Float = 0f,
    var leftStickMag:Float = 0f,
    var rightStickAngle:Float = 0f,
    var rightStickMag:Float = 0f,
//    var leflef:OneShotChannel=OneShotChannel(),
//    var spinri:OneShotChannel=OneShotChannel(),
    var selRight:OneShotChannel=OneShotChannel(),
    var selLeft:OneShotChannel=OneShotChannel()
//    var spenlef:OneShotChannel=OneShotChannel()
)

data class Weapon(
    var mobility:Float = 0.2f,
    var atkSpd:Int = 4,
    var bulLifetime:Int = 14,
    var bulspd:Int = 14,
    var recoil:Double = 3.0,
    var bulSize:Double = 5.0,
    var projectiles:Int = 1,
    var framesSinceShottah:Int = 999
)