package org.tyndalebt.storyproduceradv.model

import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.apache.commons.io.IOUtils
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.remote.UploadAudioButtonManager
import org.tyndalebt.storyproduceradv.controller.remote.sendProjectSpecificRequest
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksActivity
import org.tyndalebt.storyproduceradv.tools.file.getChildInputStream
import org.tyndalebt.storyproduceradv.tools.file.getChosenCombName

/**
 * A list of all the word links (used for saving all word links in a single file)
 **/
@JsonClass(generateAdapter = true)
class WordLinkList (val wordLinks: List<WordLink>) {
    companion object
}

@JsonClass(generateAdapter = true)
data class WordLinkRecording (
    var audioRecordingFilename : String = "",
    var textBackTranslation : String = "",
    var isTextBackTranslationSubmitted: Boolean = false) {
        companion object
}

// RK 12/28/23: persists the upload state for issue #111
enum class WordLinkUploadState {
    @Json(name="Uploaded") UPLOADED,
    @Json(name="UploadNeeded") UPLOAD_NEEDED,
    @Json(name="NotUploaded") NOT_UPLOADED
}

@JsonClass(generateAdapter = true)
data class WordLink (
        var term: String = "",
        var termForms: List<String> = listOf(),
        var alternateRenderings: List<String> = listOf(),
        var explanation: String = "",
        var relatedTerms: List<String> = listOf(),
        var wordLinkRecordings: MutableList<WordLinkRecording> = mutableListOf(),
        var uploadState: WordLinkUploadState = WordLinkUploadState.NOT_UPLOADED,
        var chosenWordLinkFile: String = "") {
    companion object
}

/**
 * Takes a string and returns a spannable string with links to open wordlink Activity
 *
 * @param string The string that contains wordlinks
 * @param fragmentActivity The current activity
 * @return A spannable string
 **/
fun stringToWordLink (string: String, fragmentActivity: FragmentActivity?) : SpannableString {
    val spannableString = SpannableString(string)
    if (Workspace.termFormToTermMap.containsKey(string.toLowerCase())) {
        val clickableSpan = createWordLinkClickableSpan(string, fragmentActivity)
        spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannableString
}

/**
 * Converts a string to a clickableSpan that will open the WordLinksActivity when clicked
 *
 * @param term the wordlink
 * @param fragmentActivity the current activity
 * @return The link to open the wordlink
 **/
private fun createWordLinkClickableSpan(term: String, fragmentActivity: FragmentActivity?): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS && fragmentActivity is WordLinksActivity) {
                fragmentActivity.replaceActivityWordLink(term)
            }
            else if (Workspace.activePhase.phaseType != PhaseType.WORD_LINKS) {
                //Start a new word links activity and keep a reference to the parent phase
                val intent = Intent(fragmentActivity, WordLinksActivity::class.java)
                intent.putExtra(PHASE, Workspace.activePhase.phaseType)
                intent.putExtra(WORD_LINKS_CLICKED_TERM, term)
                fragmentActivity?.startActivity(intent)
            }
        }

        override fun updateDrawState(drawState: TextPaint) {
            val wordLink = Workspace.termToWordLinkMap[Workspace.termFormToTermMap[term.toLowerCase()]]
            val hasRecording = wordLink?.wordLinkRecordings?.isNotEmpty()

            if(hasRecording != null && hasRecording){
                drawState.linkColor = ContextCompat.getColor(fragmentActivity!!.applicationContext, R.color.lightGray)
            }
            super.updateDrawState(drawState)
        }
    }
}

// RK 12/28/23: Update the wordlink upload state (if remote/ROCC access)
fun setWordLinkUploadState(value : WordLinkUploadState) {
    if (Workspace.isRemote()) {
        // uploadstate only matters if this is a remote context
        Workspace.activeWordLink.uploadState = value
    }
}

// RK 12/28/23: Spins through the wordlink list looking
// for wordlinks that need an upload.  See Issue #111
fun getWordLinksNeedsUpload(): MutableList<WordLink> {
    val wordLinks = ArrayList<WordLink>()
    val it: Iterator<WordLink> = Workspace.termToWordLinkMap.values.iterator()
    while (it.hasNext()) {
        val wordLink: WordLink = it.next()
        if (wordLink.uploadState == WordLinkUploadState.UPLOAD_NEEDED) {
            wordLinks.add(wordLink)
        }
    }
    return wordLinks
}

// RK 02/07/2024
// This will check the wordlinks for a particular slide and will return a
// list of the wordlinks on this slide that need an upload
// This allows the uplaod process to only update wordlinks for the current
// slide instead of the entire list.  This mirrors what happens in the ROCC UI
fun getWordLinksNeedsUploadForSlide(slideNum : Int?): MutableList<WordLink> {
    if (slideNum == null) {
        return getWordLinksNeedsUpload()
        // val wordLinksRet: MutableList<WordLink> = mutableListOf()
        // return wordLinksRet
    }
    val slide = Workspace.activeStory.slides[slideNum]
    val wordLinks = Workspace.WLSTree.getWordLinksNeedUpdateForForText(slide.content)
    return wordLinks
}

fun getWordLinksNotUpload(): MutableList<WordLink> {
    val wordLinks = ArrayList<WordLink>()
    val it: Iterator<WordLink> = Workspace.termToWordLinkMap.values.iterator()
    while (it.hasNext()) {
        val wordLink: WordLink = it.next()
        if ((wordLink.uploadState == WordLinkUploadState.UPLOAD_NEEDED) ||
            (wordLink.uploadState == WordLinkUploadState.NOT_UPLOADED)) {
            wordLinks.add(wordLink)
        }
    }
    return wordLinks
}

// RK 12/28/23:
// Will upload all wordlinks that need uploading.  See Issue #111
fun checkWordLinksNeedsUpload(context : Context, slideNumber : Int?, uploadMgr : UploadAudioButtonManager?) {

    if (slideNumber == null) {
        return   // for this scenario, if slidenumber is not defined then do nothing
    }
    //val wordLinks = getWordLinksNeedsUploadForSlide(slideNumber)  // gives updates needed only for current slide
    val wordLinks = getWordLinksNeedsUpload()  // gives updates needed from all wordlinks
    if (wordLinks.size > 0) {
        for (i in wordLinks.indices) {
            Toast.makeText(context, R.string.uploading_wordlink, Toast.LENGTH_SHORT)
                .show()

            var audioRecording = wordLinks[i].chosenWordLinkFile
            audioRecording = Story.getFilename(audioRecording)
            audioRecording = WORD_LINKS_DIR + "/" + audioRecording
            val input = getChildInputStream(context, audioRecording)
            val audioBytes = IOUtils.toByteArray(input)
            val byteString = Base64.encodeToString(audioBytes, Base64.DEFAULT)

            val js = HashMap<String, String>()
            js["WordLink"] = wordLinks[i].term
            var displayName = Story.getDisplayName(wordLinks[i].chosenWordLinkFile)
            val fileName = Story.getFilename(wordLinks[i].chosenWordLinkFile)
            if (displayName.indexOf(Workspace.activePhase.getDisplayNameAdditionalInfo()) > 0) {
                displayName = ""  // do not send displayName if it is the prompt for "Press and hold"
            }

            js["textBackTranslation"] = displayName
            js["audioRecordingFilename"] = fileName
            js["Data"] = byteString

            // XXXX val relativeUrl = context.getString(R.string.url_upload_wordlink)
            val relativeUrl = context.getString(R.string.url_upload_audio)  // XXXX try to make this work for now

            // XXXX these should not be necessary when the rocc adds real support for wordlinks
            js["TemplateTitle"] = Workspace.activeStory.title
            js["Language"] = Workspace.activeStory.language
            if (slideNumber != null) {
                js["SlideNumber"] = slideNumber.toString()
            }
            if (Workspace.activeStory.remoteId != null) {
                js["StoryId"] = Workspace.activeStory.remoteId.toString()
            }
            // XXXX end - unnecessary items for success

            sendProjectSpecificRequest(
                context,
                relativeUrl,
                {
                    Toast.makeText(
                        context,
                        R.string.upload_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    wordLinks[i].uploadState = WordLinkUploadState.UPLOADED
                    if (uploadMgr != null) {
                        uploadMgr.refreshBackground()
                    }
                },
                {
                    Toast.makeText(
                        context,
                        R.string.upload_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    wordLinks[i].uploadState = WordLinkUploadState.UPLOAD_NEEDED  //still needed
                    if (uploadMgr != null) {
                        uploadMgr.refreshBackground()
                    }
                },
                js
            )
        }
    }
}


