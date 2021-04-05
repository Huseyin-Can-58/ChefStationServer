package com.example.chefstationserver.CallBack

import com.example.chefstationserver.Model.OrderModel

interface IOrderCallbackListener {

    fun onOrderLoadSuccess(orderModel:List<OrderModel>)

    fun onOrderLoadFailed(message:String)
}