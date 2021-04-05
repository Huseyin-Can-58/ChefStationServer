package com.example.chefstationserver.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Model.FoodModel

class FoodListViewModel : ViewModel() {

    private var mutableFoodModelListData : MutableLiveData<List<FoodModel>>?=null

    fun getMutableFoodModelListData():MutableLiveData<List<FoodModel>>{

        if(mutableFoodModelListData == null)
            mutableFoodModelListData = MutableLiveData()
        mutableFoodModelListData!!.value = Common.categorySelected!!.foods
        return mutableFoodModelListData!!
    }
}