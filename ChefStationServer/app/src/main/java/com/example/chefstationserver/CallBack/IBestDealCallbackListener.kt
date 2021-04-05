package com.example.chefstationserver.CallBack

import com.example.chefstationserver.model.BestDealsModel

interface IBestDealCallbackListener {

    fun onListBestDealsLoadSuccess(bestDealsModels:List<BestDealsModel>)
    fun onListBestDealsLoadFailed(message:String)
}