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

fun randEnemy():Enemy{
    val se = Enemy()
    se.shootStats.turnSpeed = (0.01+(Math.random()/14)).toFloat()
    se.dimensions.drawSize = 20+(Math.random()*30)
    se.healthStats.maxHP = (se.dimensions.drawSize/2)
    se.healthStats.currentHp = se.healthStats.maxHP
    se.speed = (Math.random()*4).toInt()+1
    se.shootStats.wep.bulSize = 8.0+(Math.random()*40)
    se.shootStats.wep.buldmg = se.shootStats.wep.bulSize.toInt()
    se.shootStats.wep.atkSpd = (Math.random()*20).toInt()+10
    se.shootStats.wep.bulspd = (Math.random()*10).toInt()+3
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

fun processShooting(me:Shoots, sht:Boolean, weap:Weapon, bulImage:Image,notOnShop:Boolean){
    if (sht && weap.framesSinceShottah > me.shootStats.wep.atkSpd && notOnShop) {
        weap.framesSinceShottah = 0
        if(me is Player)me.didShoot=true
        for( i in 1..weap.projectiles){
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
        playStrSound(me.shootStats.shootySound)
    }
    weap.framesSinceShottah++
}
fun processTurning(me:Shoots, lef:Boolean, righ:Boolean,tSpd:Float){
    if(lef&&righ)return
    if (lef) {
        val desired = me.shootStats.angy+tSpd
        if(desired>Math.PI){
            me.shootStats.angy = -Math.PI + (desired-Math.PI)
        }else
            me.shootStats.angy += tSpd
    }else if (righ){
        val desired = me.shootStats.angy-tSpd
        if(desired<-Math.PI)me.shootStats.angy = Math.PI - (-Math.PI-desired)
        else me.shootStats.angy -= tSpd
    }
}
fun drawCrosshair(me:Shoots, g: Graphics){
    me as Entity
    g as Graphics2D
    g.color = Color.CYAN
    val strkw = 1.5f
    g.stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
    val arcdiameter = (me as Entity).dimensions.drawSize
    fun doarc(diver:Double,timeser:Double){
        val spread = (7)*(me.shootStats.wep.recoil+1)
        val bspd = me.shootStats.wep.bulspd*2
        g.drawArc(
            getWindowAdjustedPos((me.dimensions.xpos)+(diver)).toInt()-bspd,
            getWindowAdjustedPos((me.dimensions.ypos)+(diver)).toInt()-bspd,
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            ((me.shootStats.angy*180/Math.PI)-spread/2).toInt(),
            spread.toInt()
        )
    }
    doarc(me.dimensions.drawSize/4,0.5)
    doarc(-me.dimensions.drawSize/3.5,1.55)
    doarc(0.0,1.0)
    doarc(-me.dimensions.drawSize/1.7,2.15)
    g.stroke = BasicStroke(1f)
}
fun drawReload(me:Shoots, g: Graphics, weap: Weapon){
    me as Entity
    if(weap.framesSinceShottah<me.shootStats.wep.atkSpd){
        g.color = Color.CYAN
        (g as Graphics2D).stroke = BasicStroke(2f)
        g.drawLine(
            getWindowAdjustedPos (me.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-2,
            getWindowAdjustedPos  ( (me.dimensions.xpos + (me.dimensions.drawSize * (me.shootStats.wep.atkSpd - weap.framesSinceShottah) / me.shootStats.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-2
        )
        g.drawLine(
            getWindowAdjustedPos (me.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-4,
            getWindowAdjustedPos  ((me.dimensions.xpos + (me.dimensions.drawSize * (me.shootStats.wep.atkSpd - weap.framesSinceShottah) / me.shootStats.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.dimensions.ypos).toInt()-4
        )
        g.stroke = BasicStroke(1f)
    }
}

fun takeDamage(other:Entity,me:Entity):Boolean{
    me as HasHealth
    if(other is Bullet) {
        if(me is Shoots){
            if(me.shootStats.teamNumber==other.shottah.shootStats.teamNumber)return false
        }
        other.toBeRemoved = true
        var desirDam = other.damage
        if(me.healthStats.getArmored()){
            if(me.healthStats.shieldSkill<other.damage){
                desirDam = me.healthStats.shieldSkill
            }
        }
        val desirHealth = me.healthStats.currentHp - desirDam
        if(desirHealth<=0){
            me.healthStats.currentHp = 0.0
            playStrSound(me.healthStats.dieNoise)
            me.toBeRemoved = true
            val deathEnt = object: Entity{
                override var isSolid=false
                override var dimensions = EntDimens(me.dimensions.xpos,me.dimensions.ypos,me.dimensions.drawSize)
                override var toBeRemoved: Boolean = false
                override var entityTag: String = "default"
                override var speed: Int = 2
                override var color: Color = Color.BLUE
                override fun drawEntity(g: Graphics) {
                    drawAsSprite(this,dieImage,g)
                }

                var liveFrames = 8
                override fun updateEntity() {
                    liveFrames--
                    if(liveFrames<0)toBeRemoved=true
                }
            }
            entsToAdd.add(deathEnt)
            return true
        }
        me.healthStats.currentHp = desirHealth
        if(me.healthStats.getArmored()){
            me.healthStats.armorIsBroken = true
            playStrSound("swap")
        }else playStrSound(me.healthStats.ouchNoise)
        me.healthStats.armorWillBreak = true

        me.healthStats.didGetShot = true
        me.healthStats.gotShotFrames = me.healthStats.DAMAGED_ANIMATION_FRAMES

    }else if (other is MedPack && (me as HasHealth).healthStats.currentHp<me.healthStats.maxHP){
        other.toBeRemoved = true
        me.healthStats.didHeal = true
        val desiredhp = (me as HasHealth).healthStats.currentHp+20
        if (desiredhp>me.healthStats.maxHP){
            me.healthStats.currentHp = me.healthStats.maxHP
        }else{
            me.healthStats.currentHp = desiredhp
        }
    }
    return false
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
    if(other.isSolid){
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

fun drawHealth(me:HasHealth, g:Graphics){
    me as Entity
    g.color = Color.GREEN
    (g as Graphics2D).stroke = BasicStroke(2f)
    g.drawLine(
        getWindowAdjustedPos(me.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 8,
        getWindowAdjustedPos((me.dimensions.xpos + (me.dimensions.drawSize * me.healthStats.currentHp / me.healthStats.maxHP))).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 8
    )
    g.drawLine(
        getWindowAdjustedPos(me.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 10,
        getWindowAdjustedPos(me.dimensions.xpos + (me.dimensions.drawSize * me.healthStats.currentHp / me.healthStats.maxHP)).toInt(),
        getWindowAdjustedPos(me.dimensions.ypos).toInt() - 10
    )
    g.stroke = BasicStroke(1f)
}

