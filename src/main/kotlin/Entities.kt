import java.awt.*
import kotlin.math.abs
import kotlin.math.atan2
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.Rectangle
import kotlin.Boolean

class Bullet(shottah: HasHealth) : Entity {
    var shDims = (shottah as Entity).commonStuff.dimensions
    var shtbywep = shottah.healthStats.wep.copy()
    var bTeam = shottah.healthStats.teamNumber
    var anglo = shottah.healthStats.angy
//    var startDamage = shtbywep.bulSize.toInt()
    var damage = shtbywep.bulSize
    var framesAlive = 0
    var bulDir = let{
        var newcoil = shtbywep.recoil
        if(shtbywep.projectiles>1){
            newcoil += 1/(shtbywep.recoil+0.5)
        }
        anglo + ((Math.random()-0.5)*newcoil/6.0)
    }
    override var commonStuff=EntCommon(
        dimensions = run {
            val bsize = shtbywep.bulSize
            val shotBysize = shDims.drawSize
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
            imp.commonStuff.dimensions.drawSize = commonStuff.dimensions.drawSize+5
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
//            val shrinky = SHRINK_RATE
            val shrinky = shtbywep.bulSize/SHRINK_RATE
//            damage-=( shrinky*(startDamage/ commonStuff.dimensions.drawSize)).toInt()
            damage-=(shrinky)
            commonStuff.dimensions.drawSize-=shrinky
            commonStuff.dimensions.xpos+=shrinky/2
            commonStuff.dimensions.ypos+=shrinky/2
            if(
//            commonStuff.dimensions.drawSize<5.0
                commonStuff.dimensions.drawSize<shtbywep.bulSize/3
                    ||
                damage<1
                )commonStuff.toBeRemoved=true
        }
    }

}
val SHRINK_RATE = 8

class Player: HasHealth {
    var canEnterGateway:Boolean = true
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val pCont:playControls = playControls()
    var primWep = Weapon()

    var primaryEquipped = true
    var notOnShop = true
    override var commonStuff=EntCommon(
        dimensions = EntDimens(0.0,0.0,30.0),
        isSolid = true,
        speed = 9
    )

    override var healthStats=HealthStats(
        maxHP = commonStuff.dimensions.drawSize,
        currentHp = commonStuff.dimensions.drawSize,
        teamNumber = 1,
        turnSpeed = 0.1f,
        shootySound = soundType.SHOOT,
        wep = primWep
    )

    var spareWep:Weapon = Weapon(
        atkSpd = 8,
        recoil = 6.0,
        bulSize = 10.0,
        projectiles = 3,
        mobility = 0.9f,
        bulLifetime = 7,
        bulspd = 13
    )
    override fun updateEntity() {
        var didStopBlock = false
        healthStats.didHeal = false
        val onshops = allEntities.filter { it is Shop && commonStuff.dimensions.overlapsOther(it.commonStuff.dimensions) }.firstOrNull()
        if(onshops!=null){
            if(notOnShop){
                val theShop = onshops as Shop
                menuStuff = theShop.menuThings(this)
            }
            notOnShop = false
        }else {
                notOnShop = true
        }
        if(notOnShop){
            if(pCont.Swp){
                playStrSound(soundType.SWAP)
                if (primaryEquipped){
                    healthStats.wep = spareWep
                }else{
                    healthStats.wep = primWep
                }
                primaryEquipped = !primaryEquipped
            }
        }
        processShooting(this,pCont.sht&&notOnShop,this.healthStats.wep,pBulImage)
        var toMovex = 0.0
        var toMovey = 0.0

        toMovex +=  Math.cos( pCont.leftStickAngle*Math.PI/180)* pCont.leftStickMag*commonStuff.speed
        toMovey -=  Math.sin( pCont.leftStickAngle*Math.PI/180)* pCont.leftStickMag*commonStuff.speed


        if(healthStats.wep.framesSinceShottah-1<=healthStats.wep.atkSpd){
            toMovex *= healthStats.wep.mobility
            toMovey *= healthStats.wep.mobility
        }
//        else if(pCont.rightStickMag>0.01){
        var stickmagamt = pCont.rightStickMag
        val magMax = 0.98f
        if(stickmagamt>magMax)stickmagamt = magMax
        val moveamt = abs(healthStats.wep.mobility+((1-healthStats.wep.mobility)*(1-stickmagamt)))
        toMovey *= moveamt
        toMovex *= moveamt
//        }

        if(pCont.rightStickMag>0.09){
            val desAng = pCont.rightStickAngle*Math.PI/180
            val myAng =  healthStats.angy %(2*Math.PI)
            var a = (desAng - myAng + Math.PI) % (2*Math.PI) - Math.PI
            if(a<-Math.PI){
                a += Math.PI * 2
            }
            if(pCont.rightStickMag>0.95)healthStats.angy = desAng
            else{
                val desAdd = pCont.rightStickMag*0.3*a
                healthStats.angy = (healthStats.angy + desAdd)%(Math.PI*2)
            }
        }
        if(Math.abs(pCont.leftStickMag)>0.09){
            commonStuff.dimensions.xpos += toMovex
            commonStuff.dimensions.ypos += toMovey
            if(
            pCont.leftStickMag*commonStuff.speed>4
//                Math.sqrt(Math.pow(toMovex,2.0) + Math.pow(toMovey,2.0))>2
            ){
                didStopBlock = true
            }
        }

        stayInMap(this)
        if(notOnShop){
            healthStats.stopped = !didStopBlock
            if(healthStats.wep.framesSinceShottah<healthStats.shieldSkill){
                healthStats.stopped = false
            }
        }
        else healthStats.stopped = !didStopBlock

        if(healthStats.armorIsBroken){
            healthStats.armorBrokenFrames++
            if (healthStats.armorBrokenFrames>healthStats.shieldSkill*3){
                healthStats.armorIsBroken = false
                healthStats.armorBrokenFrames = 0
            }
        }
        var todraw = stillImage
        if(commonStuff.didShoot){
            pewframecount++
            if(pewframecount < 3){
                todraw = pewImage
            }else {
                pewframecount = 0
                commonStuff.didShoot=false
            }
        }
        if(didStopBlock){
            gaitcount++
            if(gaitcount < 3){
                todraw = runImage
            }else if(gaitcount>5){
                gaitcount = 0
            }
        }else{
            gaitcount = 0
        }
        if(healthStats.getArmored())todraw = pstoppedImage

        if( healthStats.armorIsBroken){
            todraw = armorBrokenImage
            healthStats.didGetShot = false
        }

            if (healthStats.didGetShot) {
                if(healthStats.gotShotFrames>0) {
                    todraw = pouchImage
                    healthStats.gotShotFrames--
                } else {
                    healthStats.didGetShot = false
                }
            }

        commonStuff.spriteu = todraw
    }

    override fun drawEntity(g: Graphics) {
        g as Graphics2D
        if(
            healthStats.wep.bulspd>20
            && healthStats.wep.recoil<4
            && healthStats.wep.framesSinceShottah>healthStats.wep.atkSpd
            ){
                g.stroke = BasicStroke(0.01f)
                val path = Path2D.Double()
                path.moveTo(getWindowAdjustedPos(commonStuff.dimensions.getMidX()),getWindowAdjustedPos(commonStuff.dimensions.getMidY()))
                path.lineTo(getWindowAdjustedPos(commonStuff.dimensions.getMidX()+Math.cos(healthStats.angy)*INTENDED_FRAME_SIZE),getWindowAdjustedPos(commonStuff.dimensions.getMidY().toInt()-(Math.sin(healthStats.angy)*INTENDED_FRAME_SIZE)))
                val intersectors = allEntities.filter {it is Wall}.filter {  path.intersects(Rectangle(getWindowAdjustedPos(it.commonStuff.dimensions.xpos).toInt(),getWindowAdjustedPos(it.commonStuff.dimensions.ypos).toInt(),getWindowAdjustedPos(it.commonStuff.dimensions.drawSize).toInt(),getWindowAdjustedPos(it.commonStuff.dimensions.drawSize).toInt()))}.sortedBy { Math.abs(it.commonStuff.dimensions.ypos-commonStuff.dimensions.ypos)+Math.abs(it.commonStuff.dimensions.xpos-commonStuff.dimensions.xpos) }
                g.color = Color.RED
                if(intersectors.isEmpty()){
                    g.draw(path)
                }else{
                    val guy = intersectors.first()
                    var amt = Math.pow(guy.commonStuff.dimensions.ypos-commonStuff.dimensions.ypos,2.0)+Math.pow(guy.commonStuff.dimensions.xpos-commonStuff.dimensions.xpos,2.0)
                    amt = Math.sqrt(amt)
                    val path2 = Path2D.Double()
                    path2.moveTo(getWindowAdjustedPos(commonStuff.dimensions.getMidX()),getWindowAdjustedPos(commonStuff.dimensions.getMidY()))
                    path2.lineTo(getWindowAdjustedPos(commonStuff.dimensions.getMidX()+Math.cos(healthStats.angy)*amt),getWindowAdjustedPos(commonStuff.dimensions.getMidY().toInt()-(Math.sin(healthStats.angy)*amt)))
                    g.draw(path2)
                }
            }
            g.color = Color.GREEN

        drawAsSprite(this,commonStuff.spriteu,g,!(healthStats.angy>Math.PI/2 || healthStats.angy<-Math.PI/2))
        if(healthStats.wep.framesSinceShottah>healthStats.wep.atkSpd)g.color = Color.GREEN
        else g.color = Color.YELLOW
        drawCrosshair(this,g)
        drawReload(this,g,this.healthStats.wep)
        drawHealth(this,g)
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
        shootySound = soundType.LASER
    )
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)
    var runTick = 0
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,commonStuff.spriteu,g,!(healthStats.angy>Math.PI/2 || healthStats.angy<-Math.PI/2))
        drawHealth(this,g)
        g.color = Color.GREEN
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
        healthStats.didHeal = false
        val filteredEnts = players
            .filter { !it.commonStuff.toBeRemoved }
            .sortedBy { abs(it.commonStuff.dimensions.xpos - commonStuff.dimensions.xpos) + abs(it.commonStuff.dimensions.ypos - commonStuff.dimensions.ypos) }
        val packEnts = allEntities
            .filter {(it is MedPack)}
            .sortedBy { abs(it.commonStuff.dimensions.xpos - commonStuff.dimensions.xpos) + abs(it.commonStuff.dimensions.ypos - commonStuff.dimensions.ypos) }

        if(filteredEnts.isNotEmpty()){
            val maxTick = commonStuff.dimensions.drawSize*2
            val firstplayer = filteredEnts.first()
            if(framesSinceDrift<ENEMY_DRIFT_FRAMES) framesSinceDrift++
            if(runTick>maxTick)runTick = 0
            runTick+=(4).toInt()
            if(runTick<maxTick/2)commonStuff.spriteu = goblinImage
            else commonStuff.spriteu = enemyWalkImage
            if(healthStats.wep.framesSinceShottah<10)commonStuff.spriteu=enemyShootImage

            var xdiff = firstplayer.commonStuff.dimensions.getMidX() - commonStuff.dimensions.getMidX()
            var ydiff = firstplayer.commonStuff.dimensions.getMidY() - commonStuff.dimensions.getMidY()
            if(!(iTried.first==commonStuff.dimensions.xpos && iTried.second==commonStuff.dimensions.ypos)){
                randnumx = (Math.random()-0.5)*2
                randnumy = (Math.random()-0.5)*2
                framesSinceDrift = 0
            } else{
                fun modifyPos(xamt:Double,yamt:Double){
                    commonStuff.dimensions.xpos+=xamt
                    commonStuff.dimensions.ypos+=yamt
                    runTick+=((xamt+yamt)).toInt()
                }

                var adjSpd = commonStuff.speed.toFloat()
                if(framesSinceDrift>=ENEMY_DRIFT_FRAMES){
                    if(healthStats.currentHp<healthStats.maxHP/3 && packEnts.isNotEmpty()){
                        val firstpack = packEnts.first()
                        val packxd = firstpack.commonStuff.dimensions.getMidX() - commonStuff.dimensions.getMidX()
                        val packyd = firstpack.commonStuff.dimensions.getMidY() - commonStuff.dimensions.getMidY()
//                        if((Math.abs(packxd)+Math.abs(packyd))<(Math.abs(xdiff)+Math.abs(ydiff))){
                            xdiff = packxd
                            ydiff = packyd
//                        }
                    }

                    if(healthStats.wep.framesSinceShottah<healthStats.wep.atkSpd){
                        adjSpd *= healthStats.wep.mobility
                        adjSpd *= healthStats.wep.mobility
                    }
                    var adjx = adjSpd.toDouble()
                    var adjy = adjSpd.toDouble()
                    if(xdiff<0)adjx=adjSpd.toDouble()*-1
                    if(ydiff<0)adjy=adjSpd.toDouble()*-1
                    if(Math.abs(xdiff)<5)adjx=0.0
                    if(Math.abs(ydiff)<5)adjy=0.0
                    modifyPos(adjx,adjy)
                }else{
                    modifyPos(adjSpd*randnumx,adjSpd*randnumy)
                }
            }
            iTried = Pair(commonStuff.dimensions.xpos,commonStuff.dimensions.ypos)
            stayInMap(this)

//            val dx = commonStuff.dimensions.getMidX() - firstplayer.commonStuff.dimensions.getMidX()
//            val dy = commonStuff.dimensions.getMidY() - firstplayer.commonStuff.dimensions.getMidY()

            val radtarget = ((atan2( -ydiff , xdiff)))
            val absanglediff = abs(radtarget-this.healthStats.angy)
            val shootem =absanglediff<0.4
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
            processShooting(this,shoot2,this.healthStats.wep,eBulImage)
            val fix = absanglediff>Math.PI-healthStats.turnSpeed
            var lef = radtarget>=healthStats.angy
            if(fix)lef = !lef
            val aimDone = absanglediff<0.1
            processTurning(this,lef&&!aimDone,!lef&&!aimDone,healthStats.turnSpeed)
        }
    }
}

class Wall : Entity{
    override var commonStuff=EntCommon(isSolid = true,spriteu = wallImage)
}

class Gateway : Entity{
    override var commonStuff=EntCommon(spriteu = gateClosedImage)
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
            if(player.pCont.selDwn){
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
        if(playersInside.size>=players.size){
            var navigate = false
            players.forEach { if(it.pCont.selUp)navigate=true }
            if(navigate){
                nextMapNum = mapnum
                changeMap = true
            }
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
    override var commonStuff=EntCommon(spriteu = gateSwitchImage)
    var beenSwitched = false
    override fun updateEntity() {
        if(!beenSwitched){
            players.forEach {
                if(it.commonStuff.dimensions.overlapsOther(commonStuff.dimensions)){
                    beenSwitched = true
                    commonStuff.spriteu = gateSwitchActiveImage
                    allEntities.filter { it is Gateway }.forEach {
                        it.commonStuff.spriteu = gateOpenImage
                        (it as Gateway).locked = false
                    }
                }
            }
        }
    }
}
var previousMapNum = 0
var nextMapNum = 1
var currentMapNum = 1
var changeMap = false

class Impact : Entity{
    var liveFrames = 4
    override var commonStuff=EntCommon(spriteu = impactImage)
    override fun updateEntity() {
       liveFrames--
        if(liveFrames<0)commonStuff.toBeRemoved=true
    }
}

class MedPack : Entity {
    override var commonStuff=EntCommon(spriteu = medpackImage)
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
    override var commonStuff=EntCommon(
        dimensions = EntDimens(drawSize = 30.0)
//        dimensions = EntDimens(other.commonStuff.dimensions.xpos+selectorXSpace,other.commonStuff.dimensions.ypos-10,25.0)
    )
    var indexer = 0
    override fun updateEntity() {
        commonStuff.dimensions.xpos=other.commonStuff.dimensions.xpos+selectorXSpace
        commonStuff.dimensions.ypos=other.commonStuff.dimensions.ypos+(indexer*statsYSpace) - 7
        if(other.pCont.selDwn){
            if(indexer+1<numStats){
                indexer++
            }
        }
        if(other.pCont.selUp){
            if(indexer-1>=0){
                indexer--
            }
        }
        if(other.pCont.selRight){
            when(indexer){
                0->{ onUp() }
                1->{ onUp1() }
                2->{ onUp2() }
                3->{ onUp3() }
            }
        }else if(other.pCont.selLeft){
            when(indexer){
                0->{ onDown() }
                1->{ onDown1() }
                2->{ onDown2() }
                3->{ onDown3() }
            }
        }
    }
}
class StatStars(val showText: ()->String,val stars:()->Int, val other:Entity,val rownumba:Int ):Entity{
    override var commonStuff=EntCommon()
    override fun updateEntity() {
        commonStuff.dimensions.xpos = statsXSpace + other.commonStuff.dimensions.xpos
        commonStuff.dimensions.ypos = statsYSpace*rownumba + other.commonStuff.dimensions.ypos
    }
    //    var fontDone = false
//    var font = Font("Courier", Font.BOLD,getWindowAdjustedPos(16.0).toInt())
    override fun drawEntity(g: Graphics) {
        g.color = Color.MAGENTA
        g.font = Font("Courier", Font.BOLD,getWindowAdjustedPos(18.0).toInt())
        g.drawString(showText(),getWindowAdjustedPos(commonStuff.dimensions.xpos).toInt(),getWindowAdjustedPos(commonStuff.dimensions.ypos+15).toInt())
        for(i in 1..stars()){
            g.drawImage(enemyShootImage,getWindowAdjustedPos(commonStuff.dimensions.xpos+(i*30.0)).toInt(),getWindowAdjustedPos(commonStuff.dimensions.ypos).toInt(),30,30,null)
        }

    }
}

class StatView(val showText: ()->String,val other:Entity,val rownumba:Int,val colNuma:Int ):Entity{
    override var commonStuff=EntCommon()
    override fun updateEntity() {
        commonStuff.dimensions.xpos = statsXSpace*colNuma + other.commonStuff.dimensions.xpos
        commonStuff.dimensions.ypos = statsYSpace*rownumba + other.commonStuff.dimensions.ypos
    }
//    var fontDone = false
//    var font = Font("Courier", Font.BOLD,getWindowAdjustedPos(16.0).toInt())
    override fun drawEntity(g: Graphics) {
        g.color = Color.MAGENTA
//        if(!fontDone){
//            fontDone=true
//        }
//        g.font = font
    g.font = Font("Courier", Font.BOLD,getWindowAdjustedPos(18.0).toInt())
    g.drawString(showText(),getWindowAdjustedPos(commonStuff.dimensions.xpos).toInt(),getWindowAdjustedPos(commonStuff.dimensions.ypos+15).toInt())
    }
}


