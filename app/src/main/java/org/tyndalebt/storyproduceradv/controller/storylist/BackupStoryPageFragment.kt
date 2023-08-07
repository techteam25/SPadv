package org.tyndalebt.storyproduceradv.controller.storylist

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.UriUtils
import org.tyndalebt.storyproduceradv.tools.file.copyFolderInternal
import org.tyndalebt.storyproduceradv.tools.file.deleteFolderInternal
import org.tyndalebt.storyproduceradv.tools.file.getFileType
import org.tyndalebt.storyproduceradv.tools.file.lastSegmentName


/**
 * BackupStoryPageFragment is subclass of StoryPageFragment that 
 * customizes the functionality for the BackupRestoreActivity page
 */
class BackupStoryPageFragment : StoryPageFragment() {

    lateinit var backupController: SelectBackupFolderController
    private var bSelectionMode = false

    companion object {
        const val ARG_POSITION = "position"

        /**
         * Creates a new instance based off of the tab position parameter
         * @param position The Tab Position
         */
        fun getInstance(position: Int): BackupStoryPageFragment {
            val BackupStoryPageFragment = BackupStoryPageFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_POSITION, position)
            BackupStoryPageFragment.arguments = bundle
            return BackupStoryPageFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = super.onCreateView(inflater, container, savedInstanceState)
        backupController = SelectBackupFolderController(activity as MainBaseActivity, activity!!, Workspace)
        setupButtons()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //setupButtons()
    }

    override fun getStoryListLayout() : Int {
        // the layout for the backup activity adds buttons for
        // backup/restore/delete functionality
        return R.layout.backup_story_list_container
    }

    override fun getAdapterRowLayout(): Int {
        // the layout for the backup activity uses a checkbox for selection
        // instead of the icon picture
        return R.layout.backup_story_list_item
    }

    override fun initItemClickListener() {
        // no item click listener for backup activity
    }

    override fun emptyStoryCheck(inflater: LayoutInflater, container: ViewGroup?) : View? {
        // story page has a button to go to select workspace folder
        // we do not want or need that for this case
        return null
    }

    // this method will take care of any special handling for the
    // view containing the file name for the backup activity vs the main
    // activity.  e.g. initializing the checkbox handling or the icon picture
    // for the file name.
    override fun initAdapterFileView(story : Story, holder : ListAdapter.FileHolder) {
        holder.txtTitle.setOnClickListener {
            var txtTitle : TextView = it as TextView
            textClicked(txtTitle)
        }
        holder.txtSubTitle.setOnClickListener {
            var txtSubTitle : TextView = it as TextView
            textClicked(txtSubTitle)
        }
        holder.checkBox!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            // if the user picks this checkbox, select all the mp4 files
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                var fileHolder : ListAdapter.FileHolder? = null
                if (adapter.mListFiles != null) {
                    for (i in 0 until adapter.mListFiles!!.size) {

                        var view = adapter.mListFiles!!.get(i)
                        if (view != null) {
                            var holder: ListAdapter.FileHolder = view!!.tag as ListAdapter.FileHolder
                            if (buttonView == holder.checkBox) {
                                fileHolder = holder
                                break
                            }
                        }
                    }
                }

                if (fileHolder != null) {
                    fileHolder.selected = isChecked
                    checkButtonState()
                }
            }
        })
    }

    // When the text is clicked, this will make sure that the
    // checkbox gets checked.
    fun textClicked(view : View) {

        var holder = getFileHolderFromTextView(view)
        if (holder != null) {
            if (holder!!.selected) {
                //holder.selected = false
                holder!!.checkBox!!.isChecked = false
            }
            else {
                //holder.selected = true
                holder!!.checkBox!!.isChecked = true
            }
        }
        //Toast.makeText(context!!,
        //        "onClick listener. Title = " + txtTitle.text + " SubTitle: " + txtSubTitle.text,
        //        Toast.LENGTH_LONG).show()
    }

    fun getFileHolderFromTextView(view : View) : ListAdapter.FileHolder? {
        var layout: LinearLayout = view.getParent() as LinearLayout
        var txtTitle: TextView = layout.getChildAt(0) as TextView
        var txtSubTitle: TextView = layout.getChildAt(1) as TextView
        return getFileHolder(txtTitle!!.text.toString(), txtSubTitle!!.text.toString())
    }

    fun getFileHolder(title : String, subTitle : String) : ListAdapter.FileHolder? {
        if (adapter.mListFiles != null) {
            for (i in 0 until adapter.mListFiles!!.size) {

                var view = adapter.mListFiles!!.get(i)
                if (view != null) {
                    var holder: ListAdapter.FileHolder = view!!.tag as ListAdapter.FileHolder
                    if (holder.txtTitle.text.equals(title) &&
                            holder.txtSubTitle.text.equals(subTitle)) {
                        return holder
                    }
                }
            }
        }
        return null
    }

    // Enables or disables the buttons based up the state of the checkboxes
    fun checkButtonState() {
        val bSelMode = isSelectionModeInternal()
        if (bSelMode != bSelectionMode) {
            bSelectionMode = bSelMode  // save it for next time

            var backupBtn: Button = lfView!!.findViewById(R.id.backup_btn)
            var deleteBtn: Button = lfView!!.findViewById(R.id.delete_btn)
            var restoreBtn: Button = lfView!!.findViewById(R.id.restore_btn)

            if (bSelMode) {
                if (!backupBtn.isEnabled()) {
                    backupBtn.setEnabled(true)
                    deleteBtn.setEnabled(true)
                    restoreBtn.setEnabled(false)
                }
            }
            else {
                if (backupBtn.isEnabled()) {
                    backupBtn.setEnabled(false)
                    deleteBtn.setEnabled(false)
                    restoreBtn.setEnabled(true)
                }
            }
        }
    }

    fun isSelectionModeInternal() : Boolean {
        return getSelectionCount() > 0
    }

    fun getSelectionCount(): Int {
        var retValue = 0
        val adapt = adapter as ListAdapter
        for (position in 0 until adapt!!.count) {
            val holder = adapt!!.getFileHolderAt(position)
            if ((holder != null) && holder!!.selected) {
                retValue++
            }
        }
        return retValue
    }

    // Sets up the backup/restore/delete buttons and the click listeners
    private fun setupButtons() {

        var backupBtn: Button = lfView!!.findViewById(R.id.backup_btn)
        var restoreBtn: Button = lfView!!.findViewById(R.id.restore_btn)
        var deleteBtn: Button = lfView!!.findViewById(R.id.delete_btn)

        backupBtn!!.setEnabled(false)
        deleteBtn!!.setEnabled(false)

        backupBtn.setOnClickListener {
            backupSelectedStories()
        }
        deleteBtn.setOnClickListener {
            deleteSelectedStories()
        }
        restoreBtn.setOnClickListener {
            restoreStories()
        }
    }

    fun backupSelectedStories() {
        if (isSelectionModeInternal()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity!!)
            if (!Workspace.storyBackupPath.uri.path.equals("/")) {

                val backupDirPath = UriUtils.getUIPathText(activity as MainBaseActivity, Workspace.storyBackupPath.uri)
                if (backupDirPath == null) {
                    // previous backup path was specified but it doesn't exist.
                    // Perhaps the usb drive has been removed.
                    // instruct the user to pick a new one
                    val pathStr = UriUtils.getUIPathTextAlways(activity as MainBaseActivity, Workspace.storyBackupPath.uri)
                    var textStr = "${activity!!.getString(R.string.backup_folder_not_found_message)}" + pathStr + "\n"

                    builder.setMessage(textStr)
                            .setNegativeButton(activity!!.getString(R.string.cancel), null)
                            .setPositiveButton(activity!!.getString(R.string.ok)) { _, _ ->
                                openFolderSelect(null)

                            }
                }
                else {
                    // previous backup folder exists, do a message box to ask to use
                    // the previous copy folder or pick a new one
                    var textStr = "${activity!!.getString(R.string.use_backup_folder_message)} " + backupDirPath + "\n"

                    builder.setMessage(textStr)
                            .setNegativeButton(activity!!.getString(R.string.change_backup_folder)) { _, _ ->
                                openFolderSelect(Workspace.storyBackupPath.uri)
                            }
                            .setPositiveButton(activity!!.getString(R.string.ok)) { _, _ ->
                                checkForExistingBackupFilesAndCopy()
                            }
                            .setNeutralButton(activity!!.getString(R.string.cancel), null)
                }
            }
            else {
                // no backup folder specified yet, instruct to pick a new one
                var textStr = "${activity!!.getString(R.string.backup_folder_not_found_message)}"

                builder.setMessage(textStr)
                        .setNegativeButton(activity!!.getString(R.string.cancel), null)
                        .setPositiveButton(activity!!.getString(R.string.ok)) { _, _ ->
                            openFolderSelect(null)
                        }
            }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    fun restoreStories() {

        var backupUri = null as Uri?
        if (Workspace.storyBackupPath != null) {
            backupUri = Workspace.storyBackupPath.uri
        }

        // Instruction message box - select the story to restore
        var textStr = "${activity!!.getString(R.string.restore_story_message)}"
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity!!)
        builder.setMessage(textStr)
                .setNegativeButton(activity!!.getString(R.string.cancel), null)
                .setPositiveButton(activity!!.getString(R.string.ok)) { _, _ ->

                    backupController.openDocumentTree(
                            SelectBackupFolderController.SELECT_RESTORE_FOLDER,
                            this, backupUri)
                }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    fun deleteSelectedStories() {
        if (isSelectionModeInternal()) {
            // Are you sure you want to delete selected folders?
            var textStr = activity!!.getString(R.string.delete_stories_message)
            if (backupWarningNeededForDelete()) {
                textStr += "\n\n" + activity!!.getString(R.string.backup_warning)
            }
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity!!)
            builder.setMessage(textStr)
                    .setNegativeButton(activity!!.getString(R.string.cancel), null)
                    .setPositiveButton(activity!!.getString(R.string.ok)) { _, _ ->
                        deleteStories(Workspace.workdocfile.uri)
                        updateStoriesAfterDelete()
                    }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    // This looks to see if the stories selected for delete exist
    // in the backup folder.  If not, this will allow a warning to
    // be displayed
    fun backupWarningNeededForDelete() : Boolean {

        // first update the workspace story list
        if (Workspace.storyBackupPath.uri.path.equals("/")) {
            return true  // backup directory not selected
        }
        val adapt = adapter as ListAdapter
        for (position in 0 until adapt!!.count) {
            val holder = adapt!!.getFileHolderAt(position)
            if (holder!!.selected) {
                // delete this story from the workspace story list
                val storyName = CurrentStoryList[position].title
                val dstUri = Uri.parse(Workspace.storyBackupPath.uri.toString() + Uri.encode("/$storyName"))
                if (getFileType(activity!!, dstUri) == null) {  // check if file exists
                    return true  // this backup does not currently exist
                }
            }
        }
        return false
    }

    // ensures that after the stories have been deleted,
    // display has been properly updated
    fun updateStoriesAfterDelete()  {

        // first update the workspace story list
        val adapt = adapter as ListAdapter
        var position = adapt!!.count - 1
        while (position >= 0) {
            val holder = adapt!!.getFileHolderAt(position)
            if (holder!!.selected) {
                // delete this story from the workspace story list
                val storyName = CurrentStoryList[position].title
                for (storyNo in 0 until Workspace.Stories.size) {
                    val story = Workspace.Stories.get(storyNo)
                    if (story.title.equals(storyName)) {
                        Workspace.Stories.removeAt(storyNo)
                        break
                    }
                }
            }
            position--
        }
        updateStoryListDisplay()
        checkButtonState()
        checkDownloadMessage()
    }

    // returns the time stamp data for story.json in the case
    // where we want to warn the user about existing files for overwrite
    // the user will be able to compare timestamps to see if he wants to continue
    fun getStoryFileTimeData(path : Uri, storyName : String) : String {
        val pathFile = DocumentFile.fromTreeUri(activity!!, path)!!
        val storyDir= pathFile.findFile(storyName)!!
        val projectDir = storyDir.findFile("project")!!
        val projectFile = projectDir.findFile("story.json")
        //return java.util.Date(projectFile!!.lastModified()).toString()
        return java.text.SimpleDateFormat(" MMM dd HH:mm:ss zzz yyyy").format(java.util.Date(projectFile!!.lastModified())).toString()

    }

    // ensures that after a story has been restored,
    // display has been properly updated (if a story has been added)
    fun updateStoriesAfterRestore(storyName : String) {

        val storyPath = Workspace.workdocfile.findFile(storyName)
        var story = Workspace.buildStory(activity!!, storyPath!!)
        if (story != null) {
            // first update the workspace story list
            var bFound = false
            val adapt = adapter as ListAdapter
            for (position in 0 until adapt!!.count) {
                //val storyName = CurrentStoryList[position].title
                for (storyNo in 0 until Workspace.Stories.size) {
                    val story = Workspace.Stories.get(storyNo)
                    if (story.title.equals(storyName)) {
                        // update the existing story
                        //Workspace.Stories.removeAt(storyNo)
                        Workspace.Stories.set(storyNo, story)
                        bFound = true
                        break
                    }
                }
            }

            if (!bFound) {
                Workspace.Stories.add(story!!)
                Workspace.sortStoriesByTitle()
            }
            updateStoryListDisplay()
            checkDownloadMessage()
        }
    }

    // Initially, if there is only a single story in the list then
    // the list will also include a message that you can download more
    // templates.  If we change the story list through restore or delete
    // this will see if we need to change the state of the download message
    fun checkDownloadMessage() {

        demoOnlyMsg = lfView.findViewById(R.id.demo_only_msg)
        if (Workspace.Stories.size <= 1) {
            demoOnlyMsg.text = getString(R.string.only_demo_present)
            demoOnlyMsg!!.visibility = View.VISIBLE
        }
        else {
            demoOnlyMsg!!.visibility = View.GONE
        }
    }

    fun checkForExistingBackupFiles(uri: Uri): String? {
        val adapt = adapter as ListAdapter
        for (position in 0 until adapt!!.count) {
            val holder = adapt!!.getFileHolderAt(position)
            if (holder!!.selected) {
                val storyName = CurrentStoryList[position].title
                val dstUri = Uri.parse(Workspace.storyBackupPath.uri.toString() + Uri.encode("/$storyName"))
                if (getFileType(activity!!, dstUri) != null) {  // check if file exists
                    if ((getSelectionCount() == 1) && !Workspace.isUnitTest) {
                        // if there is only one file selected, it will display the last
                        // modification times for the story.json files for comparison
                        var msg = activity!!.getString(R.string.overwrite_backup_message_single_file) +
                                "\n\n" + activity!!.getString(R.string.original_story_time_label) +
                                getStoryFileTimeData(Workspace.storyBackupPath.uri, storyName) +
                                "\n" + activity!!.getString(R.string.replace_story_time_label) +
                                getStoryFileTimeData(Workspace.workdocfile.uri, storyName)
                        return msg
                    }
                    else {
                        return activity!!.getString(R.string.overwrite_backup_message)
                    }
                }
            }
        }
        return null
    }

    internal fun checkForExistingBackupFilesAndCopy() {

        val msg = checkForExistingBackupFiles(Workspace.storyBackupPath.uri)
        if (msg != null) {
            val dialog = AlertDialog.Builder(activity!!)
                    .setTitle(activity!!.getString(R.string.overwrite_backup_title))
                    .setMessage(msg)
                    .setNegativeButton(activity!!.getString(R.string.no), null)
                    .setPositiveButton(activity!!.getString(R.string.yes)) { _, _ ->
                        deleteStories(Workspace.storyBackupPath.uri)
                        backupStories()
                    }
                    .create()

            dialog.show()
        }
        else {
            backupStories()
        }
    }

    fun deleteStories(dirUri: Uri) {
        if (isSelectionModeInternal()) {
            val adapt = adapter as ListAdapter
            for (position in 0 until adapt!!.count) {
                val holder = adapt!!.getFileHolderAt(position)
                if (holder!!.selected) {
                    val storyName = CurrentStoryList[position].title
                    val deleteUri = Uri.parse(dirUri.toString() + Uri.encode("/$storyName"))
                    deleteFolderInternal(activity!!, deleteUri, dirUri, storyName)
                }
            }
        }
    }


    fun restoreStory(srcUri: Uri) {

        // check for valid workdocfile
        if (Workspace.workdocfile.uri.path.equals("/") ||
                (getFileType(activity!!, Workspace.workdocfile.uri) == null)) {  // check if file exists
            return  // invalid workdocfile
        }

        // check for valid looking story uri, i.e. also contains the project folder
        // Should we also check for story.json?
        // Should we allow the user to also pick the project folder?
        val projectUri = Uri.parse(srcUri.toString() + Uri.encode("/project"))
        if ((getFileType(activity!!, srcUri) == null) ||
                (getFileType(activity!!, projectUri) == null)) {
            val dialog = AlertDialog.Builder(activity!!)
                    .setTitle(activity!!.getString(R.string.invalid_story_title))
                    .setMessage(activity!!.getString(R.string.invalid_story_message))
                    .setPositiveButton(activity!!.getString(R.string.ok), null)
                    .create()

            dialog.show()
            return
        }

        checkForExistingStoryAndRestore(srcUri)
    }

    fun backupStories() {
        if (isSelectionModeInternal()) {
            val adapt = adapter as ListAdapter
            for (position in 0 until adapt!!.count) {
                val holder = adapt!!.getFileHolderAt(position)
                if (holder!!.selected) {
                    val storyName = CurrentStoryList[position].title
                    val srcUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$storyName"))
                    copyFolderInternal(activity!!, srcUri, Workspace.storyBackupPath.uri, Workspace.workdocfile.uri, storyName)
                }
            }    // TODO - make sure copy target is not a subdir of copySrc, it shouldn't be, but it is better to check
        }
        clearSelection()
    }

    internal fun checkForExistingStoryAndRestore(srcUri: Uri) {

        val storyName = lastSegmentName(srcUri)
        val dstUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$storyName"))
        var needsWarning = false
        if (getFileType(activity!!, dstUri) != null) {  // check if story already exists in the workspace path

            // only warn about the overwrite if the story is inProgress or isComplete
            var story = null as Story?
            for (i in 0 until Workspace.Stories.size) {
                if (Workspace.Stories.get(i).title.equals(storyName)) {
                    story = Workspace.Stories.get(i)
                    break
                }
            }
            if ((story == null) || (story.inProgress || story.isComplete)) {
                needsWarning = true
            }
        }

        if (needsWarning) {
            val msg = activity!!.getString(R.string.overwrite_story_message) +
                    "\n\n" + activity!!.getString(R.string.original_story_time_label) +
                    getStoryFileTimeData(Workspace.workdocfile.uri, storyName) +
                    "\n" + activity!!.getString(R.string.replace_story_time_label) +
                    getStoryFileTimeData(Workspace.storyBackupPath.uri, storyName)

            val dialog = AlertDialog.Builder(activity!!)
                    .setTitle(activity!!.getString(R.string.overwrite_story_title))
                    .setMessage(msg)
                    .setNegativeButton(activity!!.getString(R.string.no), null)
                    .setPositiveButton(activity!!.getString(R.string.yes)) { _, _ ->

                        deleteFolderInternal(activity!!, dstUri, Workspace.workdocfile.uri, storyName)
                        doRestoreStory(srcUri, storyName)
                    }
                    .create()

            dialog.show()
            return
        }
        else {
            doRestoreStory(srcUri, storyName)
        }
    }

    fun doRestoreStory(srcUri : Uri, storyName : String) {
        // TODO - what if story is not on current backup path?
        copyFolderInternal(activity!!, srcUri, Workspace.workdocfile.uri, Workspace.storyBackupPath.uri, storyName)
        updateStoriesAfterRestore(storyName)
    }

    fun clearSelection(): Boolean {
        val adapt = adapter as ListAdapter
        for (position in 0 until adapt!!.count) {
            val holder = adapt!!.getFileHolderAt(position)
            if ((holder != null) && holder!!.selected) {
                selectRow(position, holder!!)
            }
        }
        checkButtonState()
        return false
    }

    fun selectRow(position: Int, holder: ListAdapter.FileHolder?) {
        if (holder!!.selected) {
            //holder!!.selected = false
            holder!!.checkBox!!.isChecked = false
        }
        else {
            //holder!!.selected = true
            holder!!.checkBox!!.isChecked = true
        }
        checkButtonState()
    }

    fun openFolderSelect(initUri : Uri?) {
        backupController.openDocumentTree(
                SelectBackupFolderController.SELECT_BACKUP_FOLDER,
                this, initUri)
    }

}
