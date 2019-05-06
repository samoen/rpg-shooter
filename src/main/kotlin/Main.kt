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
//var frameSize = INTENDED_FRAME_SIZE
//const val TICK_INTERVAL = 30

val backgroundImage = ImageIcon("src/main/resources/grass.png").image
var myrepaint = false
var myPanel:JPanel =object : JPanel() {
    override fun paint(g: Graphics) {
        super.paint(g)
        if(myrepaint){
            myrepaint = false
            g.drawImage(backgroundImage,0,0,myFrame.width,myFrame.width,null)
            allEntities.forEach { entity ->
                entity.drawEntity(g)
            }
            allEntities.forEach { entity ->
                entity.drawComponents(g)
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



fun revivePlayers(){
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1))entsToAdd.add(player1)
    if(!allEntities.contains(player2) && !entsToAdd.contains(player2)) entsToAdd.add(player2)
    player1.currentHp = player1.maxHP
    player1.isDead = false
    player2.currentHp = player2.maxHP
    player2.isDead = false
    player1.ypos = (INTENDED_FRAME_SIZE - player1.drawSize).toDouble()
    player1.xpos = 0.0
    player2.ypos = (INTENDED_FRAME_SIZE - player2.drawSize).toDouble()
    player2.xpos = (player1.drawSize).toDouble()
}
const val mapGridSize = 55.0
const val mapGridColumns = 16
const val mapGridRows = 16
val map1 =  "0000000010031300" +
            "0000000000001300" +
            "0000001100001200" +
            "1101201100001000" +
            "0121200000000001" +
            "0021200000000001" +
            "0121200000000001" +
            "0000000000001011" +
            "0012001110000011" +
            "0010001110000011" +
            "0010000200001001" +
            "0010001110001201" +
            "0000000010001001" +
            "0000000010001001" +
            "0000000010000001" +
            "0000001000000111"

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

fun placeMap(){
//    val starty = INTENDED_FRAME_SIZE/15
//    allEntities.forEach { if(it is Wall) it.isDead = true }
    allEntities.removeIf{it is Wall}
    val starty = 0
    var rownum = 0
    for((outerind,i) in (0..(mapGridColumns*mapGridRows)-7 step mapGridColumns).withIndex()){
        rownum++
        for((ind:Int,ch:Char) in map1.substring(i,i+mapGridColumns).withIndex()){
            if(ch=='1'){
                entsToAdd.add(Wall().also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize)*(outerind)
                })
            }else if (ch == '2'){
                entsToAdd.add(MedPack().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
            }else if (ch == '3'){
                entsToAdd.add(randEnemy().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
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
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef = true
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri = true
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
    if (e.keyCode == player.buttonSet.spinleft) player.pCont.spenlef = false
    if (e.keyCode == player.buttonSet.spinright) player.pCont.spinri = false
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
                placeMap()
            }else if(pressed2.tryConsume()) {
                if(showingmenu){
                    myFrame.contentPane = myPanel
                }else{
                    myFrame.contentPane = menuPanel

                }
                showingmenu = !showingmenu
                myFrame.revalidate()
            } else if (pressed1.tryConsume()) {
                revivePlayers()
                startWave(4)
            } else{
                if(showingmenu)menuTick()
                else{
                    if(!gamePaused){
                        gameTick()
                    }
                }
            }
        }
}
var gamePaused = false