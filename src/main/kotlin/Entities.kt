import java.awt.Color
import java.awt.Graphics
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.ImageIcon
import kotlin.math.abs
import kotlin.math.atan2

open class Entity() {
    open var xpos: Double = 50.0
    open var ypos: Double = 50.0
    open var isDead: Boolean = false
    open var entityTag: String = "default"
    open var speed: Int = 2
    open var drawSize: Double = 10.0
    open var color: Color = Color.BLUE
    open fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){

    }
    open fun updateEntity() {}
    open fun drawComponents(g: Graphics) {}
    fun overlapsOther(other: Entity):Boolean{
        return this.ypos+this.drawSize > other.ypos && this.ypos<other.ypos+other.drawSize && this.xpos+this.drawSize > other.xpos && this.xpos<other.xpos+other.drawSize
    }
    fun getMidpoint():Pair<Double,Double>{
        return Pair((xpos+(drawSize/2)),ypos+(drawSize/2))
    }
    open fun drawEntity(g: Graphics) {
        g.color = color
//        g.fillRect(xpos.toInt(), ypos.toInt(), drawSize.toInt(), drawSize.toInt())
        g.fillRect(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), getWindowAdjustedSize().toInt(), getWindowAdjustedSize().toInt())
    }
    fun getWindowAdjustedSize():Double{
        return drawSize * myFrame.width/INTENDED_FRAME_SIZE
    }

}
fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}
class Bullet(val shotBy: shoots) : Entity() {
    var bulDir = shotBy.angy + ((Math.random()-0.5)*shotBy.wep.recoil/6.0)
    override var xpos =  ((shotBy as Entity).getMidpoint().first-(shotBy.wep.bulSize/2))
    override var ypos = ((shotBy as Entity).getMidpoint().second-(shotBy.wep.bulSize/2))
    override var speed = shotBy.wep.bulspd
    override var drawSize = shotBy.wep.bulSize
    override var color = shotBy.bulColor
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (((other is Player) ||  other is Enemy || other is Wall )&&shotBy != other) {
            isDead = true
        }
    }
    override fun updateEntity() {
        ypos -= ((((Math.sin(bulDir))) * speed.toDouble()))
        xpos += ((((Math.cos(bulDir))) * speed))
        if(xpos<0)isDead = true
        if(xpos > INTENDED_FRAME_SIZE - (drawSize) - (XMAXMAGIC/myFrame.width))isDead = true
        if(ypos > INTENDED_FRAME_SIZE - drawSize) isDead = true
        if(ypos<0)isDead = true
    }

    override fun drawEntity(g: Graphics) {
        g.color = color
        g.fillOval(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), (getWindowAdjustedSize()*1.2).toInt(), (getWindowAdjustedSize().toInt()))
    }
}

class Weapon(
    var atkSpd:Int = 4,
    var bulspd:Int = 2,
    var recoil:Double = 5.0,
    var bulSize:Double = 10.0,
    var buldmg:Int = 1,
    var framesSinceShottah:Int = 999
)

class Player(val buttonSet: ButtonSet): Entity(), shoots, hasHealth,movementGetsBlocked,damagedByBullets {
//    var insideGate = false

    var spawnGate:Gateway = Gateway()
    val stillImage = ImageIcon("src/main/resources/gunman.png").image
    val runImage = ImageIcon("src/main/resources/rungunman.png").image
    val pCont:playControls = playControls()
    var swapNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/swapnoise.wav").getAbsoluteFile()))
    }
    override var shootNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/longypew.wav").getAbsoluteFile()))
    }
    override var ouchNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/ouch.wav").getAbsoluteFile()))
    }
    override var deathNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/deathclip.wav").getAbsoluteFile()))
    }
    var didMove = false
    override var speed = 10
    override var drawSize = 40.0
    override var didHeal=false
    override var angy = 0.0
    override var maxHP = drawSize
    override var currentHp = maxHP
    override var bulColor = Color.LIGHT_GRAY
    override var turnSpeed = 0.1f
    var primaryEquipped = true

    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 40.0,
        buldmg = 5
    )
    var primWep = Weapon()
    override var wep = primWep

    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
//        if(other is Gateway){
//            isDead = true
//        }
    }

    override fun dieFromBullet() {
        super.dieFromBullet()
        spawnGate.playersInside.add(this)
    }

    override fun updateEntity() {
        didMove = false
        didHeal = false
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
        xpos += toMovex
        ypos += toMovey
        if(toMovex!=0.0||toMovey!=0.0)didMove = true
        stayInMap(preControl)
        if(!showingmenu){
            processTurning(pCont.spenlef.booly,pCont.spinri.booly)
            if(pCont.Swp.tryConsume()){
                playSound(swapNoise)
                if (primaryEquipped){
                    wep = spareWep
                }else{
                    wep = primWep
                }
                primaryEquipped = !primaryEquipped
            }
            processShooting(pCont.sht.booly,this.wep)
        }
    }

    override fun drawComponents(g: Graphics) {
        drawCrosshair(g)
        drawReload(g,this.wep)
        drawHealth(g)
    }

    override fun drawEntity(g: Graphics) {
        var todraw = stillImage
        if(didMove ){
            gaitcount++
            if(gaitcount < 3){
                todraw = runImage
            }else if(gaitcount>5){
                gaitcount = 0
            }
        }else{
            gaitcount = 0
        }
        g.drawImage(todraw,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedSize().toInt(),getWindowAdjustedSize().toInt(),null)
    }
    var gaitcount = 0
}
class Enemy : Entity(), shoots, hasHealth, movementGetsBlocked,damagedByBullets{
    override var shootNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/enemypew.wav").getAbsoluteFile()))
    }
    override var ouchNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/ouch.wav").getAbsoluteFile()))
    }
    override var deathNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/deathclip.wav").getAbsoluteFile()))
    }

    override var wep = Weapon(atkSpd = 20)
    override var speed = 1
    override var xpos = 150.0
    override var drawSize = 25.0
    override var angy = 0.0
    override var didHeal=false
    override var maxHP = drawSize
    override var currentHp = maxHP
    override var bulColor = Color.RED
    override var turnSpeed = 0.05f
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
    }
    override fun updateEntity() {
        didHeal = false
        val preupdatePos = Pair(xpos, ypos)
        val willgoforpack = currentHp<maxHP
        val filteredEnts = allEntities
            .filter { it is Player }
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }
        val packEnts = allEntities
            .filter {(it is MedPack)}
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }

        if(filteredEnts.isNotEmpty()){
            framesSinceDrift++
            if(!(iTried.first==xpos && iTried.second==ypos)){
                randnumx = (Math.random()-0.5)*2
                randnumy = (Math.random()-0.5)*2
                framesSinceDrift = 0
            } else{
                if(framesSinceDrift>40){
                    var xdiff = 0.0
                    var ydiff = 0.0
                    if(willgoforpack && packEnts.isNotEmpty()){
                        xdiff = packEnts.first().getMidpoint ().first - getMidpoint().first
                        ydiff = packEnts.first().getMidpoint().second - getMidpoint().second
                    }else{
                        xdiff = filteredEnts.first().getMidpoint ().first - getMidpoint().first
                        ydiff = filteredEnts.first().getMidpoint().second - getMidpoint().second
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
            stayInMap(preupdatePos)

            val dx = getMidpoint().first - filteredEnts.first().getMidpoint().first
            val dy = getMidpoint().second - filteredEnts.first().getMidpoint().second

            val radtarget = ((atan2( dy , -dx)))
            val absanglediff = abs(radtarget-angy)
            val shootem = absanglediff<0.1
            var shoot2 = shootem
            if(shootem){
                val walls = allEntities.filter { it is Wall || it is Player }
                outer@ for( i in 1..400 step 20){
                    val pointx = (xpos+(drawSize/2))+(Math.cos(angy)*i)
                    val pointy = ypos+(drawSize/2)-(Math.sin(angy)*(i))
                    if(pointx>INTENDED_FRAME_SIZE || pointx < 0 || pointy>INTENDED_FRAME_SIZE || pointy<0)
                        break@outer
                    for (wall in walls){
                        if(pointx in wall.xpos..wall.xpos+wall.drawSize){
                            if(pointy in wall.ypos..wall.ypos+wall.drawSize){
                                if(wall is Wall)shoot2 = false
                                    break@outer
                            }

                        }
                    }
                }
            }
            processShooting(shoot2,this.wep)

            val fix = absanglediff>Math.PI-turnSpeed
            var lef = radtarget>=angy
            if(fix)lef = !lef
            processTurning(lef && !shootem,!lef && !shootem)
        }
    }

    override fun drawComponents(g: Graphics) {
        drawHealth(g)
        drawCrosshair(g)
    }
}

class Wall : Entity(){
    override var drawSize = mapGridSize
    override var color = Color.DARK_GRAY
}

class Gateway : Entity(){
//    var playersInside = mutableListOf<Player>()
//    var backgate = false
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    override var drawSize = mapGridSize
    override var color = Color.PINK
    //    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
//    }
    var canEnterGate = true

    override fun updateEntity() {
        if(!canEnterGate){
            if(!overlapsOther(player1) && !overlapsOther(player2)){
                canEnterGate = true
            }
        }
        playersInside.forEach { player ->
            if(player.pCont.sht.booly){
                player.xpos = xpos
                player.ypos = ypos
                var canSpawn = true
                for(ent in allEntities.filter { it is Player || it is Enemy }){
                    if(player.overlapsOther(ent))canSpawn = false
                    if(player.xpos+player.drawSize>INTENDED_FRAME_SIZE || player.ypos+player.drawSize>INTENDED_FRAME_SIZE)canSpawn = false
                }
                if(canSpawn){
                    canEnterGate = false
                    player.isDead = false
                    player.currentHp = player.maxHP
                    entsToAdd.add(player)
                }
            }
        }
        playersInside.removeIf{!it.isDead}
    }

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if(!locked){
            if(other is Player && !other.isDead){
                if(canEnterGate){
                    other.isDead = true
                    playersInside.add(other)
                    if(playersInside.size>=NumPlayers){
                        nextMap = map
                        nextMapNum = mapnum
                        changeMap = true
                    }
                }
            }
        }
    }
}
class GateSwitch:Entity(){
    override var drawSize = mapGridSize
    override var color = Color.YELLOW
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

class MedPack : Entity() {
    override var color = Color.GREEN
    override var drawSize = 20.0
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (other is hasHealth && (other.currentHp<other.maxHP || other.didHeal)) isDead = true
    }
}

class StatView(val showText: ()->String, val xloc:Double,val yloc:Double):Entity(){
    override fun drawEntity(g: Graphics) {
          g.drawString(showText(),getWindowAdjustedPos( xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}

class Selector(val owner:Player,xloc: Double):Entity(){
    override var xpos = xloc
    override var color = Color.BLUE
    override var drawSize = 20.0
    override var ypos = selectoryspacing[0]
    var indexer = 0
    override fun updateEntity() {
        if(owner.pCont.sht.tryConsume()){
            if(indexer+1<selectoryspacing.size){
                indexer++
                ypos=selectoryspacing[indexer]
            }
        }
        if(owner.pCont.Swp.tryConsume()){
//            ypos-=vertspacing
            if(indexer-1>=0){
                indexer--
                ypos = selectoryspacing[indexer]
            }
        }

//        if(ypos<10.0) ypos = 10.0
//        if(ypos>selectoryspacing.last())ypos = selectoryspacing.last()

        if(owner.pCont.spinri.tryConsume()){
            when(indexer){
                0->{
                    owner.speed += 1
                }
                1->{
                    owner.drawSize  += 10
                    owner.maxHP +=10
                    owner.currentHp = owner.maxHP
                }
                2->{
                    val desired = "%.4f".format(owner.turnSpeed+0.01f).toFloat()
                    if(desired<1) owner.turnSpeed = desired
                }
                3->{
                    owner.primWep.buldmg+=1
                    owner.primWep.bulSize+=1
                }
                4->{
                    if(owner.primWep.bulspd+1<30)owner.primWep.bulspd++
                }
                5->{
                    if(owner.primWep.recoil+1<30)owner.primWep.recoil++
                }
                6->{
                    if(owner.primWep.atkSpd+1<200)owner.primWep.atkSpd++
                }
            }
        }else if(owner.pCont.spenlef.tryConsume()){
            when(indexer){
                0->{
                    val desiredspeed = owner.speed-1
                    if(desiredspeed>0)owner.speed = desiredspeed
                }
                1->{
                    val desiredSize = owner.drawSize -10
                    val desiredHp = owner.maxHP-10
                    if(desiredSize>MIN_ENT_SIZE && desiredHp>0){
                        owner.drawSize = desiredSize
                        owner.maxHP = desiredHp
                    }
                    owner.currentHp = owner.maxHP
                }
                2->{
                    val desired = "%.4f".format(owner.turnSpeed-0.01f).toFloat()
                    if(desired>0) owner.turnSpeed = desired
                }
                3->{
                    val desiredSize = owner.primWep.bulSize -1
                    val desiredDmg = owner.primWep.buldmg-1
                    if(desiredSize>MIN_ENT_SIZE && desiredDmg>0){
                        owner.primWep.bulSize = desiredSize
                        owner.primWep.buldmg = desiredDmg
                    }
                }
                4->{
                    if(owner.primWep.bulspd-1>1)owner.primWep.bulspd--
                }
                5->{
                    if(owner.primWep.recoil-1>=0)owner.primWep.recoil--
                }
                6->{
                    if(owner.primWep.atkSpd-1>0)owner.primWep.atkSpd--
                }
            }
        }
    }
}
const val MIN_ENT_SIZE = 9.0
