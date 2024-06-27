package tech.nimbbl.webviewsdk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

class PaymentCancelOptionsAdapter(
    var context: Context,
    var options: Array<String>
) : RecyclerView.Adapter<PaymentCancelOptionsAdapter.ViewHolder>() {

    private var selectedString: String = ""


     fun getSelectedOption(
    ):String{
        return selectedString
    }
    override fun getItemCount(): Int {
        return options.size
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.radioOption.text = option
        holder.radioOption.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
               selectedString =  holder.radioOption.text.toString()
            }
        }
    }

     inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var radioOption: CheckBox = itemView.findViewById<View>(R.id.radioOption) as CheckBox
     }

}