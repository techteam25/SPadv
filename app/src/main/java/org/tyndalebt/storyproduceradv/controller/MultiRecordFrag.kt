package org.tyndalebt.storyproduceradv.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.BuildConfig
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.PROJECT_DIR
import org.tyndalebt.storyproduceradv.model.SLIDE_NUM
import org.tyndalebt.storyproduceradv.model.PhaseType;
//import org.tyndalebt.storyproduceradv.controller.communitywork.CommunityWorkFrag;
import org.tyndalebt.storyproduceradv.tools.file.copyToWorkspacePath
import org.tyndalebt.storyproduceradv.tools.toolbar.MultiRecordRecordingToolbar
import org.tyndalebt.storyproduceradv.tools.toolbar.PlayBackRecordingToolbar
import org.tyndalebt.storyproduceradv.tools.toolbar.RecordingToolbar
import java.io.File
import kotlin.properties.Delegates

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag(), PlayBackRecordingToolbar.ToolbarMediaListener {
    protected open var recordingToolbar: RecordingToolbar = MultiRecordRecordingToolbar()

    private var tempPicFile: File? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        setSlideNumHolder();

        setToolbar()

        setupCameraAndEditButton()

        return rootView
    }

    private fun setSlideNumHolder() {
        slideNumHolder = this.slideNum;
    }

    /**
     * Setup camera button for updating background image
     * and edit button for renaming text and local credits
     */
    fun setupCameraAndEditButton() {
        // display the image selection button on FRONTCOVER,LOCALSONG & NUMBEREDPAGE
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Add camera tool to numbered pages so that local images can be used in the story
        // If we have a numbered page, only show the camera on the Translate_Revise Phase
        
        /*
            New guy's (Jeremy) note:  This if statement seems needlessly complicated but the logic below may be necessary.
         */
        if(!(Workspace.activeStory.slides[slideNum].slideType == SlideType.NUMBEREDPAGE &&
                        Workspace.activePhase.phaseType != PhaseType.TRANSLATE_REVISE)) {
            if (Workspace.activeStory.slides[slideNum].slideType in
                    arrayOf(SlideType.FRONTCOVER, SlideType.LOCALSONG, SlideType.NUMBEREDPAGE)) {
                val imageFab: ImageView = rootView!!.findViewById<View>(R.id.insert_image_view) as ImageView
                imageFab.visibility = View.VISIBLE
                imageFab.setOnClickListener {
                    val chooser = Intent(Intent.ACTION_CHOOSER)
                    chooser.putExtra(Intent.EXTRA_TITLE, R.string.camera_select_from)

                    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
                    galleryIntent.type = "image/*"
                    chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)

                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraIntent.resolveActivity(activity!!.packageManager).also {
                        tempPicFile = File.createTempFile("temp", ".jpg", activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity!!, "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!))
                    }
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

                    startActivityForResult(chooser, ACTIVITY_SELECT_IMAGE)
                }
            }
        }

        // 0R17 - DKH 05/7/2022 Allow for text editing on the song slide
        // display the Edit  button, if on the FRONTCOVER or LOCALSONG
        val slideType : SlideType = Workspace.activeStory.slides[slideNum].slideType
        if(slideType in arrayOf(SlideType.FRONTCOVER,SlideType.LOCALSONG)) {
            //for these, use the edit text button instead of the text in the lower half.
            //In the phases that these are not there, do nothing.
            val editBox = rootView?.findViewById<View>(R.id.fragment_dramatization_edit_text) as EditText?
            editBox?.visibility = View.INVISIBLE

            val editFab = rootView!!.findViewById<View>(R.id.edit_text_view) as ImageView?
            editFab?.visibility = View.VISIBLE
            editFab?.setOnClickListener {
                val editText = EditText(context)
                editText.id = R.id.edit_text_input

                // Programmatically set layout properties for edit text field
                val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                // Apply layout properties
                editText.layoutParams = params
                editText.minLines = 5

                editText.text.insert(0, Workspace.activeSlide!!.translatedContent)

                val dialog = AlertDialog.Builder(context)
                        .setTitle(getString(R.string.enter_text))
                        .setView(editText)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.save) { _, _ ->
                            Workspace.activeSlide!!.translatedContent = editText.text.toString()
                            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
                        }.create()

                dialog.show()
            }
        }
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Allow the user to restore to the original image
        // If we have a numbered page, only show the restore on the Translate_Revise Phase
        if(slideType == SlideType.NUMBEREDPAGE && Workspace.activePhase.phaseType == PhaseType.TRANSLATE_REVISE) {

            val editFab = rootView!!.findViewById<View>(R.id.restore_image_view) as ImageView?
            editFab?.visibility = View.VISIBLE
            editFab?.setOnClickListener {
                val dialog = AlertDialog.Builder(context)
                        .setTitle(R.string.camera_revert_title)
                        .setMessage(R.string.camera_revert_message)
                        .setNegativeButton(R.string.no, null)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            Workspace.activeStory.slides[slideNum].imageFile = "${slideNum}.jpg"
                            setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
                        }
                        .create()

                dialog.show()



            }

        }
    }

    /**
     * Change the picture behind the screen.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (resultCode == Activity.RESULT_OK && requestCode == ACTIVITY_SELECT_IMAGE) {
                //copy image into workspace
                var uri = data?.data
                if (uri == null) uri = FileProvider.getUriForFile(context!!, "${BuildConfig.APPLICATION_ID}.fileprovider", tempPicFile!!)   //it was a camera intent
                onPictureSelected(uri)
            }
        }catch (e:Exception){
            Toast.makeText(context,"Error",Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun onPictureSelected(uri : Uri?) {  // RK 09/29/23 made public for test TestTranslateReviseActivity
        //copy image into workspace
        // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
        // Put extension in a common place for use by others
        Workspace.activeStory.slides[slideNum].imageFile =
            "$PROJECT_DIR/${slideNum}${Workspace.activeStory.slides[slideNum].localSlideExtension}"
        copyToWorkspacePath(
            context!!, uri!!,
            "${Workspace.activeStory.title}/${Workspace.activeStory.slides[slideNum].imageFile}"
        )
        tempPicFile?.delete()
        setPic(rootView!!.findViewById(R.id.fragment_image_view) as ImageView)
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                recordingToolbar.stopToolbarMedia()
            }
        }
    }

    protected open fun setToolbar() {
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    override fun onStartedToolbarMedia() {
        super.onStartedToolbarMedia()

        stopSlidePlayBack()
    }

    override fun onStartedSlidePlayBack() {
        super.onStartedSlidePlayBack()

        recordingToolbar.stopToolbarMedia()
    }

    /**
     * Had to add a name to this companion to differentiate it from the other containing
     * the comment circle function
     *
     * */
    companion object {
        private const val ACTIVITY_SELECT_IMAGE = 53
        //tells the toolbar the slideNum to set commentIcon visibility
        var slideNumHolder: Int? = null;
    }

    fun getRecordToolbar() : RecordingToolbar { // RK 09/29/23 for testing purposes - see TestTranslateReviseActivity
        return recordingToolbar
    }
}
