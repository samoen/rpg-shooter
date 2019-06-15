import java.awt.Graphics
import java.awt.Image
import java.io.File
import javax.swing.ImageIcon
import javax.swing.JFrame


val allEntities = mutableListOf<Entity>()
val entsToAdd = mutableListOf<Entity>()
val entsToDraw = mutableListOf<Entity>()
val players:MutableList<Player> = mutableListOf()
var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()
var frameNotClosing = true
var gamePaused = false
var myrepaint = false
var painting = false
val statsYSpace = 20.0
val statsXSpace = 40.0
val selectorXSpace = 45.0
const val INTENDED_FRAME_SIZE = 900
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
val YFRAMEMAGIC = 40
const val TICK_INTERVAL = 40
const val MIN_ENT_SIZE = 9.0
val DAMAGED_ANIMATION_FRAMES:Int = 3
val ENEMY_DRIFT_FRAMES = 30

val soundFiles:MutableMap<soundType, File> = mutableMapOf()
//val soundBank:MutableMap<String, Clip> = mutableMapOf()
//val enBulFile = File("src/main/resources/pewnew.wav").getAbsoluteFile()
val longpewFil = File("src/main/resources/newlongpew.wav").getAbsoluteFile()
val swapnoiseFile = File("src/main/resources/swapnoise.wav").getAbsoluteFile()
val dienoiseFile = File("src/main/resources/deathclip.wav").getAbsoluteFile()
val ouchnoiseFile = File("src/main/resources/ouch.wav").getAbsoluteFile()
val enemyPewFile = File("src/main/resources/enemypew.wav").getAbsoluteFile()
val stillImage = ImageIcon("src/main/resources/main.png").image
val runImage = ImageIcon("src/main/resources/walk.png").image
val goblinImage = ImageIcon("src/main/resources/main.png").image
val pewImage = ImageIcon("src/main/resources/shoot1.png").image
val backgroundImage = ImageIcon("src/main/resources/tilemap.png").image
val healthShopImage = ImageIcon("src/main/resources/hospital.png").image
val ammoShopImage = ImageIcon("src/main/resources/ammoshop.png").image
val gunShopImage = ImageIcon("src/main/resources/gunshop.png").image
val pstoppedImage = ImageIcon("src/main/resources/shield.png").image
val pouchImage = ImageIcon("src/main/resources/manhit.png").image
val armorBrokenImage = ImageIcon("src/main/resources/halfshield.png").image
val wallImage = ImageIcon("src/main/resources/brick1.png").image
val dieImage = ImageIcon("src/main/resources/deadman.png").image
val impactImage = ImageIcon("src/main/resources/shrapnel.png").image
val pBulImage = ImageIcon("src/main/resources/plasma.png").image
val eBulImage = ImageIcon("src/main/resources/badbullet.png").image
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image

var myFrame = run {
    val jf = JFrame()
    jf.isFocusable = true
    jf.iconImage = ImageIcon("gunman.png").image
    jf
}
const val mapGridColumns = 16

val map1 =  "        w       " +
        "                " +
        "                " +
        "ww          w   " +
        " whw           w" +
        "  hw     m     w" +
        " whwh          h" +
        "            w ww" +
        "  wh  www     ww" +
        "  w   www     ww" +
        "  w    h    w   " +
        "  g    b    wh  " +
        "            2   " +
        "   1            " +
        "            s   "

val map2 =  "s       we      " +
        "   3         e  " +
        "      ww     h  " +
        "    h ww        " +
        "               w" +
        "               w" +
        "    h    1     w" +
        "              ww" +
        "   h          ww" +
        "     w w       w" +
        "       h    w  w" +
        "      www   wh w" +
        "        w   w  w" +
        " e      w e w ew" +
        "        w      w"


val map3 =  "e ee e e eeeee  " +
        "   e e e e      " +
        "       2     h  " +
        "                " +
        "               w" +
        "               w" +
        "    h          w" +
        "          s   ww" +
        "              ww" +
        "               w" +
        "               w" +
        "            wh w" +
        "               w" +
        "                " +
        "                "

enum class soundType{
    SHOOT,
    OUCH,
    DIE,
    LASER,
    SWAP
}


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
    var atkSpd:Int = 60,
    var bulLifetime:Int = 15,
    var bulspd:Int = 30,
    var recoil:Double = 0.0,
    var bulSize:Double = 25.0,
    var projectiles:Int = 1,
    var framesSinceShottah:Int = 999
)