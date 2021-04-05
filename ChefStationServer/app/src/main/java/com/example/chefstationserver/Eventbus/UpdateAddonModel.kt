package com.example.chefstationserver.Eventbus

import com.example.chefstationserver.Model.AddonModel

class UpdateAddonModel {

    var addonModelList: List<AddonModel>?=null
    constructor(){}
    constructor(sizeModelList:List<AddonModel>?){
        this.addonModelList = addonModelList
    }
}