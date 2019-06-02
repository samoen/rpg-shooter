import java.awt.*
import kotlin.math.abs
import kotlin.math.atan2
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.Rectangle


class Bullet(val shottah: HasHealth) : Entity {
    var shtbywep = shottah.healthStats.wep.copy()
    var anglo = shottah.healthStats.angy
    override var isSolid=false
    override var dimensions = let {
        (shottah as Entity)
        val bsize = shtbywep.bulSize/shtbywep.projectiles
        val shotBysize = shottah.dimensions.drawSize/shtbywep.projectiles
        EntDimens(
            (shottah.getMidX() - (bsize / 2)) + (Math.cos(anglo) * shotBysize / 2) + (Math.cos(
                anglo
            ) * bsize / 2),
            ((shottah as Entity).getMidY() - (bsize / 2)) - (Math.sin(anglo) * shotBysize / 2) - (Math.sin(
                anglo
            ) * bsize / 2),
            bsize
        )
    }
    var bulImage = wallImage
    var startDamage = shtbywep.buldmg
    var damage = shtbywep.buldmg/shtbywep.projectiles
    var framesAlive = 0
    var bulDir = anglo + ((Math.random()-0.5)*shtbywep.recoil/6.0)
    override var speed = shtbywep.bulspd
    override var toBeRemoved: Boolean = false
    override fun updateEntity() {
        allEntities.filter { it is Wall && overlapsOther(it)}.forEach {
            toBeRemoved = true
            val imp = Impact()
            imp.dimensions.drawSize = dimensions.drawSize
            imp.dimensions.xpos = dimensions.xpos
            imp.dimensions.ypos = dimensions.ypos
            entsToAdd.add(imp)
        }
        dimensions.ypos -= ((((Math.sin(bulDir))) * speed.toDouble()))
        dimensions.xpos += ((((Math.cos(bulDir))) * speed))
        if(dimensions.xpos<0)toBeRemoved = true
        if(dimensions.xpos > INTENDED_FRAME_SIZE - (dimensions.drawSize) - (XMAXMAGIC/myFrame.width))toBeRemoved = true
        if(dimensions.ypos > INTENDED_FRAME_SIZE - dimensions.drawSize) toBeRemoved = true
        if(dimensions.ypos<0)toBeRemoved = true
        framesAlive++
        if(framesAlive>shtbywep.bulLifetime){
            val shrinky = shtbywep.bulSize/13
            damage-=( shrinky*(startDamage/ dimensions.drawSize)).toInt()
            dimensions.drawSize-=shrinky
            dimensions.xpos+=shrinky/2
            dimensions.ypos+=shrinky/2
//            if(damage<0)damage=0
        }
        if(dimensions.drawSize<=4 || damage<0.5)toBeRemoved=true
    }

    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,bulImage,g)
    }
}


class Player(val buttonSet: ButtonSet): Entity, HasHealth {
    override var isSolid=true
    override var dimensions = EntDimens(0.0,0.0,40.0)
    var canEnterGateway:Boolean = true
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val pCont:playControls = playControls()
    var primWep = Weapon()
    var movedRight = false
    var didMove = false
    var didShoot = false
    override var speed = 10
    override var healthStats=HealthStats().also {
        it.maxHP=dimensions.drawSize
        it.currentHp = it.maxHP
        it.teamNumber = 1
        it.turnSpeed = 0.1f
        it.shootySound = "shoot"
        it.bulColor = Color.LIGHT_GRAY
        it.wep = primWep
    }
    var tSpdMod = healthStats.turnSpeed
    var primaryEquipped = true

    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 12.0,
        buldmg = 4
    )

    override var toBeRemoved: Boolean = false
    var didSpinright = false
    var didSpinleft = false
    var notOnShop = true
    override fun updateEntity() {
        didMove = false
        healthStats.didHeal = false
        val shops = allEntities.filter { it is Shop }
        val onshops = shops.filter { overlapsOther(it) }
        var isonshop = onshops.isNotEmpty()
        if(isonshop && notOnShop){
            val theShop = onshops.first() as Shop
            menuStuff = theShop.menuThings(this)
        }
        notOnShop = !isonshop


        var toMovex = 0.0
        var toMovey = 0.0
        if (pCont.riri.booly) toMovex += speed.toDouble()
        if (pCont.leflef.booly) toMovex -= speed.toDouble()
        if (pCont.up.booly){
            toMovey -= speed.toDouble()
        }
        if (pCont.dwm.booly) {
            toMovey += speed.toDouble()
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
        dimensions.xpos += toMovex
        dimensions.ypos += toMovey
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

    }

    fun drawComponents(g: Graphics) {
        drawCrosshair(this,g)
        drawReload(this,g,this.healthStats.wep)
        drawHealth(this,g)
    }

    override fun drawEntity(g: Graphics) {
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
            todraw = stopOuchImage
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
        if(healthStats.angy>Math.PI/2 || healthStats.angy<-Math.PI/2){
            drawAsSprite(this,todraw,g)
        }else{
            g.drawImage(todraw,getWindowAdjustedPos(dimensions.xpos+dimensions.drawSize).toInt(),getWindowAdjustedPos(dimensions.ypos).toInt(),-getWindowAdjustedPos(dimensions.drawSize).toInt(),getWindowAdjustedPos(dimensions.drawSize).toInt(),null)
        }
    }
    var gaitcount = 0
    var pewframecount = 0
}
class Enemy : Entity, HasHealth{

    override var dimensions = EntDimens(0.0,0.0,25.0)
//    override var healthStats= let{
//        val ss = ShootStats()
//        ss.teamNumber = 0
//        ss.bulColor = Color.RED
//        ss.shootySound = "laser"
//        ss
//    }
    override var speed = 1
    override var healthStats=HealthStats().also {
        it.maxHP=dimensions.drawSize
        it.currentHp = it.maxHP
        it.teamNumber = 0
        it.bulColor = Color.RED
        it.shootySound = "laser"
    }
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)
    override var isSolid=true
    override var toBeRemoved: Boolean = false

    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,goblinImage,g)
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
            .filter { !it.toBeRemoved }
            .sortedBy { abs(it.dimensions.xpos - dimensions.xpos) + abs(it.dimensions.ypos - dimensions.ypos) }
        val packEnts = allEntities
            .filter {(it is MedPack)}
            .sortedBy { abs(it.dimensions.xpos - dimensions.xpos) + abs(it.dimensions.ypos - dimensions.ypos) }

        if(filteredEnts.isNotEmpty()){
            var firstplayer = filteredEnts.first()
            if(framesSinceDrift<ENEMY_DRIFT_FRAMES) framesSinceDrift++
            if(!(iTried.first==dimensions.xpos && iTried.second==dimensions.ypos)){
                randnumx = (Math.random()-0.5)*2
                randnumy = (Math.random()-0.5)*2
                framesSinceDrift = 0
            } else{
                var adjSpd = speed.toFloat()
                if(framesSinceDrift>=ENEMY_DRIFT_FRAMES){
                    var xdiff = 0.0
                    var ydiff = 0.0
                    if(healthStats.currentHp<healthStats.maxHP/3 && packEnts.isNotEmpty()){
                        val firstpack = packEnts.first()
                        val packxd = firstpack.getMidX() - getMidX()
                        val packyd = firstpack.getMidY() - getMidY()
//                        if((Math.abs(packxd)+Math.abs(packyd))<(Math.abs(xdiff)+Math.abs(ydiff))){
                            xdiff = packxd
                            ydiff = packyd
//                        }
                    }else{
                        xdiff = firstplayer.getMidX() - getMidX()
                        ydiff = firstplayer.getMidY() - getMidY()
                    }

                    if(healthStats.wep.framesSinceShottah<healthStats.wep.atkSpd){
                        adjSpd *= healthStats.wep.mobility
                        adjSpd *= healthStats.wep.mobility
                    }
                    if (xdiff>adjSpd){
                        dimensions.xpos += adjSpd
                    } else if(xdiff<-adjSpd) {
                        dimensions.xpos -= adjSpd
                    }
                    if (ydiff>adjSpd) dimensions.ypos += adjSpd
                    else if(ydiff<-adjSpd) dimensions.ypos -= adjSpd
                }else{
                    dimensions.ypos += adjSpd*randnumy
                    dimensions.xpos += adjSpd*randnumx
                }
            }
            iTried = Pair(dimensions.xpos,dimensions.ypos)
            stayInMap(this)

            val dx = getMidX() - firstplayer.getMidX()
            val dy = getMidY() - firstplayer.getMidY()

            val radtarget = ((atan2( dy , -dx)))
            val absanglediff = abs(radtarget-this.healthStats.angy)
            val shootem =absanglediff<0.2
            var shoot2 = false
            if(shootem){
                val r = Rectangle((dimensions.xpos).toInt(),(dimensions.ypos - (healthStats.wep.bulSize/(dimensions.drawSize))).toInt(),healthStats.wep.bulSize.toInt(),healthStats.wep.bulspd*80)
                val path = Path2D.Double()
                path.append(r, false)
                val t = AffineTransform()
                t.rotate(-healthStats.angy+(-Math.PI/2),(dimensions.xpos+(dimensions.drawSize/2)),(dimensions.ypos+(dimensions.drawSize/2)))
                path.transform(t)
                val intersectors = allEntities.filter {it is Wall || it is Player}.filter {  path.intersects(Rectangle(it.dimensions.xpos.toInt(),it.dimensions.ypos.toInt(),it.dimensions.drawSize.toInt(),it.dimensions.drawSize.toInt()))}.sortedBy { Math.abs(it.dimensions.ypos-dimensions.ypos)+Math.abs(it.dimensions.xpos-dimensions.xpos) }
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
    override var isSolid=true
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,wallImage,g)
    }
}

class Gateway : Entity{
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    var someoneSpawned:Entity = this
    var sumspn = false
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        if(locked) drawAsSprite(this,gateClosedImage,g)
        else drawAsSprite(this,gateOpenImage,g)
    }

    override fun updateEntity() {
        if(sumspn){
            if(!overlapsOther(someoneSpawned)){
                sumspn = false
                (someoneSpawned as Player).canEnterGateway = true
            }
        }
        var toremove:Int = -1
        
        for ((index,player) in playersInside.withIndex()){
            if(player.pCont.sht.tryConsume()){
                player.dimensions.xpos = dimensions.xpos
                player.dimensions.ypos = dimensions.ypos
                var canSpawn = true
                if(locked)canSpawn = false
                else
                for(ent in allEntities.filter { it is Player || it is Enemy }){
                    if(player.overlapsOther(ent))canSpawn = false
                    if(player.dimensions.xpos+player.dimensions.drawSize>INTENDED_FRAME_SIZE || player.dimensions.ypos+player.dimensions.drawSize>INTENDED_FRAME_SIZE)canSpawn = false
                }
                if(canSpawn){
                    toremove = index
                    sumspn = true
                    someoneSpawned = player
                    player.canEnterGateway = false
                    player.toBeRemoved = false
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
                if(pp.overlapsOther(this)){
                    if(pp.canEnterGateway&&!pp.toBeRemoved){
                        pp.toBeRemoved = true
                        pp.dimensions.xpos = dimensions.xpos
                        pp.dimensions.ypos = dimensions.ypos
                        playersInside.add(pp)
                    }
                }
            }
        }
    }
}
class GateSwitch:Entity{
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    var beenSwitched = false
    override fun updateEntity() {
        if(!beenSwitched){
            players.forEach {
                if(it.overlapsOther(this)){
                    beenSwitched = true
                    allEntities.filter { it is Gateway }.forEach {
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
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,impactImage,g)
    }

    var liveFrames = 4
    override fun updateEntity() {
       liveFrames--
        if(liveFrames<0)toBeRemoved=true
    }
}

class MedPack : Entity {
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
}

class Shop:Entity{
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    var char:Char = 'z'
    var menuThings:(Player)->List<Entity> ={ listOf()}
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    var image = backgroundImage
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,image,g)
    }
}

class Selector(val numStats:Int,val other:Player,val onUp:()->Unit,val onDown:()->Unit,val onUp1:()->Unit,val onDown1:()->Unit,val onUp2:()->Unit={},val onDown2:()->Unit={},val onUp3:()->Unit={},val onDown3:()->Unit={}):Entity{
    override var isSolid=false
    override var dimensions = EntDimens(other.dimensions.xpos+selectorXSpace,other.dimensions.ypos,20.0)
    var indexer = 0
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    override fun updateEntity() {
        if(other.pCont.sht.tryConsume()){
            if(indexer+1<numStats){
                indexer++
                dimensions.ypos+=statsYSpace
            }
        }
        if(other.pCont.Swp.tryConsume()){
            if(indexer-1>=0){
                indexer--
                dimensions.ypos -= statsYSpace
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
    override var isSolid=false
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        g.color = Color.BLUE
        g.font = g.font.deriveFont((myFrame.width/70).toFloat())
        g.drawString(showText(),getWindowAdjustedPos(xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}


