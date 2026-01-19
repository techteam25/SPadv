package org.tyndalebt.storyproduceradv.controller.remote

import android.app.AlertDialog
import android.content.Context
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.UploadState
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.checkWordLinksNeedsUpload
//import org.tyndalebt.storyproduceradv.model.getWordLinksNeedsUpload
import org.tyndalebt.storyproduceradv.model.getWordLinksNotUploadedNeedingUpload
import org.tyndalebt.storyproduceradv.tools.Network.VolleySingleton
import org.tyndalebt.storyproduceradv.tools.file.getStoryChildInputStream
import java.io.InputStream
import java.util.*

class UploadAudioButtonManager(
    val context: Context, 
    val uploadAudioButton: ImageButton, 
    val getUploadState: () -> UploadState, 
    val setUploadState: (UploadState) -> Unit,
    val getAudioRecording: () -> String,
    val slideNumber: Int?
) {

    val notUploadedIcon: VectorDrawableCompat
    val uploadingIcon: VectorDrawableCompat
    val uploadedIcon: VectorDrawableCompat

    init {
        notUploadedIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_upload_24dp, null)!!
        uploadingIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_uploading_24dp, null)!!
        uploadedIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_cloud_done_24dp, null)!!

        refreshBackground()

        uploadAudioButton.setOnClickListener {
            DoUploadBtnClick()
        }

        uploadAudioButton.setOnLongClickListener {
            when (getUploadState()) {
                UploadState.UPLOADING -> {
                    setUploadState(UploadState.NOT_UPLOADED)
                    Toast.makeText(context, R.string.cancel_uploaded, Toast.LENGTH_SHORT).show()
                    refreshBackground()
                }
                UploadState.UPLOADED -> {
                    setUploadState(UploadState.NOT_UPLOADED)
                    Toast.makeText(context, R.string.ignore_uploaded, Toast.LENGTH_SHORT).show()
                    refreshBackground()
                }
                UploadState.NOT_UPLOADED -> Toast.makeText(context, R.string.no_uploads_done, Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    // RK - 02/07/224
    // Separating the code into this method allows it to be used for unit testing purposes
    fun DoUploadBtnClick() {
        when (getUploadState()) {
            UploadState.UPLOADED -> {
                // if we need to upload wordlinks, then allow an upload
                //if (getWordLinksNeedsUpload().size > 0) {
                if (getWordLinksNotUploadedNeedingUpload().size > 0) {
                        checkWordLinksNeedsUpload(context, slideNumber, this)
                }
                else {
                    // no upload needed
                    Toast.makeText(
                        context,
                        R.string.already_uploaded,
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshBackground()
                }
            }

            UploadState.NOT_UPLOADED -> {

                if (Workspace.checkForInternet(context) == false) {
                    val dialogBuilder = AlertDialog.Builder(context)
                    dialogBuilder.setTitle(R.string.upload_failed)
                        .setMessage(R.string.remote_check_msg_no_connection)
                        .setPositiveButton("OK") { _, _ ->
                        }.create()
                        .show()
                } else {

                    val audioRecording = getAudioRecording()

                    // RK 12/28
                    // if no file specified, upload empty string.
                    // this is in case a previously uploaded audio was deleted
                    var byteString = ""
                    var input = null as InputStream?
                    if (audioRecording != null && audioRecording != "") {
                        input = getStoryChildInputStream(context, audioRecording)
                        if (input != null) {  // null if file not exists, probably removed but metadata not cleaned up
                            val audioBytes = IOUtils.toByteArray(input)
                            byteString = Base64.encodeToString(audioBytes, Base64.DEFAULT)
                        }
                    }

                    if ((input != null) ||
                        (audioRecording == null) || (audioRecording == "")) {

                        setUploadState(UploadState.UPLOADING)
                        refreshBackground()
                        Toast.makeText(context, R.string.uploading_audio, Toast.LENGTH_SHORT)
                            .show()

                        val js = HashMap<String, String>()
                        // Default to 0 as the slide number to send to the server
                        // because the server will ignore it if the request has
                        // IsWholeStory set to true.
                        var finalSlideNumber = 0
                        // Null slideNumber indicates that this is a a whole story
                        // upload button.
                        if (slideNumber == null) {
                            js["IsWholeStory"] = "true"
                        } else {
                            finalSlideNumber = slideNumber
                        }
                        sendSlideSpecificRequest(
                            context,
                            finalSlideNumber,
                            context.getString(R.string.url_upload_audio),
                            byteString,
                            {
                                Toast.makeText(
                                    context,
                                    R.string.upload_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (getUploadState() == UploadState.UPLOADING) {
                                    setUploadState(UploadState.UPLOADED)
                                    refreshBackground()
                                }
                                checkWordLinksNeedsUpload(context, slideNumber, this)
                            },
                            {
                                val nr = it.networkResponse
                                if (nr != null) {
                                    // error message details are available
                                    Toast.makeText(context, "${nr.statusCode}: ${String(nr.data, Charsets.UTF_16)}", Toast.LENGTH_LONG).show()
                                }

                                Toast.makeText(
                                    context,
                                    R.string.upload_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                                setUploadState(UploadState.NOT_UPLOADED)
                                refreshBackground()
                            },
                            js
                        )
                    } else {
                        Toast.makeText(context, R.string.no_recording_found, Toast.LENGTH_SHORT)
                            .show()
                        setUploadState(UploadState.UPLOADED)
                    }
                }
            }

            UploadState.UPLOADING -> {
                uploadAudioButton.background = uploadingIcon
                Toast.makeText(context, R.string.upload_already_started, Toast.LENGTH_LONG).show()
                setUploadState(UploadState.NOT_UPLOADED)
            }
        }
    }

    fun refreshBackground() {
        // RK - 02/07/24
        // Updated tto allow enabling in the case that a wordlink on
        // the slide needs an upload

        if (getUploadState() == UploadState.UPLOADING) {
            // if we in the process of uploading, give that state a priority
            uploadAudioButton.background = uploadingIcon
        }
        else if (getWordLinksNotUploadedNeedingUpload().size > 0) {   // checks all wordlink needs upload
        // else if (getWordLinksNeedsUpload().size > 0) {   // checks all wordlink needs upload
        // else if ((slideNumber != null) && getWordLinksNeedsUploadForSlide(slideNumber!!).size > 0) {
            // if wordlinks need an upload, ensure that the button is enabled
            uploadAudioButton.background = notUploadedIcon
        }
        else {
            // if no wordlink needed uploading, then use the upload state to determing the button
            uploadAudioButton.background = when (getUploadState()) {
                UploadState.UPLOADED -> uploadedIcon
                UploadState.NOT_UPLOADED -> notUploadedIcon
                UploadState.UPLOADING -> uploadingIcon
            }
        }
    }
}

fun sendSlideSpecificRequest(
    context: Context,
    slideNumber: Int,
    relativeUrl: String,
    content: String,
    onSuccess: (JSONObject?) -> Unit,
    onFailure: (VolleyError) -> Unit,
    js: HashMap<String, String> = HashMap()) {

    if (Workspace.activeStory.remoteId != null) {
        js["StoryId"] = Workspace.activeStory.remoteId.toString()
    }
    js["TemplateTitle"] = Workspace.activeStory.title
    js["Language"] = Workspace.activeStory.language
    js["SlideNumber"] = slideNumber.toString()
    js["Data"] = content
    sendProjectSpecificRequest(context, relativeUrl, {
        if (it != null) {
            val newStoryId = it.getInt("StoryId")
            Log.e("@pwhite", "Received id $newStoryId")
            if (Workspace.activeStory.remoteId == null) {
                Log.i("@pwhite", "Setting active story id from null to $newStoryId")
                Workspace.activeStory.remoteId = newStoryId
            } else {
                Log.e(
                    "SanityCheck",
                    "Response id ($newStoryId) should be the same story id as stored (${Workspace.activeStory.remoteId})"
                )
            }
            onSuccess(it)
        }
    }, onFailure, js)
}

fun sendProjectSpecificRequest(
    context: Context,
    relativeUrl: String,
    onSuccess: (JSONObject?) -> Unit,
    onFailure: (VolleyError) -> Unit,
    params: HashMap<String, String> = HashMap()) {

    params["Key"] = context.getString(R.string.api_token)
    params["PhoneId"] = getPhoneId(context)
    var url = Workspace.getRoccUrlPrefix(context) + relativeUrl
    val req = object : StringRequest(Method.POST, url, {
        Log.i("LOG_VOLLEY", it)
        var jsonObject: JSONObject? = null
        try {
            // RK 04/12/24
            // There was a test case variable it contained warning text also
            // The normal format looks like: {"StoryId":"3"}
            // The warning format looked like the following:
            //    <br />
            //    <b>Warning</b>:  unlink(/var/www/html/roccdev.ttapps/Files/Projects/d930e413685ebbf1/WordLinks/685cb2ec71a258d063d52d805e5899f986426de6fa24d719c33186cb2171.m4a): No such file or directory in <b>/var/www/html/roccdev.ttapps/API/utils/Model.php</b> on line <b>900</b><br />
            //    {"RecordingId":"3"}
            var index = it.indexOf("{")
            val text = if (index >= 0) it.substring(index, it.length) else it
            jsonObject = JSONObject(text)
        } catch (e: JSONException) {
            //   There was an example of the following error string in it:
            //      "Internal server error. Please report this to the server administrator."
            //   I wanted to display the error message, but toast should have shorter messages
            // val textBuf: CharSequence = StringBuffer(context.getString(R.string.upload_failed) + "\n" + it)
            // Toast.makeText(context, textBuf, Toast.LENGTH_LONG).show()
            val relativeUrl = context.getString(R.string.url_delete_wordlink_backtrans)
            if ((it.length == 0) && (url.toString().indexOf(relativeUrl) > 0)) {
                // delete wordlink could result in an empty it string.
                // treat that one special
                onSuccess(jsonObject)
            }
            else {
                Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show()
            }
        }
        if (jsonObject != null) {
            onSuccess(jsonObject)
        }
    }, {
        Log.e("LOG_VOLLEY", "HIT ERROR")
        Log.e("LOG_VOLLEY", it.toString())
        onFailure(it)
    }) {
        override fun getParams(): Map<String, String> {
            return params
        }
    }
    try {
        VolleySingleton.getInstance(context.applicationContext).addToRequestQueue(req)
    }
    catch (ex : Throwable) {
        // RK 12/28/23: unit test throws NoClassDefFoundError for org.apache.http.client.HttpClient
        if (!Workspace.isUnitTest) {
            throw ex
        }
    }
}

fun getPhoneId(context: Context): String {
    try {
        return Settings.Secure.getString(context.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
    }
    catch (ex : Throwable) {
        if (Workspace.isUnitTest) {
            // RK 12/28/23: the unit test returns a null value which throws illegalStateException
            return ""
        }
        throw ex
    }
}