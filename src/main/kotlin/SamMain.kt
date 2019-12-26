import com.studiohartman.jamepad.ControllerManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Font
import java.awt.Graphics
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JPanel
import javax.swing.WindowConstants

fun main() {
    val controllers = ControllerManager()
    controllers.initSDLGamepad()

    soundFiles[soundType.SHOOT] = longpewFil
    soundFiles[soundType.OUCH] = ouchnoiseFile
    soundFiles[soundType.DIE] = dienoiseFile
    soundFiles[soundType.LASER] = enemyPewFile
    soundFiles[soundType.SWAP] = swapnoiseFile

    for (i in 0..3) {
        if (controllers.getState(i).isConnected) players.add(Player())
    }

    placeMap(1, 0)

//    myFrame.createBufferStrategy(3)
//    myFrame.graphics.dispose()
//    myFrame.bufferStrategy.show()
    val rendChan = Channel<Boolean>()
    val otherChan = Channel<Boolean>()
    var firstFrame = true
    val myPanel: JPanel = object : JPanel() {
//            override fun getFont(): Font {
//                return fonfon
//            }

        override fun paint(g: Graphics) {
            if (firstFrame) {
                firstFrame = false
                g.font = Font("Courier", Font.BOLD, getWindowAdjustedPos(16.0).toInt())
                g.drawString("heyo", 50, 50)
            }
            g.drawImage(
                backgroundImage,
                0,
                0,
//                        getWindowAdjustedPos(INTENDED_FRAME_SIZE - (XMAXMAGIC / myFrame.width.toDouble())).toInt(),
                myFrame.width,
                myFrame.width,
                null
            )
            entsToDraw.forEach {
                it.drawEntity(g)
            }
            GlobalScope.launch {
                rendChan.send(true)
            }
        }
    }

//        myFrame.graphics.font = Font("Courier", Font.BOLD,getWindowAdjustedPos(16.0).toInt())
//        myPanel.graphics.font = Font("Courier", Font.BOLD,getWindowAdjustedPos(16.0).toInt())
//        myPanel.font = Font("Courier", Font.BOLD,getWindowAdjustedPos(16.0).toInt())
    myFrame.addWindowListener(object : WindowListener {
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
    myFrame.setBounds(0, 0, INTENDED_FRAME_SIZE, INTENDED_FRAME_SIZE + YFRAMEMAGIC)
    myFrame.isVisible = true

    playStrSound(soundType.SWAP)
    val updateTicker = ticker(delayMillis = 25, initialDelayMillis = 0)
    GlobalScope.launch {
        for(event in updateTicker){
//        while (frameNotClosing) {
            val pretime = System.currentTimeMillis()
            var pressed1contr = false
            var pressed2contr = false
            var pressed3contr = false
            controllers.update()
            for ((i, p1) in players.withIndex()) {
                val currState = controllers.getState(i)
                if (!currState.isConnected) continue
                p1.pCont.sht = currState.rb
                p1.pCont.Swp = currState.lbJustPressed
                p1.pCont.selUp = currState.xJustPressed
                p1.pCont.selDwn = currState.aJustPressed
                p1.pCont.selLeft = currState.lbJustPressed
                p1.pCont.selRight = currState.rbJustPressed
                p1.pCont.leftStickAngle = currState.leftStickAngle
                p1.pCont.leftStickMag = currState.leftStickMagnitude
                p1.pCont.rightStickAngle = currState.rightStickAngle
                p1.pCont.rightStickMag = currState.rightStickMagnitude
                if (currState.bJustPressed) {
                    pressed1contr = true
                }
                if (currState.startJustPressed) {
                    pressed2contr = true
                }
                if (currState.yJustPressed) {
                    pressed3contr = true
                }

            }
            if (pressed3contr) {
//                placeMap(map1,1,currentMapNum)
            } else if (pressed2contr) {
                gamePaused = !gamePaused
            } else if (pressed1contr) {
                startWave(4)
            } else if (changeMap) {
                changeMap = false
                placeMap(nextMapNum, currentMapNum)
            } else {
                if (!gamePaused) {
                    val preupdateEnts = allEntities.map { it.commonStuff.dimensions.copy() }
                    allEntities.forEach { entity: Entity ->
                        entity.updateEntity()
                    }
                    var timesTried = 0
                    do {
                        timesTried++
                        var triggeredReaction = false
                        val allSize = allEntities.size
                        for (dex in 0 until allSize) {
                            val ient = allEntities[dex]
                            if (ient is Player
//                                || ient is Enemy
                            ) {
                                for (j in (0) until allSize) {
                                    if (dex != j) {
                                        val jent = allEntities[j]
                                        if (jent.commonStuff.isSolid) {
                                            var collided = false
                                            if (!ient.commonStuff.toBeRemoved && !jent.commonStuff.toBeRemoved) {
                                                if (ient.commonStuff.dimensions.overlapsOther(jent.commonStuff.dimensions)) {
                                                    collided = true
                                                    blockMovement(ient, jent, preupdateEnts[dex], preupdateEnts[j])
                                                }
                                            }
                                            if (dex > j && collided && jent.commonStuff.dimensions.overlapsOther(
                                                    ient.commonStuff.dimensions
                                                )
                                            ) {
                                                if (ient.commonStuff.isSolid && jent.commonStuff.isSolid) {
                                                    if (timesTried > 5) {
                                                        println("Cannot resolve collision")
                                                    } else {
                                                        triggeredReaction = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } while (triggeredReaction)

                    allEntities.removeIf { it.commonStuff.toBeRemoved }
                    val combatants = mutableListOf<Entity>()
                    val noncombatants = mutableListOf<Entity>()
                    val bullets = mutableListOf<Entity>()
                    allEntities.forEach {
                        if (it is Player || it is Enemy){
                            combatants.add(it)
                            if(it is Player)combatants.addAll(it.menuStuff)
                        }
                        else if (it is Bullet) {
                            bullets.add(it)
                        } else noncombatants.add(it)
                    }
                    if (!rendChan.isEmpty) {
                        rendChan.receive()
                        entsToDraw.clear()
                        entsToDraw.addAll(noncombatants)
                        entsToDraw.addAll(combatants)
                        entsToDraw.addAll(bullets)
//                        for (player in players) {
//                            if (!player.notOnShop) {
//                                player.menuStuff.forEach {
//                                    it.updateEntity()
//                                    entsToDraw.add(it)
//                                }
//                            }
//                        }
                        otherChan.send(true)
                    }
                    if (entsToAdd.size > 0) allEntities.addAll(entsToAdd)
                    entsToAdd.clear()
                }
            }
//            val tickdiff = System.currentTimeMillis() - pretime
//            if (tickdiff < TARGET_UPDATE_RATE)
//                delay(TARGET_UPDATE_RATE - tickdiff)
        }
    }
    val tickerChannel = ticker(delayMillis = 30, initialDelayMillis = 0)
    GlobalScope.launch {
        for (event in tickerChannel){
            otherChan.receive()
            myPanel.repaint()
        }
//        var prepre:Long
//        while (frameNotClosing) {
//            prepre = System.currentTimeMillis()
//            otherChan.receive()
//            val tickdiff = System.currentTimeMillis() - prepre
//            if (tickdiff < TARGET_FRAME_RATE)
//                delay(TARGET_FRAME_RATE - tickdiff)
//            myPanel.repaint()
//        }
    }
}
