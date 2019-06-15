
import com.studiohartman.jamepad.ControllerManager
import java.awt.Graphics
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JPanel
import javax.swing.WindowConstants

class SamMain{
    fun samMain() {

        val controllers = ControllerManager()
        controllers.initSDLGamepad()

        soundFiles[soundType.SHOOT] = longpewFil
        soundFiles[soundType.OUCH] = ouchnoiseFile
        soundFiles[soundType.DIE] = dienoiseFile
        soundFiles[soundType.LASER] = enemyPewFile
        soundFiles[soundType.SWAP] = swapnoiseFile

        players.add(Player())
        players.add(Player().also{
                it.commonStuff.dimensions.xpos=150.0
            })
//        entsToAdd.addAll(players)
        placeMap(map1,1,1)
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
        myFrame.addWindowListener(object:WindowListener{
            override fun windowClosing(e: WindowEvent?) {
                frameNotClosing = false
                controllers.quitSDLGamepad()
                System.out.println("closed!")
            }

            override fun windowDeiconified(e: WindowEvent?) {}

            override fun windowClosed(e: WindowEvent?) {

            }

            override fun windowActivated(e: WindowEvent?) {
            }

            override fun windowDeactivated(e: WindowEvent?) {
            }

            override fun windowOpened(e: WindowEvent?) {
            }

            override fun windowIconified(e: WindowEvent?) {
            }
        })
        myFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        myFrame.contentPane = myPanel
        myFrame.title = "Gunplay"
        myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, INTENDED_FRAME_SIZE+YFRAMEMAGIC)
        myFrame.isVisible = true

        playStrSound(soundType.SWAP)

        while (frameNotClosing){
            val pretime = System.currentTimeMillis()
            var pressed1contr : Boolean = false
            var pressed2contr : Boolean = false
            var pressed3contr : Boolean = false
            controllers.update()
            for((i,p1) in players.withIndex()){
                val currState = controllers.getState(i)
                if(!currState.isConnected)continue
                p1.pCont.sht.booly = currState.rb
                p1.pCont.Swp.booly = currState.lbJustPressed
                p1.pCont.selUp.booly = currState.rbJustPressed
                p1.pCont.selDwn.booly = currState.lbJustPressed
                p1.pCont.selLeft.booly = currState.xJustPressed
                p1.pCont.selRight.booly = currState.aJustPressed
                p1.pCont.leftStickAngle = currState.leftStickAngle
                p1.pCont.leftStickMag = currState.leftStickMagnitude
                p1.pCont.rightStickAngle = currState.rightStickAngle
                p1.pCont.rightStickMag = currState.rightStickMagnitude
                if(currState.bJustPressed){
                    pressed1contr = true
                }
                if(currState.startJustPressed){
                    pressed2contr = true
                }
                if(currState.yJustPressed){
                    pressed3contr = true
                }

            }
            if(pressed3.booly || pressed3contr){
                placeMap(map1,1,1)
            }else if(pressed2.booly || pressed2contr) {
                gamePaused = !gamePaused
            } else if (pressed1.booly || pressed1contr) {
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
                        val allSize = allEntities.size
                        for(dex in 0 until allSize) {
                            val ient = allEntities[dex]
                            if(ient is Player || ient is Enemy){
                                for(j in (0)until allSize){
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
                                                    if(timesTried > 5){
                                                        println("Cannot resolve collision")
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
}