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


interface shoots{
    var shootNoise:Clip
    var angy :Double
    var wep:Weapon
    var turnSpeed:Float
    var bulColor:Color
    fun processShooting(sht:Boolean,weap:Weapon){
        if (sht && weap.framesSinceShottah > this.wep.atkSpd) {
            weap.framesSinceShottah = 0
            if(this is Player)this.didShoot=true
            val b = Bullet(this)
            var canspawn = true
            allEntities.forEach { if(it is Wall && it.overlapsOther(b))canspawn = false }
            if(canspawn)
            entsToAdd.add(Bullet(this))
            else {
                val imp = Impact()
                imp.drawSize = b.drawSize
                imp.xpos = (b).xpos
                imp.ypos = (b).ypos
                entsToAdd.add(imp)
            }

            if(shootNoise.isRunning){
                val newclip = AudioSystem.getClip().also{
                    it.open(AudioSystem.getAudioInputStream(File("src/main/resources/newlongpew.wav").getAbsoluteFile()))
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
                shootNoise.framePosition=0
                shootNoise.start()
            }
        }
        weap.framesSinceShottah++

    }

    fun processTurning(lef:Boolean,righ:Boolean){
        if (lef) {
            var desired = angy+turnSpeed
            if(desired>Math.PI){
                angy = -Math.PI + (desired-Math.PI)
            }else
                angy += turnSpeed
        }
        if (righ){
            val desired = angy-turnSpeed
            if(desired<-Math.PI)angy = Math.PI - (-Math.PI-desired)
            else angy -= turnSpeed
        }
    }
    fun drawCrosshair(g: Graphics){
        g.color = Color.CYAN
        val strkw = if(this is Player)1.2f
        else 5f
        (g as Graphics2D).stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
        val arcdiameter = (this as Entity).drawSize
        fun doarc(diver:Double,timeser:Double){
            val spread = (7)*(wep.recoil+1)
            val bspd = wep.bulspd*2
            g.drawArc(
                getWindowAdjustedPos(((this as Entity).xpos)+(diver)).toInt()-bspd,
                getWindowAdjustedPos(((this as Entity).ypos)+(diver)).toInt()-bspd,
                (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
                (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
                ((angy*180/Math.PI)-spread/2).toInt(),
                spread.toInt()
            )
        }
        if(this is Player){
            doarc(drawSize/4,0.5)
            doarc(-drawSize/3.5,1.55)
            doarc(0.0,1.0)
            doarc(-drawSize/1.7,2.15)
        }else{
            g.drawArc(
                getWindowAdjustedPos(((this as Entity).xpos)).toInt(),
                getWindowAdjustedPos(((this as Entity).ypos)).toInt(),
                (getWindowAdjustedPos((arcdiameter))).toInt(),
                (getWindowAdjustedPos((arcdiameter))).toInt(),
                ((angy*180/Math.PI)-5/2).toInt(),
                5.toInt()
            )
        }
//        doarc(-drawSize,3.0)
//        g.color = Color.ORANGE
        (g as Graphics2D).stroke = BasicStroke(1f)
    }
    fun drawReload(g: Graphics,weap: Weapon){
        if(weap.framesSinceShottah<wep.atkSpd){
            g.color = Color.CYAN
            (g as Graphics2D).stroke = BasicStroke(2f)

            g.drawLine(
                getWindowAdjustedPos ((this as Entity).xpos).toInt(),
                getWindowAdjustedPos((this as Entity).ypos).toInt()-2,
                getWindowAdjustedPos  ( (xpos + (drawSize * (wep.atkSpd - weap.framesSinceShottah) / wep.atkSpd)) ).toInt(),
                getWindowAdjustedPos(ypos).toInt()-2
            )
            g.drawLine(
                getWindowAdjustedPos ((this as Entity).xpos).toInt(),
                getWindowAdjustedPos((this as Entity).ypos).toInt()-4,
                getWindowAdjustedPos  ( (xpos + (drawSize * (wep.atkSpd - weap.framesSinceShottah) / wep.atkSpd)) ).toInt(),
                getWindowAdjustedPos(ypos).toInt()-4
            )
            g.stroke = BasicStroke(1f)
        }
    }
}
fun takeDamage(other:Entity,me:Entity):Boolean{
    if(other is Bullet && other.shotBy::class!=me::class) {
        (me as hasHealth).currentHp -= (other.shotBy as shoots).wep.buldmg
        if((me as hasHealth).currentHp<1){
            playSound((me as demByBuls).damagedByBul.deathNoise)
            me.isDead = true
            return true
        }else{
            playSound((me as demByBuls).damagedByBul.ouchNoise)
            me.damagedByBul.didGetShot = true
            me.damagedByBul.gotShotFrames = me.damagedByBul.DAMAGED_ANIMATION_FRAMES
        }
    }else if (other is MedPack && (me as hasHealth).currentHp<me.maxHP){
        me.didHeal = true
        val desiredhp = (me as hasHealth).currentHp+20
        if (desiredhp>me.maxHP){
            me.currentHp = me.maxHP
        }else{
            me.currentHp = desiredhp
        }
    }
    return false
}
interface demByBuls{
    val damagedByBul:damagedByBullets
}
class damagedByBullets(val ouchNoise:Clip,
                       val deathNoise:Clip){
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
interface movementGetsBlocked{
    fun doIGetBlockedBy(entity: Entity):Boolean {
        return (entity is Wall) || (entity is Enemy) || entity is Player
    }
    fun blockMovement(other: Entity, oldme: EntDimens,oldOther:EntDimens){

        if(doIGetBlockedBy(other)){
            val xdiff = (this as Entity).xpos - oldme.xpos
            val ydiff = (this as Entity).ypos - oldme.ypos
            val midDistX =  abs(abs(oldOther.getMidpoint().first)-abs(oldme.getMidpoint().first))
            val midDistY = abs(abs(oldOther.getMidpoint().second)-abs(oldme.getMidpoint().second))
            if(midDistX>midDistY){
                xpos += specialk((this as Entity).drawSize,this.speed,other.drawSize,xdiff,this.xpos,other.xpos,oldOther.xpos,oldme.getMidpoint().first,oldOther.getMidpoint().first)
            }else{
                ypos += specialk((this as Entity).drawSize,this.speed,other.drawSize,ydiff,this.ypos,other.ypos,oldOther.ypos,oldme.getMidpoint().second,oldOther.getMidpoint().second)
            }
        }
    }

    fun stayInMap(oldme: Pair<Double, Double>){
        var limit = INTENDED_FRAME_SIZE-(this as Entity).drawSize
        limit -= XMAXMAGIC/myFrame.width
        if(xpos>limit){
            (this as Entity).xpos -= this.xpos - limit
        }
        if((this as Entity).xpos<0){
            (this as Entity).xpos -= this.xpos
        }
        if((this as Entity).ypos>INTENDED_FRAME_SIZE-drawSize) {
            (this as Entity).ypos -= this.ypos - INTENDED_FRAME_SIZE + drawSize
        }
        if((this as Entity).ypos<0){
            (this as Entity).ypos -= this.ypos
        }
    }
}

interface hasHealth{
    var didHeal :Boolean
    var currentHp :Double
    val maxHP :Double
    fun drawHealth(g:Graphics){
        g.color = Color.GREEN
        (g as Graphics2D).stroke = BasicStroke(2f)
        g.drawLine(
            getWindowAdjustedPos((this as Entity).xpos).toInt(),
            getWindowAdjustedPos((this as Entity).ypos).toInt() - 8,
            getWindowAdjustedPos(((this as Entity).xpos + ((this as Entity).drawSize * currentHp / maxHP))).toInt(),
            getWindowAdjustedPos((this as Entity).ypos).toInt() - 8
        )
        g.drawLine(
            getWindowAdjustedPos(xpos).toInt(),
            getWindowAdjustedPos(ypos).toInt() - 10,
            getWindowAdjustedPos((this as Entity).xpos + ((this as Entity).drawSize * currentHp / maxHP)).toInt(),
            getWindowAdjustedPos((this as Entity).ypos).toInt() - 10
        )
        g.stroke = BasicStroke(1f)
    }
}
