package com.example.chefstationserver.Eventbus

import com.example.chefstationserver.Model.ShipperModel

class UpdateActiveEvent(var shipperModel: ShipperModel,var active:Boolean) {
}