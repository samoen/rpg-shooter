
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
val entsToDraw = mutableListOf<Entity>()
val players:MutableList<Player> = mutableListOf()
var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()
var gamePaused = false
var myrepaint = false
var painting = false
val statsYSpace = 20.0
val statsXSpace = 30.0
val selectorXSpace = 45.0
const val INTENDED_FRAME_SIZE = 900
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
val YFRAMEMAGIC = 40
const val TICK_INTERVAL = 40
const val MIN_ENT_SIZE = 9.0

val ENEMY_DRIFT_FRAMES = 30
val soundFiles:MutableMap<String,File> = mutableMapOf()
//val soundBank:MutableMap<String, Clip> = mutableMapOf()
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
val healthShopImage = ImageIcon("src/main/resources/tilemap.png").image
val ammoShopImage = ImageIcon("src/main/resources/tilemap.png").image
val pstoppedImage = ImageIcon("src/main/resources/plasma.png").image
val pouchImage = ImageIcon("src/main/resources/dooropen.png").image
val armorBrokenImage = ImageIcon("src/main/resources/doorshut.png").image
val wallImage = ImageIcon("src/main/resources/brick1.png").image
val dieImage = ImageIcon("src/main/resources/shrapnel.png").image
val impactImage = ImageIcon("src/main/resources/shrapnel.png").image
val pBulImage = ImageIcon("src/main/resources/plasma.png").image
val eBulImage = ImageIcon("src/main/resources/badbullet.png").image
val gateClosedImage = ImageIcon("src/main/resources/doorshut.png").image
val gateOpenImage = ImageIcon("src/main/resources/dooropen.png").image

var myFrame = run {
    val jf = JFrame()
    jf.isFocusable = true
    jf.iconImage = ImageIcon("gunman.png").image
    jf
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

fun main() {
    soundFiles["shoot"] = longpewFil
    soundFiles["ouch"] = ouchnoiseFile
    soundFiles["die"] = dienoiseFile
    soundFiles["laser"] = enemyPewFile
    soundFiles["swap"] = swapnoiseFile

//    soundBank["ouch"]= AudioSystem.getClip().also{ it.open(AudioSystem.getAudioInputStream(ouchnoiseFile)) }
//    soundBank["die"]= AudioSystem.getClip().also{ it.open(AudioSystem.getAudioInputStream(dienoiseFile)) }
//    soundBank["swap"] = AudioSystem.getClip().also{ it.open(AudioSystem.getAudioInputStream(swapnoiseFile)) }
//    soundBank["shoot"] = AudioSystem.getClip().also{ it.open(AudioSystem.getAudioInputStream(longpewFil)) }
//    soundBank["laser"] = AudioSystem.getClip().also{ it.open(AudioSystem.getAudioInputStream(enemyPewFile)) }

    players.add(Player(
        ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD8,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD4,KeyEvent.VK_NUMPAD6))
        .also { it.commonStuff.speed = 8 })
    players.add(
        Player(
            ButtonSet(
                KeyEvent.VK_W,
                KeyEvent.VK_S,
                KeyEvent.VK_A,
                KeyEvent.VK_D,
                KeyEvent.VK_F,
                KeyEvent.VK_V,
                KeyEvent.VK_C,
                KeyEvent.VK_B
            )
        ).also{
        it.commonStuff.dimensions.xpos=150.0
        it.commonStuff.speed = 8
        it.commonStuff.dimensions.drawSize = 40.0
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
            if(myrepaint){
                myrepaint = false
//                    super.paint(g)
                g.drawImage(backgroundImage,0,0, getWindowAdjustedPos(INTENDED_FRAME_SIZE-(XMAXMAGIC/myFrame.width.toDouble())).toInt(),myFrame.width,null)
                entsToDraw.forEach {
                    it.drawEntity(g)
                }
                painting = false
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
            startWave(4)
        } else if(changeMap){
            changeMap=false
            placeMap(nextMap,nextMapNum,currentMapNum)
        } else{
                if(!gamePaused){
                    val preupdateEnts = allEntities.map { it.commonStuff.dimensions.copy() }
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
                                        if(jent.commonStuff.isSolid){
                                            var collided = false
                                            if(!ient.commonStuff.toBeRemoved && !jent.commonStuff.toBeRemoved){
                                                if(ient.commonStuff.dimensions.overlapsOther(jent.commonStuff.dimensions)){
                                                    collided = true
                                                    blockMovement(ient,jent,preupdateEnts[dex],preupdateEnts[j])
                                                }
                                            }
                                            if(dex>j && collided && jent.commonStuff.dimensions.overlapsOther(ient.commonStuff.dimensions)) {
                                                if (ient.commonStuff.isSolid && jent.commonStuff.isSolid) {
                                                    if(timesTried > players.size+1){
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
                        }
                    }while (triggeredReaction)
                    allEntities.removeIf { it.commonStuff.toBeRemoved }
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
                    painting = true
                    myPanel.repaint()
                    while (painting){Thread.sleep(1)}
                    if(entsToAdd.size>0) allEntities.addAll(entsToAdd)
                    entsToAdd.clear()
                }
        }
        val tickdiff = System.currentTimeMillis() - pretime
        if(tickdiff<TICK_INTERVAL) Thread.sleep(TICK_INTERVAL-tickdiff)
    }
}
