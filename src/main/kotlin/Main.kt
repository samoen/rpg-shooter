import java.awt.*
import java.awt.event.*
import javax.swing.*

val allEntities = mutableListOf<Entity>()
val entsToAdd = mutableListOf<Entity>()


val player1 = Player(ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD1,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD7,KeyEvent.VK_NUMPAD9)).also { it.speed = 8 }
val player2 = Player(ButtonSet(KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_V,KeyEvent.VK_C,KeyEvent.VK_B)).also{
    it.xpos=150.0
    it.speed = 8
    it.drawSize = 40.0
}

var pressed1 = OneShotChannel()
var pressed2 = OneShotChannel()
var pressed3 = OneShotChannel()

var showingmenu = false
const val INTENDED_FRAME_SIZE = 900
val XMAXMAGIC = INTENDED_FRAME_SIZE*15
//const val TICK_INTERVAL = 30

val backgroundImage = ImageIcon("src/main/resources/grass.png").image
var myrepaint = false
var myPanel:JPanel =object : JPanel() {
    override fun paint(g: Graphics) {
        super.paint(g)
        if(myrepaint){
            myrepaint = false
            g.drawImage(backgroundImage,0,0,myFrame.width,myFrame.width,null)
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
            if(showingmenu){
                menuEntities.forEach { it.updateEntity() }
                menuEntities.forEach { entity ->
                    entity.drawEntity(g)
                }
            }
        }
    }
}.also {
//    it.isDoubleBuffered = true
//    it.background = Color.DARK_GRAY
}

var menuPanel:JPanel = object:JPanel(){
    override fun paint(g: Graphics) {
        super.paint(g)
        menuEntities.forEach { entity ->
            entity.drawEntity(g)
        }
    }
}.also {
    it.background = Color.PINK
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
var menuEntities = mutableListOf<Entity>()



fun menuTick(){
    menuEntities.forEach { it.updateEntity() }
    menuPanel.repaint()
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
                if(collided) {
                    val iBlockedTrigger = (jent is movementGetsBlocked && jent.doIGetBlockedBy(ient))
                    val jBlockedTrigger = (ient is movementGetsBlocked && ient.doIGetBlockedBy(jent))
                    if (iBlockedTrigger||jBlockedTrigger) {
                        if(timesTried > 100){
                            if(jent is Wall){
                                jent.isDead = true
                            }else if(ient is Wall){
                                ient.isDead = true
                            }else{
                                ient.isDead = true
                                jent.isDead = true
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
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1))entsToAdd.add(player1)
    if(!allEntities.contains(player2) && !entsToAdd.contains(player2)) entsToAdd.add(player2)
    player1.isDead = false
    player2.isDead = false
//    player1.ypos = (INTENDED_FRAME_SIZE - player1.drawSize)
//    player1.xpos = 0.0
//    player2.ypos = (INTENDED_FRAME_SIZE - player2.drawSize)
//    player2.xpos = (player1.drawSize)
    if(heal){
        player1.currentHp = player1.maxHP
        player2.currentHp = player2.maxHP
    }
}
const val mapGridSize = 55.0
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
            "  w         wh  " +
            "            2   " +
            "   1            " +
            "            s   "

val map2 =  "s       w       " +
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
    currentMapNum = mapNum
    allEntities.clear()
    val starty = 0
    var rownum = 0
    for((outerind,i) in (0..(mapGridColumns*mapGridRows)-7 step mapGridColumns).withIndex()){
        rownum++
        for((ind:Int,ch:Char) in map.substring(i,i+mapGridColumns).withIndex()){
            if(ch=='w'){
                entsToAdd.add(Wall().also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize)*(outerind)
                })
                continue
            }
            if (ch == 'h'){
                entsToAdd.add(MedPack().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
                continue
            }
            if (ch == 'e'){
                entsToAdd.add(randEnemy().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
                continue
            }
            if(ch == 's'){
                entsToAdd.add(GateSwitch().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
                continue
            }
//            if(ch == 'x'){
////            if(ch == 'a' || ch=='A'){
//                val spawnGate = Gateway()
////                spawnGate.backgate = true
////                if(ch=='A'){
//                    spawnGate.map = map1
////                }else{
//                    spawnGate.mapnum = 1
////                }
//                playerSpawn = Pair(ind.toDouble()+(ind* mapGridSize),starty + (mapGridSize+1)*(outerind+1))
//                spawnGate.xpos = playerSpawn.first
//                spawnGate.ypos = playerSpawn.second
//                player1.xpos = playerSpawn.first
//                player1.ypos = playerSpawn.second
//                player2.xpos = playerSpawn.first + (player1.drawSize)
//                player2.ypos = playerSpawn.second
//                entsToAdd.add(player1)
//                entsToAdd.add(player2)
//                entsToAdd.add(spawnGate)
//                continue
//            }

            val charint :Int= Character.getNumericValue(ch)
            if(charint in 1..9){
                 val mappy:String =when(charint){
                     1->map1
                     2->map2
                     3->map3
                     else ->map1
                 }
                val gatex = ind.toDouble()+(ind* mapGridSize)
                val gatey = starty + (mapGridSize+1)*(outerind+1)
                val gate = Gateway().also {
                    it.map = mappy
                    it.mapnum = charint
                    it.xpos = gatex
                    it.ypos = gatey
                }
                if(charint==fromMapNum){
                    player1.xpos = gatex
                    player1.ypos = gatey
                    player1.spawnGate = gate
                    player2.xpos = gatex + (player1.drawSize)
                    player2.ypos = gatey
                    player2.spawnGate = gate
                    entsToAdd.add(player1)
                    entsToAdd.add(player2)
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
    se.maxHP = (se.drawSize/2)
    se.currentHp = se.maxHP
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
val selectoryspacing = listOf(0.0,40.0,80.0,120.0,160.0,200.0,240.0)
fun main() {
    entsToAdd.addAll(listOf(
        player1,
        player2
//        , Wall()
    ))

    val xspacing = listOf(100.0,170.0)

    menuEntities.addAll(
        listOf(
            StatView({"Run Speed"},0.0,selectoryspacing[0]),
            StatView({"Health"},0.0,selectoryspacing[1]),
            StatView({"Turn Speed"},0.0,selectoryspacing[2]),
            StatView({"Wep1 Damage"},0.0,selectoryspacing[3]),
            StatView({"Wep1 Velocity"},0.0,selectoryspacing[4]),
            StatView({"Wep1 Recoil"},0.0,selectoryspacing[5]),
            StatView({"Wep1 Reload"},0.0,selectoryspacing[6])
        )
    )

    menuEntities.addAll(
        listOf(
            Selector(player1, xspacing[0]+30),
            StatView({ player1.speed.toString() }, xspacing[0], selectoryspacing[0]),
            StatView({ player1.maxHP.toInt().toString() }, xspacing[0], selectoryspacing[1]),
            StatView({ player1.turnSpeed.toString() }, xspacing[0], selectoryspacing[2]),
            StatView({ player1.primWep.buldmg.toString() }, xspacing[0], selectoryspacing[3]),
            StatView({ player1.primWep.bulspd.toString() }, xspacing[0], selectoryspacing[4]),
            StatView({ player1.primWep.recoil.toString() }, xspacing[0], selectoryspacing[5]),
            StatView({ player1.primWep.atkSpd.toString() }, xspacing[0], selectoryspacing[6]),
            Selector(player2, xspacing[1]+30),
            StatView({ player2.speed.toString() }, xspacing[1], selectoryspacing[0]),
            StatView({ player2.maxHP.toInt().toString() }, xspacing[1], selectoryspacing[1]),
            StatView({ player2.turnSpeed.toString() }, xspacing[1], selectoryspacing[2]),
            StatView({ player2.primWep.buldmg.toString() }, xspacing[1], selectoryspacing[3]),
            StatView({ player2.primWep.bulspd.toString() }, xspacing[1], selectoryspacing[4]),
            StatView({ player2.primWep.recoil.toString() }, xspacing[1], selectoryspacing[5]),
            StatView({ player2.primWep.atkSpd.toString() }, xspacing[1], selectoryspacing[6])
        )
    )
    myFrame.addKeyListener(
        object :KeyListener{
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyPressed(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.tryProduce()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.tryProduce()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.tryProduce()
                    playerKeyPressed(player1,e)
                    playerKeyPressed(player2,e)
                }
            }
            override fun keyReleased(e: KeyEvent?) {
                if(e!=null){
                    if (e.keyCode == KeyEvent.VK_1) pressed1.release()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.release()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.release()
                    playerKeyReleased(player1,e)
                    playerKeyReleased(player2,e)
                }
            }
        }
    )
//    myFrame.createBufferStrategy(3)
//    myFrame.graphics.dispose()
//    myFrame.bufferStrategy.show()

    myFrame.title = "Gunplay"
    myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, 40+INTENDED_FRAME_SIZE.toInt())
    myFrame.isVisible = true
    myFrame.contentPane = myPanel
        while (true){
            Thread.sleep(30)
            if(pressed3.tryConsume()){
//                    gamePaused = !gamePaused
                placeMap(map1,1,1)
            }else if(pressed2.tryConsume()) {
//                if(showingmenu){
//                    myFrame.contentPane = myPanel
//                }else{
//                    myFrame.contentPane = menuPanel
//
//                }
                showingmenu = !showingmenu
//                myFrame.revalidate()
            } else if (pressed1.tryConsume()) {
//                revivePlayers(true)
                startWave(4)
            } else if(changeMap){
                changeMap=false
                placeMap(nextMap,nextMapNum,currentMapNum)
                revivePlayers(false)
            } else{
//                if(showingmenu)menuTick()
//                else{
                    if(!gamePaused){
                        gameTick()
                    }
//                }
            }
        }
}
var gamePaused = false