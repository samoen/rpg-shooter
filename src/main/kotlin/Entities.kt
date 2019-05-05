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
        if (!(other is MedPack) && shotBy != other && !(other is Bullet)) {
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
        g.fillOval(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), getWindowAdjustedSize().toInt(), getWindowAdjustedSize().toInt())
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
    override var speed = 10
    override var drawSize = 40.0
//    override var color = Color.BLACK
    override var angy = 0.0
    override val maxHP = 30
    override var currentHp = maxHP
    override var bulColor = Color.LIGHT_GRAY
    override var turnSpeed = 0.1

    var primaryEquipped = true
    var spareWep:Weapon = Weapon(
        atkSpd = 60,
        bulspd = 15,
        recoil = 0.0,
        bulSize = 40.0,
        buldmg = 5
    )
    var primWep = Weapon()

    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
        if(other is MedPack){
            currentHp+=5
            if (currentHp>maxHP)currentHp = maxHP
        }
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
    override var bulColor = Color.RED
    override var turnSpeed = 0.05
    var framesSinceDrift = 100
    var randnumx = 0.0
    var randnumy = 0.0
    var iTried = Pair(-1.0,-1.0)

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        blockMovement(other,oldme,oldOther)
        takeDamage(other)
    }
    override fun updateEntity() {
        var preupdatePos = Pair(xpos, ypos)
        val filteredEnts = allEntities
            .filter { it is Player}
            .sortedBy { abs(it.xpos - xpos) + abs(it.ypos - ypos) }

            if(filteredEnts.isNotEmpty()){
                framesSinceDrift++
                if(!(iTried.first==xpos && iTried.second==ypos)){
                    randnumx = (Math.random()-0.5)*2
                    randnumy = (Math.random()-0.5)*2
                    framesSinceDrift = 0
                } else{
                    if(framesSinceDrift>40){
                        val xdiff = filteredEnts.first().getMidpoint ().first - getMidpoint().first
                        if (xdiff>speed){
                            xpos += speed
                        } else if(xdiff<-speed) {
                            xpos -= speed
                        }
                        val ydiff = filteredEnts.first().getMidpoint().second - getMidpoint().second
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

                var radtarget = ((atan2( dy.toDouble() , -dx.toDouble())))
                val absanglediff = abs(radtarget-angy)
                var shootem = absanglediff<0.1
                var shoot2 = shootem
                if(shootem){
//                    var vully = Bullet(this)
//                    var buls = mutableListOf<Bullet>()
//                    for( i in 0..200.toInt() step 10){
//                        var bbq = Bullet(this)
//                        for(j in 0..i)bbq.updateEntity()
//                        buls.add(bbq)
////                        vully.updateEntity()
//
//                    }
//                    var vul2 = Bullet(this)
//                    for( i in 0..(3*drawSize/wep.bulspd).toInt()){
//                        vul2.updateEntity()
//                    }
//                    var vul3 = Bullet(this)
//                    for( i in 0..100/wep.bulspd.toInt()){
//                        vul3.updateEntity()
//                    }
//                    vully.updateEntity()
//                    vully.updateEntity()


//                    val enites = allEntities.filter { it is Player || it is Wall || (it is Enemy && it!=this) }
//
//                        outer@ for (entie in enites) {
////                        if(it is Player)break
//                        for(ent in buls){
//                            if(ent.overlapsOther(entie)){
//                                if(entie is Player){
//                                    break@outer
//                                }else
//                                shootem = false
//                            }
//                        }
////                        if(it.overlapsOther(vully)||it.overlapsOther(vul2)||it.overlapsOther(vul3)){
////                            shootem = false
////                        }
//                    }

                    val walls = allEntities.filter { it is Wall || it is Player }
                    outer@ for( i in 1..300 step 20){
                        var pointx = xpos+(Math.cos(angy)*i)
                        var pointy = ypos-(Math.sin(angy)*(i))
                        for (wall in walls){
                            if(pointx in wall.xpos..wall.xpos+wall.drawSize){
                                if(pointy in wall.ypos..wall.ypos+wall.drawSize){
                                    if(wall is Wall)shoot2 = false
                                        break@outer
                                }

                            }
                        }
                    }
//                    if(map1[locToIndex(pointx,pointy)-1]=='1'){
//                        shootem = false
//                    }
//                    allEntities.forEach {
//                        if(it is Wall){
//                            if(pointx in it.xpos..it.xpos+it.drawSize)
//                                if(pointy in it.ypos..it.ypos+it.drawSize)
//                                    shootem = false
//                        }
//                    }
                }
                processShooting(shoot2,this.wep)

                var fix = absanglediff>Math.PI-turnSpeed
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
    override var ypos = 0.0
    override var xpos = 0.0
}

class MedPack : Entity() {
    override var color = Color.GREEN
    override var drawSize = 20.0
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (other is Player) isDead = true
    }
}

class StatView(val showText: ()->String, val xloc:Double,val yloc:Double):Entity(){
    override fun drawEntity(g: Graphics) {
          g.drawString(showText(),getWindowAdjustedPos( xloc).toInt(),getWindowAdjustedPos(yloc).toInt())
    }
}

class Selector(val pnum:Int,val owner:Player,val xloc: Double,val numstats:Int):Entity(){
//    var statup : (Entity)->Unit = {}
//    var statdown : (Entity)->Unit = {}
//    val vertspacing = 40.0
//    val maxvert = vertspacing*numstats

    override var xpos = xloc
    override var color = Color.BLUE
    override var drawSize = 20.0
    override var ypos = selectoryspacing[0]-this.drawSize/2
    var indexer = 0
    override fun updateEntity() {
        if(owner.pCont.dwm.tryConsume()){
            if(indexer+1<selectoryspacing.size){
                indexer++
                ypos=selectoryspacing[indexer]-this.drawSize/2
            }
        }
        if(owner.pCont.up.tryConsume()){
//            ypos-=vertspacing
            if(indexer-1>=0){
                indexer--
                ypos = selectoryspacing[indexer]-this.drawSize/2
            }
        }

//        if(ypos<10.0) ypos = 10.0
//        if(ypos>selectoryspacing.last())ypos = selectoryspacing.last()

        if(owner.pCont.sht.tryConsume()){
//            for(i:Int in 0..numstats){
//                if(ypos == i*vertspacing){
//                    owner.speed+=1
//                }
//            }
            when(indexer){
                0->{
                    owner.speed += 1
                }
                1->{
                    owner.drawSize += 1
                }
                2->{
                    owner.turnSpeed +=0.05
                }
            }
        }else if(owner.pCont.Swp.tryConsume()){
            when(indexer){
                0->{
                    owner.speed -=1
                    if(owner.speed<0)owner.speed = 0
                }
                1->{
                    owner.drawSize -=1
                    if(owner.drawSize<10.0)owner.drawSize = 10.0
                }
                2->{
                    owner.turnSpeed -=0.05
                }
            }
        }
    }
}
