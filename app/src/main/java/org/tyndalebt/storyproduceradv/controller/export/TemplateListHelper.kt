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
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.VIDEO_DIR
import org.tyndalebt.storyproduceradv.tools.file.UriUtils
import org.tyndalebt.storyproduceradv.tools.file.getChildDocuments
import org.tyndalebt.storyproduceradv.tools.file.getFileType
import java.io.File
import java.io.OutputStream


/**
 * Created by annmcostantino on 10/1/2017.
 */
class TemplateListHelper : RefreshViewListener, OnCheckedChangeListener {
    var mActivity: MainBaseActivity? = null
    var mStory: Story? = null

    private var mShareSection: LinearLayout? = null
    private var mNoTemplatesText: TextView? = null
    private var mVideosListView: ListView? = null

    private var templatesAdapter: ExportedTemplatesAdapter? = null

    private var bSelectionMode = false
    private var bSelectionModeMp4 = false
    private var bSelectionMode3gp = false

    //accordion variables
    private val sectionIds = intArrayOf(R.id.share_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)

    lateinit var copyController: SelectCopyFolderController


    fun initView(activity: MainBaseActivity, story: Story?) {
        mActivity = activity
        mStory = story

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = mActivity!!.findViewById(sectionIds[i])
        }

        copyController = SelectCopyFolderController(mActivity!!, mActivity!!, Workspace)

        //share view
        mShareSection = mActivity!!.findViewById(R.id.share_section)
        templatesAdapter = ExportedTemplatesAdapter(this)
        mVideosListView = mActivity!!.findViewById(R.id.videos_list)!!
        mVideosListView!!.adapter = templatesAdapter
        mVideosListView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        mNoTemplatesText = mActivity!!.findViewById(R.id.no_templates_text)

        val templatesCreated = buildTemplateList()

        var copyBtn: Button = mActivity!!.findViewById(R.id.copy_files)

        copyBtn!!.setEnabled(false)  // start off invisible until edit mode
        copyBtn!!.visibility = View.GONE  // Hide for now until we add logic to need it
        if (templatesCreated.isEmpty()) {
            copyBtn!!.visibility = View.GONE
        }

        // RK 6-13-2023 - See issue #75 for more details on the copy button
        copyBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(mActivity!!)
            if (!Workspace.videoCopyPath.uri.path.equals("/")) {

                val copyDirPath = UriUtils.getUIPathText(mActivity!! as MainBaseActivity, Workspace.videoCopyPath.uri)
                if (copyDirPath == null) {
                    // previous copy path was specified but it doesn't exist.
                    // Perhaps the usb drive has been removed.
                    // instruct the user to pick a new one

                    val pathStr = UriUtils.getUIPathTextAlways(mActivity!! as MainBaseActivity, Workspace.videoCopyPath.uri )
                    var textStr = "${mActivity!!.getString(R.string.copy_folder_not_found_message)}" + pathStr + "\n"
/*
                    builder.setMessage(textStr)
                            .setNegativeButton(mActivity!!.getString(R.string.cancel), null)
                            .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                                copyController.openDocumentTree(
                                        SelectCopyFolderController.SELECT_COPY_FOLDER,
                                        this, null)
                            }

*/
                }
                else {
                    // previous copy folder exists, do a message box to ask to use
                    // the previous copy folder or pick a new one
                    var textStr = "${mActivity!!.getString(R.string.use_copy_folder_message)} " + copyDirPath + "\n"
/*
                    builder.setMessage(textStr)
                            .setNegativeButton(mActivity!!.getString(R.string.change_copy_folder)) { _, _ ->
                                copyController.openDocumentTree(
                                        SelectCopyFolderController.SELECT_COPY_FOLDER,
                                        this, Workspace.videoCopyPath.uri)
                            }
                            .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                                checkForExistingFilesAndCopy(Workspace.videoCopyPath.uri)
                            }
                            .setNeutralButton(mActivity!!.getString(R.string.cancel), null)

*/
                }
            }
            else {
                // no copy folder specified yet, instruct to pick a new one
                var textStr = "${mActivity!!.getString(R.string.copy_folder_not_found_message)}"
/*
                builder.setMessage(textStr)
                        .setNegativeButton(mActivity!!.getString(R.string.cancel), null)
                        .setPositiveButton(mActivity!!.getString(R.string.ok)) { _, _ ->
                            copyController.openDocumentTree(
                                    SelectCopyFolderController.SELECT_COPY_FOLDER,
                                    this, null)
                        }

*/
            }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    fun getTemplatesAdapter(): ExportedTemplatesAdapter { // used by unit tests
        return templatesAdapter!!
    }

    fun getVideosListView(): ListView? { // used by unit tests
        return mVideosListView
    }

    fun buildTemplateList(): MutableList<String> {
        val folderTemplateList = getChildDocuments(mActivity!!, NEW_TEMPLATES_DIR)
        val templatesCreated: MutableList<String> = ArrayList()
        val basePath = getAbsolutePathFromDocumentUri(mActivity!!, Workspace.workdocfile.uri)
        for (i in 0 until folderTemplateList.size) {
            val templateFolder = basePath + "$NEW_TEMPLATES_DIR/${folderTemplateList[i]}"
            val dir = File(templateFolder)
            if (dir.isDirectory) {
                templatesCreated.add(folderTemplateList[i])
            }
        }
        if (templatesCreated.isNotEmpty()) {
            mNoTemplatesText!!.visibility = View.GONE
        }
        else {
            mNoTemplatesText!!.visibility = View.VISIBLE
        }
        templatesAdapter!!.setZipList(templatesCreated, basePath!!)
        return templatesCreated
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    override fun refreshViews() {
        buildTemplateList()
    }

    fun checkActivityResult(request: Int, result: Int, data: Intent?) {

        if (SelectCopyFolderController.SELECT_COPY_FOLDER_REQUEST_CODES.contains(request)) {
            copyController.onFolderSelected(request, result, data)
        }
    }

    private fun clearSelection() {
        for (ctr in 0 until templatesAdapter!!.count) {
            // val view = mVideosListView!!.getChildAt(ctr)
            val view = templatesAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            checkbox.isChecked = false
        }
    }

    fun getSelectedVideos(): List<String> {
        var videoPaths: ArrayList<String> = java.util.ArrayList()
        for (ctr in 0 until templatesAdapter!!.count) {
            // val view = mVideosListView!!.getChildAt(ctr)
            val view = templatesAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            if (checkbox.isChecked) {
                videoPaths.add(templatesAdapter!!.getItem(ctr))
            }
        }
        return videoPaths
    }


    // is any item selected?
    fun isSelectionMode(): Boolean {
        for (ctr in 0 until templatesAdapter!!.count) {
            // val view = mVideosListView!!.getChildAt(ctr)
            val view = templatesAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            if (checkbox.isChecked) {
                return true
            }
        }
        return false
    }

    // are all mp4 items selected?
    fun isSelectionModeMp4(): Boolean {
        var bFound = false
        for (ctr in 0 until templatesAdapter!!.count) {
            // val view = mVideosListView!!.getChildAt(ctr)
            val view = templatesAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            if (isFileMp4(checkbox.text.toString())) {
                bFound = true
                if (!checkbox.isChecked) {
                    return false
                }
            }
        }
        return bFound
    }

    // are all 3gp items selected?
    fun isSelectionMode3gp(): Boolean {
        var bFound = false
        for (ctr in 0 until templatesAdapter!!.count) {
            // val view = mVideosListView!!.getChildAt(ctr)
            val view = templatesAdapter!!.getView(ctr, mVideosListView, mVideosListView!!)
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            if (isFile3gp(checkbox.text.toString())) {
                bFound = true
                if (!checkbox.isChecked) {
                    return false
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

        bSelectionModeMp4 = isSelectionModeMp4()
        bSelectionMode3gp = isSelectionMode3gp()
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
