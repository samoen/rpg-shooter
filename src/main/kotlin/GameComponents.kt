import java.awt.*
import java.awt.event.KeyEvent
import javax.sound.sampled.*
import kotlin.math.abs

fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}

fun drawAsSprite(entity: Entity,image:Image,g:Graphics){
    g.drawImage(image,getWindowAdjustedPos(entity.dimensions.xpos).toInt(),getWindowAdjustedPos(entity.dimensions.ypos).toInt(),getWindowAdjustedPos(entity.dimensions.drawSize).toInt(),getWindowAdjustedPos(entity.dimensions.drawSize).toInt(),null)
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
fun revivePlayers(heal:Boolean){
    if(!allEntities.contains(player0) && !entsToAdd.contains(player0))entsToAdd.add(player0)
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1)) entsToAdd.add(player1)
    player0.toBeRemoved = false
    player1.toBeRemoved = false
//    player0.dimensions.ypos = (INTENDED_FRAME_SIZE - player0.dimensions.drawSize)
//    player0.dimensions.xpos = 0.0
//    player1.dimensions.ypos = (INTENDED_FRAME_SIZE - player1.dimensions.drawSize)
//    player1.dimensions.xpos = (player0.dimensions.drawSize)
    if(heal){
        player0.hasHealth.currentHp = player0.hasHealth.maxHP
        player1.hasHealth.currentHp = player1.hasHealth.maxHP
    }
}

fun randEnemy():Enemy{
    val se = Enemy()
    se.tshd.turnSpeed = (0.01+(Math.random()/14)).toFloat()
    se.dimensions.drawSize = 20+(Math.random()*30)
    se.hasHealth.maxHP = (se.dimensions.drawSize/2)
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
        e.dimensions.xpos = (lastsize)
        lastsize += e.dimensions.drawSize
        e.dimensions.ypos = 10.0
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

fun processShooting(me:shoots,sht:Boolean,weap:Weapon,bulImage:Image){
    if (sht && weap.framesSinceShottah > me.tshd.wep.atkSpd) {
        weap.framesSinceShottah = 0
        if(me is Player)me.didShoot=true
        var numproj = 1
        numproj = ((me.tshd.wep.recoil/(me.tshd.wep.bulspd+me.tshd.wep.buldmg))).toInt()
        for( i in 0..numproj){
            val b = Bullet(me)
            b.bulImage = bulImage
            var canspawn = true
            allEntities.forEach { if(it is Wall && it.overlapsOther(b))canspawn = false }
            if(canspawn)
                entsToAdd.add(b)
            else {
                val imp = Impact()
                imp.dimensions.drawSize = b.dimensions.drawSize
                imp.dimensions.xpos = (b).dimensions.xpos
                imp.dimensions.ypos = (b).dimensions.ypos
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
    g as Graphics2D
    g.color = Color.CYAN
    val strkw = 1.5f
//        if(me is Player)1.2f
//    else 5f
    g.stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
    val arcdiameter = (me as Entity).dimensions.drawSize
    fun doarc(diver:Double,timeser:Double){
        val spread = (7)*(me.tshd.wep.recoil+1)
        val bspd = me.tshd.wep.bulspd*2
        g.drawArc(
            getWindowAdjustedPos((me.dimensions.xpos)+(diver)).toInt()-bspd,
            getWindowAdjustedPos((me.dimensions.ypos)+(diver)).toInt()-bspd,
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            ((me.tshd.angy*180/Math.PI)-spread/2).toInt(),
            spread.toInt()
        )
    }
//    if(me is Player){
        doarc(me.dimensions.drawSize/4,0.5)
        doarc(-me.dimensions.drawSize/3.5,1.55)
        doarc(0.0,1.0)
        doarc(-me.dimensions.drawSize/1.7,2.15)
//    }else{
//        g.drawArc(
//            getWindowAdjustedPos((me.dimensions.xpos)).toInt(),
//            getWindowAdjustedPos((me.dimensions.ypos)).toInt(),
//            (getWindowAdjustedPos((arcdiameter))).toInt(),
//            (getWindowAdjustedPos((arcdiameter))).toInt(),
//            ((me.tshd.angy*180/Math.PI)-5/2).toInt(),
//            5
//        )
//    }
    g.stroke = BasicStroke(1f)
}
fun drawReload(me:shoots,g: Graphics,weap: Weapon){
    me as Entity
    if(weap.framesSinceShottah<me.tshd.wep.atkSpd){
        g.color = Color.CYAN
        (g as Graphics2D).stroke = BasicStroke(2f)

        g.drawLine(
            getWindowAdjustedPos (me.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-2,
            getWindowAdjustedPos  ( (me.dimensions.xpos + (me.dimensions.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-2
        )
        g.drawLine(
            getWindowAdjustedPos (me.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-4,
            getWindowAdjustedPos  ((me.dimensions.xpos + (me.dimensions.drawSize * (me.tshd.wep.atkSpd - weap.framesSinceShottah) / me.tshd.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-4
        )
        g.stroke = BasicStroke(1f)
    }
}
class shd{
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
            me.toBeRemoved = true
            val deathEnt = object: Entity{
                override var dimensions = EntDimens(me.dimensions.xpos,me.dimensions.ypos,me.dimensions.drawSize)
                override var toBeRemoved: Boolean = false
                override var entityTag: String = "default"
                override var speed: Int = 2
                override var color: Color = Color.BLUE
                override fun drawEntity(g: Graphics) {
                    drawAsSprite(this,wallImage,g)
                }

                var liveFrames = 8
                override fun updateEntity() {
                    liveFrames--
                    if(liveFrames<0)toBeRemoved=true
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
        val xdiff = me.dimensions.xpos - oldme.xpos
        val ydiff = me.dimensions.ypos - oldme.ypos
        val midDistX =  abs(abs(oldOther.getMidX())-abs(oldme.getMidX()))
        val midDistY = abs(abs(oldOther.getMidY())-abs(oldme.getMidY()))
        if(midDistX>midDistY){
            me.dimensions.xpos += specialk(me.dimensions.drawSize,me.speed,other.dimensions.drawSize,xdiff,me.dimensions.xpos,other.dimensions.xpos,oldOther.xpos,oldme.getMidX(),oldOther.getMidX())
        }else{
            me.dimensions.ypos += specialk(me.dimensions.drawSize,me.speed,other.dimensions.drawSize,ydiff,me.dimensions.ypos,other.dimensions.ypos,oldOther.ypos,oldme.getMidY(),oldOther.getMidY())
        }
    }
}
fun stayInMap(me:Entity){
    var limit = INTENDED_FRAME_SIZE-me.dimensions.drawSize
    limit -= XMAXMAGIC/myFrame.width
    if(me.dimensions.xpos>limit){
        me.dimensions.xpos -= me.dimensions.xpos - limit
    }
    if(me.dimensions.xpos<0){
        me.dimensions.xpos -= me.dimensions.xpos
    }
    if(me.dimensions.ypos>INTENDED_FRAME_SIZE-me.dimensions.drawSize) {
        me.dimensions.ypos -= me.dimensions.ypos - INTENDED_FRAME_SIZE + me.dimensions.drawSize
    }
    if(me.dimensions.ypos<0){
        me.dimensions.ypos -= me.dimensions.ypos
    }
}

fun drawHealth(me:hasHealth, g:Graphics){
    me as Entity
    g.color = Color.GREEN
    (g as Graphics2D).stroke = BasicStroke(2f)
    g.drawLine(
        getWindowAdjustedPos(me.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 8,
        getWindowAdjustedPos((me.dimensions.xpos + (me.dimensions.drawSize * me.hasHealth.currentHp / me.hasHealth.maxHP))).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 8
    )
    g.drawLine(
        getWindowAdjustedPos(me.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 10,
        getWindowAdjustedPos(me.dimensions.xpos + (me.dimensions.drawSize * me.hasHealth.currentHp / me.hasHealth.maxHP)).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 10
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
