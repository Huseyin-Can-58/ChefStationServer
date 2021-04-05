package com.example.chefstationserver.CallBack

import com.example.chefstationserver.Model.CategoryModel

interface ICategoryCallbackListener {

    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)

    fun onCategoryLoadFailed(message:String)
}