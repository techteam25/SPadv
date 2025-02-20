package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.NEW_TEMPLATES_DIR
import java.io.File

class ExportedTemplatesAdapter(private val listHelper: TemplateListHelper) : BaseAdapter() {

    private var zipFiles: List<String> = ArrayList()
    private var templateFolders: List<String> = ArrayList()
    private var mListViews: ArrayList<View?>? = null
    private val mInflater: LayoutInflater

    init {
        mInflater = listHelper.mActivity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    fun setZipList(paths: List<String>, pBasePath: String) {
        val tempZip : MutableList<String> = ArrayList()
        for (i in paths.indices){
            val templateZip = pBasePath + "$NEW_TEMPLATES_DIR/${paths[i]}.zip"
            val zipFile = File(templateZip)
            if (zipFile.exists()) {
                tempZip.add(paths[i])
            } else {
                tempZip.add("")
            }
        }
        zipFiles = tempZip
        templateFolders = paths
        mListViews = null  // reset the views list if it was being used
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return zipFiles.size
    }

    override fun getItem(position: Int): String {
        return zipFiles[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val path = templateFolders[position]

        //split the path so we can get just the file name which will be used in the view
        val splitPath = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fileName = splitPath[splitPath.size - 1]

        if (mListViews == null) {
            // create the cache and initially null it out
            mListViews = ArrayList<View?>(zipFiles.size)
            for (i in templateFolders.indices){
               mListViews!!.add(null)
            }
        }
        var retVal = mListViews!!.get(position)
        if (retVal != null) {
            // if we have an existing value, use it
            return retVal
        }

        //recreate the holder every time because the views are changing around
        var rowView : View? = null

        rowView = mInflater.inflate(R.layout.created_template_row, null)
        mListViews!!.set(position, rowView)  // cache the view for use later

        val holder = RowViewHolder()
        holder.textView = rowView.findViewById(R.id.video_title)
        holder.textView!!.text = fileName

        holder.uploadButton = rowView.findViewById(R.id.file_upload_button)
        if (zipFiles[position] == "") {
            holder.uploadButton!!.visibility = View.GONE
        } else {
            holder.uploadButton!!.setOnClickListener {
                var msgDialog = AlertDialog.Builder(listHelper.mActivity!!)
                        .setTitle(R.string.upload_server)
                        .setMessage(R.string.template_upload_wait)
                        .setPositiveButton(listHelper.mActivity!!.getString(R.string.ok)) { _, _ ->
                            if (listHelper.mActivity!!.goForIt(fileName)) {
                                listHelper.buildTemplateList()
                            }
                        }
                        .setNegativeButton(listHelper.mActivity!!.getString(R.string.cancel)) { _, _ -> listHelper.mActivity!!.finish()}
                        .create()
                msgDialog.show()
            }
        }
        rowView.tag = holder

        return rowView
    }

    class RowViewHolder {
        var textView: TextView? = null
        var uploadButton: ImageButton? = null
    }
}


