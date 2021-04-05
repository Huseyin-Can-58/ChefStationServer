package com.example.chefstationserver.ui.discount

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstationserver.Adapter.MyDiscountAdapter
import com.example.chefstationserver.CallBack.IMyButtonCallBack
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Common.MySwipeHelper
import com.example.chefstationserver.Eventbus.ToastEvent
import com.example.chefstationserver.R
import com.example.chefstationserver.model.DiscountModel
import com.google.firebase.database.FirebaseDatabase
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DiscountFragment : Fragment() {

    var recycler_discount:RecyclerView?=null
    var dialog: AlertDialog?=null
    var layoutAnimationController:LayoutAnimationController?=null
    var adapter:MyDiscountAdapter?=null
    var discountModelList:MutableList<DiscountModel>?=null
    private lateinit var viewModel: DiscountViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[DiscountViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_discount,container,false)
        initViews(root)
        viewModel.messageError.observe(viewLifecycleOwner, Observer {
            Toast.makeText(context,it,Toast.LENGTH_SHORT).show()
            dialog!!.dismiss()
        })
        viewModel.getDiscountMutableLiveData().observe(viewLifecycleOwner, Observer{
            dialog!!.dismiss()
            if(it == null)
                discountModelList = ArrayList<DiscountModel>()
            else
                discountModelList = it.toMutableList()
            adapter = MyDiscountAdapter(context!!,discountModelList!!)
            recycler_discount!!.adapter = adapter
            recycler_discount!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initViews(root: View?) {

        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        setHasOptionsMenu(true)
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context!!,R.anim.layout_item_from_left)
        recycler_discount = root!!.findViewById(R.id.recycler_discount) as RecyclerView
        val layoutManager = LinearLayoutManager(context!!)
        recycler_discount!!.layoutManager = layoutManager
        recycler_discount!!.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

        val swipeHelper: MySwipeHelper = object:MySwipeHelper(context!!,recycler_discount!!,200){

            override fun instantiateMyButton(viewHolder: RecyclerView.ViewHolder, buffer: MutableList<MyButton>) {

                buffer.add(MyButton(context!!,"Sil",30,0, Color.parseColor("#333639"),
                object: IMyButtonCallBack{
                    override fun onClick(pos: Int) {
                        Common.discountSelected = discountModelList!![pos]
                        showDeleteDialog()
                    }

                }))

                buffer.add(MyButton(context!!,"Güncelle",30,0, Color.parseColor("#414243"),
                        object: IMyButtonCallBack{
                            override fun onClick(pos: Int) {
                                Common.discountSelected = discountModelList!![pos]
                                showUpdateDialog()
                            }

                        }))

            }
        }

    }

    private fun showUpdateDialog() {

        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        val selectedDate = Calendar.getInstance()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)

        builder.setTitle("Güncelle")
        builder.setMessage("Lütfen gerekli bilgileri doldurunuz")
        val itemView = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_update_discount,null)

        val edt_code = itemView.findViewById<View>(R.id.edt_code) as EditText
        val edt_percent = itemView.findViewById<View>(R.id.edt_percent) as EditText
        val edt_valid = itemView.findViewById<View>(R.id.edt_valid) as EditText
        val img_calendar = itemView.findViewById<View>(R.id.pickDate) as ImageView

        //Set

        edt_code.setText(Common.discountSelected!!.key)
        edt_code.isEnabled = false
        edt_percent.setText(StringBuilder().append(Common.discountSelected!!.percent))
        edt_valid.setText(StringBuilder().append(simpleDateFormat.format(Common.discountSelected!!.untilDate)))

        //Event

        val listener = DatePickerDialog.OnDateSetListener{ view: DatePicker?, year:Int, month:Int, day:Int ->
            selectedDate.set(Calendar.YEAR,year)
            selectedDate.set(Calendar.MONTH,month)
            selectedDate.set(Calendar.DAY_OF_MONTH,day)
            edt_valid.setText(simpleDateFormat.format(selectedDate.time))
        }

        img_calendar.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                    requireContext(),listener,calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        builder.setView(itemView)
        builder.setNegativeButton("İPTAL",{dialogInterface, i ->
            dialogInterface.dismiss()
        })
        builder.setPositiveButton("GÜNCELLE",{dialogInterface, i ->
            val updateData:MutableMap<String,Any> = HashMap()
            updateData.put("percent",edt_percent.text.toString().toInt())
            updateData.put("untilDate",selectedDate.timeInMillis)
            updateDiscount(updateData)
        })

        val dialog = builder.create()
        dialog.show()
    }

    private fun showDeleteDialog() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Sil")
        builder.setMessage("Bu indirimi gerçekten silmek istiyor musunuz?")
        builder.setNegativeButton("İPTAL",{dialogInterface, i ->
            dialogInterface.dismiss()
        })
        builder.setPositiveButton("SİL",{dialogInterface, i ->
            deleteDiscount()
        })

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateDiscount(updateData: MutableMap<String, Any>) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DISCOUNT)
                .child(Common.discountSelected!!.key!!)
                .updateChildren(updateData)
                .addOnFailureListener { e ->
                    Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { e ->
                    viewModel!!.loadDiscount()
                    EventBus.getDefault().post(ToastEvent(Common.ACTION.UPDATE,true))
                }
    }

    private fun deleteDiscount() {

        FirebaseDatabase.getInstance()
                .getReference(Common.DISCOUNT)
                .child(Common.discountSelected!!.key!!)
                .removeValue()
                .addOnFailureListener { e ->
                    Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { e ->
                    viewModel!!.loadDiscount()
                    EventBus.getDefault().post(ToastEvent(Common.ACTION.DELETE,true))
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.discount_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_create)
            showAddDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun showAddDialog() {

        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        val selectedDate = Calendar.getInstance()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)

        builder.setTitle("Oluştur")
        builder.setMessage("Lütfen gerekli bilgileri doldurunuz")
        val itemView = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_update_discount,null)

        val edt_code = itemView.findViewById<View>(R.id.edt_code) as EditText
        val edt_percent = itemView.findViewById<View>(R.id.edt_percent) as EditText
        val edt_valid = itemView.findViewById<View>(R.id.edt_valid) as EditText
        val img_calendar = itemView.findViewById<View>(R.id.pickDate) as ImageView

        //Event

        val listener = DatePickerDialog.OnDateSetListener{ view: DatePicker?, year:Int, month:Int, day:Int ->
            selectedDate.set(Calendar.YEAR,year)
            selectedDate.set(Calendar.MONTH,month)
            selectedDate.set(Calendar.DAY_OF_MONTH,day)
            edt_valid.setText(simpleDateFormat.format(selectedDate.time))
        }

        img_calendar.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                    requireContext(),listener,calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        builder.setView(itemView)
        builder.setNegativeButton("İPTAL",{dialogInterface, i ->
            dialogInterface.dismiss()
        })
        builder.setPositiveButton("OLUŞTUR",{dialogInterface, i ->

            val discountModel = DiscountModel()
            discountModel.key = edt_code.text.toString().toLowerCase()
            discountModel.percent = edt_percent.text.toString().toInt()
            discountModel.untilDate = selectedDate.timeInMillis

            createDiscount(discountModel)
        })

        val dialog = builder.create()
        dialog.show()
    }

    private fun createDiscount(discountModel: DiscountModel) {

        FirebaseDatabase.getInstance()
                .getReference(Common.DISCOUNT)
                .child(discountModel!!.key!!)
                .setValue(discountModel)
                .addOnFailureListener { e ->
                    Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { e ->
                    viewModel!!.loadDiscount()
                    EventBus.getDefault().post(ToastEvent(Common.ACTION.CREATE,true))
                }

    }
}