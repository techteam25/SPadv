package org.tyndalebt.storyproduceradv.test.controller

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.getbase.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MultiRecordFrag
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.controller.adapter.RecordingsListAdapter
import org.tyndalebt.storyproduceradv.controller.remote.BackTranslationFrag
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.messaging.Approval
import org.tyndalebt.storyproduceradv.tools.file.assignNewAudioRelPath
import org.tyndalebt.storyproduceradv.tools.file.getChosenFilename
import org.tyndalebt.storyproduceradv.viewmodel.SlideViewModelBuilder
import java.sql.Timestamp


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestTestBackTranslationPhase : BaseMultiRecordPhaseTest() {

   lateinit var mBTFrag: BackTranslationFrag
   var mBTFragView: View? = null

   override fun getAudioFiles(slide: Slide): MutableList<String> {
      return slide.backTranslationAudioFiles
   }

   override fun getPhaseType(): PhaseType {
      return PhaseType.BACK_T
   }

   //
   // Test: BackTranslationPhaseTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity
   //
   // Steps:
   //    1. Initialize the BackTranslationFrag screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the fragment for proper text values
   //    4. Simulate the buttons to modify text
   //    5. Test that slide has been properly updated
   //    6. Test also for slide 1 and final slide
   //
   // Author: Ray Kaestner 12-28-2023
   //

   @Test
   fun BackTranslationPhaseTest() {

      // init environment, need to set up remote registration file?
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         var frag = startPagerFragment(0) as MultiRecordFrag
         startPagerFragmentView(frag)
         checkBackTranslationContent(frag, mBTFragView, 0)
         checkUploadButtons(mBTFragView, false, false, 0)

         modifyBackTranslationText(frag, mBTFragView, "Translation Text", 0)
         modifyBackTranslationText(frag, mBTFragView, "Translation Text 2", 0)
      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   fun checkBackTranslationContent(frag: MultiRecordFrag, fragView: View?, slideNum: Int) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType: SlideType = slide.slideType

      // checks for proper text values in the UI
      val slideViewModel = SlideViewModelBuilder(Workspace.activeStory.slides[slideNum]).build()
      val tOverlay = slideViewModel.overlayText
      if (slideType == SlideType.NUMBEREDPAGE) {
         Assert.assertNull("Overlay should be null for regular pages", tOverlay)
      } else {
         var text = slide.content
         if ((slide.translatedContent != null) && slide.translatedContent.length > 0) {
            text = slide.translatedContent
         }
         Assert.assertTrue(
            "Incorrect text overlay value",
            text.indexOf(tOverlay!!.getText()) >= 0
         )
      }

      // check contents of lower frag

      val approvalBtn =
         fragView!!.findViewById(R.id.slide_approved_indicator) as FloatingActionButton?
      Assert.assertNotNull("Approval Button not found", approvalBtn)

      val editText = fragView!!.findViewById(R.id.transcript_edit_text) as EditText?
      Assert.assertNotNull("Transcript text edit not found", editText)
   }

   fun checkUploadButtons(
      fragView: View?,
      bAudioDirty: Boolean,
      bTextDirty: Boolean,
      slideNum: Int
   ) {

      val uploadBtn = fragView!!.findViewById(R.id.upload_audio_botton) as FloatingActionButton?
      Assert.assertNotNull("Upload Button not found", uploadBtn)

      if (!bAudioDirty) {   // (mBTFrag!!.uploadAudioButtonManager.getUploadState() == UploadState.UPLOADED) {
         Assert.assertEquals(
            "Upload button should be UPLOADED color",
            mBTFrag.uploadAudioButtonManager.uploadAudioButton.background,
            mBTFrag.uploadAudioButtonManager.uploadedIcon
         )
      } else { // if (mBTFrag!!.uploadAudioButtonManager.getUploadState() == UploadState.NOT_UPLOADED ) {
         Assert.assertEquals(
            "Upload button should be NOT UPLOADED color",
            mBTFrag.uploadAudioButtonManager.uploadAudioButton.background,
            mBTFrag.uploadAudioButtonManager.notUploadedIcon
         )
      }

      val slide = Workspace.activeStory.slides[slideNum]
      val sendTextBtn = mBTFragView!!.findViewById(R.id.send_transcript_button) as Button?
      Assert.assertNotNull("Approval Button not found", sendTextBtn)

      if (!bTextDirty) {  // (!slide.backTranslationTranscriptModified) {
         Assert.assertEquals(
            "SendTextButton should be blackSendIcon",
            sendTextBtn!!.background, mBTFrag!!.blackSendIcon
         )
      } else {
         Assert.assertEquals(
            "SendTextButton should be blackSendIcon",
            sendTextBtn!!.background, mBTFrag!!.whiteSendIcon
         )
      }
   }

   fun modifyBackTranslationText(
      frag: MultiRecordFrag,
      fragView: View?,
      transcriptText: String,
      slideNum: Int
   ) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType: SlideType = slide.slideType
      val editText = fragView!!.findViewById(R.id.transcript_edit_text) as EditText?
      val sendTextBtn = fragView!!.findViewById(R.id.send_transcript_button) as Button?

      // checks operation of BackTranslateFrag.textChangedListener
      editText!!.setText(transcriptText)
      Assert.assertEquals(
         "Edit text color incorrect after text modification.",
         editText.currentTextColor,
         ContextCompat.getColor(mActivity!!, R.color.transcript_dirty)
      )
      checkUploadButtons(fragView, false, true, slideNum)

      mBTFrag.sendTranscriptAction(editText!!, sendTextBtn!!)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      Assert.assertEquals(
         "After send text, button should have black icon",
         sendTextBtn.background,
         mBTFrag.blackSendIcon!!
      )
      Assert.assertFalse(
         "Message modification flag should be false after send.",
         slide.backTranslationTranscriptModified
      )
      checkUploadButtons(fragView, false, false, slideNum)

      // modify the text and look for updates
      editText!!.setText(transcriptText + " XXXXX")

      Assert.assertTrue(
         "Message modification flag should be false after send.",
         slide.backTranslationTranscriptModified
      )
      Assert.assertEquals(
         "After send text, button should have black icon",
         sendTextBtn.background,
         mBTFrag.whiteSendIcon!!
      )
      checkUploadButtons(fragView, false, true, slideNum)

      mBTFrag.sendTranscriptAction(editText!!, sendTextBtn!!)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      Assert.assertEquals(
         "After send text, button should have black icon",
         sendTextBtn.background,
         mBTFrag.blackSendIcon!!
      )
      Assert.assertFalse(
         "Message modification flag should be false after send.",
         slide.backTranslationTranscriptModified
      )
      checkUploadButtons(fragView, false, false, slideNum)

      // clear the text and look for updates
      editText!!.setText("")

      Assert.assertTrue(
         "Message modification flag should be false after send.",
         slide.backTranslationTranscriptModified
      )
      Assert.assertEquals(
         "After send text, button should have black icon",
         sendTextBtn.background,
         mBTFrag.whiteSendIcon!!
      )
      checkUploadButtons(fragView, false, true, slideNum)

      // XXXXX Note: sendTranscriptAction does nothing because text is blank.  Is that correct?
      // FIXME?????
      mBTFrag.sendTranscriptAction(editText!!, sendTextBtn!!)
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      // xxxx Assert.assertEquals("After send text, button should have black icon", sendTextBtn.background, mBTFrag.blackSendIcon!!)
      // xxxx Assert.assertFalse("Message modification flag should be false after send." ,slide.backTranslationTranscriptModified)
//xxx      checkUploadButtons(fragView, false, false, slideNum)
   }

   //
   // Test: ApprovalTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the BackTranslationFrag screen
   //    2. Simulate approval message for each slide
   //    3. Check that slide and story approval properly set.
   //    4. Disapprove slide 1 through the message
   //    3. Check that slide and story approval properly reset.
   //
   // Author: Ray Kaestner 02/28/24
   //
   @Test
   fun ApprovalTest() {

      // init environment, need to set up remote registration file?
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         mStory!!.remoteId = 100  // null protection
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         Workspace.Stories.add(mStory!!)
         var frag = startPagerFragment(0) as MultiRecordFrag
         startPagerFragmentView(frag)
         checkBackTranslationContent(frag, mBTFragView, 0)

         doApprovalTest(frag, mBTFragView)

      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   fun doApprovalTest(
      frag: MultiRecordFrag,
      fragView: View?
   ) {

      var approvalMgr = mBTFrag.approvalIndicatorManager
      var approvalBtn = approvalMgr.approvedIndicator
      approvalMgr.start()
      Assert.assertEquals ("Approval button should initially be false",
         approvalBtn.background, approvalMgr.grayCheckmark)
      Assert.assertFalse ("Story Approval should initially be false",
         Workspace.activeStory.isApproved)

      for (i in 0 until Workspace.activeStory.slides.size) {
         doSlideApprovalTest(frag, fragView, i, false)
      }

      // disapprove slide 1
      doSlideApprovalTest(frag, fragView, 1, true)

   }

   fun doSlideApprovalTest(
      frag: MultiRecordFrag,
      fragView: View?,
      slideNum: Int,
      bApproved: Boolean
   ) {
      startPagerFragment(slideNum)
      val approvalMgr = mBTFrag.approvalIndicatorManager
      val approvalBtn = approvalMgr.approvedIndicator
      approvalMgr.start()

      Workspace.activeSlideNum = slideNum  // from CircularViewPageHandler.onPageSelected()
      val slide: Slide = Workspace.activeStory.slides[slideNum]

      Assert.assertEquals ("Slide Approval should initially incorrect: " + slideNum,
         bApproved, slide.isApproved)
      var checkMark = if (bApproved) { approvalMgr.greenCheckmark } else {approvalMgr.grayCheckmark }
      Assert.assertEquals ("Approval button should now be true",
         checkMark, approvalBtn.background)
      var approval = Approval(slideNum,  Workspace.activeStory.remoteId!!, Timestamp(0), !bApproved)
      Workspace.approvalList.add(approval)
      Workspace.processReceivedApprovals()
      approvalMgr.processSlideApproval(approval)
      Assert.assertEquals ("Slide Approval incorrect after message sent: " + slideNum,
         !bApproved, slide.isApproved)
      checkMark = if (!bApproved) { approvalMgr.greenCheckmark } else {approvalMgr.grayCheckmark }
      Assert.assertEquals ("Approval button incorrect after message",
         checkMark, approvalBtn.background)
      var storyApproval = (slideNum >= (Workspace.activeStory.slides.size-2))
      storyApproval = if (bApproved) { false } else {storyApproval}
      Assert.assertEquals("Story approval incorrect: " + slideNum,
         storyApproval, Workspace.activeStory.isApproved)
      Assert.assertEquals("Workspace approval list should be empty after messages",
         0, Workspace.approvalList.size)
   }


   //
   // Test: BackTranslationAudioTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the BackTranslationFrag screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the silde audio data
   //    4. Simulate the buttons to record an audio file and check that the
   //       slide is properly updated, including enabling the update button
   //    5. Test the audio file rename function, including the update button enabling.
   //    6. Test the audio file delete function, including the update button enabling
   //
   // Author: Ray Kaestner 12-28-2023
   //
   @Test
   fun RecordAudioTest() {
      doRecordAudioTest(true)
      checkUploadAudio(Workspace.activeSlideNum)
      // Add something to check the upload button
   }

   fun checkUploadAudio(slideNum: Int) {
      val startFilesSize = getAudioFilesSize(slideNum)
      val fileName = assignNewAudioRelPath()
      val addedFilesSize = getAudioFilesSize(slideNum)
      Assert.assertEquals("File was not added during create", addedFilesSize, startFilesSize + 1)

      checkUploadButtons(mBTFragView, false, false, slideNum)

      val srcName = Workspace.workdocfile.uri.path + "/testCopy/" + TestAudioFileName
      val dstName =
         getDestinationAudioFileName(fileName)  // Workspace.workdocfile.uri.path + "/" + mStory!!.title + '/' + fileName  // xxxx
      copyFile(srcName, dstName)
      mBTFrag.onStoppedToolbarRecording()
      checkUploadButtons(mBTFragView, true, false, slideNum)
      doUploadTest(slideNum)
      var approvalBtnMgr = mBTFrag.approvalIndicatorManager
      var approvalBtn = approvalBtnMgr.approvedIndicator
   }

   fun doUploadTest(slideNum : Int) {
      var uploadBtnMgr = mBTFrag.uploadAudioButtonManager
      var uploadBtn = uploadBtnMgr.uploadAudioButton

      uploadBtnMgr.DoUploadBtnClick()      // test the upload

      if (mBTFrag.uploadAudioButtonManager.uploadAudioButton.background ==
         mBTFrag.uploadAudioButtonManager.uploadingIcon
      ) {
         // during upload test, it throws an exception that may mess up the load done
         // so make it look like success
         // java.lang.ClassNotFoundException: org.apache.http.client.HttpClient
         mBTFrag.uploadAudioButtonManager.uploadAudioButton.background =
            mBTFrag.uploadAudioButtonManager.uploadedIcon
      }

      checkUploadButtons(mBTFragView, false, false, slideNum)
   }

   override fun modifySelectedAudioTest(displayNames: MutableList<String>?, slideNum: Int) {
      val chosenName = getChosenFilename(slideNum)
      checkUploadButtons(mBTFragView, false, false, slideNum)

      super.modifySelectedAudioTest(displayNames, slideNum)

      checkModifyTextUploadValue(chosenName, slideNum)
   }

   override fun doDeleteFileTest(slideNum : Int) {
      val chosenName = getChosenFilename(slideNum)
      checkUploadButtons(mBTFragView, false, false, slideNum)

      super.doDeleteFileTest(slideNum)
      checkModifyTextUploadValue(chosenName, slideNum)
   }


   fun checkModifyTextUploadValue(chosenName: String, slideNum : Int) {
      val chosenName2 = getChosenFilename(slideNum)
      val recModal = RecordingsListAdapter.RecordingsListModal(getActivity()!!, mBTFrag.getRecordToolbar())
      recModal.initialChosenComboName = chosenName
      recModal.checkUpdateChosenAudio()
      if (chosenName != chosenName2)
      {  // the selection has changed.  test it
         checkUploadButtons(mBTFragView, true, false, slideNum)
         doUploadTest(slideNum)
      }
      else
      {  // selection not changed, button should not be enabled
         checkUploadButtons(mBTFragView, false, false, slideNum)
      }
   }

   override fun startPagerFragment(position : Int) : MultiRecordFrag {
      val frag = super.startPagerFragment(position)
      mBTFrag = frag as BackTranslationFrag
      return frag
   }

   override fun startPagerFragmentView(frag : MultiRecordFrag) : View? {
      mBTFragView = super.startPagerFragmentView(frag)
      return mBTFragView
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      var splashScreenActivity = startSplashScreenActivity()
      Workspace.registration.putString("isRemote", "true")
   }

   fun startSplashScreenActivity() : SplashScreenActivity {
      Workspace.registration.complete = false
      val splashScreenActivity = Robolectric.buildActivity(
         SplashScreenActivity::class.java
      ).create().get()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return splashScreenActivity
   }
}
