package com.example.chefstationserver.CallBack

import com.example.chefstationserver.model.MostPopularModel

interface IMostPopularCallbackListener {

    fun onListMostPopularLoadSuccess(mostPopularModels:List<MostPopularModel>)
    fun onListMostPopularLoadFailed(message:String)
}