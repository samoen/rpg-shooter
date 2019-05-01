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
    open fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens):Boolean {
        return true
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
        g.fillRect(xpos.toInt(), ypos.toInt(), drawSize.toInt(), drawSize.toInt())
    }
}

class Bullet(val shotBy: shoots) : Entity() {
    var bulDir = shotBy.angy + ((Math.random()-0.5)*shotBy.wep.recoil/6.0)
    override var xpos =  ((shotBy as Entity).getMidpoint().first-(shotBy.wep.bulSize/2))
    override var ypos = ((shotBy as Entity).getMidpoint().second-(shotBy.wep.bulSize/2))
    override var speed = shotBy.wep.bulspd
    override var drawSize = shotBy.wep.bulSize
    override var color = shotBy.bulColor
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens):Boolean{
        if (!(other is MedPack) && shotBy != other && !(other is Bullet)) {
            isDead = true
        }
        return true
    }
    override fun updateEntity() {
        ypos -= ((((Math.sin(bulDir))) * speed.toDouble()))
        xpos += ((((Math.cos(bulDir))) * speed))
        if (xpos > FRAME_SIZE - drawSize || 0 > xpos || ypos > FRAME_SIZE - drawSize || 0 > xpos) {
            isDead = true
        }
    }

    override fun drawEntity(g: Graphics) {
        g.color = color
        g.fillOval(xpos.toInt(), ypos.toInt(), drawSize.toInt(), drawSize.toInt())
    }
}

class Weapon(var atkSpd:Int = 4,
        var bulspd:Int = 2,
        var recoil:Double = 5.0,
        var bulSize:Double = 10.0,
        var buldmg:Int = 1,
             var framesSinceShottah:Int = 99)

class Player(val buttonSet: ButtonSet): Entity(), shoots, hasHealth,movementGetsBlocked,damagedByBullets {
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
    override var wep = Weapon()
    override var speed = 8
    override var drawSize = 40.0
    override var color = Color.GRAY
    override var angy = 0.0
    override val maxHP = 30
    override var currentHp = maxHP
    override var bulColor = Color.PINK
    override var turnSpeed = 0.2

    var primaryEquipped = true
    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 40.0,
        buldmg = 5
    )
    var primWep = Weapon()

    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens):Boolean{
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
        if(other is MedPack){
            currentHp+=5
            if (currentHp>maxHP)currentHp = maxHP
        }
        return !doIGetBlockedBy(other)
    }

    override fun updateEntity() {
        didMove = false
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
        processTurning(pCont.spenlef,pCont.spinri)
        if(pCont.Swp.booly){
            playSound(swapNoise)
            pCont.Swp.lockDown()
//            pCont.Swp.booly = false
//            pCont.Swp.locked = true
            if (primaryEquipped){
                wep = spareWep
            }else{
                wep = primWep
            }
            primaryEquipped = !primaryEquipped
        }
        processShooting(pCont.sht.booly,this.wep)
    }

    override fun drawComponents(g: Graphics) {
        drawCrosshair(g,false)
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
        g.drawImage(todraw,xpos.toInt(),ypos.toInt(),drawSize.toInt(),drawSize.toInt(),null)
    }
    var gaitcount = 0
}
class Enemy : Entity(), shoots, hasHealth, movementGetsBlocked,damagedByBullets{
    override var shootNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/pewnew.wav").getAbsoluteFile()))
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
    override val maxHP = 10
    override var currentHp = maxHP
    override var bulColor = Color.BLUE
    override var turnSpeed = 0.05
    var framesSinceDrift = 100
    var randnumx = 0
    var randnumy = 0
    var iTried = Pair(-1.0,-1.0)

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens):Boolean{
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
        return !doIGetBlockedBy(other)
    }
    override fun updateEntity() {
        var preupdatePos = Pair(xpos, ypos)
        val filteredEnts = allEntities
            .filter { it is Player}
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }

            if(filteredEnts.isNotEmpty()){
                framesSinceDrift++
                if(!(iTried.first==xpos && iTried.second==ypos)){
                    randnumx = (Math.random() * 10).toInt()
                    randnumy = (Math.random() * 10).toInt()
                    framesSinceDrift = 0
                } else{
                    if(framesSinceDrift>20){
                        if (filteredEnts.first().getMidpoint ().first > getMidpoint().first) xpos += speed
                        else xpos -= speed
                        if (filteredEnts.first().getMidpoint().second > getMidpoint().second) ypos += speed
                        else ypos -= speed
                    }else{
                        ypos += ((randnumy - 5))*speed/3
                        xpos += (randnumx - 5)*speed/3
                    }
                }
                iTried = Pair(xpos,ypos)
                stayInMap(preupdatePos)

                val dx = getMidpoint().first - filteredEnts.first().getMidpoint().first
                val dy = getMidpoint().second - filteredEnts.first().getMidpoint().second
                var radtarget = ((atan2( dy.toDouble() , -dx.toDouble())))
                var shootem = abs(radtarget-angy)<0.1

                processShooting(shootem,this.wep)
                processTurning(radtarget>angy && !shootem,radtarget<angy && !shootem)
            }
    }

    override fun drawComponents(g: Graphics) {
        drawHealth(g)
        drawCrosshair(g,false)
    }
}

class Wall : Entity(){
    override var drawSize = 60.0
    override var color = Color.RED
    override var ypos = 200.0
    override var xpos = 200.0
}

class MedPack : Entity() {
    override var color = Color.GREEN
    override var drawSize = 20.0
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens):Boolean{
        if (other is Player) isDead = true
        return true
    }
}

class StatView(val showText: ()->String, val xloc:Double,val yloc:Double):Entity(){
    override fun drawEntity(g: Graphics) {
          g.drawString(showText(),xloc.toInt(),yloc.toInt())
    }
}

class Selector(val pnum:Int,val owner:Player,val xloc: Double):Entity(){
    var statup : (Entity)->Unit = {}
    var statdown : (Entity)->Unit = {}
    override var ypos = 0.0
    override var xpos = xloc
    override var color = Color.BLUE
    override var drawSize = 20.0
    override fun updateEntity() {
        if(owner.pCont.dwm.booly){
            owner.pCont.dwm.lockDown()
            ypos+=30
        }
        if(owner.pCont.up.booly){
            owner.pCont.up.lockDown()
            ypos-=30
        }
        if(ypos<0) ypos = 0.0
        if(ypos>30.0)ypos = 30.0

        if(owner.pCont.sht.booly){
            owner.pCont.sht.lockDown()
            when(ypos){
                0.0->{
                    owner.speed += 1
                }
                30.0->{
                    owner.drawSize += 1
                }
            }
        }else if(owner.pCont.Swp.booly){
            owner.pCont.Swp.lockDown()
            when(ypos){
                0.0->{
                    owner.speed -=1
                    if(owner.speed<0)owner.speed = 0
                }
                30.0->{
                    owner.drawSize -=1
                    if(owner.drawSize<10.0)owner.drawSize = 10.0
                }
            }
        }
    }
}
