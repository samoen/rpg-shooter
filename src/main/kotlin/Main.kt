import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.*
import javax.swing.*

val allEntities = mutableListOf<Entity>()
val entsToAdd = mutableListOf<Entity>()

class LockableBool(var locked:Boolean=false,var booly:Boolean=false){
    fun lockDown(){
        booly = false
        locked = true
    }

    fun trySet(){
        if(!locked){
            booly=true
        }
    }
    fun release(){
        locked = false
        booly = false
    }
}
class playControls(var up:LockableBool=LockableBool(), var dwm:LockableBool=LockableBool(), var sht:LockableBool=LockableBool(), var Swp:LockableBool=LockableBool(),var riri:LockableBool=LockableBool(), var leflef:LockableBool=LockableBool(), var spinri:Boolean=false, var spenlef:Boolean=false)

val player1 = Player(ButtonSet(KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_NUMPAD1,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD7,KeyEvent.VK_NUMPAD9)).also { it.speed = 8 }
val player2 = Player(ButtonSet(KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_V,KeyEvent.VK_C,KeyEvent.VK_B)).also{
    it.xpos=150.0
    it.speed = 8
    it.drawSize = 40.0
}

var pressed1 = LockableBool()
var pressed2 = LockableBool()
var pressed3 = LockableBool()

var showingmenu = false

const val FRAME_SIZE = 500
const val TICK_INTERVAL = 30

val backgroundImage = ImageIcon("src/main/resources/grass.png").image

var myPanel:JPanel =object : JPanel() {
    override fun paint(g: Graphics) {
        super.paint(g)
        g.drawImage(backgroundImage,0,0,FRAME_SIZE,FRAME_SIZE,null)
        allEntities.forEach { entity ->
            entity.drawEntity(g)
        }
        allEntities.forEach { entity ->
            entity.drawComponents(g)
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

var myFrame=JFrame().also {
    it.isFocusable = true
    it.iconImage = ImageIcon("gunman.png").image
}
var menuEntities = mutableListOf<Entity>()

class EntDimens(val xpos:Double,val ypos:Double,val drawSize:Double){
    fun getMidpoint():Pair<Double,Double>{
        return Pair((xpos+(drawSize/2)),ypos+(drawSize/2))
    }
}

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
    myPanel.repaint()
    allEntities.addAll(entsToAdd)
    entsToAdd.clear()
}



fun revivePlayers(){
//    allEntities.removeIf { (it !is Player) }
    if(!allEntities.contains(player1) && !entsToAdd.contains(player1))entsToAdd.add(player1)
//    allEntities.clear()
    if(!allEntities.contains(player2) && !entsToAdd.contains(player2)) entsToAdd.add(player2)
    player1.currentHp = player1.maxHP
    player1.isDead = false
    player2.currentHp = player2.maxHP
    player2.isDead = false
//    var lastWidth = 0.0
//    allEntities.forEachIndexed {i,e->
        player1.ypos = (FRAME_SIZE - player1.drawSize*2-30).toDouble()
        player1.xpos = 1.0
    player2.ypos = (FRAME_SIZE - player2.drawSize*2-30).toDouble()
//    player2.xpos = (player1.drawSize+5).toDouble()
    player2.xpos = (200).toDouble()
//        lastWidth += e.drawSize
//    }
}

fun startWave(numberofenemies: Int, sizeofenemies: Double, colourofenemies: Color, wallseed: Int) {
//    revivePlayers()
    for (i in 1..numberofenemies) {
        val se = Enemy()
        se.drawSize = sizeofenemies
        se.color = colourofenemies
        se.xpos = (2 * i * sizeofenemies)
        se.ypos = 10.0
        entsToAdd.add(se)
    }
//    for (i in 0..wallseed-1) {
//        val wall = Wall()
//        wall.xpos = (i*wall.drawSize * FRAME_SIZE /(wallseed*wall.drawSize))
//        wall.ypos =  (((Math.random()*(FRAME_SIZE-(3*100))))+70)
//        allEntities.add(wall)
//    }
    for(i in 1..4){
        entsToAdd.add(MedPack().also {
            it.xpos = i*it.drawSize*2 + 10
            it.ypos = 300.0
        })
    }
}

class ButtonSet(val up:Int,val down:Int,val left:Int,val right:Int,val swapgun:Int,val shoot:Int,val spinleft:Int,val spinright:Int)

fun playerKeyPressed(player: Player, e:KeyEvent){
    if (e.keyCode == player.buttonSet.swapgun) player.pCont.Swp.trySet()
    if (e.keyCode == player.buttonSet.up) player.pCont.up.trySet()
    if (e.keyCode == player.buttonSet.down) player.pCont.dwm.trySet()
    if (e.keyCode == player.buttonSet.shoot) player.pCont.sht.trySet()
    if (e.keyCode == player.buttonSet.right) player.pCont.riri.trySet()
    if (e.keyCode == player.buttonSet.left) player.pCont.leflef.trySet()
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
        player2,
        Wall())
    )
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
                    if (e.keyCode == KeyEvent.VK_1) pressed1.trySet()
                    if (e.keyCode == KeyEvent.VK_2) pressed2.trySet()
                    if (e.keyCode == KeyEvent.VK_3) pressed3.trySet()
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
    myFrame.setBounds(0, 0, FRAME_SIZE, FRAME_SIZE)
    myFrame.isVisible = true
    myFrame.contentPane = myPanel
//    startWave(4, (Math.random() * 30) + 10, Color.LIGHT_GRAY, 5)
    runBlocking {
        launch {
            while (true){
                delay(30)
                if(pressed3.booly){
                    pressed3.lockDown()
                    gamePaused = !gamePaused
                }else if(pressed2.booly) {
                    pressed2.lockDown()
                    if(showingmenu){
                        myFrame.contentPane = myPanel
                    }else{
                        myFrame.contentPane = menuPanel

                    }
                    showingmenu = !showingmenu
                    myFrame.revalidate()
                } else if (pressed1.booly) {
                    pressed1.lockDown()
                    revivePlayers()
                    startWave(4, (Math.random() * 30) + 10, Color.LIGHT_GRAY, 5)
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
    }
}
var gamePaused = false