package com.example.chefstationserver.Eventbus

import com.example.chefstationserver.Model.SizeModel

class UpdateSizeModel {
    var sizeModelList: List<SizeModel>?=null
    constructor(){}
    constructor(sizeModelList:List<SizeModel>?){
        this.sizeModelList = sizeModelList
    }
}