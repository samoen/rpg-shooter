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
        return this.ypos+this.drawSize > other.ypos &&
                this.ypos<other.ypos+other.drawSize &&
                this.xpos+this.drawSize > other.xpos &&
                this.xpos<other.xpos+other.drawSize
    }
    fun getMidpoint():Pair<Double,Double>{
        return Pair((xpos+(drawSize/2)),ypos+(drawSize/2))
    }
    open fun drawEntity(g: Graphics) {
        g.color = color
        g.fillRect(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), getWindowAdjustedPos(drawSize).toInt(), getWindowAdjustedPos(drawSize).toInt())
    }
}
fun getWindowAdjustedPos(pos:Double):Double{
    return pos * myFrame.width/INTENDED_FRAME_SIZE
}
class Bullet(val shotBy: shoots) : Entity() {
    var bulDir = shotBy.angy + ((Math.random()-0.5)*shotBy.wep.recoil/6.0)
    override var drawSize = shotBy.wep.bulSize
    override var xpos =  ((shotBy as Entity).getMidpoint().first-(shotBy.wep.bulSize/2))+(Math.cos(shotBy.angy)*0.8*shotBy.drawSize)
    override var ypos = ((shotBy as Entity).getMidpoint().second-(shotBy.wep.bulSize/2))-(Math.sin(shotBy.angy)*0.8*shotBy.drawSize)
    override var speed = shotBy.wep.bulspd
    override var color = shotBy.bulColor
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (((other is Player) ||  other is Enemy || other is Wall )&&shotBy != other) {
            isDead = true
            val imp = Impact()
            imp.drawSize = drawSize
            imp.xpos = xpos
            imp.ypos = ypos
            entsToAdd.add(imp)
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
        g.fillOval(getWindowAdjustedPos(xpos).toInt(), getWindowAdjustedPos(ypos).toInt(), (getWindowAdjustedPos(drawSize)*1.3).toInt(), (getWindowAdjustedPos(drawSize)*1.3).toInt())
    }
}

class Weapon(
    var atkSpd:Int = 4,
    var bulspd:Int = 2,
    var recoil:Double = 5.0,
    var bulSize:Double = 6.0,
    var buldmg:Int = 1,
    var framesSinceShottah:Int = 999
)

class Player(val buttonSet: ButtonSet,val playerNumber:Int): Entity(), shoots, hasHealth,movementGetsBlocked,damagedByBullets {
    var canEnterGateway:Boolean = true
    var isInsideGate:Boolean = true
    var menushowign = false
    var specificMenus = mutableMapOf<Char,Boolean>('b' to false, 'g' to false)
    var menuStuff:List<Entity> = listOf()
    var spawnGate:Gateway = Gateway()
    val stillImage = ImageIcon("src/main/resources/main.png").image
    val runImage = ImageIcon("src/main/resources/walk.png").image
    val pewImage = ImageIcon("src/main/resources/shoot1.png").image
    val pCont:playControls = playControls()
    var swapNoise:Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/swapnoise.wav").getAbsoluteFile()))
    }
    override var shootNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/newlongpew.wav").getAbsoluteFile()))
    }
    override var ouchNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/ouch.wav").getAbsoluteFile()))
    }
    override var deathNoise: Clip = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(File("src/main/resources/deathclip.wav").getAbsoluteFile()))
    }
    var movedRight = false
    var didMove = false
    var didShoot = false
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
        bulSize = 12.0,
        buldmg = 3
    )
    var primWep = Weapon()
    override var wep = primWep

    override fun collide(other: Entity, oldme: EntDimens, oldOther:EntDimens){
        if(!isDead){
            blockMovement(other,oldme,oldOther)
            takeDamage(other)
        }
    }

    override fun dieFromBullet() {
        super.dieFromBullet()
        currentHp = maxHP
        menushowign = false
        for (specificMenu in specificMenus) {
            specificMenu.setValue(false)
        }
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
        if(toMovex>0)movedRight = true
        if(toMovex<0)movedRight = false
        if(toMovex!=0.0||toMovey!=0.0)didMove = true
        stayInMap(preControl)
        if(!menushowign && specificMenus.values.all { !it }){
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
        if(angy>Math.PI/2 || angy<-Math.PI/2){
            g.drawImage(todraw,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
        }else{
            g.drawImage(todraw,getWindowAdjustedPos(xpos+drawSize).toInt(),getWindowAdjustedPos(ypos).toInt(),-getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
        }
    }
    var gaitcount = 0
    var pewframecount = 0
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

    override fun drawEntity(g: Graphics) {
        super.drawEntity(g)
//        val r = Rectangle((xpos).toInt(),(ypos - (wep.bulSize/(drawSize))).toInt(),wep.bulSize.toInt(),700)
//        val path = Path2D.Double()
//        path.append(r, false)
//        val t = AffineTransform()
//        t.rotate(-angy+(-Math.PI/2),(xpos+(drawSize/2)),(ypos+(drawSize/2)))
//        path.transform(t)
//        (g as Graphics2D).draw(path)
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
            val shootem =absanglediff<0.1
            var shoot2 = false
            if(shootem){
//                val walls = allEntities.filter { it is Wall || it is Player }
//                outer@ for( i in 1..400 step 20){
//                    val pointx = (xpos+(drawSize/2))+(Math.cos(angy)*i)
//                    val pointy = ypos+(drawSize/2)-(Math.sin(angy)*(i))
//                    if(pointx>INTENDED_FRAME_SIZE || pointx < 0 || pointy>INTENDED_FRAME_SIZE || pointy<0)
//                        break@outer
//                    for (wall in walls){
//                        if(pointx in wall.xpos..wall.xpos+wall.drawSize){
//                            if(pointy in wall.ypos..wall.ypos+wall.drawSize){
//                                if(wall is Wall)shoot2 = false
//                                    break@outer
//                            }
//
//                        }
//                    }
//                }
                val r = Rectangle((xpos).toInt(),(ypos - (wep.bulSize/(drawSize))).toInt(),wep.bulSize.toInt(),700)
                val path = Path2D.Double()
                path.append(r, false)
                val t = AffineTransform()
                t.rotate(-angy+(-Math.PI/2),(xpos+(drawSize/2)),(ypos+(drawSize/2)))
                path.transform(t)
                val intersectors = allEntities.filter {it is Wall || it is Player}.filter {  path.intersects(Rectangle(it.xpos.toInt(),it.ypos.toInt(),it.drawSize.toInt(),it.drawSize.toInt()))}.sortedBy { Math.abs(it.ypos-ypos)+Math.abs(it.xpos-xpos) }
                if(intersectors.isNotEmpty()) if (intersectors.first() is Player) shoot2 = true
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
val wallImage = ImageIcon("src/main/resources/brick1.png").image
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image
class Wall : Entity(){
    override var drawSize = 20.0
    override var color = Color.DARK_GRAY
    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
        g.drawImage(wallImage,getWindowAdjustedPos(xpos).toInt(),getWindowAdjustedPos(ypos).toInt(),getWindowAdjustedPos(drawSize).toInt(),getWindowAdjustedPos(drawSize).toInt(),null)
    }
}

class Gateway : Entity(){
    var playersInside = mutableListOf<Player>()
    var map = map1
    var mapnum = 1
    var locked = true
    override var drawSize = 20.0
    override var color = Color.PINK
    //    override fun drawEntity(g: Graphics) {
//        super.drawEntity(g)
//    }
    var someoneSpawned:Entity = Entity()
    var sumspn = false
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
class GateSwitch:Entity(){
    override var drawSize = 20.0
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

class Impact : Entity(){
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

class MedPack : Entity() {
    override var color = Color.GREEN
    override var drawSize = 20.0
    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if (other is hasHealth && (other.currentHp<other.maxHP || other.didHeal)) isDead = true
    }
}

class BlackSmith(val char:Char):Entity(){
    override var color = Color.CYAN
    override var drawSize = 35.0
    override fun updateEntity() {
        if(player0.menushowign){
            if(!overlapsOther(player0)){
                player0.menushowign = false
            }
        }
        if(player1.menushowign){
            if(!overlapsOther(player1)){
                player1.menushowign = false
            }
        }
    }

    override fun collide(other: Entity, oldme: EntDimens, oldOther: EntDimens){
        if(other is Player){
            if(!other.menushowign){
                other.menuStuff = listOf(
                    StatView({"Dmg"},other.xpos,0+other.ypos),
                    StatView({"Vel"},other.xpos,statsYSpace+other.ypos),
                    StatView({"Rec"},other.xpos,statsYSpace*2+other.ypos),
                    StatView({"Rel"},other.xpos,statsYSpace*3+other.ypos),
                    object:Entity(){
                        override var xpos = other.xpos+selectorXSpace
                        override var color = Color.BLUE
                        override var drawSize = 20.0
                        override var ypos = other.ypos
                        var indexer = 0
                        override fun updateEntity() {
                            if(other.pCont.sht.tryConsume()){
                                if(indexer+1<4){
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
                                    0->{
                                        other.wep.buldmg+=1
                                        other.wep.bulSize+=3
                                    }
                                    1->{
                                        if(other.wep.bulspd+1<50)other.wep.bulspd++
                                    }
                                    2->{
                                        if(other.wep.recoil+1<30)other.wep.recoil++
                                    }
                                    3->{
                                        if(other.wep.atkSpd+1<200)other.wep.atkSpd++
                                    }
                                }
                            }else if(other.pCont.spenlef.tryConsume()){
                                when(indexer){
                                    0->{
                                        val desiredDmg = other.wep.buldmg-1
                                        val desiredSize = other.wep.bulSize -3
                                        if(desiredSize>(MIN_ENT_SIZE/2) && desiredDmg>0){
                                            other.wep.bulSize = desiredSize
                                            other.wep.buldmg = desiredDmg
                                        }
                                    }
                                    1->{
                                        if(other.wep.bulspd-1>1)other.wep.bulspd--
                                    }
                                    2->{
                                        if(other.wep.recoil-1>=0)other.wep.recoil--
                                    }
                                    3->{
                                        if(other.wep.atkSpd-1>0)other.wep.atkSpd--
                                    }
                                }
                            }
                        }
                    },
//                    Selector(other, other.xpos+30,4),
                    StatView({other.wep.buldmg.toString() }, statsXSpace+other.xpos, other.ypos),
                    StatView({other.wep.bulspd.toString() }, statsXSpace+other.xpos, statsYSpace+other.ypos),
                    StatView({other.wep.recoil.toInt().toString() }, statsXSpace+other.xpos, 2*statsYSpace+other.ypos),
                    StatView({other.wep.atkSpd.toString() }, statsXSpace+other.xpos,  3*statsYSpace+other.ypos))

                other.menushowign = true
            }
        }
    }
}
class Gym(val char:Char):Entity(){

    override var color = Color.WHITE
    override var drawSize = 35.0
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
            if(!other.specificMenus[char]!!){
                other.menuStuff = listOf(
                    StatView({"Run"},other.xpos,other.ypos),
                    StatView({"HP"},other.xpos,statsYSpace+other.ypos),
                    StatView({"Turn"},other.xpos,2*statsYSpace+other.ypos),
                    object:Entity(){
                        override var xpos = other.xpos+selectorXSpace
                        override var color = Color.BLUE
                        override var drawSize = 20.0
                        override var ypos = other.ypos
                        var indexer = 0
                        override fun updateEntity() {
                            if(other.pCont.sht.tryConsume()){
                                if(indexer+1<3){
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
                                    0->{
                                        other.speed += 1
                                    }
                                    1->{
                                        other.drawSize  += 3
                                        other.maxHP +=10
                                        other.currentHp = other.maxHP
                                    }
                                    2->{
                                        val desired = "%.4f".format(other.turnSpeed+0.01f).toFloat()
                                        if(desired<1) other.turnSpeed = desired
                                    }
                                }
                            }else if(other.pCont.spenlef.tryConsume()){
                                when(indexer){
                                    0->{
                                        val desiredspeed = other.speed-1
                                        if(desiredspeed>0)other.speed = desiredspeed
                                    }
                                    1->{
                                        val desiredSize = other.drawSize-3
                                        val desiredHp = other.maxHP-10
                                        if(desiredSize>MIN_ENT_SIZE && desiredHp>0){
                                            other.drawSize = desiredSize
                                            other.maxHP = desiredHp
                                        }
                                        other.currentHp = other.maxHP
                                    }
                                    2->{
                                        val desired = "%.4f".format(other.turnSpeed-0.01f).toFloat()
                                        if(desired>0) other.turnSpeed = desired
                                    }
                                }
                            }
                        }
                    },
                    StatView({other.speed.toString() }, statsXSpace+other.xpos, other.ypos),
                    StatView({other.maxHP.toInt().toString() }, statsXSpace+other.xpos, statsYSpace+other.ypos),
                    StatView({( other.turnSpeed*100).toInt().toString() }, statsXSpace+other.xpos, 2*statsYSpace+other.ypos)
                )

                other.specificMenus[char] = true
            }
        }
    }
}
class StatView(val showText: ()->String, val xloc:Double,val yloc:Double):Entity(){
    override fun drawEntity(g: Graphics) {
        g.color = Color.BLUE
        g.font = g.font.deriveFont((myFrame.width/70).toFloat())
        g.drawString(showText(),getWindowAdjustedPos(xloc).toInt(),getWindowAdjustedPos(yloc+15).toInt())
    }
}

const val MIN_ENT_SIZE = 9.0
