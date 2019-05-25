import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.KeyEvent
import java.io.File
import java.util.*
import java.util.logging.Handler
import javax.sound.sampled.*
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

fun drawAsSprite(entity: Entity,image:Image,g:Graphics){
    g.drawImage(image,getWindowAdjustedPos(entity.xpos).toInt(),getWindowAdjustedPos(entity.ypos).toInt(),getWindowAdjustedPos(entity.drawSize).toInt(),getWindowAdjustedPos(entity.drawSize).toInt(),null)
}
fun playStrSound(str:String){
            if(soundBank[str]!!.isRunning){
            val newclip = AudioSystem.getClip().also{
                it.open(AudioSystem.getAudioInputStream(soundFiles[str]))
            }
            newclip.start()
        }else{
                soundBank[str]!!.framePosition=0
                soundBank[str]!!.start()
        }
}
//fun playSound(clip: Clip){
//
//    if(clip.isRunning){
//        clip.stop()
//        AudioSystem.getAudioInputStream(enBulFile)
//    }
//    clip.framePosition = 0
//    clip.start()
//}
fun revivePlayers(heal:Boolean){
    if(!allEntities.contains(player0) && !entsToAdd.contains(player0))entsToAdd.add(player0)
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1)) entsToAdd.add(player1)
    player0.isDead = false
    player1.isDead = false
//    player0.ypos = (INTENDED_FRAME_SIZE - player0.drawSize)
//    player0.xpos = 0.0
//    player1.ypos = (INTENDED_FRAME_SIZE - player1.drawSize)
//    player1.xpos = (player0.drawSize)
    if(heal){
        player0.hasHealth.currentHp = player0.hasHealth.maxHP
        player1.hasHealth.currentHp = player1.hasHealth.maxHP
    }
}

fun randEnemy():Enemy{
    val se = Enemy()
    se.tshd.turnSpeed = (0.01+(Math.random()/14)).toFloat()
    se.drawSize = 20+(Math.random()*30)
    se.hasHealth.maxHP = (se.drawSize/2)
    se.hasHealth.currentHp = se.hasHealth.maxHP
    se.speed = (Math.random()*4).toInt()+1
    se.tshd.wep.bulSize = 8.0+(Math.random()*40)
    se.tshd.wep.buldmg = se.tshd.wep.bulSize.toInt()
    se.tshd.wep.atkSpd = (Math.random()*20).toInt()+10
    se.tshd.wep.bulspd = (Math.random()*10).toInt()+3
    return  se
}

fun startWave(numberofenemies: Int) {
    var lastsize = 0.0
    for (i in 1..numberofenemies) {
        val e = randEnemy()
        e.xpos = (lastsize)
        lastsize += e.drawSize
        e.ypos = 10.0
        entsToAdd.add(e)
    }
}

fun playerKeyPressed(player: Player, e: KeyEvent){
    if (e.keyCode == player.buttonSet.swapgun) player.pCont.Swp.tryProduce()
    if (e.keyCode == player.buttonSet.up) player.pCont.up.tryProduce()
    if (e.keyCode == player.buttonSet.down) player.pCont.dwm.tryProduce()
    if (e.keyCode == player.buttonSet.shoot) player.pCont.sht.tryProduce()
    if (e.keyCode == player.buttonSet.right) player.pCont.riri.tryProduce()
    if (e.keyCode == player.buttonSet.left) player.pCont.leflef.tryProduce()
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.tryProduce()
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.tryProduce()
}

fun playerKeyReleased(player: Player,e: KeyEvent){
    if (e.keyCode == player.buttonSet.swapgun) {
        player.pCont.Swp.release()
    }
    if (e.keyCode == player.buttonSet.up) {
        player.pCont.up.release()
    }
    if (e.keyCode == player.buttonSet.down) {
        player.pCont.dwm.release()
    }
    if (e.keyCode == player.buttonSet.shoot){
        player.pCont.sht.release()
    }
    if (e.keyCode == player.buttonSet.right){
        player.pCont.riri.release()
    }
    if (e.keyCode == player.buttonSet.left) {
        player.pCont.leflef.release()
    }
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.release()
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.release()
}

fun processShooting(me:shoots,sht:Boolean,weap:Weapon){
    if (sht && weap.framesSinceShottah > me.tshd.wep.atkSpd) {
        weap.framesSinceShottah = 0
        if(me is Player)me.didShoot=true
        var numproj = 1
        numproj = ((me.tshd.wep.recoil/(me.tshd.wep.bulspd+me.tshd.wep.buldmg))).toInt()
        for( i in 0..numproj){
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
        }
        playStrSound(me.tshd.shootySound)
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
    me as Entity
    g.color = Color.CYAN
    val strkw = if(me is Player)1.2f
    else 5f
    (g as Graphics2D).stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
    val arcdiameter = (me as Entity).drawSize
    fun doarc(diver:Double,timeser:Double){
        val spread = (7)*(me.tshd.wep.recoil+1)
        val bspd = me.tshd.wep.bulspd*2
        g.drawArc(
            getWindowAdjustedPos((me.xpos)+(diver)).toInt()-bspd,
            getWindowAdjustedPos((me.ypos)+(diver)).toInt()-bspd,
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
            getWindowAdjustedPos((me.xpos)).toInt(),
            getWindowAdjustedPos((me.ypos)).toInt(),
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
    me as Entity
    if(weap.framesSinceShottah<me.tshd.wep.atkSpd){
        g.color = Color.CYAN
        (g as Graphics2D).stroke = BasicStroke(2f)

        g.drawLine(
            getWindowAdjustedPos (me.xpos).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-2,
            getWindowAdjustedPos  ( (me.xpos + (me.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-2
        )
        g.drawLine(
            getWindowAdjustedPos (me.xpos).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-4,
            getWindowAdjustedPos  ((me.xpos + (me.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.ypos).toInt()-4
        )
        g.stroke = BasicStroke(1f)
    }
}
class shd{
//    var shootNoise:Clip = AudioSystem.getClip().also{
//        it.open(AudioSystem.getAudioInputStream(enemyPewFile))
//    }
    var shootySound:String = "die"
    var angy :Double = 0.0
    var wep:Weapon=Weapon()
    var turnSpeed:Float = 0.05f
    var bulColor:Color=Color.RED
}
interface shoots{
    var tshd :shd
}
fun takeDamage(other:Entity,me:Entity):Boolean{
    me as hasHealth
    if(other is Bullet && other.shotBy::class!=me::class) {
        me.hasHealth.currentHp -= other.damage
        if((me as hasHealth).hasHealth.currentHp<1){
            playStrSound(me.damagedByBul.dieNoise)
            me.isDead = true
            val deathEnt = object: Entity{
                override var xpos: Double = me.xpos
                override var ypos: Double = me.ypos
                override var isDead: Boolean = false
                override var entityTag: String = "default"
                override var speed: Int = 2
                override var drawSize: Double = me.drawSize
                override var color: Color = Color.BLUE
                override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
                    g.drawImage(wallImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
                }

                var liveFrames = 8
                override fun updateEntity() {
                    liveFrames--
                    if(liveFrames<0)isDead=true
                }
            }
            entsToAdd.add(deathEnt)
            return true
        }else{
            playStrSound(me.damagedByBul.ouchNoise)
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



class damagedByBullets{
    var ouchNoise = "ouch"
    var dieNoise = "die"
//    val ouchNoise:Clip = AudioSystem.getClip().also{
//        it.open(AudioSystem.getAudioInputStream(ouchnoiseFile))
//    }
//    val deathNoise:Clip = AudioSystem.getClip().also{
//        it.open(AudioSystem.getAudioInputStream(dienoiseFile))
//    }
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
    val damagedByBul:damagedByBullets
}
