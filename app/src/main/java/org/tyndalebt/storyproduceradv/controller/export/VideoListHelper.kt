package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.VIDEO_DIR
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.UriUtils
import org.tyndalebt.storyproduceradv.tools.file.getChildDocuments
import org.tyndalebt.storyproduceradv.tools.file.getFileType
import org.tyndalebt.storyproduceradv.tools.file.getWorkspaceUri
import java.io.File
import java.io.OutputStream


/**
 * Created by annmcostantino on 10/1/2017.
 */
class VideoListHelper : RefreshViewListener, OnCheckedChangeListener {
    var mActivity: BaseActivity? = null
    var mStory: Story? = null
    var mbVideoUI = false
    var mbInCheckChanged = false

    private var mShareSection: LinearLayout? = null
    private var mNoVideosText: TextView? = null
    private var mVideosListView: ListView? = null

    private var videosAdapter: ExportedVideosAdapter? = null

    private var bSelectionMode = false
    private var bSelectionModeMp4 = false
    private var bSelectionMode3gp = false

    //accordion variables
    private val sectionIds = intArrayOf(R.id.share_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)

    lateinit var copyController: SelectCopyFolderController


    fun initView(activity: BaseActivity, story: Story?) {
        mActivity = activity
        mStory = story
        mbVideoUI = mStory == null

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = mActivity!!.findViewById(sectionIds[i])
        }

        copyController = SelectCopyFolderController(mActivity!!, mActivity!!, Workspace)

        //share view
        mShareSection = mActivity!!.findViewById(R.id.share_section)
        videosAdapter = ExportedVideosAdapter(this)
        mVideosListView = mActivity!!.findViewById(R.id.videos_list)!!
        mVideosListView!!.adapter = videosAdapter
        mVideosListView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        mNoVideosText = mActivity!!.findViewById(R.id.no_videos_text)

        val presentVideos = getChildDocuments(mActivity!!, VIDEO_DIR)
        val exportedVideos: MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size) {
            if ((mStory == null) || (presentVideos[i] in mStory!!.outputVideos)) {
                exportedVideos.add(presentVideos[i])
            }
        }

        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)

        if (mbVideoUI) {
            var copyBtn: Button = mActivity!!.findViewById(R.id.copy_files)
            var gpBtn: CheckBox = mActivity!!.findViewById(R.id.dumbphone_3gp)
            var mp4Btn: CheckBox = mActivity!!.findViewById(R.id.smartphone_mp4)

            copyBtn!!.setEnabled(false)  // start off invisible until edit mode

            if (!exportedVideos.isNotEmpty()) {
                gpBtn!!.visibility = View.GONE
                mp4Btn!!.visibility = View.GONE
                copyBtn!!.visibility = View.GONE
            }

            // RK 6-13-2023 - See issue #75 for more details on the copy button
            copyBtn.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(mActivity!!)
                if (!Workspace.videoCopyPath.uri.path.equals("/")) {

                    val copyDirPath = getUIPathText(Workspace.videoCopyPath.uri)
                    if (copyDirPath == null) {
                        // previous copy path was specified but it doesn't exist.
                        // Perhaps the usb drive has been removed.
                        // instruct the user to pick a new one
                        val docId = DocumentsContract.getDocumentId(Workspace.videoCopyPath.uri)
                        val pathData = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val pathStr = "[" + pathData.get(0) + "]/" + pathData.get(1)
                        var textStr = "${mActivity!!.getString(R.string.copy_folder_not_found_message)}" + pathStr + "\n"

                        builder.setMessage(textStr)
                                .setNegativeButton(mActivity!!.getString(R.string.cancel), null)
                                .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                                    copyController.openDocumentTree(
                                            SelectCopyFolderController.SELECT_COPY_FOLDER,
                                            this)
                                }
                    }
                    else {
                        // previous copy folder exists, do a message box to ask to use
                        // the previous copy folder or pick a new one
                        var textStr = "${mActivity!!.getString(R.string.use_copy_folder_message)} " + copyDirPath + "\n"

                        builder.setMessage(textStr)
                                .setNegativeButton(mActivity!!.getString(R.string.change_copy_folder)) { _, _ ->
                                    copyController.openDocumentTree(
                                            SelectCopyFolderController.SELECT_COPY_FOLDER,
                                            this)
                                }
                                .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                                    checkForExistingFilesAndCopy(Workspace.videoCopyPath.uri)
                                }
                                .setNeutralButton(mActivity!!.getString(R.string.cancel), null)
                    }
                }
                else {
                    // no copy folder specified yet, instruct to pick a new one
                    var textStr = "${mActivity!!.getString(R.string.copy_folder_not_found_message)}"

                    builder.setMessage(textStr)
                            .setNegativeButton(mActivity!!.getString(R.string.cancel), null)
                            .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                                copyController.openDocumentTree(
                                        SelectCopyFolderController.SELECT_COPY_FOLDER,
                                        this)
                            }
                }
                val alert: AlertDialog = builder.create()
                alert.show()
            }

            mp4Btn.setOnCheckedChangeListener(object : OnCheckedChangeListener {
                // if the user picks this checkbox, select all the mp4 files
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    if (!mbInCheckChanged) {
                        for (ctr in 0 until videosAdapter!!.count) {
                            // val view = mVideosListView!!.getChildAt(ctr)
                            val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                            if (isFileMp4(checkbox.text.toString())) {
                                checkbox.setChecked(isChecked)
                            }
                        }
                    }
                }
            })

            gpBtn.setOnCheckedChangeListener(object : OnCheckedChangeListener {
                // if the user picks this checkbox, select all the 3gp files
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    if (!mbInCheckChanged) {
                        for (ctr in 0 until videosAdapter!!.count) {
                            // val view = mVideosListView!!.getChildAt(ctr)
                            val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                            if (isFile3gp(checkbox.text.toString())) {
                                checkbox.setChecked(isChecked)
                            }
                        }
                    }
                }
            })
        }
        else {
            var videoBtn: Button = mActivity!!.findViewById(R.id.copy_share_btn)
            var gpText: TextView = mActivity!!.findViewById(R.id.dumbphone_3gp)
            var mp4Text: TextView = mActivity!!.findViewById(R.id.smartphone_mp4)

            if (!exportedVideos.isNotEmpty()) {
                gpText!!.visibility = View.GONE
                mp4Text!!.visibility = View.GONE
                videoBtn!!.visibility = View.GONE
            }

            videoBtn.setOnClickListener {
                mActivity!!.showVideos()
            }
        }
    }

    fun getVideosAdapter(): ExportedVideosAdapter { // used by unit tests
        return videosAdapter!!
    }

    fun getVideosListView(): ListView? { // used by unit tests
        return mVideosListView
    }

    fun getUIPathText(uri: Uri?): String? {
        // translates the uri path to a ui string to display for the copy folder
        var uriStr = UriUtils.getPathFromUri(mActivity, uri!!)
        if (uriStr != null) {
            uriStr = getUIPathTextInternal(uri, uriStr)
            var uiUri = Uri.parse(uriStr)
            return uiUri.path!!
        }
        return null
    }

    fun getUIPathTextInternal(uri: Uri, uriStr: String): String {

        // At this point the videoFileUriStr will look something like this: /storage/emulated/0/
        // This is the actual path. However, it needs be changed to the SD Card (/sdcard/)
        // which is a symbolic link to the emulated storage path.
        // sdcard/: Is a symlink to...
        //      /storage/sdcard0 (Android 4.0+)
        // In Story Publisher Adv, the version will never be less than Android 4.0
        // We will instead show it as an optional [sdcard]
        // The below code will change: /storage/emulated/0/ to /storage/[sdcard]/
        val replaceStr = getStorageText(uri, uriStr)
        var retVal = uriStr.replace(Regex("(/storage\\/emulated\\/)\\d+"), replaceStr)

        // Also, the SD-Card could show up as /storage/####-####/ where # is a hexidecimal value
        retVal = retVal.replace(Regex("(/storage)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"), replaceStr)

        // Also, the SD-Card could show up as /mnt/media_rw/####-####/ where # is a hexidecimal value for earlier android releases
        retVal = retVal.replace(Regex("(/mnt/media_rw)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"), replaceStr)

        // Also, this is for a usb memory stick
        retVal = retVal.replace(Regex("(/dev/bus/usb)\\/[0-9]{3}\\/[0-9]{3}"), replaceStr)
        return retVal
    }

    fun getStorageText(uri: Uri, uriStr: String): String {
        val segment = uri.lastPathSegment
        val isPrimary = (segment!!.indexOf("primary") == 0) ||
                (segment!!.indexOf("raw") == 0)
        if (isPrimary) {
            return "[" + mActivity!!.getString(R.string.internal) + "]"
        }
        else if (uriStr.indexOf("usb") >= 0) {
            return "[" + mActivity!!.getString(R.string.external) + "]"
        }
        return "[" + mActivity!!.getString(R.string.sdcard) + "]"
    }


    /**
     * Get handles to all necessary views and add some listeners.
     */
    override fun refreshViews() {

        val presentVideos = getChildDocuments(mActivity!!, VIDEO_DIR)
        val exportedVideos: MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size) {
            if ((mStory == null) || (presentVideos[i] in mStory!!.outputVideos)) {
                exportedVideos.add(presentVideos[i])
            }
        }

        //If the file has been deleted, remove it.
        if (mStory != null) {
            val toRemove = mutableListOf<Int>()
            for (i in 0 until mStory!!.outputVideos.size) {
                if (mStory!!.outputVideos[i] !in presentVideos) {
                    toRemove.add(0, i) //add at beginning
                }
            }
            for (i in toRemove) {
                mStory!!.outputVideos.removeAt(i)
            }
        }

        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }
        else {
            mNoVideosText!!.visibility = View.VISIBLE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)
    }

    fun checkActivityResult(request: Int, result: Int, data: Intent?) {

        if (SelectCopyFolderController.SELECT_COPY_FOLDER_REQUEST_CODES.contains(request)) {
            copyController.onFolderSelected(request, result, data)
        }
    }

    fun clearSelection() {
        if (mbVideoUI) {  // only for video page, not share page?
            for (ctr in 0 until videosAdapter!!.count) {
                // val view = mVideosListView!!.getChildAt(ctr)
                val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                checkbox.setChecked(false)
            }
        }
    }

    fun getSelectedVideos(): List<String> {
        var videoPaths: ArrayList<String> = java.util.ArrayList()
        if (mbVideoUI) {  // only for video page, not share page?
            for (ctr in 0 until videosAdapter!!.count) {
                // val view = mVideosListView!!.getChildAt(ctr)
                val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                if (checkbox.isChecked) {
                    videoPaths.add(videosAdapter!!.getItem(ctr))
                }
            }
        }
        return videoPaths
    }


    // is any item selected?
    fun isSelectionMode(): Boolean {
        if (mbVideoUI) {  // only for video page, not share page?
            for (ctr in 0 until videosAdapter!!.count) {
                // val view = mVideosListView!!.getChildAt(ctr)
                val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                if (checkbox.isChecked) {
                    return true
                }
            }
        }
        return false
    }

    // are all mp4 items selected?
    fun isSelectionModeMp4(): Boolean {
        var bFound = false
        if (mbVideoUI) {  // only for video page, not share page?
            for (ctr in 0 until videosAdapter!!.count) {
                // val view = mVideosListView!!.getChildAt(ctr)
                val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                if (isFileMp4(checkbox.text.toString())) {
                    bFound = true
                    if (!checkbox.isChecked) {
                        return false
                    }
                }
            }
        }
        return bFound
    }

    // are all 3gp items selected?
    fun isSelectionMode3gp(): Boolean {
        var bFound = false
        if (mbVideoUI) {  // only for video page, not share page?
            for (ctr in 0 until videosAdapter!!.count) {
                // val view = mVideosListView!!.getChildAt(ctr)
                val view = videosAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
                val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
                if (isFile3gp(checkbox.text.toString())) {
                    bFound = true
                    if (!checkbox.isChecked) {
                        return false
                    }
                }
            }
        }
        return bFound
    }

    // when an item is selected, check to see how that affects other ui elements
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val bSelMode = isSelectionMode()
        if (bSelMode != bSelectionMode) {
            bSelectionMode = bSelMode  // save it for next time

            //var openPathBtn : Button = mActivity!!.findViewById(R.id.open_videos_path)
            var copyBtn: Button = mActivity!!.findViewById(R.id.copy_files)

            if (bSelMode) {
                if (!copyBtn.isEnabled()) {
                    copyBtn.setEnabled(true)
                }
            }
            else {
                if (copyBtn.isEnabled()) {
                    copyBtn.setEnabled(false)
                }
            }
        }

        var gpBtn: CheckBox = mActivity!!.findViewById(R.id.dumbphone_3gp)
        var mp4Btn: CheckBox = mActivity!!.findViewById(R.id.smartphone_mp4)

        bSelectionModeMp4 = isSelectionModeMp4()
        bSelectionMode3gp = isSelectionMode3gp()
        try {
            mbInCheckChanged = true
            if (mp4Btn.isChecked() != bSelectionModeMp4) {
                mp4Btn.setChecked(bSelectionModeMp4)
            }
            if (gpBtn.isChecked() != bSelectionMode3gp) {
                gpBtn.setChecked(bSelectionMode3gp)
            }
        }
        finally {
            mbInCheckChanged = false
        }

    }

    fun isFileMp4(fileName: String): Boolean {
        return fileName.endsWith(".mp4")
    }

    fun isFile3gp(fileName: String): Boolean {
        return fileName.endsWith(".3gp")
    }

    internal fun getVideoFileUri(videoName: String): Uri {
        return Uri.parse(Workspace.workdocfile.uri.toString() +
                Uri.encode("/$VIDEO_DIR/$videoName"))
    }

    fun checkForExistingFiles(uri : Uri) : Boolean {
        val selectedVideos = getSelectedVideos()
        for (ctr in 0 until selectedVideos!!.size)
        {
            val videoUriOut = getVideoFileUriOutput(uri, selectedVideos!!.get(ctr))
            if (getFileType(mActivity!!, videoUriOut) != null) {  // check if file exists
                return true
            }
        }
        return false
    }

    internal fun checkForExistingFilesAndCopy(uri : Uri) {

        if (checkForExistingFiles(uri)) {
            val dialog = AlertDialog.Builder(mActivity!!)
                    .setTitle(mActivity!!.getString(R.string.overwrite_video_title))
                    .setMessage(mActivity!!.getString(R.string.overwrite_video_message))
                    .setNegativeButton(mActivity!!.getString(R.string.no), null)
                    .setPositiveButton(mActivity!!.getString(R.string.yes)) { _, _ ->
                        copyVideos(uri)
                        //dismiss()
                    }
                    .create()

            dialog.show()
        }
        else {
            copyVideos(uri)
        }
    }

    fun getVideoFileUriOutput(uri : Uri, videoName : String) : Uri {
        return Uri.parse(uri.toString() +
                Uri.encode("/$videoName"))
    }

    fun copyVideos(uri : Uri) {
        var outDir : DocumentFile? = null
        try {
            outDir = DocumentFile.fromTreeUri(mActivity!!, uri)
        }
        catch (ex : Throwable) {
            if (Workspace.isUnitTest) {
                outDir = DocumentFile.fromSingleUri(mActivity!!, uri)
            }
            else {
                throw ex
            }
        }
        val selectedVideos = getSelectedVideos()
        for (ctr in 0 until selectedVideos!!.size) {
            copyVideo(selectedVideos!!.get(ctr), outDir!!)
        }
        clearSelection()
    }

    internal fun copyVideo(videoName : String, outDir : DocumentFile) {
        try {
            val videoUri  = getVideoFileUri(videoName)
            val videoUriOut  = getVideoFileUriOutput(outDir.uri, videoName)

            val ipfd = mActivity!!.contentResolver.openFileDescriptor(
                    videoUri, "r")
            val instream = ParcelFileDescriptor.AutoCloseInputStream(ipfd)


            // TODO: refactor FileIO.getPFD() to be able to reuse that code
            //       Currently it assumes it is contained in the Workspace directory
            //       and this is not.
            var pfd: ParcelFileDescriptor? = null
            var outstream: OutputStream? = null
            try {
                // if file does not exist, create it
                // if we came from the directory dialog, we have permission to write.
                // if not, then we need to get permission to write
                if (getFileType(mActivity!!, videoUriOut) == null) {

                    var mType = getFileType(mActivity!!, videoUri)
                    try {
                        DocumentsContract.createDocument(mActivity!!.contentResolver, outDir.uri, mType!!, videoName)
                    }
                    catch (e: Exception) {
                        if (Workspace.isUnitTest) {
                            // RK 3-27-2023
                            // Directory create will throw because of incompatibility in contentResolver
                            // in Robolectric test runner.  Create the directory the old fashioned way
                            // See TestDownloadActivity
                            val file = File(videoUriOut.path)
                            if (!file.createNewFile())
                                throw e
                        }
                        else
                            throw e
                    }
                }

                try {
                    pfd = mActivity!!.contentResolver.openFileDescriptor(videoUriOut, "w")
                }
                catch(e:java.lang.Exception){
                    if (Workspace.isUnitTest) {  // do we realy neeed this one?
                        // manually create the file
                        val file = File(videoUriOut.path)
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                    else
                        FirebaseCrashlytics.getInstance().recordException(e)
                }

                outstream = ParcelFileDescriptor.AutoCloseOutputStream(pfd)
            } catch (ex : java.lang.Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
            }

            val buffer = ByteArray(1024)
            var read: Int
            while (instream.read(buffer).also { read = it } != -1) {
                outstream!!.write(buffer, 0, read)
            }
            outstream?.close()
            instream.close()

        } catch (ex: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(ex)

        }
    }
}


interface RefreshViewListener {
    fun refreshViews()
}
