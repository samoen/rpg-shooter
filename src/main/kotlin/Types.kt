class ButtonSet(val up:Int,val down:Int,val left:Int,val right:Int,val swapgun:Int,val shoot:Int,val spinleft:Int,val spinright:Int)

class OneShotChannel(var locked:Boolean=false, var booly:Boolean=false){
    fun tryConsume():Boolean{
        if(booly){
            booly = false
            locked = true
            return true
        }else return false
    }

    fun tryProduce(){
        if(!locked){
            booly=true
        }
    }
    fun release(){
        locked = false
        booly = false
    }
}

class EntDimens(val xpos:Double,val ypos:Double,val drawSize:Double){
    fun getMidpoint():Pair<Double,Double>{
        return Pair((xpos+(drawSize/2)),ypos+(drawSize/2))
    }
    fun getMidY():Double{
        return ypos+(drawSize/2)
    }
    fun getMidX():Double{
        return xpos+(drawSize/2)
    }
}

class playControls(var up:OneShotChannel=OneShotChannel(), var dwm:OneShotChannel=OneShotChannel(), var sht:OneShotChannel=OneShotChannel(), var Swp:OneShotChannel=OneShotChannel(), var riri:OneShotChannel=OneShotChannel(), var leflef:OneShotChannel=OneShotChannel(), var spinri:OneShotChannel=OneShotChannel(), var spenlef:OneShotChannel=OneShotChannel())
