package com.example.chefstationserver.CallBack

import com.example.chefstationserver.model.DiscountModel

interface IDiscountCallbackListener {

    fun onDiscountLoadSuccess(discountList:List<DiscountModel>)

    fun onDiscountLoadFailed(message:String)
}