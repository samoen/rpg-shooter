import java.awt.*
import java.awt.event.KeyEvent
import javax.sound.sampled.*
import kotlin.math.abs

fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}

fun drawAsSprite(entity: Entity,image:Image,g:Graphics,flipped:Boolean){
    var flipAdjust = 1
    var flipaddAdj = 0.0
    if(flipped){
        flipAdjust = -1
        flipaddAdj = entity.commonStuff.dimensions.drawSize
    }
    g.drawImage(image,getWindowAdjustedPos(entity.commonStuff.dimensions.xpos + flipaddAdj).toInt(),getWindowAdjustedPos(entity.commonStuff.dimensions.ypos).toInt(),getWindowAdjustedPos(entity.commonStuff.dimensions.drawSize).toInt()*flipAdjust,getWindowAdjustedPos(entity.commonStuff.dimensions.drawSize).toInt(),null)
}

fun playStrSound(str:soundType){
//        if(soundBank.containsKey(str)){
//            if(soundBank[str]!!.isRunning){
                AudioSystem.getClip().also{
                    it.open(AudioSystem.getAudioInputStream(soundFiles[str]))
                }.start()
//            }else{
//                soundBank[str]!!.framePosition=0
//                soundBank[str]!!.start()
//            }
//        }
}

fun randEnemy():Enemy{
    val se = Enemy()
    se.healthStats.turnSpeed = (0.01+(Math.random()/14)).toFloat()
    se.commonStuff.dimensions.drawSize = 20+(Math.random()*30)
    se.healthStats.maxHP = (se.commonStuff.dimensions.drawSize/2)
    se.healthStats.currentHp = se.healthStats.maxHP
    se.commonStuff.speed = (Math.random()*4).toInt()+1
    se.healthStats.wep.bulSize = 8.0+(Math.random()*25)
//    se.healthStats.wep.buldmg = se.healthStats.wep.bulSize.toInt()
    se.healthStats.wep.atkSpd = (Math.random()*20).toInt()+10
    se.healthStats.wep.bulspd = (Math.random()*11).toInt()+3
    se.healthStats.wep.bulLifetime = 20
    return  se
}

fun startWave(numberofenemies: Int) {
    var lastsize = 0.0
    for (i in 1..numberofenemies) {
        val e = randEnemy()
        e.commonStuff.dimensions.xpos = (lastsize+20)
        lastsize += e.commonStuff.dimensions.drawSize
        e.commonStuff.dimensions.ypos = 10.0
        entsToAdd.add(e)
    }
}

//fun playerKeyPressed(player: Player, e: KeyEvent){
//    if (e.keyCode == player.buttonSet.swapgun) player.pCont.Swp.tryProduce()
//    if (e.keyCode == player.buttonSet.up) player.pCont.up.tryProduce()
//    if (e.keyCode == player.buttonSet.down) player.pCont.dwm.tryProduce()
//    if (e.keyCode == player.buttonSet.shoot) player.pCont.sht.tryProduce()
//    if (e.keyCode == player.buttonSet.right) player.pCont.riri.tryProduce()
//    if (e.keyCode == player.buttonSet.left) player.pCont.leflef.tryProduce()
//    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.tryProduce()
//    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.tryProduce()
//}

//fun playerKeyReleased(player: Player,e: KeyEvent){
//    if (e.keyCode == player.buttonSet.swapgun) {
//        player.pCont.Swp.release()
//    }
//    if (e.keyCode == player.buttonSet.up) {
//        player.pCont.up.release()
//    }
//    if (e.keyCode == player.buttonSet.down) {
//        player.pCont.dwm.release()
//    }
//    if (e.keyCode == player.buttonSet.shoot){
//        player.pCont.sht.release()
//    }
//    if (e.keyCode == player.buttonSet.right){
//        player.pCont.riri.release()
//    }
//    if (e.keyCode == player.buttonSet.left) {
//        player.pCont.leflef.release()
//    }
//    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.release()
//    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.release()
//}

fun processShooting(me:HasHealth, sht:Boolean, weap:Weapon, bulImage:Image){
    if (sht && weap.framesSinceShottah > me.healthStats.wep.atkSpd) {
        weap.framesSinceShottah = 0
        if(me is Player)me.didShoot=true
        for( i in 1..weap.projectiles){
            val b = Bullet(me)
            b.commonStuff.spriteu = bulImage
            var canspawn = true
            allEntities.forEach { if(it is Wall && it.commonStuff.dimensions.overlapsOther(b.commonStuff.dimensions))canspawn = false }
            if(canspawn)
                entsToAdd.add(b)
            else {
                val imp = Impact()
                imp.commonStuff.dimensions.drawSize = b.commonStuff.dimensions.drawSize
                imp.commonStuff.dimensions.xpos = (b).commonStuff.dimensions.xpos
                imp.commonStuff.dimensions.ypos = (b).commonStuff.dimensions.ypos
                entsToAdd.add(imp)
            }
        }
        playStrSound(me.healthStats.shootySound)
    }
    weap.framesSinceShottah++
}
fun processTurning(me:HasHealth, lef:Boolean, righ:Boolean,tSpd:Float){
    if(lef&&righ)return
    if (lef) {
        val desired = me.healthStats.angy+tSpd
        if(desired>Math.PI){
            me.healthStats.angy = -Math.PI + (desired-Math.PI)
        }else
            me.healthStats.angy += tSpd
    }else if (righ){
        val desired = me.healthStats.angy-tSpd
        if(desired<-Math.PI)me.healthStats.angy = Math.PI - (-Math.PI-desired)
        else me.healthStats.angy -= tSpd
    }
}
fun drawCrosshair(me:HasHealth, g: Graphics){
    g as Graphics2D
    g.color = Color.CYAN
    val strkw = 2.5f
    g.stroke = BasicStroke(strkw *myFrame.width/INTENDED_FRAME_SIZE)
    val arcdiameter = (me as Entity).commonStuff.dimensions.drawSize
    fun doarc(diver:Double,timeser:Double){
        val spread = (7)*(me.healthStats.wep.recoil+1)
        val bspd = me.healthStats.wep.bulspd*2
        g.drawArc(
            getWindowAdjustedPos((me.commonStuff.dimensions.xpos)+(diver)).toInt()-bspd,
            getWindowAdjustedPos((me.commonStuff.dimensions.ypos)+(diver)).toInt()-bspd,
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            (getWindowAdjustedPos((arcdiameter)*timeser)+bspd*2).toInt(),
            ((me.healthStats.angy*180/Math.PI)-spread/4).toInt(),
            (spread/2).toInt()
        )
    }
    doarc(me.commonStuff.dimensions.drawSize/4,0.5)
    doarc(-me.commonStuff.dimensions.drawSize/3.5,1.55)
    doarc(0.0,1.0)
    doarc(-me.commonStuff.dimensions.drawSize/1.7,2.15)
    g.stroke = BasicStroke(1f)
}
fun drawReload(me:HasHealth, g: Graphics, weap: Weapon){
    me as Entity
    if(weap.framesSinceShottah<me.healthStats.wep.atkSpd){
        g.color = Color.CYAN
        (g as Graphics2D).stroke = BasicStroke(2f)
        g.drawLine(
            getWindowAdjustedPos (me.commonStuff.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt()-2,
            getWindowAdjustedPos  ( (me.commonStuff.dimensions.xpos + (me.commonStuff.dimensions.drawSize * (me.healthStats.wep.atkSpd - weap.framesSinceShottah) / me.healthStats.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt()-2
        )
        g.drawLine(
            getWindowAdjustedPos (me.commonStuff.dimensions.xpos).toInt(),
            getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt()-4,
            getWindowAdjustedPos  ((me.commonStuff.dimensions.xpos + (me.commonStuff.dimensions.drawSize * (me.healthStats.wep.atkSpd - weap.framesSinceShottah) / me.healthStats.wep.atkSpd)) ).toInt(),
            getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt()-4
        )
        g.stroke = BasicStroke(1f)
    }
}

fun takeDamage(other:Entity,me:Entity){
    me as HasHealth
    other as Bullet
    other.commonStuff.toBeRemoved = true
    var desirDam = other.damage
    if(me.healthStats.getArmored()){
        if(me.healthStats.shieldSkill<other.damage){
            desirDam = me.healthStats.shieldSkill
        }
    }
    val desirHealth = me.healthStats.currentHp - desirDam
    if(desirHealth<=0){
        if(me is Player){
            me.healthStats.currentHp = me.healthStats.maxHP
            me.spawnGate.playersInside.add(me)
        }else me.healthStats.currentHp = 0.0
        playStrSound(me.healthStats.dieNoise)
        me.commonStuff.toBeRemoved = true
        val deathEnt = object: Entity{
            var liveFrames = 8
            override var commonStuff=EntCommon(spriteu = dieImage, dimensions = EntDimens(me.commonStuff.dimensions.xpos,me.commonStuff.dimensions.ypos,me.commonStuff.dimensions.drawSize))
            override fun updateEntity() {
                liveFrames--
                if(liveFrames<0)commonStuff.toBeRemoved=true
            }
        }
        entsToAdd.add(deathEnt)
    }else{
        me.healthStats.currentHp = desirHealth
        if(me.healthStats.getArmored()){
            me.healthStats.armorIsBroken = true
            playStrSound(soundType.SWAP)
        }else playStrSound(me.healthStats.ouchNoise)
        me.healthStats.didGetShot = true
        me.healthStats.gotShotFrames = DAMAGED_ANIMATION_FRAMES
    }
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
        if(takebackx>0)takebackx+=0.1
        else if(takebackx<0)takebackx-=0.1
        if(abs(takebackx)<mespd+2) {
            return(takebackx)
        }
    }
    return 0.0
}

fun blockMovement(me:Entity,other: Entity, oldme: EntDimens,oldOther:EntDimens){
    val xdiff = me.commonStuff.dimensions.xpos - oldme.xpos
    val ydiff = me.commonStuff.dimensions.ypos - oldme.ypos
    val midDistX =  abs(abs(oldOther.getMidX())-abs(oldme.getMidX()))
    val midDistY = abs(abs(oldOther.getMidY())-abs(oldme.getMidY()))
    if(midDistX>midDistY){
        me.commonStuff.dimensions.xpos += specialk(me.commonStuff.dimensions.drawSize,me.commonStuff.speed,other.commonStuff.dimensions.drawSize,xdiff,me.commonStuff.dimensions.xpos,other.commonStuff.dimensions.xpos,oldOther.xpos,oldme.getMidX(),oldOther.getMidX())
    }else{
        me.commonStuff.dimensions.ypos += specialk(me.commonStuff.dimensions.drawSize,me.commonStuff.speed,other.commonStuff.dimensions.drawSize,ydiff,me.commonStuff.dimensions.ypos,other.commonStuff.dimensions.ypos,oldOther.ypos,oldme.getMidY(),oldOther.getMidY())
    }
}
fun stayInMap(me:Entity){
    var limit = INTENDED_FRAME_SIZE-me.commonStuff.dimensions.drawSize
    limit -= XMAXMAGIC/myFrame.width
    if(me.commonStuff.dimensions.xpos>limit){
        me.commonStuff.dimensions.xpos -= me.commonStuff.dimensions.xpos - limit
    }
    if(me.commonStuff.dimensions.xpos<0){
        me.commonStuff.dimensions.xpos -= me.commonStuff.dimensions.xpos
    }
    if(me.commonStuff.dimensions.ypos>INTENDED_FRAME_SIZE-me.commonStuff.dimensions.drawSize) {
        me.commonStuff.dimensions.ypos -= me.commonStuff.dimensions.ypos - INTENDED_FRAME_SIZE + me.commonStuff.dimensions.drawSize
    }
    if(me.commonStuff.dimensions.ypos<0){
        me.commonStuff.dimensions.ypos -= me.commonStuff.dimensions.ypos
    }
}

fun drawHealth(me:HasHealth, g:Graphics){
    g.color = Color.GREEN
    (g as Graphics2D).stroke = BasicStroke(2f)
    g.drawLine(
        getWindowAdjustedPos(me.commonStuff.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt() - 8,
        getWindowAdjustedPos((me.commonStuff.dimensions.xpos + (me.commonStuff.dimensions.drawSize * me.healthStats.currentHp / me.healthStats.maxHP))).toInt(),
        getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt() - 8
    )
    g.drawLine(
        getWindowAdjustedPos(me.commonStuff.dimensions.xpos).toInt(),
        getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt() - 10,
        getWindowAdjustedPos(me.commonStuff.dimensions.xpos + (me.commonStuff.dimensions.drawSize * me.healthStats.currentHp / me.healthStats.maxHP)).toInt(),
        getWindowAdjustedPos(me.commonStuff.dimensions.ypos).toInt() - 10
    )
    g.stroke = BasicStroke(1f)
}

fun placeMap(map:String, mapNum:Int,fromMapNum:Int){
    val mapGridSize = (INTENDED_FRAME_SIZE/mapGridColumns.toDouble())-2
    currentMapNum = mapNum
    allEntities.clear()
    val starty = 0
    for(rownumber in 0 until (map.length/mapGridColumns)){
        for((ind:Int,ch:Char) in map.substring(rownumber*mapGridColumns,(rownumber*mapGridColumns)+mapGridColumns).withIndex()){
            if(ch=='w'){
                entsToAdd.add(Wall().also {
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'h'){
                entsToAdd.add(MedPack().also {
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'e'){
                entsToAdd.add(randEnemy().also {
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 's'){
                entsToAdd.add(GateSwitch().also {
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 'b'){
                entsToAdd.add(Shop().also {
                    it.char = 'b'
                    it.commonStuff.spriteu = gunShopImage
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                    it.menuThings = {other->listOf(
                        StatView({"Rcl"},other,0,0),
                        StatView({"Rld"},other,1,0),
                        StatView({"Mob"},other,2,0),
                        Selector(3,other,
                            {
                                val desired = other.healthStats.wep.recoil+0.5
                                if(desired<15)other.healthStats.wep.recoil=desired
                            },{
                                val desired = other.healthStats.wep.recoil-0.5
                                if(desired>=0.0)other.healthStats.wep.recoil=desired
                            },{
                                if(other.healthStats.wep.atkSpd+1<200){
                                    other.healthStats.wep.atkSpd++
                                }
                            },{
                                if(other.healthStats.wep.atkSpd-1>1)other.healthStats.wep.atkSpd--
                            },{
                                val desired = other.healthStats.wep.mobility+0.1f
                                if(desired<=1.001f) other.healthStats.wep.mobility = desired
                            },{
                                val desired = other.healthStats.wep.mobility-0.1f
                                if(desired>=0)other.healthStats.wep.mobility = desired
                            }),
                        StatView({other.healthStats.wep.recoil.toString() }, other,0,1),
                        StatView({other.healthStats.wep.atkSpd.toString() }, other,1,1),
                        StatView({( other.healthStats.wep.mobility*10).toInt().toString() }, other,2,1)
                    )
                    }
                })
                continue
            }
            if(ch == 'm'){
                entsToAdd.add(Shop().also {
                    it.commonStuff.spriteu = ammoShopImage
                    it.char = 'm'
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                    it.menuThings = {other->listOf(
                        StatView({"Dmg"},other,0,0),
                        StatView({"Rng"},other,1,0),
                        StatView({"Buk"},other,2,0),
                        Selector(3,other,
                            {
//                                other.healthStats.wep.buldmg+=1
                                other.healthStats.wep.bulSize+=1
                            },{
//                                val desiredDmg = other.healthStats.wep.buldmg-1
                                val desiredSize = other.healthStats.wep.bulSize-1
                                if(desiredSize>(MIN_ENT_SIZE/2) ){
                                    other.healthStats.wep.bulSize = desiredSize
//                                    other.healthStats.wep.buldmg = desiredDmg
                                }
                            },{
                                if(other.healthStats.wep.bulspd+1<50 && other.healthStats.wep.bulLifetime+1<100){
                                    other.healthStats.wep.bulspd++
                                    other.healthStats.wep.bulLifetime++
                                }
                            },{
                                if(other.healthStats.wep.bulspd-1>2 && other.healthStats.wep.bulLifetime-1>1){
                                    other.healthStats.wep.bulspd--
                                    other.healthStats.wep.bulLifetime--
                                }
                            },{
                                if(other.healthStats.wep.projectiles+1<20)other.healthStats.wep.projectiles++
                            },{
                                if(other.healthStats.wep.projectiles-1>=1)other.healthStats.wep.projectiles--
                            }),
                        StatView({other.healthStats.wep.bulSize.toInt().toString() }, other,0,1),
                        StatView({other.healthStats.wep.bulspd.toString() }, other,1,1),
                        StatView({other.healthStats.wep.projectiles.toString() }, other,2,1)
                    )}
                })
                continue
            }
            if(ch == 'g'){
                entsToAdd.add(Shop().also {
                    it.commonStuff.spriteu = healthShopImage
                    it.menuThings = {other->listOf(
                        StatView({"Run"},other,0,0),
                        StatView({"HP"},other,1,0),
//                        StatView({"Turn"},other,2,0),
                        StatView({"Blk"},other,2,0),
                        Selector(3,other,
                            {
                            other.commonStuff.speed += 1
                        },{
                            val desiredspeed = other.commonStuff.speed-1
                            if(desiredspeed>0)other.commonStuff.speed = desiredspeed
                        },{
                            other.commonStuff.dimensions.drawSize  += 3
                            other.healthStats.maxHP += 3
                            other.healthStats.currentHp = other.healthStats.maxHP
                        },{
                            val desiredSize = other.commonStuff.dimensions.drawSize-3
                            val desiredHp = other.healthStats.maxHP-10
                            if(desiredSize>MIN_ENT_SIZE && desiredHp>0){
                                other.commonStuff.dimensions.drawSize = desiredSize
                                other.healthStats.maxHP = desiredHp
                            }
                            other.healthStats.currentHp = other.healthStats.maxHP
                        },
//                            {
//                            val desired = "%.4f".format(other.healthStats.turnSpeed+0.01f).toFloat()
//                            if(desired<1) other.healthStats.turnSpeed = desired
//                        }, {
//                            val desired = "%.4f".format(other.healthStats.turnSpeed-0.01f).toFloat()
//                            if(desired>0) other.healthStats.turnSpeed = desired
//                        },
                            {
                            other.healthStats.shieldSkill += 1
                        },{
                            val desired = other.healthStats.shieldSkill-1
                            if(desired>=1)other.healthStats.shieldSkill = desired
                        }),
                        StatView({other.commonStuff.speed.toString() }, other,0,1),
                        StatView({other.healthStats.maxHP.toInt().toString() }, other,1,1),
//                        StatView({( other.healthStats.turnSpeed*100).toInt().toString() }, other,2,1),
                        StatView({( other.healthStats.shieldSkill).toInt().toString() }, other,2,1)
                    )}
                    it.char = 'g'
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            val charint:Int= Character.getNumericValue(ch)
            if(charint in 1..9){
                val mappy:String =when(charint){
                    1->map1
                    2->map2
                    3->map3
                    else ->map1
                }
                val gatex = ind.toDouble()+(ind* mapGridSize)
                val gatey = starty + (mapGridSize+1)*(rownumber+1)
                val gate = Gateway().also {
                    it.map = mappy
                    it.mapnum = charint
                    it.commonStuff.dimensions.xpos = gatex
                    it.commonStuff.dimensions.ypos = gatey
                    it.commonStuff.dimensions.drawSize = mapGridSize
                }
                if(charint==fromMapNum){
                    var lastsize = 0.0
                    for(player in players){
                        player.commonStuff.dimensions.xpos = gatex + lastsize
                        player.commonStuff.dimensions.ypos = gatey
                        player.spawnGate = gate
                        lastsize = (player.commonStuff.dimensions.drawSize)
                        if(!allEntities.contains(player) && !entsToAdd.contains(player))entsToAdd.add(player)
                        player.commonStuff.toBeRemoved = false
                    }
                }
                entsToAdd.add(gate)
                continue
            }

        }
    }
}