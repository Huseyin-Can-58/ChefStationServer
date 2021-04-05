package com.example.chefstationserver.ui.most_populars

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstationserver.CallBack.IBestDealCallbackListener
import com.example.chefstationserver.CallBack.IMostPopularCallbackListener
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Model.CategoryModel
import com.example.chefstationserver.model.BestDealsModel
import com.example.chefstationserver.model.MostPopularModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MostPopularViewModel : ViewModel(), IMostPopularCallbackListener {

    private  var mostPopularsListMutable : MutableLiveData<List<MostPopularModel>>?=null
    private  var messageError : MutableLiveData<String> = MutableLiveData()
    private  var mostPopularsCallBackListener : IMostPopularCallbackListener

    init{
        mostPopularsCallBackListener = this
    }

    fun getMostPopularsList() :MutableLiveData<List<MostPopularModel>>{

        if(mostPopularsListMutable == null){

            mostPopularsListMutable = MutableLiveData()
            loadMostPopulars()
        }

        return mostPopularsListMutable!!
    }

    fun loadMostPopulars() {
        val tempList = ArrayList<MostPopularModel>()
        val mostPopularsRef = FirebaseDatabase.getInstance().getReference(Common.MOST_POPULARS)
        mostPopularsRef.addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                mostPopularsCallBackListener.onListMostPopularLoadFailed((p0.message))
            }

            override fun onDataChange(p0: DataSnapshot) {

                for(itemSnapShot in p0.children)
                {

                    val model = itemSnapShot.getValue<MostPopularModel>(MostPopularModel::class.java)
                    model!!.key = itemSnapShot.key!!
                    tempList.add(model)
                }

                mostPopularsCallBackListener.onListMostPopularLoadSuccess(tempList)
            }



        })
    }

    fun getMessageError():MutableLiveData<String>{

        return messageError
    }

    override fun onListMostPopularLoadSuccess(mostPopularModels: List<MostPopularModel>) {
        mostPopularsListMutable!!.value = mostPopularModels
    }

    override fun onListMostPopularLoadFailed(message: String) {
        messageError.value = message
    }
}