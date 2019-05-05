import java.awt.*
import java.io.File
import javax.sound.sampled.*
import kotlin.math.abs

val enBulFile = File("src/main/resources/pewnew.wav").getAbsoluteFile()

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
    var turnSpeed:Double
    var bulColor:Color
    fun processShooting(sht:Boolean,weap:Weapon){
        if (sht && weap.framesSinceShottah > this.wep.atkSpd) {
            playSound(this.shootNoise)
            weap.framesSinceShottah = 0
            entsToAdd.add(Bullet(this))
        }
        weap.framesSinceShottah++

    }
    fun processTurning(lef:Boolean,righ:Boolean){
        if (lef) {
            angy += turnSpeed
            if(angy>Math.PI)angy = -Math.PI + turnSpeed
        }
        if (righ){
            angy -= turnSpeed
            if(angy<-Math.PI)angy = Math.PI - turnSpeed
        }
    }
    fun drawCrosshair(g: Graphics){
        if(this.wep.buldmg>2){
            g.color = Color.RED
        }else g.color = Color.MAGENTA

        (g as Graphics2D).stroke = BasicStroke(4f *myFrame.width/INTENDED_FRAME_SIZE)

        val arcdiameter = (this as Entity).drawSize
        fun doarc(diver:Double,timeser:Double){
            val spread = (wep.recoil+1)*6
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
        doarc(drawSize/4,0.5)
        doarc(-drawSize/1.7,2.15)
        doarc(0.0,1.0)
        doarc(-drawSize/3.5,1.55)
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

interface damagedByBullets{
    val ouchNoise:Clip
    val deathNoise:Clip
    fun takeDamage(other:Entity){
        if(other is Bullet && other.shotBy::class!=this::class) {
            (this as hasHealth).currentHp -= (other.shotBy as shoots).wep.buldmg
            if((this as hasHealth).currentHp<1){
                playSound(deathNoise)
                dieFromBullet()
            }else{
                playSound(ouchNoise)
            }

        }
    }
    fun dieFromBullet(){
        (this as Entity).isDead = true
    }
}

interface movementGetsBlocked{
    fun doIGetBlockedBy(entity: Entity):Boolean {
        return !(entity is MedPack) && !(entity is Bullet)
    }
    fun blockMovement(other: Entity, oldme: EntDimens,oldOther:EntDimens){
        
        fun specialk(diff:Double,mepos:Double,otherpos:Double,oldotherpos:Double,oldmecoord:Double,oldothercoord:Double):Double{
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
                if(xright) mepos + (this as Entity).drawSize - otherpos
                else otherpos + other.drawSize - mepos

                var takebackx: Double = (meMovingatOtherx / (meMovingatOtherx+otherMovingAtMex)) * overlapx
                if (xright) takebackx = takebackx * -1.0
                if(abs(takebackx)<(this as Entity).speed+2) {
                    return takebackx
                }
            }
            return 0.0
        }
        
        if(doIGetBlockedBy(other)){
            val xdiff = (this as Entity).xpos - oldme.xpos
            val ydiff = (this as Entity).ypos - oldme.ypos
            val midDistX =  abs(abs(oldOther.getMidpoint().first)-abs(oldme.getMidpoint().first))
            val midDistY = abs(abs(oldOther.getMidpoint().second)-abs(oldme.getMidpoint().second))
            if(midDistX>midDistY){
                (this as Entity).xpos +=  specialk(xdiff,this.xpos,other.xpos,oldOther.xpos,oldme.getMidpoint().first,oldOther.getMidpoint().first)
            }else{
                (this as Entity).ypos +=  specialk(ydiff,this.ypos,other.ypos,oldOther.ypos,oldme.getMidpoint().second,oldOther.getMidpoint().second)
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
    var currentHp :Int
    val maxHP :Int
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
