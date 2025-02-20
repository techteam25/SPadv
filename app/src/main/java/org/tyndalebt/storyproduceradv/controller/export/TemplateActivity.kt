package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.tools.file.*

class TemplateActivity : MainBaseActivity()  {

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_template)
        initActionBar()
        invalidateOptionsMenu()

        var createBtn: Button = findViewById(R.id.create_template_btn)
        var cancelBtn: Button = findViewById(R.id.cancel_action)
        var txtTitle: EditText = findViewById(R.id.new_title)
        var lblNumber: TextView = findViewById(R.id.story_number)
        var destStoryName = ""

        var array = Workspace.activeStory.title.split(" ").toTypedArray()
        lblNumber.text = array[0]
        var newArray = array.drop(1).toTypedArray()
        destStoryName = newArray.joinToString(" ")

        txtTitle.setText(destStoryName)
        createBtn.setOnClickListener {
            if (txtTitle.text.toString() != destStoryName) {
                destStoryName = lblNumber.text.toString() + " " + txtTitle.text.toString()
                msgDialog = AlertDialog.Builder(this)
                        .setTitle("")
                        .setMessage(R.string.new_template_wait)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            buildTemplate(destStoryName)
                        }
                        .create()
                msgDialog.show()
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

    private fun buildTemplate(pDestStoryName: String) {
        val srcStoryName = Workspace.activeStory.title
        val destStoryName = "$NEW_TEMPLATES_DIR/$pDestStoryName"
        val destLanguage = Workspace.registration.getString("newLanguage")
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
            if (story.learnAudioFile != "") {
                arrList.add(story.learnAudioFile)
                deleteFileArray(arrList, destStoryName)
                story.learnAudioFile = ""
            }
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
            zipTemplate(this, Workspace.workdocfile.uri, destStoryName, "$destStoryName.zip")
            if (Build.VERSION.SDK_INT > 8) {
                val policy = ThreadPolicy.Builder()
                        .permitAll().build()
                StrictMode.setThreadPolicy(policy)
                msgDialog.dismiss()
                msgDialog = AlertDialog.Builder(this)
                        .setTitle(R.string.upload_server)
                        .setMessage(R.string.template_upload_wait)
                        .setPositiveButton(this.getString(R.string.ok)) { _, _ ->
                            if (goForIt(pDestStoryName)) {
                                this.finish()
                            }
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ -> this.finish()}
                        .create()
                msgDialog.show()
            }
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

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        //mHelper.checkActivityResult(request, result, data)
    }
}
