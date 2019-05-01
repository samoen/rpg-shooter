//import kotlinx.coroutines.*
//import java.awt.Canvas
//import java.awt.Dimension
//import java.awt.Graphics
//import java.awt.event.ActionEvent
//import java.awt.event.ActionListener
//import java.awt.event.KeyEvent
//import java.awt.event.KeyListener
//import javax.swing.JButton
//import javax.swing.JFrame
//import javax.swing.JLabel
//import javax.swing.JPanel
//
//class samshape(var posx:Int,var posy:Int,var size:Int)
//fun main(){
//    var sh1 = samshape(50,50,50)
//    var sh2 = samshape(0,0,30)
//
//    var upPressed = false
//
//    val jf = JFrame()
//    val jp = object:JPanel(){
//        override fun paint(g: Graphics?) {
//            super.paint(g)
////            g?.drawRect(sh1.posx,sh1.posy,sh1.size,sh1.size)
////            g?.drawRect(sh2.posx,sh2.posy,sh2.size,sh2.size)
//        }
//    }
//    jf.addKeyListener(object :KeyListener{
//        override fun keyTyped(e: KeyEvent?) {
//
//        }
//        override fun keyPressed(e: KeyEvent?) {
//            if(e?.keyCode==KeyEvent.VK_UP){
//                upPressed = true
//            }
//        }
//
//        override fun keyReleased(e: KeyEvent?) {
//            if(e?.keyCode==KeyEvent.VK_UP){
//                upPressed = false
//            }
//        }
//    })
//
//    jf.setBounds(500,500,500,500)
//    jf.contentPane = jp
//    jf.isVisible = true
//
//    runBlocking {
//        launch {
//            while (true){
//                jp.graphics.drawRect(sh1.posx,sh1.posy,sh1.size,sh1.size)
//                jp.revalidate()
//                delay(40)
//            }
//        }
////        val job1:Job = launch{
////            while(true){
////                println("TICK1")
////                sh2.posy+=1
////                jp.repaint()
////                delay(100)
////            }
////        }
////        val job2:Job = launch{
////            while(true){
////                println("TICK2")
////                if(upPressed){
////                    sh1.posy+=5
////                }
////                jp.repaint()
////                delay(200)
////            }
////        }
////        launch{
////            while(true){
////                println("TICK3")
////                    sh2.posy-=1
////                jp.repaint()
////                delay(200)
////            }
////        }
//    }
//}