import java.awt.*
import java.awt.event.*
import javax.swing.*

val allEntities = mutableListOf<Entity>()
val entsToAdd = mutableListOf<Entity>()
val statsYSpace = 20.0
val statsXSpace = 30.0
val selectorXSpace = 45.0
val player0 = Player(ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD8,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD4,KeyEvent.VK_NUMPAD6),0).also { it.speed = 8 }
val player1 = Player(ButtonSet(KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_V,KeyEvent.VK_C,KeyEvent.VK_B),1).also{
    it.xpos=150.0
    it.speed = 8
    it.drawSize = 40.0
}

var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()

const val INTENDED_FRAME_SIZE = 900
const val INTENDED_FRAME_WIDTH = INTENDED_FRAME_SIZE*2
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
val YFRAMEMAGIC = 40
const val TICK_INTERVAL = 40

val backgroundImage = ImageIcon("src/main/resources/floor1.png").image
var myrepaint = false
var myPanel:JPanel =object : JPanel() {
    override fun paint(g: Graphics) {
        super.paint(g)
        if(myrepaint){
            myrepaint = false
            g.drawImage(backgroundImage,0,0, (myFrame.width).toInt(),myFrame.width,null)
            val players = mutableListOf<Entity>()
            allEntities.forEach { entity ->
                if(entity is Player){
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
}.also {
//    it.isDoubleBuffered = true
//    it.background = Color.DARK_GRAY
}

var myFrame=object:JFrame(){

}.also {
    it.isFocusable = true
    it.iconImage = ImageIcon("gunman.png").image
//    it.addComponentListener(object :ComponentListener{
//        override fun componentResized(e: ComponentEvent?) {
//            newframesize = it.width
//        }
//
//        override fun componentMoved(e: ComponentEvent?) {
//        }
//
//        override fun componentHidden(e: ComponentEvent?) {
//        }
//
//        override fun componentShown(e: ComponentEvent?) {
//        }
//    })
}

fun gameTick(){
    val preupdateEnts = mutableListOf<EntDimens>()
    allEntities.forEach { entity: Entity ->
        preupdateEnts.add(EntDimens(entity.xpos,entity.ypos,entity.drawSize))
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
                    val iBlockedTrigger = (jent is movementGetsBlocked && doIGetBlockedBy(ient))
                    val jBlockedTrigger = (ient is movementGetsBlocked && doIGetBlockedBy(jent))
                    if (iBlockedTrigger||jBlockedTrigger) {
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
    myrepaint = true
    myPanel.repaint()
    allEntities.addAll(entsToAdd)
    entsToAdd.clear()
}



fun revivePlayers(heal:Boolean){
    if(!allEntities.contains(player0) && !entsToAdd.contains(player0))entsToAdd.add(player0)
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1)) entsToAdd.add(player1)
    player0.isDead = false
    player1.isDead = false
//    player0.ypos = (INTENDED_FRAME_SIZE - player0.drawSize)
//    player0.xpos = 0.0
//    player1.ypos = (INTENDED_FRAME_SIZE - player1.drawSize)
//    player1.xpos = (player0.drawSize)
    if(heal){
        player0.hasHealth.currentHp = player0.hasHealth.maxHP
        player1.hasHealth.currentHp = player1.hasHealth.maxHP
    }
}

const val mapGridColumns = 16
const val mapGridRows = 15
val map1 =  "        w       " +
            "                " +
            "      ww        " +
            "ww wh ww    w   " +
            " whwh          w" +
            "  hwh          w" +
            " whwh           " +
            "            w ww" +
            "  wh  www     ww" +
            "  w   www     ww" +
            "  w    h    w   w" +
            "  g    b    wh  " +
            "            2   " +
            "   1            " +
            "            s   "

val map2 =  "s       we      " +
            "   3            " +
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

//fun locToMapCoord(x:Double,y:Double):Pair<Int,Int>{
//    var row = (y/mapGridSize).toInt()
//    var col = (x/mapGridSize).toInt()
//    return Pair(col,row)
//}
//fun locToIndex(x:Double,y:Double):Int{
//    var fromrows = mapGridColumns*(locToMapCoord(x,y).second)
//    var lastcol = locToMapCoord(x,y).first
//    var result = fromrows+lastcol
//    if(result<1)result = 1
//    if(result>map1.length-1)result = map1.length-1
//    return result
//}

fun placeMap(map:String, mapNum:Int,fromMapNum:Int){
    val mapGridSize = (INTENDED_FRAME_SIZE/mapGridColumns.toDouble())-2
    currentMapNum = mapNum
    allEntities.clear()
    val starty = 0
    for(rownumber in 0 until (map.length/mapGridColumns)){
        for((ind:Int,ch:Char) in map.substring(rownumber*mapGridColumns,(rownumber*mapGridColumns)+mapGridColumns).withIndex()){
            if(ch=='w'){
                entsToAdd.add(Wall().also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'h'){
                entsToAdd.add(MedPack().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if (ch == 'e'){
                entsToAdd.add(randEnemy().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 's'){
                entsToAdd.add(GateSwitch().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 'b'){
                entsToAdd.add(BlackSmith('b').also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
                })
                continue
            }
            if(ch == 'g'){
                entsToAdd.add(Gym('g').also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(rownumber+1)
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
                    it.xpos = gatex
                    it.ypos = gatey
                    it.drawSize = mapGridSize
                }
                if(charint==fromMapNum){
                    player0.xpos = gatex
                    player0.ypos = gatey
                    player0.spawnGate = gate
                    player1.xpos = gatex + (player0.drawSize)
                    player1.ypos = gatey
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

fun randEnemy():Enemy{
    val se = Enemy()
    se.turnSpeed = (0.01+(Math.random()/15)).toFloat()
    se.drawSize = 20+(Math.random()*30)
    se.hasHealth.maxHP = (se.drawSize/2)
    se.hasHealth.currentHp = se.hasHealth.maxHP
    se.speed = (Math.random()*3).toInt()+1
    se.wep.bulSize = 8.0+(Math.random()*40)
    se.wep.buldmg = se.wep.bulSize.toInt()
    se.wep.atkSpd = (Math.random()*20).toInt()+10
    se.wep.bulspd = (Math.random()*10).toInt()+3
    return  se
}

fun startWave(numberofenemies: Int) {
    var lastsize = 0.0
    for (i in 1..numberofenemies) {
        val e = randEnemy()
        e.xpos = (lastsize)
        lastsize += e.drawSize
        e.ypos = 10.0
        entsToAdd.add(e)
    }
}

fun playerKeyPressed(player: Player, e:KeyEvent){
    if (e.keyCode == player.buttonSet.swapgun) player.pCont.Swp.tryProduce()
    if (e.keyCode == player.buttonSet.up) player.pCont.up.tryProduce()
    if (e.keyCode == player.buttonSet.down) player.pCont.dwm.tryProduce()
    if (e.keyCode == player.buttonSet.shoot) player.pCont.sht.tryProduce()
    if (e.keyCode == player.buttonSet.right) player.pCont.riri.tryProduce()
    if (e.keyCode == player.buttonSet.left) player.pCont.leflef.tryProduce()
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.tryProduce()
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.tryProduce()
}

fun playerKeyReleased(player: Player,e: KeyEvent){
    if (e.keyCode == player.buttonSet.swapgun) {
        player.pCont.Swp.release()
    }
    if (e.keyCode == player.buttonSet.up) {
        player.pCont.up.release()
    }
    if (e.keyCode == player.buttonSet.down) {
        player.pCont.dwm.release()
    }
    if (e.keyCode == player.buttonSet.shoot){
        player.pCont.sht.release()
    }
    if (e.keyCode == player.buttonSet.right){
        player.pCont.riri.release()
    }
    if (e.keyCode == player.buttonSet.left) {
        player.pCont.leflef.release()
    }
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef.release()
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri.release()
}

fun main() {
//    playerhootNoise.open(playshoostrm)
    entsToAdd.addAll(listOf(
        player0,
        player1
//        , Wall()
    ))
    playSound(player0.shootNoise)
//    player0.collide(player1,EntDimens(player0.xpos,player0.ypos,player0.drawSize),EntDimens(player1.xpos,player1.ypos,player1.drawSize))

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

    myFrame.title = "Gunplay"
    myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, INTENDED_FRAME_SIZE+YFRAMEMAGIC)
    myFrame.isVisible = true
    myFrame.contentPane = myPanel
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
                    gameTick()
                }
        }
        val tickdiff = System.currentTimeMillis() - pretime
        if(tickdiff<TICK_INTERVAL) Thread.sleep(TICK_INTERVAL-tickdiff)
    }
}
var gamePaused = false