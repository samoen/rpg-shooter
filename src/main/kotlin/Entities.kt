import java.awt.*
import javax.swing.ImageIcon
import kotlin.math.abs
import kotlin.math.atan2
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.Rectangle


class Bullet(val shotBy: shoots) : Entity {
    override var dimensions = EntDimens(
        ((shotBy as Entity).getMidX()-(shotBy.tshd.wep.bulSize/2))+(Math.cos(shotBy.tshd.angy)*shotBy.dimensions.drawSize/2)+(Math.cos(shotBy.tshd.angy)*shotBy.tshd.wep.bulSize/2),
        ((shotBy as Entity).getMidY()-(shotBy.tshd.wep.bulSize/2))-(Math.sin(shotBy.tshd.angy)*shotBy.dimensions.drawSize/2)-(Math.sin(shotBy.tshd.angy)*shotBy.tshd.wep.bulSize/2),
        shotBy.tshd.wep.bulSize
    )
    var bulImage = wallImage
    var damage = shotBy.tshd.wep.buldmg
    var framesAlive = 0
    var bulDir = shotBy.tshd.angy + ((Math.random()-0.5)*shotBy.tshd.wep.recoil/6.0)
    override var speed = shotBy.tshd.wep.bulspd
    override var color = shotBy.tshd.bulColor
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (((other is Player) ||  other is Enemy || other is Wall )&&shotBy != other) {
            toBeRemoved = true
            if(other is Wall){
                val imp = Impact()
                imp.dimensions.drawSize = dimensions.drawSize
                imp.dimensions.xpos = dimensions.xpos
                imp.dimensions.ypos = dimensions.ypos
                entsToAdd.add(imp)
            }
        }
    }
    override fun updateEntity() {
        dimensions.ypos -= ((((Math.sin(bulDir))) * speed.toDouble()))
        dimensions.xpos += ((((Math.cos(bulDir))) * speed))
        if(dimensions.xpos<0)toBeRemoved = true
        if(dimensions.xpos > INTENDED_FRAME_SIZE - (dimensions.drawSize) - (XMAXMAGIC/myFrame.width))toBeRemoved = true
        if(dimensions.ypos > INTENDED_FRAME_SIZE - dimensions.drawSize) toBeRemoved = true
        if(dimensions.ypos<0)toBeRemoved = true
        framesAlive++
        if(framesAlive>BULLET_ALIVE){
            val shrinky = shotBy.tshd.wep.bulSize/4
            damage-=shrinky.toInt()
            if(damage<0)damage=0
            dimensions.drawSize-=shrinky
            dimensions.xpos+=shrinky/2
            dimensions.ypos+=shrinky/2

        }
        if(dimensions.drawSize<=0)toBeRemoved=true
    }

    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,bulImage,g)
    }
}



class Player(val buttonSet: ButtonSet,val playerNumber:Int): Entity, shoots, hasHealth {
    override var dimensions = EntDimens(0.0,0.0,40.0)
    override val damagedByBul = damagedByBullets()
    var canEnterGateway:Boolean = true
    var specificMenus = mutableMapOf<Char,Boolean>('b' to false, 'g' to false)
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val pCont:playControls = playControls()
    var primWep = Weapon()
    override var tshd= let {
        val s =shd()
        s.turnSpeed = 0.1f
        s.shootySound = "shoot"
        s.bulColor = Color.LIGHT_GRAY
        s.wep = primWep
        s
    }

    var movedRight = false
    var didMove = false
    var didShoot = false
    var strafeRun:Float = 0.3f
    override var speed = 10
    override var hasHealth=healthHolder().also {
        it.maxHP=dimensions.drawSize
        it.currentHp = it.maxHP
    }
    var primaryEquipped = true

    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 12.0,
        buldmg = 4
    )

//    override var wep = primWep
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var color: Color = Color.BLUE
    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){
        if(!toBeRemoved){
            blockMovement(this,other,oldme,oldOther)
            val died = takeDamage(other,this)
            if(died){
                hasHealth.currentHp = hasHealth.maxHP
                for (specificMenu in specificMenus) {
                    specificMenu.setValue(false)
                }
                spawnGate.playersInside.add(this)
            }
        }
    }

    override fun updateEntity() {
        didMove = false
        hasHealth.didHeal = false
//        var preControl = Pair(xpos, ypos)
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
        if(tshd.wep.framesSinceShottah<tshd.wep.atkSpd){
            toMovex *= strafeRun
            toMovey *= strafeRun
        }
        dimensions.xpos += toMovex
        dimensions.ypos += toMovey
        if(toMovex>0)movedRight = true
        if(toMovex<0)movedRight = false
        if(toMovex!=0.0||toMovey!=0.0)didMove = true
        stayInMap(this)
        if(specificMenus.values.all { !it }){
            processTurning(this,pCont.spenlef.booly,pCont.spinri.booly)
            if(pCont.Swp.tryConsume()){
                playStrSound("swap")
                if (primaryEquipped){
                    tshd.wep = spareWep
                }else{
                    tshd.wep = primWep
                }
                primaryEquipped = !primaryEquipped
            }
            processShooting(this,pCont.sht.booly,this.tshd.wep,pBulImage)
        }
    }

    override fun drawComponents(g: Graphics) {
        drawCrosshair(this,g)
        drawReload(this,g,this.tshd.wep)
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
        }
        if (damagedByBul.didGetShot) {
            if(damagedByBul.gotShotFrames>0) {
                todraw = backgroundImage
                damagedByBul.gotShotFrames--
            } else {
                damagedByBul.didGetShot = false
            }
        }
        if(tshd.angy>Math.PI/2 || tshd.angy<-Math.PI/2){
            drawAsSprite(this,todraw,g)
        }else{
            g.drawImage(todraw,getWindowAdjustedPos(dimensions.xpos+dimensions.drawSize).toInt(),getWindowAdjustedPos(dimensions.ypos).toInt(),-getWindowAdjustedPos(dimensions.drawSize).toInt(),getWindowAdjustedPos(dimensions.drawSize).toInt(),null)
        }
    }
    var gaitcount = 0
    var pewframecount = 0
}
class Enemy : Entity, shoots, hasHealth{
    override var dimensions = EntDimens(0.0,0.0,25.0)
    override var tshd=shd().also {
        it.bulColor = Color.RED
        it.shootySound = "laser"
    }
    override val damagedByBul = damagedByBullets()
    override var speed = 1
    override var hasHealth=healthHolder().also {
        it.maxHP=dimensions.drawSize
        it.currentHp = it.maxHP
    }
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var color: Color = Color.BLUE
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        blockMovement(this,other,oldme,oldOther)
        takeDamage(other,this)
    }

    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,goblinImage,g)
//        val r = Rectangle((xpos).toInt(),(ypos - (tshd.wep.bulSize/(drawSize))).toInt(),tshd.wep.bulSize.toInt(),700)
//        val path = Path2D.Double()
//        path.append(r, false)
//        val t = AffineTransform()
//        t.rotate(-tshd.angy+(-Math.PI/2),(xpos+(drawSize/2)),(ypos+(drawSize/2)))
//        path.transform(t)
//        (g as Graphics2D).draw(path)
    }

    override fun updateEntity() {
        if (damagedByBul.didGetShot) {
            if(damagedByBul.gotShotFrames>0) {
                color = Color.ORANGE
                damagedByBul.gotShotFrames--
            } else {
                color = Color.BLUE
                damagedByBul.didGetShot = false
            }
        }
        hasHealth.didHeal = false
        val filteredEnts = allEntities
            .filter { it is Player }
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
                if(framesSinceDrift>=ENEMY_DRIFT_FRAMES){
                    var xdiff = 0.0
                    var ydiff = 0.0
                    if(hasHealth.currentHp<hasHealth.maxHP/3 && packEnts.isNotEmpty()){
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
                    if (xdiff>speed){
                        dimensions.xpos += speed
                    } else if(xdiff<-speed) {
                        dimensions.xpos -= speed
                    }
                    if (ydiff>speed) dimensions.ypos += speed
                    else if(ydiff<-speed) dimensions.ypos -= speed
                }else{
                    dimensions.ypos += speed*randnumy
                    dimensions.xpos += speed*randnumx
                }
            }
            iTried = Pair(dimensions.xpos,dimensions.ypos)
            stayInMap(this)

            val dx = getMidX() - firstplayer.getMidX()
            val dy = getMidY() - firstplayer.getMidY()

            val radtarget = ((atan2( dy , -dx)))
            val absanglediff = abs(radtarget-this.tshd.angy)
            val shootem =absanglediff<0.2
            var shoot2 = false
            if(shootem){
                val r = Rectangle((dimensions.xpos).toInt(),(dimensions.ypos - (tshd.wep.bulSize/(dimensions.drawSize))).toInt(),tshd.wep.bulSize.toInt(),700)
                val path = Path2D.Double()
                path.append(r, false)
                val t = AffineTransform()
                t.rotate(-tshd.angy+(-Math.PI/2),(dimensions.xpos+(dimensions.drawSize/2)),(dimensions.ypos+(dimensions.drawSize/2)))
                path.transform(t)
                val intersectors = allEntities.filter {it is Wall || it is Player}.filter {  path.intersects(Rectangle(it.dimensions.xpos.toInt(),it.dimensions.ypos.toInt(),it.dimensions.drawSize.toInt(),it.dimensions.drawSize.toInt()))}.sortedBy { Math.abs(it.dimensions.ypos-dimensions.ypos)+Math.abs(it.dimensions.xpos-dimensions.xpos) }
                if(intersectors.isNotEmpty()) if (intersectors.first() is Player) shoot2 = true
            }
            processShooting(this,shoot2,this.tshd.wep,eBulImage)
            val fix = absanglediff>Math.PI-tshd.turnSpeed
            var lef = radtarget>=tshd.angy
            if(fix)lef = !lef
            processTurning(this,lef && !shootem,!lef && !shootem)
        }
    }

    override fun drawComponents(g: Graphics) {
        drawHealth(this,g)
        drawCrosshair(this,g)
    }
}
val ENEMY_DRIFT_FRAMES = 30
val wallImage = ImageIcon("src/main/resources/brick1.png").image
val impactImage = ImageIcon("src/main/resources/shrapnel.png").image
val pBulImage = ImageIcon("src/main/resources/plasma.png").image
val eBulImage = ImageIcon("src/main/resources/badbullet.png").image
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image
class Wall : Entity{
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var color = Color.DARK_GRAY
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,wallImage,g)
    }
}

class Gateway : Entity{
    override var dimensions = EntDimens(0.0,0.0,20.0)
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    override var color = Color.PINK
    var someoneSpawned:Entity = this
    var sumspn = false
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
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
    }

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if(!locked){
            if(other is Player){
                if(other.canEnterGateway&&!other.toBeRemoved){
                    other.toBeRemoved = true
                    other.dimensions.xpos = dimensions.xpos
                    other.dimensions.ypos = dimensions.ypos
                    playersInside.add(other)
                }
            }
        }
    }
}
class GateSwitch:Entity{
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var color = Color.YELLOW
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if(other is Player){
         allEntities.filter { it is Gateway }.forEach {
             (it as Gateway).locked = false
             it.color = Color.BLACK
             color = Color.ORANGE
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
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override var color: Color = Color.BLUE
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
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var color = Color.GREEN
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (other is hasHealth && (other.hasHealth.currentHp<other.hasHealth.maxHP || other.hasHealth.didHeal)) toBeRemoved = true
    }
}

class Shop:Entity{
    override var dimensions = EntDimens(0.0,0.0,20.0)
    var char:Char = 'a'
    var menuThings:(Player)->List<Entity> ={e-> listOf()}
    override var color = Color.WHITE
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    var image = backgroundImage
    override fun drawEntity(g: Graphics) {
        drawAsSprite(this,image,g)
    }

    override fun updateEntity() {
        if(player0.specificMenus[char]!!){
            if(!overlapsOther(player0)){
                player0.specificMenus[char] = false
            }
        }
        if(player1.specificMenus[char]!!){
            if(!overlapsOther(player1)){
                player1.specificMenus[char]=false
            }
        }

    }

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if(other is Player){
            if(other.specificMenus[char]==false){
                other.menuStuff = menuThings(other)
                other.specificMenus[char] = true
            }
        }
    }
}
class Selector(val numStats:Int,val other:Player,val onUp:()->Unit,val onDown:()->Unit,val onUp1:()->Unit,val onDown1:()->Unit,val onUp2:()->Unit,val onDown2:()->Unit,val onUp3:()->Unit={},val onDown3:()->Unit={}):Entity{
    override var dimensions = EntDimens(other.dimensions.xpos+selectorXSpace,other.dimensions.ypos,20.0)
    override var color = Color.BLUE
    var indexer = 0
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
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
    override var dimensions = EntDimens(0.0,0.0,20.0)
    override var toBeRemoved: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override var color: Color = Color.BLUE
    override fun drawEntity(g: Graphics) {
        g.color = Color.BLUE
        g.font = g.font.deriveFont((myFrame.width/70).toFloat())
        g.drawString(showText(),getWindowAdjustedPos(xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}


