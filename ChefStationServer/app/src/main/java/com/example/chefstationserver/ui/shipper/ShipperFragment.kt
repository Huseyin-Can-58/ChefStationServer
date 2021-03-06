package com.example.chefstationserver.ui.shipper

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstationserver.Adapter.MyShipperAdapter
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Eventbus.ChangeMenuClick
import com.example.chefstationserver.Eventbus.UpdateActiveEvent
import com.example.chefstationserver.Model.ShipperModel
import com.example.chefstationserver.R
import com.google.firebase.database.FirebaseDatabase
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ShipperFragment : Fragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyShipperAdapter?=null
    private var recycler_shipper: RecyclerView?=null

    internal var shipperModels : List<ShipperModel> = ArrayList<ShipperModel>()
    internal var saveBeforeSearchList : List<ShipperModel> = ArrayList<ShipperModel>()

    companion object {
        fun newInstance() = ShipperFragment()
    }

    private lateinit var viewModel: ShipperViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val itemView = inflater.inflate(R.layout.fragment_shipper, container, false)
        viewModel = ViewModelProviders.of(this).get(ShipperViewModel::class.java)
        initViews(itemView)
        viewModel.getMessageError().observe(this,Observer {
            Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })

        viewModel.getShipperList().observe(this, Observer {
            dialog.dismiss()
            shipperModels = it
            if(saveBeforeSearchList.size == 0)
                saveBeforeSearchList = it
            adapter = MyShipperAdapter(context!!,shipperModels)
            recycler_shipper!!.adapter = adapter
            recycler_shipper!!.layoutAnimation = layoutAnimationController
        })
        return itemView
    }

    private fun initViews(root: View) {

        setHasOptionsMenu(true)

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        recycler_shipper = root.findViewById(R.id.recycler_shipper) as RecyclerView
        recycler_shipper!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recycler_shipper!!.layoutManager = layoutManager
        recycler_shipper!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.food_list_menu,menu)

        // Arama g??r??n??m??n?? olu??turma
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName!!))
        //Event
        searchView.setOnQueryTextListener(object:androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(search: String?): Boolean {
                startSearchFood(search!!)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }


        })
        // Arama metnini temizleme
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener{
            val tx = searchView.findViewById<View>(R.id.search_src_text) as EditText
            // Metni temizleme
            tx.setText("")
            // Kuyru??u temizleme
            searchView.setQuery("",false)
            searchView.onActionViewCollapsed()
            menuItem.collapseActionView()
            viewModel.getShipperList().value = saveBeforeSearchList
        }
    }

    private fun startSearchFood(s: String) {

        val resultShipper : MutableList<ShipperModel> = ArrayList()
        for(i in shipperModels.indices){

            val shipperModel = shipperModels[i]
            if(shipperModel.phone!!.toLowerCase().contains(s.toLowerCase()) || shipperModel.name!!.toLowerCase().contains(s.toLowerCase())){

                // Sonu??lar??n index de??erleri d??necek
                resultShipper.add(shipperModel)
            }
        }
        // Arama sonu??lar??n?? g??ncelleme

        viewModel!!.getShipperList().value = resultShipper
    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(UpdateActiveEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateActiveEvent::class.java)
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onUpdateActiveEvent(updateActiveEvent: UpdateActiveEvent){

        var status:String

        if (updateActiveEvent.active == true){

            status = "aktif"
        } else {

            status = "pasif"
        }

        val updateData = HashMap<String,Any>()
        updateData.put("active",updateActiveEvent.active)
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPER_REF)
            .child(updateActiveEvent.shipperModel!!.key!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnSuccessListener { aVoid ->
                Toast.makeText(context,"Durum "+status+" olarak g??ncellendi",Toast.LENGTH_SHORT).show()
            }
    }

}