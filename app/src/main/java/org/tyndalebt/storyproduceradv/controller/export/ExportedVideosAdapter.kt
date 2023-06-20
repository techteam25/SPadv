package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.VIDEO_DIR
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.getWorkspaceUri

class ExportedVideosAdapter(private val listHelper: VideoListHelper) : BaseAdapter() {

    private var videoPaths: List<String> = ArrayList()
    private var mListViews: ArrayList<View?>? = null
    private val mInflater: LayoutInflater

    init {
        mInflater = listHelper.mActivity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    fun setVideoPaths(paths: List<String>) {
         val tempVideos : MutableList<String> = ArrayList()
        for (i in 0 until paths.size){
            if ((listHelper.mStory == null) || (paths[i] in listHelper.mStory!!.outputVideos)) {
                tempVideos.add(paths[i])
            }
        }
        // RK 2-13-23 - Added sort for longer lists
        tempVideos.sortBy { it.toString() }

        videoPaths = tempVideos
        mListViews = null  // reset the views list if it was being used
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return videoPaths.size
    }

    override fun getItem(position: Int): String {
        return videoPaths[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val path = videoPaths[position]

        //split the path so we can get just the file name witch will be used in the view
        val splitPath = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fileName = splitPath[splitPath.size - 1]

        if (listHelper.mbVideoUI) {
            if (mListViews == null) {
                // create the cache and initially null it out
                mListViews = ArrayList<View?>(videoPaths.size)
                for (i in 0 until videoPaths.size){
                   mListViews!!.add(null)
                }
            }
            var retVal = mListViews!!.get(position)
            if (retVal != null) {
                // if we have an existing value, use it
                return retVal
            }
        }


        //recreate the holder every time because the views are changing around
        var rowView : View? = null
        if (!listHelper.mbVideoUI) {
            rowView = mInflater.inflate(R.layout.exported_video_row, null)
        }
        else {
            rowView = mInflater.inflate(R.layout.main_video_row, null)
            mListViews!!.set(position, rowView)  // cache the view for use later
        }
        val holder = RowViewHolder()
        if (!listHelper.mbVideoUI) {
            holder.textView = rowView.findViewById(R.id.video_title)
            holder.textView!!.text = fileName
        }
        else {
            holder.checkBox = rowView.findViewById(R.id.video_title_cb)
            holder.checkBox!!.setOnCheckedChangeListener(listHelper)
            holder.checkBox!!.text = fileName
        }

        holder.shareButton = rowView.findViewById(R.id.file_share_button)
        holder.shareButton!!.setOnClickListener { showShareFileChooser(path, fileName) }
        if (!listHelper.mbVideoUI) {
            holder.playButton = rowView.findViewById(R.id.video_play_button)!!
            holder.deleteButton = rowView.findViewById(R.id.file_delete_button)

            holder.playButton!!.setOnClickListener { showPlayVideoChooser(path) }
            holder.deleteButton!!.setOnClickListener { showDeleteDialog(path) }
        }
        rowView.tag = holder

        return rowView
    }

    class RowViewHolder {
        var textView: TextView? = null
        var checkBox: CheckBox? = null
        var playButton: ImageButton? = null
        var shareButton: ImageButton? = null
        var deleteButton: ImageButton? = null
    }

    private fun showPlayVideoChooser(path: String) {
        val videoIntent = Intent(android.content.Intent.ACTION_VIEW)
        val uri = getWorkspaceUri("$VIDEO_DIR/$path")

        videoIntent.setDataAndNormalize(uri!!)
        videoIntent.putExtra(Intent.EXTRA_STREAM, uri)
        videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        listHelper.mActivity!!.startActivity(Intent.createChooser(videoIntent,
                listHelper.mActivity!!.getString(R.string.file_view)))
    }

    private fun showShareFileChooser(path: String, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
        shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, fileName)
        val uri = getWorkspaceUri("$VIDEO_DIR/$path")
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        //TODO replace with documentLaunchMode for the activity to make compliant with API 18
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        listHelper.mActivity!!.startActivity(Intent.createChooser(shareIntent,
                listHelper.mActivity!!.getString(R.string.send_video)))
    }

    private fun showDeleteDialog(path: String) {
        val dialog = AlertDialog.Builder(listHelper.mActivity)
                .setTitle(listHelper.mActivity!!.getString(R.string.delete_video_title))
                .setMessage(listHelper.mActivity!!.getString(R.string.delete_video_message))
                .setNegativeButton(listHelper.mActivity!!.getString(R.string.no), null)
                .setPositiveButton(listHelper.mActivity!!.getString(R.string.yes)) { _, _ ->
                    Workspace.deleteVideo(listHelper.mActivity!!, path)
                    listHelper.refreshViews()
                }
                .create()

        dialog.show()
    }
}


