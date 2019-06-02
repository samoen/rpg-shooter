import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JPanel

val allEntities = mutableListOf<Entity>()
val entsToAdd = mutableListOf<Entity>()
val players:MutableList<Player> = mutableListOf()
var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()
var gamePaused = false

val statsYSpace = 20.0
val statsXSpace = 30.0
val selectorXSpace = 45.0
const val INTENDED_FRAME_SIZE = 900
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
val YFRAMEMAGIC = 40
const val TICK_INTERVAL = 40
const val MIN_ENT_SIZE = 9.0
val BULLET_ALIVE = 14

val ENEMY_DRIFT_FRAMES = 30

val soundFiles:MutableMap<String,File> = mutableMapOf()
//val enBulFile = File("src/main/resources/pewnew.wav").getAbsoluteFile()
val longpewFil = File("src/main/resources/newlongpew.wav").getAbsoluteFile()
val swapnoiseFile = File("src/main/resources/swapnoise.wav").getAbsoluteFile()
val dienoiseFile = File("src/main/resources/deathclip.wav").getAbsoluteFile()
val ouchnoiseFile = File("src/main/resources/ouch.wav").getAbsoluteFile()

val enemyPewFile = File("src/main/resources/enemypew.wav").getAbsoluteFile()
val stillImage = ImageIcon("src/main/resources/main.png").image
val runImage = ImageIcon("src/main/resources/walk.png").image
val goblinImage = ImageIcon("src/main/resources/walk.png").image
val pewImage = ImageIcon("src/main/resources/shoot1.png").image
val backgroundImage = ImageIcon("src/main/resources/tilemap.png").image
val pstoppedImage = ImageIcon("src/main/resources/floor1.png").image
val pouchImage = ImageIcon("src/main/resources/dooropen.png").image
val stopOuchImage = ImageIcon("src/main/resources/doorshut.png").image
val wallImage = ImageIcon("src/main/resources/brick1.png").image
val dieImage = ImageIcon("src/main/resources/shrapnel.png").image
val impactImage = ImageIcon("src/main/resources/shrapnel.png").image
val pBulImage = ImageIcon("src/main/resources/plasma.png").image
val eBulImage = ImageIcon("src/main/resources/badbullet.png").image
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image
var myrepaint = false
val soundBank:MutableMap<String, Clip> = mutableMapOf()
var myFrame=object:JFrame(){

}.also {
    it.isFocusable = true
    it.iconImage = ImageIcon("gunman.png").image
}

const val mapGridColumns = 16
val map1 =  "        w       " +
            "       e        " +
            "      ww        " +
            "ww wh ww    w   " +
            " whwh          w" +
            "  hwh    m     w" +
            " whwh          h" +
            "            w ww" +
            "  wh  www     ww" +
            "  w   www     ww" +
            "  w    h    w   " +
            "  g    b    wh  " +
            "            2   " +
            "   1            " +
            "            s   "

val map2 =  "s       we      " +
            "   3         e  " +
            "      ww     h  " +
            "    h ww        " +
            "               w" +
            "               w" +
            "    h    1     w" +
            "              ww" +
            "   h          ww" +
            "     w w       w" +
            "       h    w  w" +
            "      www   wh w" +
            "        w   w  w" +
            "        w   w  w" +
            "        w      w"

val map3 =  "                " +
            "                " +
            "       2     h  " +
            "                " +
            "               w" +
            "               w" +
            "    h          w" +
            "          s   ww" +
            "              ww" +
            "               w" +
            "               w" +
            "            wh w" +
            "               w" +
            "                " +
            "                "

fun placeMap(map:String, mapNum:Int,fromMapNum:Int){
    val mapGridSize = (INTENDED_FRAME_SIZE/mapGridColumns.toDouble())-2
    currentMapNum = mapNum
    allEntities.clear()
    val starty = 0
    for(rownumber in 0 until (map.length/mapGridColumns)){
        for((ind:Int,ch:Char) in map.substring(rownumber*mapGridColumns,(rownumber*mapGridColumns)+mapGridColumns).withIndex()){
            if(ch=='w'){
                entsToAdd.add(Wall().also {
                    it.dimensions.drawSize = mapGridSize
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'h'){
                entsToAdd.add(MedPack().also {
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'e'){
                entsToAdd.add(randEnemy().also {
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 's'){
                entsToAdd.add(GateSwitch().also {
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 'b'){
                entsToAdd.add(Shop().also {
                    it.char = 'b'
                    it.dimensions.drawSize = mapGridSize
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                    it.menuThings = {other->listOf(
                        StatView({"Vel"},other.dimensions.xpos,other.dimensions.ypos),
                        StatView({"Rcl"},other.dimensions.xpos,statsYSpace+other.dimensions.ypos),
                        StatView({"Rld"},other.dimensions.xpos,statsYSpace*2+other.dimensions.ypos),
                        StatView({"Mob"},other.dimensions.xpos,3*statsYSpace+other.dimensions.ypos),
                        Selector(4,other,
                        {
                            if(other.healthStats.wep.bulspd+1<50)other.healthStats.wep.bulspd++
                        },{
                            if(other.healthStats.wep.bulspd-1>1)other.healthStats.wep.bulspd--
                        },{
                            if(other.healthStats.wep.recoil+1<23)other.healthStats.wep.recoil++
                        },{
                            if(other.healthStats.wep.recoil-1>=0)other.healthStats.wep.recoil--
                        },{
                            if(other.healthStats.wep.atkSpd+1<200){
                                other.healthStats.wep.atkSpd++
//                                other.healthStats.wep.framesSinceShottah = 999
                            }
                        },{
                            if(other.healthStats.wep.atkSpd-1>1)other.healthStats.wep.atkSpd--
                        },{
                                val desired = other.healthStats.wep.mobility+0.1f
                                if(desired<=1.001f) other.healthStats.wep.mobility = desired
                            },{
                                val desired = other.healthStats.wep.mobility-0.1f
                                if(desired>=0)other.healthStats.wep.mobility = desired
                            }),
                        StatView({other.healthStats.wep.bulspd.toString() }, statsXSpace+other.dimensions.xpos, other.dimensions.ypos),
                        StatView({other.healthStats.wep.recoil.toInt().toString() }, statsXSpace+other.dimensions.xpos, statsYSpace+other.dimensions.ypos),
                        StatView({other.healthStats.wep.atkSpd.toString() }, statsXSpace+other.dimensions.xpos,  2*statsYSpace+other.dimensions.ypos),
                        StatView({( other.healthStats.wep.mobility*10).toInt().toString() }, statsXSpace+other.dimensions.xpos, 3*statsYSpace+other.dimensions.ypos)
                    )
                    }
                })
                continue
            }
            if(ch == 'm'){
                entsToAdd.add(Shop().also {
                    it.char = 'm'
                    it.dimensions.drawSize = mapGridSize
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                    it.menuThings = {other->listOf(
                        StatView({"Dmg"},other.dimensions.xpos,other.dimensions.ypos),
                        StatView({"Lftm"},other.dimensions.xpos,statsYSpace+other.dimensions.ypos),
                        StatView({"Buck"},other.dimensions.xpos,2*statsYSpace+other.dimensions.ypos),
                        Selector(3,other,
                            {
                                other.healthStats.wep.buldmg+=1
                                other.healthStats.wep.bulSize+=3
                            },{
                                val desiredDmg = other.healthStats.wep.buldmg-1
                                val desiredSize = other.healthStats.wep.bulSize -3
                                if(desiredSize>(MIN_ENT_SIZE/2) && desiredDmg>0){
                                    other.healthStats.wep.bulSize = desiredSize
                                    other.healthStats.wep.buldmg = desiredDmg
                                }
                            },{
                                if(other.healthStats.wep.bulLifetime+1<100)other.healthStats.wep.bulLifetime++
                            },{
                                if(other.healthStats.wep.bulLifetime-1>=1)other.healthStats.wep.bulLifetime--
                            },{
                                if(other.healthStats.wep.projectiles+1<15)other.healthStats.wep.projectiles++
                            },{
                                if(other.healthStats.wep.projectiles-1>=1)other.healthStats.wep.projectiles--
                            }),
                        StatView({other.healthStats.wep.buldmg.toString() }, statsXSpace+other.dimensions.xpos, other.dimensions.ypos),
                        StatView({other.healthStats.wep.bulLifetime.toString() }, statsXSpace+other.dimensions.xpos,  statsYSpace+other.dimensions.ypos),
                        StatView({other.healthStats.wep.projectiles.toString() }, statsXSpace+other.dimensions.xpos,  2*statsYSpace+other.dimensions.ypos)
                    )}
                })
                continue
            }
            if(ch == 'g'){
                entsToAdd.add(Shop().also {
                    it.menuThings = {other->listOf(
                        StatView({"Run"},other.dimensions.xpos,other.dimensions.ypos),
                        StatView({"HP"},other.dimensions.xpos,statsYSpace+other.dimensions.ypos),
                        StatView({"Turn"},other.dimensions.xpos,2*statsYSpace+other.dimensions.ypos),
                        StatView({"Block"},other.dimensions.xpos,3*statsYSpace+other.dimensions.ypos),
                        Selector(4,other,{
                            other.speed += 1
                        },{
                            val desiredspeed = other.speed-1
                            if(desiredspeed>0)other.speed = desiredspeed
                        },{
                            other.dimensions.drawSize  += 3
                            other.healthStats.maxHP +=10
                            other.healthStats.currentHp = other.healthStats.maxHP
                        },{
                            val desiredSize = other.dimensions.drawSize-3
                            val desiredHp = other.healthStats.maxHP-10
                            if(desiredSize>MIN_ENT_SIZE && desiredHp>0){
                                other.dimensions.drawSize = desiredSize
                                other.healthStats.maxHP = desiredHp
                            }
                            other.healthStats.currentHp = other.healthStats.maxHP
                        },{
                            val desired = "%.4f".format(other.healthStats.turnSpeed+0.01f).toFloat()
                            if(desired<1) other.healthStats.turnSpeed = desired
                        },{
                            val desired = "%.4f".format(other.healthStats.turnSpeed-0.01f).toFloat()
                            if(desired>0) other.healthStats.turnSpeed = desired
                        },{
                            other.healthStats.shieldSkill += 1
                        },{
                            val desired = other.healthStats.shieldSkill-1
                            if(desired>=1)other.healthStats.shieldSkill = desired
                        }),
                        StatView({other.speed.toString() }, statsXSpace+other.dimensions.xpos, other.dimensions.ypos),
                        StatView({other.healthStats.maxHP.toInt().toString() }, statsXSpace+other.dimensions.xpos, statsYSpace+other.dimensions.ypos),
                        StatView({( other.healthStats.turnSpeed*100).toInt().toString() }, statsXSpace+other.dimensions.xpos, 2*statsYSpace+other.dimensions.ypos),
                        StatView({( other.healthStats.shieldSkill).toInt().toString() }, statsXSpace+other.dimensions.xpos, 3*statsYSpace+other.dimensions.ypos)
                    )}
                    it.char = 'g'
                    it.dimensions.drawSize = mapGridSize
                    it.dimensions.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.dimensions.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            val charint :Int= Character.getNumericValue(ch)
            if(charint in 1..9){
                 val mappy:String =when(charint){
                     1->map1
                     2->map2
                     3->map3
                     else ->map1
                 }
                val gatex = ind.toDouble()+(ind* mapGridSize)
                val gatey = starty + (mapGridSize+1)*(rownumber+1)
                val gate = Gateway().also {
                    it.map = mappy
                    it.mapnum = charint
                    it.dimensions.xpos = gatex
                    it.dimensions.ypos = gatey
                    it.dimensions.drawSize = mapGridSize
                }
                if(charint==fromMapNum){
                    var lastsize = 0.0
                    for(player in players){
                        player.dimensions.xpos = gatex + lastsize
                        player.dimensions.ypos = gatey
                        player.spawnGate = gate
                        lastsize = (player.dimensions.drawSize)
                        if(!allEntities.contains(player) && !entsToAdd.contains(player))entsToAdd.add(player)
                        player.toBeRemoved = false
                    }
                }
                entsToAdd.add(gate)
                continue
            }

        }
    }
}
val entsToDraw = mutableListOf<Entity>()

fun main() {

    soundFiles["shoot"] = longpewFil
    soundFiles["ouch"] = ouchnoiseFile
    soundFiles["die"] = dienoiseFile
    soundFiles["laser"] = enemyPewFile
    soundFiles["swap"] = swapnoiseFile

    soundBank["ouch"]= AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(ouchnoiseFile))
    }
    soundBank["die"]= AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(dienoiseFile))
    }
    soundBank["swap"] = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(swapnoiseFile))
    }
    soundBank["shoot"] = AudioSystem.getClip().also{
                    it.open(AudioSystem.getAudioInputStream(longpewFil))
    }
    soundBank["laser"] = AudioSystem.getClip().also{
        it.open(AudioSystem.getAudioInputStream(enemyPewFile))
    }
    players.add(Player(ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD8,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD4,KeyEvent.VK_NUMPAD6)).also { it.speed = 8 })
    players.add( Player(ButtonSet(KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_V,KeyEvent.VK_C,KeyEvent.VK_B)).also{
        it.dimensions.xpos=150.0
        it.speed = 8
        it.dimensions.drawSize = 40.0
    })
    entsToAdd.addAll(players)

    myFrame.addKeyListener(
        object :KeyListener{
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyPressed(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.tryProduce()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.tryProduce()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.tryProduce()
                    for(player in players){
                        playerKeyPressed(player,e)
                    }
                }
            }
            override fun keyReleased(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.release()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.release()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.release()
                    for (player in players){
                        playerKeyReleased(player,e)
                    }
                }
            }
        }
    )
//    myFrame.createBufferStrategy(3)
//    myFrame.graphics.dispose()
//    myFrame.bufferStrategy.show()


    val myPanel:JPanel =object : JPanel() {
        override fun paint(g: Graphics) {
            super.paint(g)
            if(myrepaint){
                myrepaint = false
                g.drawImage(backgroundImage,0,0, getWindowAdjustedPos(INTENDED_FRAME_SIZE-(XMAXMAGIC/myFrame.width.toDouble())).toInt(),myFrame.width,null)
                entsToDraw.forEach {
                    it.drawEntity(g)
                }
                players.forEach {
                    if(!it.toBeRemoved)
                        it.drawComponents(g)
                }
            }
        }
    }
    myFrame.contentPane = myPanel
    myFrame.title = "Gunplay"
    myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, INTENDED_FRAME_SIZE+YFRAMEMAGIC)
    myFrame.isVisible = true
//    playSound(player0.healthStats.shootNoise)

    while (true){
        val pretime = System.currentTimeMillis()
        if(pressed3.tryConsume()){
            placeMap(map1,1,1)
        }else if(pressed2.tryConsume()) {
            gamePaused = !gamePaused
        } else if (pressed1.tryConsume()) {
            startWave(4)
        } else if(changeMap){
            changeMap=false
            placeMap(nextMap,nextMapNum,currentMapNum)
        } else{
                if(!gamePaused){
                    val preupdateEnts = allEntities.map { it.dimensions.copy() }
                    allEntities.forEach { entity: Entity ->
                        entity.updateEntity()
                    }
                    var timesTried = 0
                    do{
                        timesTried++
                        var triggeredReaction = false
                        for(dex in 0 until allEntities.size) {
                            val ient = allEntities[dex]
                            if(ient is Player || ient is Enemy){
                                for(j in (0)until allEntities.size){
                                if(dex!=j){
                                        val jent = allEntities[j]
                                        var collided = false
                                        if(!ient.toBeRemoved && !jent.toBeRemoved){
                                            if(ient.overlapsOther(jent)){
                                                    collided = true
                                                    blockMovement(ient,jent,preupdateEnts[dex],preupdateEnts[j])
                                                    val died = takeDamage(jent,ient)
                                                    if(died && ient is Player){
                                                        ient.healthStats.currentHp = ient.healthStats.maxHP
                                                        ient.spawnGate.playersInside.add(ient)
                                                    }
                                            }
                                        }
                                        if(collided && jent.overlapsOther(ient)) {
                                            if (ient.isSolid && jent.isSolid) {
                                                if(timesTried > 10){
                                                    println("Cannot resolve collision!")
                                                }else{
                                                    triggeredReaction = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }while (triggeredReaction)
                    allEntities.removeIf { it.toBeRemoved }
                    entsToDraw.clear()
                    val combatants = mutableListOf<Entity>()
                    val noncombatants = mutableListOf<Entity>()
                    val bullets = mutableListOf<Entity>()
                    allEntities.forEach {
                        if(it is Player || it is Enemy)combatants.add(it)
                        else if(it is Bullet){bullets.add(it)}
                        else noncombatants.add(it)
                    }
                    entsToDraw.addAll(noncombatants)
                    entsToDraw.addAll(combatants)
                    entsToDraw.addAll(bullets)
                    for(player in players){
                        if(!player.notOnShop){
                            player.menuStuff.forEach {
                                it.updateEntity()
                                entsToDraw.add(it)
                            }
                        }
                    }
                    myrepaint = true
                    myFrame.repaint()
                    if(entsToAdd.size>0) allEntities.addAll(entsToAdd)
                    entsToAdd.clear()
                }
        }
        val tickdiff = System.currentTimeMillis() - pretime
        if(tickdiff<TICK_INTERVAL) Thread.sleep(TICK_INTERVAL-tickdiff)
    }
}
