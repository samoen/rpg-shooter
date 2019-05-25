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
val statsYSpace = 20.0
val statsXSpace = 30.0
val selectorXSpace = 45.0
val player0 = Player(ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD8,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD4,KeyEvent.VK_NUMPAD6),0).also { it.speed = 8 }
val player1 = Player(ButtonSet(KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_V,KeyEvent.VK_C,KeyEvent.VK_B),1).also{
    it.dimensions.xpos=150.0
    it.speed = 8
    it.dimensions.drawSize = 40.0
}

var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()

var gamePaused = false
const val INTENDED_FRAME_SIZE = 900
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
val YFRAMEMAGIC = 40
const val TICK_INTERVAL = 40
const val MIN_ENT_SIZE = 9.0
val BULLET_ALIVE = 14

val soundFiles:MutableMap<String,File> = mutableMapOf()

val enBulFile = File("src/main/resources/pewnew.wav").getAbsoluteFile()
val longpewFil = File("src/main/resources/newlongpew.wav").getAbsoluteFile()
val swapnoiseFile = File("src/main/resources/swapnoise.wav").getAbsoluteFile()
val dienoiseFile = File("src/main/resources/deathclip.wav").getAbsoluteFile()
val ouchnoiseFile = File("src/main/resources/ouch.wav").getAbsoluteFile()
val enemyPewFile = File("src/main/resources/enemypew.wav").getAbsoluteFile()
val stillImage = ImageIcon("src/main/resources/main.png").image
val runImage = ImageIcon("src/main/resources/walk.png").image
val pewImage = ImageIcon("src/main/resources/shoot1.png").image
val backgroundImage = ImageIcon("src/main/resources/floor1.png").image
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
            " e    ww    e   " +
            "ww wh ww    w   " +
            " whwh          w" +
            "  hwh          w" +
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
                        StatView({"Dmg"},other.dimensions.xpos,0+other.dimensions.ypos),
                        StatView({"Vel"},other.dimensions.xpos,statsYSpace+other.dimensions.ypos),
                        StatView({"Rec"},other.dimensions.xpos,statsYSpace*2+other.dimensions.ypos),
                        StatView({"Rel"},other.dimensions.xpos,statsYSpace*3+other.dimensions.ypos),
                        Selector(4,other,{
                            other.tshd.wep.buldmg+=1
                            other.tshd.wep.bulSize+=3
                        },{
                            val desiredDmg = other.tshd.wep.buldmg-1
                            val desiredSize = other.tshd.wep.bulSize -3
                            if(desiredSize>(MIN_ENT_SIZE/2) && desiredDmg>0){
                                other.tshd.wep.bulSize = desiredSize
                                other.tshd.wep.buldmg = desiredDmg
                            }
                        },{
                            if(other.tshd.wep.bulspd+1<50)other.tshd.wep.bulspd++
                        },{
                            if(other.tshd.wep.bulspd-1>1)other.tshd.wep.bulspd--
                        },{
                            if(other.tshd.wep.recoil+1<23)other.tshd.wep.recoil++
                        },{
                            if(other.tshd.wep.recoil-1>=0)other.tshd.wep.recoil--
                        },{
                            if(other.tshd.wep.atkSpd+1<200)other.tshd.wep.atkSpd++
                        },{
                            if(other.tshd.wep.atkSpd-1>0)other.tshd.wep.atkSpd--
                        }),
                        StatView({other.tshd.wep.buldmg.toString() }, statsXSpace+other.dimensions.xpos, other.dimensions.ypos),
                        StatView({other.tshd.wep.bulspd.toString() }, statsXSpace+other.dimensions.xpos, statsYSpace+other.dimensions.ypos),
                        StatView({other.tshd.wep.recoil.toInt().toString() }, statsXSpace+other.dimensions.xpos, 2*statsYSpace+other.dimensions.ypos),
                        StatView({other.tshd.wep.atkSpd.toString() }, statsXSpace+other.dimensions.xpos,  3*statsYSpace+other.dimensions.ypos))}
                })
                continue
            }
            if(ch == 'g'){
                entsToAdd.add(Shop().also {
                    it.menuThings = {other->listOf(
                        StatView({"Run"},other.dimensions.xpos,other.dimensions.ypos),
                        StatView({"HP"},other.dimensions.xpos,statsYSpace+other.dimensions.ypos),
                        StatView({"Turn"},other.dimensions.xpos,2*statsYSpace+other.dimensions.ypos),
                        StatView({"Mob"},other.dimensions.xpos,3*statsYSpace+other.dimensions.ypos),
                        Selector(4,other,{
                            other.speed += 1
                        },{
                            val desiredspeed = other.speed-1
                            if(desiredspeed>0)other.speed = desiredspeed
                        },{
                            other.dimensions.drawSize  += 3
                            other.hasHealth.maxHP +=10
                            other.hasHealth.currentHp = other.hasHealth.maxHP
                        },{
                            val desiredSize = other.dimensions.drawSize-3
                            val desiredHp = other.hasHealth.maxHP-10
                            if(desiredSize>MIN_ENT_SIZE && desiredHp>0){
                                other.dimensions.drawSize = desiredSize
                                other.hasHealth.maxHP = desiredHp
                            }
                            other.hasHealth.currentHp = other.hasHealth.maxHP
                        },{
                            val desired = "%.4f".format(other.tshd.turnSpeed+0.01f).toFloat()
                            if(desired<1) other.tshd.turnSpeed = desired
                        },{
                            val desired = "%.4f".format(other.tshd.turnSpeed-0.01f).toFloat()
                            if(desired>0) other.tshd.turnSpeed = desired
                        },{
                            val desired = other.strafeRun+0.1f
                            if(desired<=1.001f) other.strafeRun = desired
                        },{
                            val desired = other.strafeRun-0.1f
                            if(desired>=0)other.strafeRun = desired
                        }),
                        StatView({other.speed.toString() }, statsXSpace+other.dimensions.xpos, other.dimensions.ypos),
                        StatView({other.hasHealth.maxHP.toInt().toString() }, statsXSpace+other.dimensions.xpos, statsYSpace+other.dimensions.ypos),
                        StatView({( other.tshd.turnSpeed*100).toInt().toString() }, statsXSpace+other.dimensions.xpos, 2*statsYSpace+other.dimensions.ypos),
                        StatView({( other.strafeRun*10).toInt().toString() }, statsXSpace+other.dimensions.xpos, 3*statsYSpace+other.dimensions.ypos)
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
                    player0.dimensions.xpos = gatex
                    player0.dimensions.ypos = gatey
                    player0.spawnGate = gate
                    player1.dimensions.xpos = gatex + (player0.dimensions.drawSize)
                    player1.dimensions.ypos = gatey
                    player1.spawnGate = gate
                    entsToAdd.add(player0)
                    entsToAdd.add(player1)
                }

                entsToAdd.add(gate)
                continue
            }

        }
    }
}

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

    entsToAdd.addAll(listOf(
        player0,
        player1
//        , Wall()
    ))
//    playSound(player0.tshd.shootNoise)

    myFrame.addKeyListener(
        object :KeyListener{
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyPressed(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.tryProduce()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.tryProduce()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.tryProduce()
                    playerKeyPressed(player0,e)
                    playerKeyPressed(player1,e)
                }
            }
            override fun keyReleased(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.release()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.release()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.release()
                    playerKeyReleased(player0,e)
                    playerKeyReleased(player1,e)
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
                val preupdateEnts = mutableListOf<EntDimens>()
                allEntities.forEach { entity: Entity ->
                    preupdateEnts.add(EntDimens(entity.dimensions.xpos,entity.dimensions.ypos,entity.dimensions.drawSize))
                    entity.updateEntity()
                }
                var timesTried = 0
                do{
                    timesTried++
                    var triggeredReaction = false
                    for (i in 0 until allEntities.size) {
                        for (j in (i + 1) until allEntities.size) {
                            val ient = allEntities[i]
                            val jent = allEntities[j]
                            var collided = false
                            if(ient.overlapsOther(jent)){
                                ient.collide(jent, preupdateEnts[i],preupdateEnts[j])
                                collided = true
                            }
                            if(jent.overlapsOther(ient)){
                                jent.collide(ient, preupdateEnts[j],preupdateEnts[i])
                                collided = true
                            }
                            if(collided && !ient.isDead && !jent.isDead && jent.overlapsOther(ient)) {
                                if ((ient is Player || ient is Enemy) && (jent is Player ||jent is Enemy)) {
                                    if(timesTried > 10){
                                        println("Cannot resolve collision!")
                                        if(jent is Wall){
//                                jent.isDead = true
                                        }else if(ient is Wall){
//                                ient.isDead = true
                                        }else{

//                                ient.isDead = true
//                                jent.isDead = true
                                        }
                                    }else{
                                        triggeredReaction = true
                                    }
                                }
                            }
                        }
                    }
                }while (triggeredReaction)
                allEntities.removeIf { it.isDead }

                g.drawImage(backgroundImage,0,0, getWindowAdjustedPos(INTENDED_FRAME_SIZE-(XMAXMAGIC/myFrame.width.toDouble())).toInt(),myFrame.width,null)
                val players = mutableListOf<Entity>()
                allEntities.forEach { entity ->
                    if(entity is Player || entity is Enemy){
                        players.add(entity)
                    }else entity.drawEntity(g)
                }
                players.forEach {
                    it.drawEntity(g)
                }

                allEntities.forEach { entity ->
                    entity.drawComponents(g)
                }
                if(player0.specificMenus.values.any { it }){
                    player0.menuStuff.forEach {
                        it.updateEntity()
                        it.drawEntity(g)
                    }
                }
                if(player1.specificMenus.values.any { it }){
                    player1.menuStuff.forEach {
                        it.updateEntity()
                        it.drawEntity(g)
                    }
                }
            }
        }
    }
    myFrame.contentPane = myPanel
    myFrame.title = "Gunplay"
    myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, INTENDED_FRAME_SIZE+YFRAMEMAGIC)
    myFrame.isVisible = true

    while (true){
        val pretime = System.currentTimeMillis()
        if(pressed3.tryConsume()){
            placeMap(map1,1,1)
        }else if(pressed2.tryConsume()) {
            gamePaused = !gamePaused
        } else if (pressed1.tryConsume()) {
//                revivePlayers(true)
            startWave(4)
        } else if(changeMap){
            changeMap=false
            placeMap(nextMap,nextMapNum,currentMapNum)
            revivePlayers(false)
        } else{
                if(!gamePaused){
                    myrepaint = true
                    myPanel.repaint()
                    if(entsToAdd.size>0) allEntities.addAll(entsToAdd)
                    entsToAdd.clear()
                }
        }
        val tickdiff = System.currentTimeMillis() - pretime
        if(tickdiff<TICK_INTERVAL) Thread.sleep(TICK_INTERVAL-tickdiff)
    }
}