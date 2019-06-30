import java.awt.*
import javax.sound.sampled.AudioSystem
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
    se.commonStuff.dimensions.drawSize = 20+(Math.random()*50)
    se.healthStats.maxHP = (se.commonStuff.dimensions.drawSize)
    se.healthStats.currentHp = se.healthStats.maxHP
    se.commonStuff.speed = (Math.random()*4).toInt()+1
    se.healthStats.wep.bulSize = 8.0+(Math.random()*25)
//    se.healthStats.wep.buldmg = se.healthStats.wep.bulSize.toInt()
    se.healthStats.wep.mobility = Math.random().toFloat()
    se.healthStats.wep.atkSpd = (Math.random()*30).toInt()+10
    se.healthStats.wep.bulspd = (Math.random()*19).toInt()+4
    se.healthStats.wep.bulLifetime = 23
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

fun processShooting(me:HasHealth, sht:Boolean, weap:Weapon, bulImage:Image){
    if (sht && weap.framesSinceShottah > me.healthStats.wep.atkSpd) {
        weap.framesSinceShottah = 0
        me.commonStuff.didShoot=true
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
    if(weap.framesSinceShottah<me.healthStats.wep.atkSpd){
        g.color = Color.YELLOW
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

fun takeDamage(bullet:Bullet, me:HasHealth){
    bullet.commonStuff.toBeRemoved = true
    var desirDam = bullet.damage
    var shieldProc = false
    if(me.healthStats.getArmored()){
        if(me.healthStats.shieldSkill<bullet.damage){
            desirDam = me.healthStats.shieldSkill.toDouble()
            shieldProc = true
        }
    }
    val desirHealth = me.healthStats.currentHp - desirDam
    if(desirHealth<=0){
        if(me is Player){
            me.healthStats.currentHp = me.healthStats.maxHP
            me.spawnGate.playersInside.add(me)
            me.commonStuff.toBeRemoved = true
            if(players.all { it.commonStuff.toBeRemoved && it.spawnGate.locked }){
                changeMap = true
                nextMapNum = currentMapNum
                currentMapNum = previousMapNum
            }
        }else me.healthStats.currentHp = 0.0
        playStrSound(me.healthStats.dieNoise)
        me.commonStuff.toBeRemoved = true
        val deathEnt = object: Entity{
            var liveFrames = 8
            override var commonStuff=EntCommon(
                spriteu = let{
                    if(me is Player) dieImage
                    else enemyDeadImage
                },
                dimensions = EntDimens(me.commonStuff.dimensions.xpos,me.commonStuff.dimensions.ypos,me.commonStuff.dimensions.drawSize))
            override fun updateEntity() {
                liveFrames--
                if(liveFrames<0)commonStuff.toBeRemoved=true
            }
        }
        entsToAdd.add(deathEnt)
    }else{
        me.healthStats.currentHp = desirHealth
        if(me.healthStats.getArmored()){
            if(shieldProc) me.healthStats.armorIsBroken = true
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
    if(abs(midDistX-midDistY)<0.05)return
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

fun placeMap(mapNum:Int,fromMapNum:Int){
    val map=getMapFromNum(mapNum)
    val mapGridSize = ((INTENDED_FRAME_SIZE/mapGridColumns.toDouble())-2.0)
    previousMapNum = fromMapNum
    currentMapNum = mapNum
    allEntities.clear()
    val starty = -10
    for(rownumber in 0 until (map.length/mapGridColumns)){
        for((ind:Int,ch:Char) in map.substring(rownumber*mapGridColumns,(rownumber*mapGridColumns)+mapGridColumns).withIndex()){
            if(ch=='w'){
                entsToAdd.add(Wall().also {
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = (ind +(ind* mapGridSize)).toDouble()
                    it.commonStuff.dimensions.ypos = (starty + (mapGridSize)*(rownumber+1)).toDouble()
                })
                continue
            }
            if (ch == 'h'){
                entsToAdd.add(MedPack().also {
                    it.commonStuff.dimensions.drawSize = mapGridSize/2
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)+mapGridSize/4
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize)*(rownumber+1)+mapGridSize/4
                })
                continue
            }
            if (ch == 'e'){
                entsToAdd.add(randEnemy().also {
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize)*(rownumber+1)
                })
                continue
            }
            if(ch == 's'){
                entsToAdd.add(GateSwitch().also {
                    it.commonStuff.dimensions.drawSize = mapGridSize/2
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)+mapGridSize/4
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize)*(rownumber+1)+mapGridSize/4
                })
                continue
            }
            if(ch == 'b'){
                entsToAdd.add(Shop().also {
                    it.char = 'b'
                    it.commonStuff.spriteu = gunShopImage
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize)*(rownumber+1)
                    it.menuThings = {other->listOf(
                        StatView({"Rcl"},other,0,0),
                        StatView({"Rld"},other,1,0),
                        StatView({"Mob"},other,2,0),
                        Selector(3,other,
                            {
                                val desired = other.healthStats.wep.recoil-1
                                if(desired>=0.0)other.healthStats.wep.recoil=desired
                            },{
                                val desired = other.healthStats.wep.recoil+1
                                if(desired<=MAX_RECOIL)other.healthStats.wep.recoil=desired

                            },{
                                val desired = other.healthStats.wep.atkSpd-3
                                if(desired>=2)other.healthStats.wep.atkSpd=desired

                            },{
                                val desired = other.healthStats.wep.atkSpd+3
                                if(desired<=MAX_RELOED)other.healthStats.wep.atkSpd=desired
                            },{
                                var desired = other.healthStats.wep.mobility+0.1f
                                desired = "%.1f".format(desired).toFloat()
                                if(desired<=1f) other.healthStats.wep.mobility = desired
                            },{
                                var desired = other.healthStats.wep.mobility-0.1f
                                desired = "%.1f".format(desired).toFloat()
                                if(desired>=0)other.healthStats.wep.mobility = desired
                            }),
                        StatStars({other.healthStats.wep.recoil.toString() },{((MAX_RECOIL/1)-(other.healthStats.wep.recoil/1)).toInt()}, other,0),
                        StatStars({other.healthStats.wep.atkSpd.toString() },{((MAX_RELOED/3)-(other.healthStats.wep.atkSpd/3)).toInt()}, other,1),
                        StatStars({other.healthStats.wep.mobility.toString() },{(other.healthStats.wep.mobility/0.1f).toInt()}, other,2)
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
                                other.healthStats.wep.bulSize+=5
                            },{
                                val desiredSize = other.healthStats.wep.bulSize-5
                                if(desiredSize>=(5.0) ){
                                    other.healthStats.wep.bulSize = desiredSize
                                }
                            },{

                                if(other.healthStats.wep.bulspd+2<50){
                                    other.healthStats.wep.bulspd+=2
                                    other.healthStats.wep.bulLifetime++
                                }
                            },{
                                if(other.healthStats.wep.bulspd-2>=1){
                                    other.healthStats.wep.bulspd-=2
                                    other.healthStats.wep.bulLifetime--
                                }
                            },{
                                val desiredproj = other.healthStats.wep.projectiles+2
                                if(desiredproj<=13)other.healthStats.wep.projectiles=desiredproj
                            },{
                                val desiredproj = other.healthStats.wep.projectiles-2
                                if(desiredproj>=1)other.healthStats.wep.projectiles=desiredproj
                            }),
                        StatStars({other.healthStats.wep.bulSize.toInt().toString() }, {((other.healthStats.wep.bulSize-5)/5).toInt()},other,0),
                        StatStars({other.healthStats.wep.bulspd.toString() },{((other.healthStats.wep.bulspd-1)/2).toInt()}, other,1),
                        StatStars({other.healthStats.wep.projectiles.toString() },{((other.healthStats.wep.projectiles-1)/2).toInt()}, other,2)
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
                        StatView({"Blk"},other,2,0),
                        Selector(3,other,
                            {
                                val desiredspd = other.commonStuff.speed+2
                                if(desiredspd<30) other.commonStuff.speed = desiredspd
                        },{
                            val desiredspeed = other.commonStuff.speed-2
                            if(desiredspeed>=3)other.commonStuff.speed = desiredspeed
                        },{
                            other.commonStuff.dimensions.drawSize  += 3
                            other.healthStats.maxHP += 3
//                            other.healthStats.currentHp = other.healthStats.maxHP
                        },{
                            val desiredSize = other.commonStuff.dimensions.drawSize-3
                            val desiredHp = other.healthStats.maxHP-3
                            if(desiredHp>=15){
                                other.commonStuff.dimensions.drawSize = desiredSize
                                other.healthStats.maxHP = desiredHp
                            }
                            other.healthStats.currentHp = other.healthStats.maxHP
                        },
                            {
                                val desired = other.healthStats.shieldSkill-2
                                if(desired>=1)other.healthStats.shieldSkill = desired
                        },{
                                val desiredshld = other.healthStats.shieldSkill+2
                                if(desiredshld<=MAX_SHIELD_SKILL) other.healthStats.shieldSkill = desiredshld
                        }),
                        StatStars({other.commonStuff.speed.toString() },{(((other.commonStuff.speed) - 3)/2).toInt()}, other,0),
                        StatStars({other.healthStats.maxHP.toInt().toString() }, {((other.healthStats.maxHP-15)/3).toInt()},other,1),
                        StatStars({( other.healthStats.shieldSkill).toInt().toString() },{(MAX_SHIELD_SKILL/2)-((other.healthStats.shieldSkill-1)/2).toInt()}, other,2)
                    )}
                    it.char = 'g'
                    it.commonStuff.dimensions.drawSize = mapGridSize
                    it.commonStuff.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.commonStuff.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            val charint:Int= Character.getNumericValue(ch)
            if(charint in 0..9){
                val mappy:String =getMapFromNum(charint)
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

fun getMapFromNum(num:Int):String=when(num){
    0->map0
    1->map1
    2->map2
    3->map3
    else ->map1
}