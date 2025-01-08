package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.tools.file.*
import java.io.InputStream

class TemplateActivity : MainBaseActivity()  {

    // private val mHelper = VideoListHelper()
    private var msgDialog: AlertDialog? = null
    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_template)
        doSetContentView(R.layout.activity_create_template)
        initActionBar()
        invalidateOptionsMenu()

        var createBtn: Button = findViewById(R.id.create_template_btn)
        var cancelBtn: Button = findViewById(R.id.cancel_action)
        var txtTitle: EditText = findViewById(R.id.new_title)
        var txtLanguage: EditText = findViewById(R.id.new_language)

        txtTitle.setText(Workspace.activeStory.title)
        val reg = Workspace.registration
        txtLanguage.setText(reg.getString("language", ""))
        createBtn.setOnClickListener {
            if (txtTitle.text.toString() != Workspace.activeStory.title) {
                buildTemplate(txtTitle.text.toString(), txtLanguage.text.toString())
            } else {
                msgDialog = AlertDialog.Builder(this)
                        .setTitle(R.string.choose_unique_title)
                        .setMessage("")
                        .setPositiveButton(this.getString(R.string.ok)) { _, _ ->
                        }
                        .setCancelable(false)
                        .create()

                msgDialog?.show()
            }
        }
        cancelBtn.setOnClickListener {
            try {
                startActivity(Intent(this, ShareActivity::class.java))
                finish()
            }
            catch (ex : Throwable) {
                //ex.printStackTrace()
            }
        }

    }
    private fun buildTemplate(destStoryName: String, destLanguage: String) {
        val srcStoryName = Workspace.activeStory.title
        if (createNewTemplate(srcStoryName, destStoryName)) {
            // Read up new json and change it to use translated name, audio files, text (if present) and clear translated fields
            val destUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destStoryName"))
            //val pathFile = DocumentFile.fromTreeUri(this, destUri)!!
            val pathFile = DocumentFile.fromSingleUri(this, destUri)!!
            val story = storyFromJson(this, pathFile)!!
            story.title = destStoryName
            story.language = destLanguage
            story.lastPhaseType = PhaseType.LEARN
            story.lastSlideNum = 0
            story.isApproved = false
            story.outputVideos = ArrayList()
            val arrList = java.util.ArrayList<String>()
            arrList.add(story.learnAudioFile)
            deleteFileArray(arrList, destStoryName)
            story.learnAudioFile = ""
            story.localCredits = ""
            val count = story!!.slides.size
            for (slide in story!!.slides) {
                var narrationFolder: String = ""
                val segments = slide.narrationFile.split("/")
                if (segments.size > 1) {
                    narrationFolder = segments[0]
                }
                if (slide.translatedContent != "") {
                    slide.content = slide.translatedContent
                    slide.translatedContent = ""
                }
                if (slide.chosenTranslateReviseFile != "") {
                    if (slide.narrationFile != "") {
                        val deleteUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destStoryName/" + slide.narrationFile))
                        deleteFile(this, deleteUri)
                        Log.d("deleteNarration", "/$destStoryName/${slide.narrationFile}")
                    }
                    var destName = getFileNameFromCombined(slide.chosenTranslateReviseFile)
                    if (narrationFolder == "") {
                        slide.narrationFile = destName.replace("project/", "")
                    } else {
                        slide.narrationFile = destName.replace("project", narrationFolder)
                    }
                    val srcUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destStoryName/$destName"))
                    val destDirUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/${destStoryName}/${narrationFolder}"))
                    copyFile(this, srcUri, destDirUri)
                }
                deleteFileArray(slide.voiceStudioAudioFiles, destStoryName)
                deleteFileArray(slide.translateReviseAudioFiles, destStoryName)
                deleteFileArray(slide.communityWorkAudioFiles, destStoryName)
                deleteFileArray(slide.backTranslationAudioFiles, destStoryName)
                deleteFileArray(slide.accuracyCheckAudioFiles, destStoryName)

                deleteRecordingList(slide.draftRecordings, destStoryName)
                deleteRecordingList(slide.backTranslationRecordings, destStoryName)

                slide.isApproved = false
                slide.isChecked = false
                slide.voiceStudioAudioFiles = ArrayList()
                slide.translateReviseAudioFiles = ArrayList()
                slide.communityWorkAudioFiles = ArrayList()
                slide.backTranslationAudioFiles = ArrayList()
                slide.accuracyCheckAudioFiles = ArrayList()
                slide.chosenTranslateReviseFile = ""
                slide.chosenBackTranslationFile = ""
                slide.chosenVoiceStudioFile = ""
                slide.draftRecordings = RecordingList()
                slide.backTranslationRecordings = RecordingList()
            }
            story.toJson(this)
            initWorkspace()
        }
    }

    private fun getFileNameFromCombined(pName: String) : String {
        val stringArray: List<String> = pName.split("|")
        if (stringArray.size > 1) {
            return stringArray[1]
        } else {
            return pName
        }
    }

    private fun deleteRecordingList(fileList: RecordingList, destStoryName: String) {
        for (file1 in fileList.getFiles()) {
            var file2 = file1.fileName
            val deleteUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destStoryName/$file2"))
            deleteFile(this, deleteUri)
            Log.d("deleteRecording", "/$destStoryName/$file1")
        }
    }

    private fun deleteFileArray(fileList: MutableList <String>, destStoryName: String) {
        for (file1 in fileList) {
            var file2 = getFileNameFromCombined(file1)
            val deleteUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destStoryName/$file2"))
            deleteFile(this, deleteUri)
            Log.d("deleteFile", "/$destStoryName/$file1")
        }
    }

    private fun createNewTemplate(srcFolder : String, destFolder : String) : Boolean {
        val destUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$destFolder"))
        val srcUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$srcFolder"))
        val baseSrcUri = Workspace.workdocfile.uri
        if (fileExists(this, srcUri) && isDirectory(this, srcUri)) {

            if (!fileExists(this, destUri)) {
                // create the destination folder
                createFolder(this, Uri.parse(Workspace.workdocfile.uri.toString()), destFolder, false)
            }
            val children = getFolderChildren(this, baseSrcUri, srcFolder)
            for (child in children) {
                var relPath2 = "$srcFolder/$child"
                val newUri = Uri.parse(baseSrcUri.toString() + Uri.encode("/${relPath2}"))

                if (isDirectory(this, newUri)) {
                    // if this is another directory
                    // create a new directory uri and copy it
                    //copyFolderInternal(this, newUri, destUri, baseSrcUri, relPath2)
                    var destPath2 = "$destFolder/$child"
                    if (!createNewTemplate(relPath2, destPath2)) {
                        return false
                    }
                }
                else {
                    if (!copyFile(this, newUri, destUri)) {
                        return false
                    }
                }
            }
            return true
        }
        return true
    }


    private fun copyFolderTemplate(context : Context, srcUri : Uri, dstUri : Uri, baseSrcUri : Uri, relPath : String) : Boolean {

        if (!fileExists(context, dstUri)) {
            createFolder(context, dstUri, "", true)
        }

        if (fileExists(context, srcUri) && isDirectory(context, srcUri)) {

            val lastSegment = lastSegmentName(srcUri)
            val dstDirUri = Uri.parse(dstUri.toString() + Uri.encode("/$lastSegment"))

            if (!fileExists(context, dstDirUri)) {
                // create the destination folder
                createFolder(context, dstUri, lastSegment, false)
            }

            val children = getFolderChildren(context, baseSrcUri, relPath)
            for (child in children) {
                var relPath2 = relPath + "/" + child
                val newUri = Uri.parse(baseSrcUri.toString() + Uri.encode("/${relPath2}"))

                if (isDirectory(context, newUri)) {
                    // if this is another directory
                    // create a new directory uri and copy it
                    copyFolderTemplate(context, newUri, dstDirUri, baseSrcUri, relPath2)
                }
                else {
                    if (!copyFile(context, newUri, dstDirUri)) {
                        return false
                    }
                }
            }
            return false
        }
        return true
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId() : Int {
      return R.id.video_share
    }

    override fun getTitleString() : String? {
        return getString(R.string.video_share)
    }

    override fun getTitleColor2() : Int {
        return R.color.darkGray
    }

    override fun openHelpFile() : InputStream {
        return  Phase.openHelpDocFile(PhaseType.COPY_VIDEOS, Workspace.activeStory.language,this)
    }

    //Override setContentView to coerce into child view.
    fun doSetContentView(id: Int) {
        //val layout : LinearLayout = findViewById(R.id.linear_layout)
        //val inflater = layoutInflater
        //inflater.inflate(id, layout)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        //mHelper.checkActivityResult(request, result, data)
    }
}
