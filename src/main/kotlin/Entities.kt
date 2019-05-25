import java.awt.*
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.ImageIcon
import kotlin.math.abs
import kotlin.math.atan2
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.Rectangle

interface Entity {
    var xpos: Double
    var ypos: Double
    var isDead: Boolean
    var entityTag: String
    var speed: Int
    var drawSize: Double
    var color: Color


    fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){

    }
    fun updateEntity() {}
    fun drawComponents(g: Graphics) {}
    fun overlapsOther(other: Entity):Boolean{
        return this.ypos+this.drawSize > other.ypos &&
                this.ypos<other.ypos+other.drawSize &&
                this.xpos+this.drawSize > other.xpos &&
                this.xpos<other.xpos+other.drawSize
    }
    fun getMidY():Double{
       return ypos+(drawSize/2)
    }
    fun getMidX():Double{
        return xpos+(drawSize/2)
    }
    fun drawEntity(g: Graphics) {
        g.color = color
        g.fillRect(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), getWindowAdjustedPos(drawSize).toInt(), getWindowAdjustedPos(drawSize).toInt())
    }
}
fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}
val BULLET_ALIVE = 14
class Bullet(val shotBy: shoots) : Entity {
    var damage = shotBy.tshd.wep.buldmg
    var framesAlive = 0
    var bulDir = shotBy.tshd.angy + ((Math.random()-0.5)*shotBy.tshd.wep.recoil/6.0)
    override var drawSize = shotBy.tshd.wep.bulSize
    override var xpos =  ((shotBy as Entity).getMidX()-(shotBy.tshd.wep.bulSize/2))+(Math.cos(shotBy.tshd.angy)*shotBy.drawSize/2)+(Math.cos(shotBy.tshd.angy)*shotBy.tshd.wep.bulSize/2)
    override var ypos = ((shotBy as Entity).getMidY()-(shotBy.tshd.wep.bulSize/2))-(Math.sin(shotBy.tshd.angy)*shotBy.drawSize/2)-(Math.sin(shotBy.tshd.angy)*shotBy.tshd.wep.bulSize/2)
    override var speed = shotBy.tshd.wep.bulspd
    override var color = shotBy.tshd.bulColor

    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (((other is Player) ||  other is Enemy || other is Wall )&&shotBy != other) {
            isDead = true
            if(other is Wall){
                val imp = Impact()
                imp.drawSize = drawSize
                imp.xpos = xpos
                imp.ypos = ypos
                entsToAdd.add(imp)
            }
        }
    }
    override fun updateEntity() {
        ypos -= ((((Math.sin(bulDir))) * speed.toDouble()))
        xpos += ((((Math.cos(bulDir))) * speed))
        if(xpos<0)isDead = true
        if(xpos > INTENDED_FRAME_SIZE - (drawSize) - (XMAXMAGIC/myFrame.width))isDead = true
        if(ypos > INTENDED_FRAME_SIZE - drawSize) isDead = true
        if(ypos<0)isDead = true
        framesAlive++
        if(framesAlive>BULLET_ALIVE){
            val shrinky = shotBy.tshd.wep.bulSize/4
            damage-=shrinky.toInt()
            if(damage<0)damage=0
            drawSize-=shrinky
            xpos+=shrinky/2
            ypos+=shrinky/2

        }
        if(drawSize<=0)isDead=true
    }

    override fun drawEntity(g: Graphics) {
        g.color = color
        g.fillOval(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), (getWindowAdjustedPos(drawSize)).toInt(), (getWindowAdjustedPos(drawSize)).toInt())
    }
}

val stillImage = ImageIcon("src/main/resources/main.png").image
val runImage = ImageIcon("src/main/resources/walk.png").image
val pewImage = ImageIcon("src/main/resources/shoot1.png").image

class Player(val buttonSet: ButtonSet,val playerNumber:Int): Entity, shoots, hasHealth,demByBuls {

    override val damagedByBul = damagedByBullets()
    var canEnterGateway:Boolean = true
    var specificMenus = mutableMapOf<Char,Boolean>('b' to false, 'g' to false)
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val pCont:playControls = playControls()
    var swapNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(swapnoiseFile))
    }
    var primWep = Weapon()
    override var tshd= let {
        val s =shd()
        s.shootNoise = AudioSystem.getClip().also{
            it.open(AudioSystem.getAudioInputStream(longpewFil))
        }
        s.bulColor = Color.LIGHT_GRAY
        s.wep = primWep
        s
    }

    var movedRight = false
    var didMove = false
    var didShoot = false
    var strafeRun:Float = 0.3f
    override var speed = 10
    override var drawSize = 40.0
    override var hasHealth=healthHolder().also {
        it.maxHP=drawSize
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
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var color: Color = Color.BLUE
    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){
        if(!isDead){
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
        var preControl = Pair(xpos, ypos)
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
        xpos += toMovex
        ypos += toMovey
        if(toMovex>0)movedRight = true
        if(toMovex<0)movedRight = false
        if(toMovex!=0.0||toMovey!=0.0)didMove = true
        stayInMap(this)
        if(specificMenus.values.all { !it }){
            processTurning(this,pCont.spenlef.booly,pCont.spinri.booly)
            if(pCont.Swp.tryConsume()){
                playSound(swapNoise)
                if (primaryEquipped){
                    tshd.wep = spareWep
                }else{
                    tshd.wep = primWep
                }
                primaryEquipped = !primaryEquipped
            }
            processShooting(this,pCont.sht.booly,this.tshd.wep)
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
            g.drawImage(todraw,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
        }else{
            g.drawImage(todraw,getWindowAdjustedPos(xpos+drawSize).toInt(),getWindowAdjustedPos(ypos).toInt(),-getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
        }
    }
    var gaitcount = 0
    var pewframecount = 0
}
class Enemy : Entity, shoots, hasHealth,demByBuls{
    override var tshd=shd().also {
        it.bulColor = Color.RED
    }
    override val damagedByBul = damagedByBullets()
    override var speed = 1
    override var xpos = 150.0
    override var drawSize = 25.0
    override var hasHealth=healthHolder().also {
        it.maxHP=drawSize
        it.currentHp = it.maxHP
    }
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var color: Color = Color.BLUE
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        blockMovement(this,other,oldme,oldOther)
        takeDamage(other,this)
    }

    override fun drawEntity(g: Graphics) {
        super.drawEntity(g)
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
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }
        val packEnts = allEntities
            .filter {(it is MedPack)}
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }

        if(filteredEnts.isNotEmpty()){
            var firstplayer = filteredEnts.first()
            if(framesSinceDrift<ENEMY_DRIFT_FRAMES) framesSinceDrift++
            if(!(iTried.first==xpos && iTried.second==ypos)){
                randnumx = (Math.random()-0.5)*2
                randnumy = (Math.random()-0.5)*2
                framesSinceDrift = 0
            } else{
                if(framesSinceDrift>=ENEMY_DRIFT_FRAMES){
                    var xdiff = firstplayer.getMidX() - getMidX()
                    var ydiff = firstplayer.getMidY() - getMidY()
                    if(hasHealth.currentHp<hasHealth.maxHP && packEnts.isNotEmpty()){
                        val firstpack = packEnts.first()
                        val packxd = firstpack.getMidX() - getMidX()
                        val packyd = firstpack.getMidY() - getMidY()
                        if((Math.abs(packxd)+Math.abs(packyd))<(Math.abs(xdiff)+Math.abs(ydiff))){
                            xdiff = packxd
                            ydiff = packyd
                        }
                    }
                    if (xdiff>speed){
                        xpos += speed
                    } else if(xdiff<-speed) {
                        xpos -= speed
                    }
                    if (ydiff>speed) ypos += speed
                    else if(ydiff<-speed) ypos -= speed
                }else{
                    ypos += speed*randnumy
                    xpos += speed*randnumx
                }
            }
            iTried = Pair(xpos,ypos)
            stayInMap(this)

            val dx = getMidX() - firstplayer.getMidX()
            val dy = getMidY() - firstplayer.getMidY()

            val radtarget = ((atan2( dy , -dx)))
            val absanglediff = abs(radtarget-this.tshd.angy)
            val shootem =absanglediff<0.2
            var shoot2 = false
            if(shootem){
                val r = Rectangle((xpos).toInt(),(ypos - (tshd.wep.bulSize/(drawSize))).toInt(),tshd.wep.bulSize.toInt(),700)
                val path = Path2D.Double()
                path.append(r, false)
                val t = AffineTransform()
                t.rotate(-tshd.angy+(-Math.PI/2),(xpos+(drawSize/2)),(ypos+(drawSize/2)))
                path.transform(t)
                val intersectors = allEntities.filter {it is Wall || it is Player}.filter {  path.intersects(Rectangle(it.xpos.toInt(),it.ypos.toInt(),it.drawSize.toInt(),it.drawSize.toInt()))}.sortedBy { Math.abs(it.ypos-ypos)+Math.abs(it.xpos-xpos) }
                if(intersectors.isNotEmpty()) if (intersectors.first() is Player) shoot2 = true
            }
            processShooting(this,shoot2,this.tshd.wep)
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
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image
class Wall : Entity{
    override var drawSize = 20.0
    override var color = Color.DARK_GRAY
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
        g.drawImage(wallImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
    }
}

class Gateway : Entity{
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    override var drawSize = 20.0
    override var color = Color.PINK
    //    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
//    }
    var someoneSpawned:Entity = this
    var sumspn = false
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun drawEntity(g: Graphics) {
        if(locked)
        g.drawImage(gateClosedImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
        else g.drawImage(gateOpenImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
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
                player.xpos = xpos
                player.ypos = ypos
                var canSpawn = true
                if(locked)canSpawn = false
                else
                for(ent in allEntities.filter { it is Player || it is Enemy }){
                    if(player.overlapsOther(ent))canSpawn = false
                    if(player.xpos+player.drawSize>INTENDED_FRAME_SIZE || player.ypos+player.drawSize>INTENDED_FRAME_SIZE)canSpawn = false
                }
                if(canSpawn){
                    toremove = index
                    sumspn = true
                    someoneSpawned = player
                    player.canEnterGateway = false
                    player.isDead = false
                    entsToAdd.add(player)
                    break
                }
            }
        }
//        if(toremove in 0 until playersInside.size)
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
            if(other is Player
//                && !playersInside.map { it.playerNumber }.contains(other.playerNumber)
            ){
                if(other.canEnterGateway&&!other.isDead){
                    other.isDead = true
                    other.xpos = xpos
                    other.ypos = ypos
                    playersInside.add(other)
                }
            }
        }
    }
}
class GateSwitch:Entity{
    override var drawSize = 20.0
    override var color = Color.YELLOW
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
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
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override var drawSize: Double = 10.0
    override var color: Color = Color.BLUE
    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
        g.drawImage(wallImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
    }

    var liveFrames = 4
    override fun updateEntity() {
       liveFrames--
        if(liveFrames<0)isDead=true
    }
}

class MedPack : Entity {
    override var color = Color.GREEN
    override var drawSize = 20.0
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (other is hasHealth && (other.hasHealth.currentHp<other.hasHealth.maxHP || other.hasHealth.didHeal)) isDead = true
    }
}

class Shop:Entity{
    var char:Char = 'a'
    var menuThings:(Player)->List<Entity> ={e-> listOf()}
    override var color = Color.WHITE
    override var drawSize = 35.0
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    var image = backgroundImage
    override fun drawEntity(g: Graphics) {
        g.drawImage(image,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
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
    override var xpos = other.xpos+selectorXSpace
    override var color = Color.BLUE
    override var drawSize = 20.0
    override var ypos = other.ypos
    var indexer = 0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override fun updateEntity() {
        if(other.pCont.sht.tryConsume()){
            if(indexer+1<numStats){
                indexer++
                ypos+=statsYSpace
            }
        }
        if(other.pCont.Swp.tryConsume()){
            if(indexer-1>=0){
                indexer--
                ypos -= statsYSpace
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
    override var xpos: Double = 50.0
    override var ypos: Double = 50.0
    override var isDead: Boolean = false
    override var entityTag: String = "default"
    override var speed: Int = 2
    override var drawSize: Double = 10.0
    override var color: Color = Color.BLUE
    override fun drawEntity(g: Graphics) {
        g.color = Color.BLUE
        g.font = g.font.deriveFont((myFrame.width/70).toFloat())
        g.drawString(showText(),getWindowAdjustedPos(xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}

const val MIN_ENT_SIZE = 9.0
