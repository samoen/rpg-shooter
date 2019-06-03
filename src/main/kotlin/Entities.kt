import java.awt.*
import kotlin.math.abs
import kotlin.math.atan2
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.Rectangle


class Bullet(shottah: HasHealth) : Entity {
    var shDims = (shottah as Entity).commonStuff.dimensions
    var shtbywep = shottah.healthStats.wep.copy()
    var bTeam = shottah.healthStats.teamNumber
    var anglo = shottah.healthStats.angy
    var startDamage = shtbywep.buldmg
    var damage = shtbywep.buldmg/shtbywep.projectiles
    var framesAlive = 0
    var bulDir = anglo + ((Math.random()-0.5)*shtbywep.recoil/6.0)
    override var commonStuff=EntCommon(
        dimensions = run {
            val bsize = shtbywep.bulSize/shtbywep.projectiles
            val shotBysize = shDims.drawSize/shtbywep.projectiles
            EntDimens(
                (shDims.getMidX() - (bsize / 2)) + (Math.cos(anglo) * shotBysize / 2) + (Math.cos(anglo) * bsize / 2),
                (shDims.getMidY() - (bsize / 2)) - (Math.sin(anglo) * shotBysize / 2) - (Math.sin(anglo) * bsize / 2),
                bsize
            )
        },
        speed = shtbywep.bulspd
    )
    override fun updateEntity() {
        allEntities.filter { it is HasHealth && it.commonStuff.dimensions.overlapsOther(this.commonStuff.dimensions) }.forEach {
            it as HasHealth
            if(it.healthStats.teamNumber!=bTeam) takeDamage(this,it)
        }

        allEntities.filter { it is Wall && commonStuff.dimensions.overlapsOther(it.commonStuff.dimensions)}.forEach {
            commonStuff.toBeRemoved = true
            val imp = Impact()
            imp.commonStuff.dimensions.drawSize = commonStuff.dimensions.drawSize
            imp.commonStuff.dimensions.xpos = commonStuff.dimensions.xpos
            imp.commonStuff.dimensions.ypos = commonStuff.dimensions.ypos
            entsToAdd.add(imp)
        }
        commonStuff.dimensions.ypos -= ((((Math.sin(bulDir))) * commonStuff.speed.toDouble()))
        commonStuff.dimensions.xpos += ((((Math.cos(bulDir))) * commonStuff.speed))
        if(commonStuff.dimensions.xpos<0)commonStuff.toBeRemoved = true
        if(commonStuff.dimensions.xpos > INTENDED_FRAME_SIZE - (commonStuff.dimensions.drawSize) - (XMAXMAGIC/myFrame.width))commonStuff.toBeRemoved = true
        if(commonStuff.dimensions.ypos > INTENDED_FRAME_SIZE - commonStuff.dimensions.drawSize) commonStuff.toBeRemoved = true
        if(commonStuff.dimensions.ypos<0)commonStuff.toBeRemoved = true
        framesAlive++
        if(framesAlive>shtbywep.bulLifetime){
            val shrinky = shtbywep.bulSize/13
            damage-=( shrinky*(startDamage/ commonStuff.dimensions.drawSize)).toInt()
            commonStuff.dimensions.drawSize-=shrinky
            commonStuff.dimensions.xpos+=shrinky/2
            commonStuff.dimensions.ypos+=shrinky/2
        }
        if(commonStuff.dimensions.drawSize<=4 || damage<0.5)commonStuff.toBeRemoved=true
    }

}


class Player(val buttonSet: ButtonSet): HasHealth {
    override var commonStuff=EntCommon(
        isSolid = true
    )
    var canEnterGateway:Boolean = true
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val pCont:playControls = playControls()
    var primWep = Weapon()
    var movedRight = false
    var didMove = false
    var didShoot = false
    override var healthStats=HealthStats(
        maxHP = commonStuff.dimensions.drawSize,
        currentHp = commonStuff.dimensions.drawSize,
        teamNumber = 1,
        turnSpeed = 0.1f,
        shootySound = "shoot",
        wep = primWep
    )
    var tSpdMod = healthStats.turnSpeed
    var primaryEquipped = true

    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 12.0,
        buldmg = 4
    )

    var didSpinright = false
    var didSpinleft = false
    var notOnShop = true
    override fun updateEntity() {
        didMove = false
        healthStats.didHeal = false
        val onshops = allEntities.filter { it is Shop && commonStuff.dimensions.overlapsOther(it.commonStuff.dimensions) }.firstOrNull()
        if(onshops!=null){
            if(notOnShop){
                val theShop = onshops as Shop
                menuStuff = theShop.menuThings(this)
            }
            notOnShop = false
        }else notOnShop = true

        var toMovex = 0.0
        var toMovey = 0.0
        if (pCont.riri.booly) toMovex += commonStuff.speed.toDouble()
        if (pCont.leflef.booly) toMovex -= commonStuff.speed.toDouble()
        if (pCont.up.booly){
            toMovey -= commonStuff.speed.toDouble()
        }
        if (pCont.dwm.booly) {
            toMovey += commonStuff.speed.toDouble()
        }
        if(toMovex!=0.0&&toMovey!=0.0){
            toMovex=toMovex*0.707
            toMovey=toMovey*0.707
        }
        if(healthStats.wep.framesSinceShottah<healthStats.wep.atkSpd){
            toMovex *= healthStats.wep.mobility
            toMovey *= healthStats.wep.mobility
        }
//        val notOnShop = specificMenus.values.all { !it }
        if(notOnShop){
            if(pCont.spenlef.booly||pCont.spinri.booly){
                toMovex *= healthStats.wep.mobility
                toMovey *= healthStats.wep.mobility
            }
        }
        commonStuff.dimensions.xpos += toMovex
        commonStuff.dimensions.ypos += toMovey
        if(toMovex>0)movedRight = true
        if(toMovex<0)movedRight = false
        if(toMovex!=0.0||toMovey!=0.0)didMove = true
        stayInMap(this)
        if(notOnShop){
            if(pCont.spenlef.booly == pCont.spinri.booly){
                tSpdMod = healthStats.turnSpeed
            }

            if(didSpinright && !pCont.spinri.booly){
                tSpdMod = healthStats.turnSpeed
            }
            if(didSpinleft && !pCont.spenlef.booly){
                tSpdMod = healthStats.turnSpeed
            }
            didSpinright=pCont.spinri.booly && !pCont.spenlef.booly
            didSpinleft= pCont.spenlef.booly && !pCont.spinri.booly
            tSpdMod-= healthStats.turnSpeed/15
            if(tSpdMod<0)tSpdMod=0.0f
            processTurning(this,pCont.spenlef.booly,pCont.spinri.booly,healthStats.turnSpeed-tSpdMod)
            if(pCont.Swp.tryConsume()){
                playStrSound("swap")
                if (primaryEquipped){
                    healthStats.wep = spareWep
                }else{
                    healthStats.wep = primWep
                }
                primaryEquipped = !primaryEquipped
            }
        }
        processShooting(this,pCont.sht.booly,this.healthStats.wep,pBulImage,notOnShop)
        
        if(notOnShop)healthStats.stopped =!pCont.sht.booly && !pCont.spenlef.booly && !pCont.spinri.booly && !didMove
        else healthStats.stopped = !didMove

        if(healthStats.armorIsBroken){
            healthStats.armorBrokenFrames++
            if (healthStats.armorBrokenFrames>healthStats.shieldSkill*3){
                healthStats.armorIsBroken = false
                healthStats.armorBrokenFrames = 0
            }
        }
        var todraw = stillImage
        if(didShoot){
            pewframecount++
            if(pewframecount < 3){
                todraw = pewImage
            }else {
                pewframecount = 0
                didShoot=false
            }
        }
        if(didMove){
            gaitcount++
            if(gaitcount < 3){
                todraw = runImage
            }else if(gaitcount>5){
                gaitcount = 0
            }
        }else{
            gaitcount = 0
            if(healthStats.getArmored())todraw = pstoppedImage
        }
        if( healthStats.armorIsBroken){
            todraw = armorBrokenImage
            healthStats.didGetShot = false
        }else{
            if (healthStats.didGetShot) {
                if(healthStats.gotShotFrames>0) {
                    todraw = pouchImage
                    healthStats.gotShotFrames--
                } else {
                    healthStats.didGetShot = false
                }
            }
        }
        commonStuff.spriteu = todraw
    }

    fun drawComponents(g: Graphics) {
        drawCrosshair(this,g)
        drawReload(this,g,this.healthStats.wep)
        drawHealth(this,g)
    }

    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,commonStuff.spriteu,g,!(healthStats.angy>Math.PI/2 || healthStats.angy<-Math.PI/2))
    }
    var gaitcount = 0
    var pewframecount = 0
}
class Enemy : HasHealth{
    override var commonStuff=EntCommon(isSolid = true,spriteu = goblinImage)
    override var healthStats=HealthStats(
        maxHP = commonStuff.dimensions.drawSize,
        currentHp = commonStuff.dimensions.drawSize,
        teamNumber = 0,
        shootySound = "laser"
    )
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)

    override fun drawEntity(g: Graphics) {
        super.drawEntity(g)
        drawHealth(this,g)
        drawCrosshair(this,g)
//        val r = Rectangle((xpos).toInt(),(ypos - (healthStats.wep.bulSize/(drawSize))).toInt(),healthStats.wep.bulSize.toInt(),700)
//        val path = Path2D.Double()
//        path.append(r, false)
//        val t = AffineTransform()
//        t.rotate(-healthStats.angy+(-Math.PI/2),(xpos+(drawSize/2)),(ypos+(drawSize/2)))
//        path.transform(t)
//        (g as Graphics2D).draw(path)
    }

    override fun updateEntity() {
//        if (healthStats.didGetShot) {
//            if(healthStats.gotShotFrames>0) {
//                color = Color.ORANGE
//                healthStats.gotShotFrames--
//            } else {
//                color = Color.BLUE
//                healthStats.didGetShot = false
//            }
//        }
        healthStats.didHeal = false
        val filteredEnts = players
            .filter { !it.commonStuff.toBeRemoved }
            .sortedBy { abs(it.commonStuff.dimensions.xpos - commonStuff.dimensions.xpos) + abs(it.commonStuff.dimensions.ypos - commonStuff.dimensions.ypos) }
        val packEnts = allEntities
            .filter {(it is MedPack)}
            .sortedBy { abs(it.commonStuff.dimensions.xpos - commonStuff.dimensions.xpos) + abs(it.commonStuff.dimensions.ypos - commonStuff.dimensions.ypos) }

        if(filteredEnts.isNotEmpty()){
            var firstplayer = filteredEnts.first()
            if(framesSinceDrift<ENEMY_DRIFT_FRAMES) framesSinceDrift++
            if(!(iTried.first==commonStuff.dimensions.xpos && iTried.second==commonStuff.dimensions.ypos)){
                randnumx = (Math.random()-0.5)*2
                randnumy = (Math.random()-0.5)*2
                framesSinceDrift = 0
            } else{
                var adjSpd = commonStuff.speed.toFloat()
                if(framesSinceDrift>=ENEMY_DRIFT_FRAMES){
                    var xdiff = 0.0
                    var ydiff = 0.0
                    if(healthStats.currentHp<healthStats.maxHP/3 && packEnts.isNotEmpty()){
                        val firstpack = packEnts.first()
                        val packxd = firstpack.commonStuff.dimensions.getMidX() - commonStuff.dimensions.getMidX()
                        val packyd = firstpack.commonStuff.dimensions.getMidY() - commonStuff.dimensions.getMidY()
//                        if((Math.abs(packxd)+Math.abs(packyd))<(Math.abs(xdiff)+Math.abs(ydiff))){
                            xdiff = packxd
                            ydiff = packyd
//                        }
                    }else{
                        xdiff = firstplayer.commonStuff.dimensions.getMidX() - commonStuff.dimensions.getMidX()
                        ydiff = firstplayer.commonStuff.dimensions.getMidY() - commonStuff.dimensions.getMidY()
                    }

                    if(healthStats.wep.framesSinceShottah<healthStats.wep.atkSpd){
                        adjSpd *= healthStats.wep.mobility
                        adjSpd *= healthStats.wep.mobility
                    }
                    if (xdiff>adjSpd){
                        commonStuff.dimensions.xpos += adjSpd
                    } else if(xdiff<-adjSpd) {
                        commonStuff.dimensions.xpos -= adjSpd
                    }
                    if (ydiff>adjSpd) commonStuff.dimensions.ypos += adjSpd
                    else if(ydiff<-adjSpd) commonStuff.dimensions.ypos -= adjSpd
                }else{
                    commonStuff.dimensions.ypos += adjSpd*randnumy
                    commonStuff.dimensions.xpos += adjSpd*randnumx
                }
            }
            iTried = Pair(commonStuff.dimensions.xpos,commonStuff.dimensions.ypos)
            stayInMap(this)

            val dx = commonStuff.dimensions.getMidX() - firstplayer.commonStuff.dimensions.getMidX()
            val dy = commonStuff.dimensions.getMidY() - firstplayer.commonStuff.dimensions.getMidY()

            val radtarget = ((atan2( dy , -dx)))
            val absanglediff = abs(radtarget-this.healthStats.angy)
            val shootem =absanglediff<0.2
            var shoot2 = false
            if(shootem){
                val r = Rectangle((commonStuff.dimensions.xpos).toInt(),(commonStuff.dimensions.ypos - (healthStats.wep.bulSize/(commonStuff.dimensions.drawSize))).toInt(),healthStats.wep.bulSize.toInt(),healthStats.wep.bulspd*80)
                val path = Path2D.Double()
                path.append(r, false)
                val t = AffineTransform()
                t.rotate(-healthStats.angy+(-Math.PI/2),(commonStuff.dimensions.xpos+(commonStuff.dimensions.drawSize/2)),(commonStuff.dimensions.ypos+(commonStuff.dimensions.drawSize/2)))
                path.transform(t)
                val intersectors = allEntities.filter {it is Wall || it is Player}.filter {  path.intersects(Rectangle(it.commonStuff.dimensions.xpos.toInt(),it.commonStuff.dimensions.ypos.toInt(),it.commonStuff.dimensions.drawSize.toInt(),it.commonStuff.dimensions.drawSize.toInt()))}.sortedBy { Math.abs(it.commonStuff.dimensions.ypos-commonStuff.dimensions.ypos)+Math.abs(it.commonStuff.dimensions.xpos-commonStuff.dimensions.xpos) }
                if(intersectors.isNotEmpty()) if (intersectors.first() is Player) shoot2 = true
            }
            processShooting(this,shoot2,this.healthStats.wep,eBulImage,true)
            val fix = absanglediff>Math.PI-healthStats.turnSpeed
            var lef = radtarget>=healthStats.angy
            if(fix)lef = !lef
            processTurning(this,lef && !shootem,!lef && !shootem,healthStats.turnSpeed)
        }
    }
}

class Wall : Entity{
    override var commonStuff=EntCommon(isSolid = true,spriteu = wallImage)
}

class Gateway : Entity{
    override var commonStuff=EntCommon()
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    var someoneSpawned:Entity = this
    var sumspn = false

    override fun updateEntity() {
        if(sumspn){
            if(!commonStuff.dimensions.overlapsOther(someoneSpawned.commonStuff.dimensions)){
                sumspn = false
                (someoneSpawned as Player).canEnterGateway = true
            }
        }
        var toremove:Int = -1
        
        for ((index,player) in playersInside.withIndex()){
            if(player.pCont.sht.tryConsume()){
                player.commonStuff.dimensions.xpos = commonStuff.dimensions.xpos
                player.commonStuff.dimensions.ypos = commonStuff.dimensions.ypos
                var canSpawn = true
                if(locked)canSpawn = false
                else
                for(ent in allEntities.filter { it is Player || it is Enemy }){
                    if(player.commonStuff.dimensions.overlapsOther(ent.commonStuff.dimensions))canSpawn = false
                    if(player.commonStuff.dimensions.xpos+player.commonStuff.dimensions.drawSize>INTENDED_FRAME_SIZE || player.commonStuff.dimensions.ypos+player.commonStuff.dimensions.drawSize>INTENDED_FRAME_SIZE)canSpawn = false
                }
                if(canSpawn){
                    toremove = index
                    sumspn = true
                    someoneSpawned = player
                    player.canEnterGateway = false
                    player.commonStuff.toBeRemoved = false
                    entsToAdd.add(player)
                    break
                }
            }
        }
        if(toremove!=-1)
            playersInside.removeAt(toremove)
        if(playersInside.size>=NumPlayers){
            nextMap = map
            nextMapNum = mapnum
            changeMap = true
        }
        if(!locked){
            for (pp in players){
                if(pp.commonStuff.dimensions.overlapsOther(commonStuff.dimensions)){
                    if(pp.canEnterGateway&&!pp.commonStuff.toBeRemoved){
                        pp.commonStuff.toBeRemoved = true
                        pp.commonStuff.dimensions.xpos = commonStuff.dimensions.xpos
                        pp.commonStuff.dimensions.ypos = commonStuff.dimensions.ypos
                        playersInside.add(pp)
                    }
                }
            }
        }
    }
}
class GateSwitch:Entity{
    override var commonStuff=EntCommon(spriteu = gateClosedImage)
    var beenSwitched = false
    override fun updateEntity() {
        if(!beenSwitched){
            players.forEach {
                if(it.commonStuff.dimensions.overlapsOther(commonStuff.dimensions)){
                    beenSwitched = true
                    allEntities.filter { it is Gateway }.forEach {
                        it.commonStuff.spriteu = gateOpenImage
                        (it as Gateway).locked = false
                    }
                }
            }
        }
    }
}
var nextMap = map1
var nextMapNum = 1
var currentMapNum = 1
var changeMap = false
var NumPlayers = 2

class Impact : Entity{
    var liveFrames = 4
    override var commonStuff=EntCommon(spriteu = impactImage)
    override fun updateEntity() {
       liveFrames--
        if(liveFrames<0)commonStuff.toBeRemoved=true
    }
}

class MedPack : Entity {
    override var commonStuff=EntCommon()
    override fun updateEntity() {
        allEntities.filter { it is HasHealth }.forEach {
            it as HasHealth
            if(commonStuff.dimensions.overlapsOther(it.commonStuff.dimensions)){
                if(it.healthStats.currentHp<it.healthStats.maxHP){
                    commonStuff.toBeRemoved = true
                    val desiredhp = it.healthStats.currentHp+20
                    if (desiredhp>it.healthStats.maxHP){
                        it.healthStats.currentHp = it.healthStats.maxHP
                    }else{
                        it.healthStats.currentHp = desiredhp
                    }
                }
            }
        }
    }
}

class Shop:Entity{
    override var commonStuff=EntCommon(spriteu = backgroundImage)
    var char:Char = 'z'
    var menuThings:(Player)->List<Entity> ={ listOf()}
}

class Selector(val numStats:Int,val other:Player,val onUp:()->Unit,val onDown:()->Unit,val onUp1:()->Unit,val onDown1:()->Unit,val onUp2:()->Unit={},val onDown2:()->Unit={},val onUp3:()->Unit={},val onDown3:()->Unit={}):Entity{
    override var commonStuff=EntCommon(dimensions = EntDimens(other.commonStuff.dimensions.xpos+selectorXSpace,other.commonStuff.dimensions.ypos,20.0))
    var indexer = 0
    override fun updateEntity() {
        if(other.pCont.sht.tryConsume()){
            if(indexer+1<numStats){
                indexer++
                commonStuff.dimensions.ypos+=statsYSpace
            }
        }
        if(other.pCont.Swp.tryConsume()){
            if(indexer-1>=0){
                indexer--
                commonStuff.dimensions.ypos -= statsYSpace
            }
        }
        if(other.pCont.spinri.tryConsume()){
            when(indexer){
                0->{ onUp() }
                1->{ onUp1() }
                2->{ onUp2() }
                3->{ onUp3() }
            }
        }else if(other.pCont.spenlef.tryConsume()){
            when(indexer){
                0->{ onDown() }
                1->{ onDown1() }
                2->{ onDown2() }
                3->{ onDown3() }
            }
        }
    }
}
class StatView(val showText: ()->String, val xloc:Double,val yloc:Double):Entity{
    override var commonStuff=EntCommon()
    override fun drawEntity(g: Graphics) {
        g.color = Color.BLUE
        g.font = g.font.deriveFont((myFrame.width/70).toFloat())
        g.drawString(showText(),getWindowAdjustedPos(xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}


