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
                            ient.isDead = true
                            jent.isDead = true
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
const val mapGridSize = 60.0
val map1 =  "10111011" +
            "00212011" +
            "00121011" +
            "10001010"

fun placeMap(){
    val starty = INTENDED_FRAME_SIZE/7
    var rownum = 0
    for((outerind,i) in (0..25 step 8).withIndex()){
        rownum++
        for((ind:Int,ch:Char) in map1.substring(i,i+8).withIndex()){
            if(ch=='1'){
                entsToAdd.add(Wall().also {
                    it.drawSize = mapGridSize
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
            }else if (ch == '2'){
                entsToAdd.add(MedPack().also {
                    it.xpos = ind.toDouble()+(ind* mapGridSize)
                    it.ypos = starty + (mapGridSize+1)*(outerind+1)
                })
            }
        }
    }
}

fun startWave(numberofenemies: Int, sizeofenemies: Double, colourofenemies: Color) {
    for (i in 1..numberofenemies) {
        val se = Enemy()
        se.drawSize = sizeofenemies
        se.color = colourofenemies
        se.xpos = (2 * i * sizeofenemies)
        se.ypos = 10.0
        entsToAdd.add(se)
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

fun main() {
    entsToAdd.addAll(listOf(
        player1,
        player2
//        , Wall()
    ))
    menuEntities.addAll(
        listOf(
            StatView({"speed"},0.0,10.0),
            StatView({"size"},0.0,50.0)
        )
    )
    val nums = listOf(40.0,170.0)
    val topnums = listOf(80.0,200.0)

    menuEntities.addAll(
        listOf(
            Selector(0, player1, topnums[0]),
            StatView({ player1.speed.toString() }, nums[0], 10.0),
            StatView({ player1.drawSize.toString() }, nums[0], 50.0),
            Selector(1, player2, topnums[1]),
            StatView({ player2.speed.toString() }, nums[1], 10.0),
            StatView({ player2.drawSize.toString() }, nums[1], 50.0)
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
//    startWave(4, (Math.random() * 30) + 10, Color.LIGHT_GRAY, 5)
//    runBlocking {
//        launch {
            while (true){
//                if(newframesize!=frameSize)frameSize = newframesize
                Thread.sleep(30)
                if(pressed3.tryConsume()){
                    gamePaused = !gamePaused
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
                    placeMap()
                    startWave(4, (Math.random() * 30) + 10, Color.LIGHT_GRAY)
                } else{
                    if(showingmenu)menuTick()
                    else{
                        if(!gamePaused){
                            gameTick()
                        }
                    }
                }
            }
//        }
//    }
}
var gamePaused = false