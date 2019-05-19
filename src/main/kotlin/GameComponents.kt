import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.*
import java.io.File
import java.util.*
import java.util.logging.Handler
import javax.sound.sampled.*
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

val enBulFile = File("src/main/resources/pewnew.wav").getAbsoluteFile()
//val playerhootNoise: Clip = AudioSystem.getClip()
//val playshoostrm = AudioSystem.getAudioInputStream(File("src/main/resources/longypew.wav").getAbsoluteFile())

fun playSound(clip:Clip){
    if(clip.isRunning){
        clip.stop()
        AudioSystem.getAudioInputStream(enBulFile)
    }
    clip.framePosition = 0
    clip.start()
}
val longpewFil = File("src/main/resources/newlongpew.wav").getAbsoluteFile()
val swapnoiseFile = File("src/main/resources/swapnoise.wav").getAbsoluteFile()
val dienoiseFile = File("src/main/resources/deathclip.wav").getAbsoluteFile()
val ouchnoiseFile = File("src/main/resources/ouch.wav").getAbsoluteFile()
val enemyPewFile = File("src/main/resources/enemypew.wav").getAbsoluteFile()
fun processShooting(me:shoots,sht:Boolean,weap:Weapon){
    if (sht && weap.framesSinceShottah > me.tshd.wep.atkSpd) {
        weap.framesSinceShottah = 0
        if(me is Player)me.didShoot=true
        val b = Bullet(me)
        var canspawn = true
        allEntities.forEach { if(it is Wall && it.overlapsOther(b))canspawn = false }
        if(canspawn)
            entsToAdd.add(Bullet(me))
        else {
            val imp = Impact()
            imp.drawSize = b.drawSize
            imp.xpos = (b).xpos
            imp.ypos = (b).ypos
            entsToAdd.add(imp)
        }

        if(me.tshd.shootNoise.isRunning){
            val newclip = AudioSystem.getClip().also{
                it.open(AudioSystem.getAudioInputStream(longpewFil))
            }
            newclip.start()
//                Thread(Runnable({
//                    shootNoise.stop()
//                                        shootNoise.flush()
//                    shootNoise.framePosition = 0
//                    shootNoise.drain()
//                })).start()
//                shootNoise.stop()
//                (shootNoise.getControl(BooleanControl.Type.MUTE)as BooleanControl).value = true
//                shootNoise.start()
//                shootNoise.framePosition = 0
//                shootNoise=newclip
//                (shootNoise.getControl(FloatControl.Type.MASTER_GAIN)as FloatControl).value -=40
//                shootNoise.start()


        }else{
            me.tshd.shootNoise.framePosition=0
            me.tshd.shootNoise.start()
        }
    }
    weap.framesSinceShottah++

}
fun processTurning(me:shoots,lef:Boolean,righ:Boolean){
    if (lef) {
        val desired = me.tshd.angy+me.tshd.turnSpeed
        if(desired>Math.PI){
            me.tshd.angy = -Math.PI + (desired-Math.PI)
        }else
            me.tshd.angy += me.tshd.turnSpeed
    }
    if (righ){
        val desired = me.tshd.angy-me.tshd.turnSpeed
        if(desired<-Math.PI)me.tshd.angy = Math.PI - (-Math.PI-desired)
        else me.tshd.angy -= me.tshd.turnSpeed
    }
}
fun drawCrosshair(me:shoots,g: Graphics){
    g.color = Color.CYAN
    val strkw = if(me is Player)1.2f
    else 5f
    (g as Graphics2D).stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
    val arcdiameter = (me as Entity).drawSize
    fun doarc(diver:Double,timeser:Double){
        val spread = (7)*(me.tshd.wep.recoil+1)
        val bspd = me.tshd.wep.bulspd*2
        g.drawArc(
            getWindowAdjustedPos(((me as Entity).xpos)+(diver)).toInt()-bspd,
            getWindowAdjustedPos(((me as Entity).ypos)+(diver)).toInt()-bspd,
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            ((me.tshd.angy*180/Math.PI)-spread/2).toInt(),
            spread.toInt()
        )
    }
    if(me is Player){
        doarc(me.drawSize/4,0.5)
        doarc(-me.drawSize/3.5,1.55)
        doarc(0.0,1.0)
        doarc(-me.drawSize/1.7,2.15)
    }else{
        g.drawArc(
            getWindowAdjustedPos(((me as Entity).xpos)).toInt(),
            getWindowAdjustedPos(((me as Entity).ypos)).toInt(),
            (getWindowAdjustedPos((arcdiameter))).toInt(),
            (getWindowAdjustedPos((arcdiameter))).toInt(),
            ((me.tshd.angy*180/Math.PI)-5/2).toInt(),
            5.toInt()
        )
    }
//        doarc(-drawSize,3.0)
//        g.color = Color.ORANGE
    (g as Graphics2D).stroke = BasicStroke(1f)
}
fun drawReload(me:shoots,g: Graphics,weap: Weapon){
    if(weap.framesSinceShottah<me.tshd.wep.atkSpd){
        g.color = Color.CYAN
        (g as Graphics2D).stroke = BasicStroke(2f)

        g.drawLine(
            getWindowAdjustedPos ((me as Entity).xpos).toInt(),
            getWindowAdjustedPos((me as Entity).ypos).toInt()-2,
            getWindowAdjustedPos  ( (me.xpos + (me.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-2
        )
        g.drawLine(
            getWindowAdjustedPos ((me as Entity).xpos).toInt(),
            getWindowAdjustedPos((me as Entity).ypos).toInt()-4,
            getWindowAdjustedPos  ( (me.xpos + (me.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-4
        )
        g.stroke = BasicStroke(1f)
    }
}
class shd{
    var shootNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(enemyPewFile))
    }
    var angy :Double = 0.0
    var wep:Weapon=Weapon()
    var turnSpeed:Float = 0.05f
    var bulColor:Color=Color.RED
}
interface shoots{
    var tshd :shd
}
fun takeDamage(other:Entity,me:Entity):Boolean{
    if(other is Bullet && other.shotBy::class!=me::class) {
        (me as hasHealth).hasHealth.currentHp -= (other.shotBy as shoots).tshd.wep.buldmg
        if((me as hasHealth).hasHealth.currentHp<1){
            playSound((me as demByBuls).damagedByBul.deathNoise)
            me.isDead = true
            return true
        }else{
            playSound((me as demByBuls).damagedByBul.ouchNoise)
            me.damagedByBul.didGetShot = true
            me.damagedByBul.gotShotFrames = me.damagedByBul.DAMAGED_ANIMATION_FRAMES
        }
    }else if (other is MedPack && (me as hasHealth).hasHealth.currentHp<me.hasHealth.maxHP){
        me.hasHealth.didHeal = true
        val desiredhp = (me as hasHealth).hasHealth.currentHp+20
        if (desiredhp>me.hasHealth.maxHP){
            me.hasHealth.currentHp = me.hasHealth.maxHP
        }else{
            me.hasHealth.currentHp = desiredhp
        }
    }
    return false
}
interface demByBuls{
    val damagedByBul:damagedByBullets
}
class damagedByBullets{
    val ouchNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(ouchnoiseFile))
    }
    val deathNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(dienoiseFile))
    }
    val DAMAGED_ANIMATION_FRAMES = 3
    var didGetShot:Boolean = false
    var gotShotFrames = DAMAGED_ANIMATION_FRAMES
}
fun specialk(mesize:Double,mespd:Int,othersize:Double,diff:Double,mepos:Double,otherpos:Double,oldotherpos:Double,oldmecoord:Double,oldothercoord:Double):Double{
    if(diff!=0.0){
        val otherxdiff = otherpos - oldotherpos
        val xright = oldmecoord<oldothercoord

        val meMovingatOtherx =
            if(diff>0 && xright) diff
            else if(diff<0 && !xright) -diff
            else 0.0

        val otherMovingAtMex =
            if(otherxdiff>0 && !xright) otherxdiff
            else if(otherxdiff<0 && xright) -otherxdiff
            else 0.0

        val overlapx =
            if(xright) mepos + mesize - otherpos
            else otherpos + othersize - mepos

        var takebackx: Double = (meMovingatOtherx / (meMovingatOtherx+otherMovingAtMex)) * overlapx
        if (xright) takebackx = takebackx * -1.0
        if(takebackx>0)takebackx+=0.001
        else if(takebackx<0)takebackx-=0.001
        if(abs(takebackx)<mespd+2) {
            return(takebackx)
        }
    }
    return 0.0
}
//fun doIGetBlockedBy(entity: Entity):Boolean {
//    return (entity is Wall) || (entity is Enemy) || entity is Player
//}

fun blockMovement(me:Entity,other: Entity, oldme: EntDimens,oldOther:EntDimens){
    if((other is Wall) || (other is Enemy) || other is Player){
        val xdiff = me.xpos - oldme.xpos
        val ydiff = me.ypos - oldme.ypos
        val midDistX =  abs(abs(oldOther.getMidX())-abs(oldme.getMidX()))
        val midDistY = abs(abs(oldOther.getMidY())-abs(oldme.getMidY()))
        if(midDistX>midDistY){
            me.xpos += specialk(me.drawSize,me.speed,other.drawSize,xdiff,me.xpos,other.xpos,oldOther.xpos,oldme.getMidX(),oldOther.getMidX())
        }else{
            me.ypos += specialk(me.drawSize,me.speed,other.drawSize,ydiff,me.ypos,other.ypos,oldOther.ypos,oldme.getMidY(),oldOther.getMidY())
        }
    }
}
fun stayInMap(me:Entity){
    var limit = INTENDED_FRAME_SIZE-me.drawSize
    limit -= XMAXMAGIC/myFrame.width
    if(me.xpos>limit){
        me.xpos -= me.xpos - limit
    }
    if(me.xpos<0){
        me.xpos -= me.xpos
    }
    if(me.ypos>INTENDED_FRAME_SIZE-me.drawSize) {
        me.ypos -= me.ypos - INTENDED_FRAME_SIZE + me.drawSize
    }
    if(me.ypos<0){
        me.ypos -= me.ypos
    }
}

fun drawHealth(me:hasHealth, g:Graphics){
    me as Entity
    g.color = Color.GREEN
    (g as Graphics2D).stroke = BasicStroke(2f)
    g.drawLine(
        getWindowAdjustedPos(me.xpos).toInt(),
        getWindowAdjustedPos(me.ypos).toInt() - 8,
        getWindowAdjustedPos((me.xpos + (me.drawSize * me.hasHealth.currentHp / me.hasHealth.maxHP))).toInt(),
        getWindowAdjustedPos(me.ypos).toInt() - 8
    )
    g.drawLine(
        getWindowAdjustedPos(me.xpos).toInt(),
        getWindowAdjustedPos(me.ypos).toInt() - 10,
        getWindowAdjustedPos(me.xpos + (me.drawSize * me.hasHealth.currentHp / me.hasHealth.maxHP)).toInt(),
        getWindowAdjustedPos(me.ypos).toInt() - 10
    )
    g.stroke = BasicStroke(1f)
}
class healthHolder{
    var didHeal :Boolean = false
    var currentHp :Double = 10.0
    var maxHP :Double = 10.0
}
interface hasHealth{
    var hasHealth:healthHolder
}
