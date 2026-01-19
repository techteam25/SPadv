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
import org.tyndalebt.storyproduceradv.tools.file.toJson

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

// returns a list of all wordlinks that need upload or that have
// a recording that have never been uploaded
fun getWordLinksNotUploadedNeedingUpload(): MutableList<WordLink> {
    val wordLinks = ArrayList<WordLink>()
    val it: Iterator<WordLink> = Workspace.termToWordLinkMap.values.iterator()
    while (it.hasNext()) {
        val wordLink: WordLink = it.next()
        if (wordLink.uploadState == WordLinkUploadState.UPLOAD_NEEDED) {
            wordLinks.add(wordLink)
        }
        else if ((wordLink.uploadState == WordLinkUploadState.NOT_UPLOADED) &&
                    (wordLink.chosenWordLinkFile != null) &&
                    (wordLink.chosenWordLinkFile.isNotEmpty())) {
            wordLinks.add(wordLink)
        }
    }
    return wordLinks
}

// RK 12/28/23:
// Will upload all wordlinks that need uploading.  See Issue #111
fun checkWordLinksNeedsUpload(context : Context, slideNumber : Int?, uploadMgr : UploadAudioButtonManager?) {

    //val wordLinks = getWordLinksNeedsUploadForSlide(slideNumber)  // gives updates needed only for current slide
    //val wordLinks = getWordLinksNeedsUpload()  // gives updates needed from all wordlinks
    val wordLinks = getWordLinksNotUploadedNeedingUpload()  // gives updates needed from all wordlinks
    if (wordLinks.size > 0) {
        var toastMsg = context.getString(R.string.uploading_wordlink)
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT)
            .show()
        for (i in wordLinks.indices) {
            //var toastMsg = context.getString(R.string.uploading_wordlink) + wordLinks[i].term
            //Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT)
            //    .show()

            val js = HashMap<String, String>()
            js["term"] = wordLinks[i].term

            var indexNo = 1
            for (j in wordLinks[i].wordLinkRecordings.indices) {
                var audioRec = wordLinks[i].wordLinkRecordings[j]

                var backTrans = Story.getDisplayName(audioRec.audioRecordingFilename)
                if (backTrans.indexOf(Phase.WORDLINK_EMPTY_DISPLAYNAME) < 0) {
                    js["textBackTranslation$indexNo"]  = backTrans
                    indexNo++
                }
            }
            var resId = R.string.url_upload_wordlink_backtrans
            if ((wordLinks[i].wordLinkRecordings.size <= 0) || (indexNo == 1)) {
                resId = R.string.url_delete_wordlink_backtrans
            }
            val relativeUrl = context.getString(resId)

            sendProjectSpecificRequest(
                context,
                relativeUrl,
                {
                    worklinkUploadSuccess(context, wordLinks[i])
                    if (uploadMgr != null) {
                        uploadMgr.refreshBackground()
                    }
                },
                {
                    val nr = it.networkResponse
                    if ((resId == R.string.url_delete_wordlink_backtrans) &&
                        (nr != null) && (nr.statusCode == 404)) {

                        // Note that the most common error for delete
                        // is that the item does not exist.  This is a valid
                        // error, but should be ignored, meaning that the item
                        // has already been deleted
                        worklinkUploadSuccess(context, wordLinks[i])
                    }
                    else {
                        if (nr != null) {
                            // error message details are available
                            Toast.makeText(context, "${nr.statusCode}: ${String(nr.data, Charsets.UTF_16)}", Toast.LENGTH_LONG).show()
                        }

                        var toastMsg =
                            context.getString(R.string.wordlink_upload_failed) + wordLinks[i].term
                        Toast.makeText(
                            context,
                            toastMsg,
                            Toast.LENGTH_SHORT
                        ).show()

                        modifyUploadStateAndSave(
                            context,
                            wordLinks[i],
                            WordLinkUploadState.UPLOAD_NEEDED
                        )
                    }
                    if (uploadMgr != null) {
                        uploadMgr.refreshBackground()
                    }
                },
                js
            )
        }
    }
}

private fun worklinkUploadSuccess(context : Context, wordLink: WordLink) {
    var toastMsg = context.getString(R.string.wordlink_upload_success) + wordLink.term
    //Toast.makeText(
    //    context,
    //    toastMsg,
    //    Toast.LENGTH_SHORT
    //).show()
    modifyUploadStateAndSave(context, wordLink, WordLinkUploadState.UPLOADED)

}

private fun modifyUploadStateAndSave(context: Context, wordLink: WordLink, uploadState: WordLinkUploadState) {
    if (Workspace.isRemote() &&
         (wordLink.uploadState != uploadState)) {
        wordLink.uploadState = uploadState
        doSaveWordLink(context, wordLink)
    }
}

/**
 * Saves the specified word link to the workspace and exports an up-to-date json file for all word links
 **/

fun doSaveWordLink(context: Context, wordLink: WordLink) {
    Workspace.termToWordLinkMap[wordLink.term] = wordLink   // replace the list item with the new definition
    val wordLinkList = WordLinkList(Workspace.termToWordLinkMap.values.toList())
    Thread(Runnable{
        context.let {
            wordLinkList.toJson(it)
        }
    }).start()  // save the list
}


