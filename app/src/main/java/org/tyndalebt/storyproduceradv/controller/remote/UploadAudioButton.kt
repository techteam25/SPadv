package org.tyndalebt.storyproduceradv.controller.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import org.tyndalebt.storyproduceradv.model.UploadState
import android.provider.Settings
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import android.util.Base64
import android.util.Log
import android.widget.*
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest;
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.json.JSONException
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.checkWordLinksNeedsUpload
import org.tyndalebt.storyproduceradv.model.getWordLinksNeedsUpload
import org.tyndalebt.storyproduceradv.model.getWordLinksNeedsUploadForSlide
import org.tyndalebt.storyproduceradv.tools.Network.VolleySingleton
import org.tyndalebt.storyproduceradv.tools.file.getStoryChildInputStream
import java.util.*
import java.io.InputStream
import kotlin.collections.HashMap

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
                if (getWordLinksNeedsUpload().size > 0) {
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
        // else if (getWordLinksNeedsUpload().size > 0) {   // checks all wordlink needs upload
        else if ((slideNumber != null) && getWordLinksNeedsUploadForSlide(slideNumber!!).size > 0) {
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
    onSuccess: (JSONObject) -> Unit,
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
        val newStoryId = it.getInt("StoryId")
        Log.e("@pwhite", "Received id $newStoryId")
        if (Workspace.activeStory.remoteId == null) {
            Log.i("@pwhite", "Setting active story id from null to $newStoryId")
            Workspace.activeStory.remoteId = newStoryId
        } else {
            Log.e("SanityCheck", "Response id ($newStoryId) should be the same story id as stored (${Workspace.activeStory.remoteId})")
        }
        onSuccess(it)
    }, onFailure, js)
}

fun sendProjectSpecificRequest(
    context: Context,
    relativeUrl: String,
    onSuccess: (JSONObject) -> Unit,
    onFailure: (VolleyError) -> Unit,
    params: HashMap<String, String> = HashMap()) {

    params["Key"] = context.getString(R.string.api_token)
    params["PhoneId"] = getPhoneId(context)
    val url = Workspace.getRoccUrlPrefix(context) + relativeUrl
    val req = object : StringRequest(Method.POST, url, {
        Log.i("LOG_VOLLEY", it)
        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(it)
        } catch (e: JSONException) {
            Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show()
        }
        if (jsonObject != null) {
            onSuccess(jsonObject)
        }
    }, {
        Log.e("LOG_VOLLEY", "HIT ERROR")
        Log.e("LOG_VOLLEY", it.toString())
        val nr = it.networkResponse
        if (nr != null) {
            Toast.makeText(context, "${nr.statusCode}: ${String(nr.data, Charsets.UTF_8)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.upload_failed, Toast.LENGTH_SHORT).show()
        }
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