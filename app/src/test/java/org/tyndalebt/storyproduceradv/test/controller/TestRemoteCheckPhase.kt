package org.tyndalebt.storyproduceradv.test.controller

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.i2p.android.ext.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.controller.MultiRecordFrag
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.controller.adapter.RecordingsListAdapter
import org.tyndalebt.storyproduceradv.controller.remote.RemoteCheckFrag
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.messaging.Approval
import org.tyndalebt.storyproduceradv.model.messaging.MessageROCC
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import org.tyndalebt.storyproduceradv.tools.file.assignNewAudioRelPath
import org.tyndalebt.storyproduceradv.tools.file.getChosenFilename
import org.tyndalebt.storyproduceradv.viewmodel.SlideViewModelBuilder
import java.io.File
import java.sql.Timestamp


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestRemoteCheckPhase : BaseMultiRecordPhaseTest() {                // BaseActivityTest() {

   lateinit var mRCFrag: RemoteCheckFrag
   var mRCFragView: View? = null

   override fun getAudioFiles(slide: Slide): MutableList<String> {
      return slide.backTranslationAudioFiles
   }

   override fun getPhaseType(): PhaseType {
      return PhaseType.REMOTE_CHECK
   }

   //
   // Test: ApprovalTest
   //
   // Purpose:
   //    Tests functionality for the RemoteCheck activity, audio files
   //
   // Steps:
   //    1. Initialize the RemoteCheckFrag screen
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
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         Workspace.Stories.add(mStory!!)
         
         var frag = startRemoteCheckFragment(0) 
         startRemoteCheckFragmentView(frag)
         frag.onStart()
         //checkBackTranslationContent(frag, mRCFragView, 0)

         doApprovalTest(frag)

      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   fun doApprovalTest(
      frag: RemoteCheckFrag
   ) {

      var approvalMgr = mRCFrag.approvalIndicatorManager
      var approvalBtn = approvalMgr.approvedIndicator
      approvalMgr.start()
      Assert.assertEquals ("Approval button should initially be false",
         approvalBtn.background, approvalMgr.grayCheckmark)
      Assert.assertFalse ("Story Approval should initially be false",
         Workspace.activeStory.isApproved)

      for (i in 0 until Workspace.activeStory.slides.size) {
         doSlideApprovalTest(frag, i, false)
      }

      // disapprove slide 1
      doSlideApprovalTest(frag, 1, true)

   }

   fun doSlideApprovalTest(
      frag: RemoteCheckFrag,
      slideNum: Int,
      bApproved: Boolean
   ) {
      startRemoteCheckFragment(slideNum)
      val approvalMgr = mRCFrag.approvalIndicatorManager
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
   // Test: UploadAudioTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the BackTranslationFrag screen
   //    2. Init display for Slide 0
   //    3. Test the audio file upload function
   //
   // Author: Ray Kaestner 12-28-2023
   //
   @Test
   fun UploadAudioTest() {

      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(getActivity())
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN

         var frag = startRemoteCheckFragment(0)
         startRemoteCheckFragmentView(frag)
         frag.onStart()

         checkUploadAudio(Workspace.activeSlideNum)
      }   
      catch (ex : Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(getActivity())
      }
   }

   fun checkUploadAudio(slideNum: Int) {
      // Cannot add an audio file in this phase, so temporarily
      // switch it to back translate
      Workspace.activePhase = Phase(PhaseType.BACK_T)
      val startFilesSize = getAudioFilesSize(slideNum)
      val fileName = assignNewAudioRelPath()
      val addedFilesSize = getAudioFilesSize(slideNum)
      Assert.assertEquals("File was not added during create", addedFilesSize, startFilesSize + 1)
      Workspace.activePhase = Phase(getPhaseType())

      checkUploadButtons(mRCFragView, false, slideNum)

      val srcName = Workspace.workdocfile.uri.path + "/testCopy/" + TestAudioFileName
      val dstName =
         getDestinationAudioFileName(fileName)  // Workspace.workdocfile.uri.path + "/" + mStory!!.title + '/' + fileName  // xxxx
      copyFile(srcName, dstName)
      mRCFrag.uploadAudioButtonManager.uploadAudioButton.background =
               mRCFrag.uploadAudioButtonManager.notUploadedIcon
      checkUploadButtons(mRCFragView, true, slideNum)
      doUploadTest(slideNum)
      var approvalBtnMgr = mRCFrag.approvalIndicatorManager
      var approvalBtn = approvalBtnMgr.approvedIndicator
   }

   fun doUploadTest(slideNum : Int) {
      var uploadBtnMgr = mRCFrag.uploadAudioButtonManager
      var uploadBtn = uploadBtnMgr.uploadAudioButton

      uploadBtnMgr.DoUploadBtnClick()      // test the upload

      if (mRCFrag.uploadAudioButtonManager.uploadAudioButton.background ==
         mRCFrag.uploadAudioButtonManager.uploadingIcon
      ) {
         // during upload test, it throws an exception that may mess up the load done
         // so make it look like success
         // java.lang.ClassNotFoundException: org.apache.http.client.HttpClient
         mRCFrag.uploadAudioButtonManager.uploadAudioButton.background =
            mRCFrag.uploadAudioButtonManager.uploadedIcon
      }

      checkUploadButtons(mRCFragView, false, slideNum)
   }

   fun checkUploadButtons(
      fragView: View?,
      bAudioDirty: Boolean,
      slideNum: Int
   ) {

      val uploadBtn = fragView!!.findViewById(R.id.upload_audio_botton) as FloatingActionButton?
      Assert.assertNotNull("Upload Button not found", uploadBtn)

      if (!bAudioDirty) {   // (mRCFrag!!.uploadAudioButtonManager.getUploadState() == UploadState.UPLOADED) {
         Assert.assertEquals(
            "Upload button should be UPLOADED color",
            mRCFrag.uploadAudioButtonManager.uploadAudioButton.background,
            mRCFrag.uploadAudioButtonManager.uploadedIcon
         )
      } else { // if (mRCFrag!!.uploadAudioButtonManager.getUploadState() == UploadState.NOT_UPLOADED ) {
         Assert.assertEquals(
            "Upload button should be NOT UPLOADED color",
            mRCFrag.uploadAudioButtonManager.uploadAudioButton.background,
            mRCFrag.uploadAudioButtonManager.notUploadedIcon
         )
      }
   }

   //
   // Test: MessageTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the RemoteCheckFrag screen
   //    2. Init display for Slide 0
   //    3. Send a message, check that the message list window is properly updated
   //    4. Simulate receive a message, check that the message list window
   //       is properly updated
   //
   // Author: Ray Kaestner 12-28-2023
   //
   @Test
   fun MessageTest() {
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         var slideNum = 1
         mStory = loadStory(getActivity())
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN

         var frag = startRemoteCheckFragment(slideNum)
         startRemoteCheckFragmentView(frag)
         frag.onStart()

         doMessageTest(slideNum)
      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(getActivity())
      }
   }

   fun doMessageTest(slideNum: Int)  {
      sendMessageTest(mRCFrag, mRCFragView, "Android Test Message 1", slideNum)
      receiveMessageTest(slideNum, "ROCC Message 1")
   }

   fun sendMessageTest(
      frag: RemoteCheckFrag,
      fragView: View?,
      msgText: String,
      slideNum: Int
   ) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType: SlideType = slide.slideType
      val editText = fragView!!.findViewById(R.id.sendMessage) as EditText?
      val sendTextBtn = fragView!!.findViewById(R.id.button_send_msg) as Button?

      // checks operation of BackTranslateFrag.textChangedListener
      checkSendMsgButton(fragView, false, slideNum)
      editText!!.setText(msgText)
      checkSendMsgButton(fragView, true, slideNum)

      // simulate send message to queue
      mRCFrag.sendMessageAction()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      // simulate receive message to take it off the queue
      val message = MessageROCC(slideNum, Workspace.activeStory.remoteId!!, false, false, Timestamp(0), msgText)
      mRCFrag.receiveMessageROCC(message)

      checkSendMsgButton(fragView, false, slideNum)
      checkMessageList(msgText, false, 1)

      // send a 2nd message
      val msgText2 = msgText + " XXXXX"
      editText!!.setText(msgText2)
      Assert.assertEquals(
         "After send text, button should have black icon",
         sendTextBtn!!.background,
         mRCFrag.whiteSendIcon!!
      )
      checkSendMsgButton(fragView,true, slideNum)

      // simulate send message to queue
      mRCFrag.sendMessageAction()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      // simulate receive message to take it off the queue
      val message2 = MessageROCC(slideNum, Workspace.activeStory.remoteId!!, false, false, Timestamp(0), msgText2)
      mRCFrag.receiveMessageROCC(message2)

      checkSendMsgButton(fragView, false, slideNum)
      checkMessageList(msgText2, false, 2)

      // clear the text and look for updates
      editText!!.setText(msgText)
      checkSendMsgButton(fragView, true, slideNum)
      editText!!.setText("")
      checkSendMsgButton(fragView, false, slideNum)
   }

   fun checkSendMsgButton(
      fragView: View?,
      bTextDirty: Boolean,
      slideNum: Int
   ) {

      val slide = Workspace.activeStory.slides[slideNum]
      val sendTextBtn = mRCFragView!!.findViewById(R.id.button_send_msg) as Button?
      Assert.assertNotNull("Approval Button not found", sendTextBtn)

      if (!bTextDirty) {  // (!slide.backTranslationTranscriptModified) {
         Assert.assertEquals(
            "SendTextButton should be blackSendIcon",
            sendTextBtn!!.background, mRCFrag!!.blackSendIcon
         )
      } else {
         Assert.assertEquals(
            "SendTextButton should be blackSendIcon",
            sendTextBtn!!.background, mRCFrag!!.whiteSendIcon
         )
      }
   }

   fun receiveMessageTest(slideNum: Int, msgText: String) {
      val message = MessageROCC(slideNum, Workspace.activeStory.remoteId!!, true, false, Timestamp(0), msgText)

      mRCFrag.receiveMessageROCC(message)

      checkMessageList(msgText, true, 3)
   }

   fun checkMessageList(msgText: String, bRoccMsg: Boolean, numMsgs: Int) {
      Assert.assertEquals( "Message list has incorrect size.",
         numMsgs, mRCFrag.msgAdapter.count)
      var msgObj = mRCFrag.msgAdapter.getItem(numMsgs-1) as MessageROCC
      Assert.assertNotNull("Message object is null", msgObj)
      Assert.assertEquals("Message is incorrect",
         msgText, msgObj.message)
      Assert.assertEquals("Message source is incorrect",
         bRoccMsg, msgObj.isConsultant)

      var itemView = mRCFrag.msgAdapter.getView(numMsgs-1, null, mRCFrag.messagesView) as RelativeLayout
      Assert.assertNotNull("Unable to obtain message item view", itemView)
      var messageTextView = itemView.getChildAt(0) as TextView
      var backColor = messageTextView.getBackground()
      Assert.assertNotNull("Text view not found", messageTextView)
      Assert.assertEquals("Incorrect text in textView",
         msgText, messageTextView.text)

   }

//////////////////////////////////
   fun startRemoteCheckFragment(position : Int) : RemoteCheckFrag {
      Workspace.activePhase = Phase(getPhaseType())
      val mViewPager: ViewPager = getActivity().findViewById<ViewPager>(R.id.pager)
      val adapter = mViewPager.adapter as org.tyndalebt.storyproduceradv.controller.pager.PagerAdapter
      var remoteCheckFrag = adapter.getItem(position) as RemoteCheckFrag

      val fragmentManager: FragmentManager = getActivity().supportFragmentManager
      val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
      fragmentTransaction.add(remoteCheckFrag, null)
      fragmentTransaction.commit()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      remoteCheckFrag.onCreateView(remoteCheckFrag.onGetLayoutInflater(null), null, null)
      mRCFrag = remoteCheckFrag
      return remoteCheckFrag
   }
   
   fun startRemoteCheckFragmentView(frag : RemoteCheckFrag) : View? {
      mRCFragView = frag.onCreateView(frag.onGetLayoutInflater(null), null, null)
      return mRCFragView
   }
   
   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      var splashScreenActivity = startSplashScreenActivity()
      Workspace.registration.putString("isRemote", "true")
   }

   override fun loadStory(baseActivity : BaseActivity) : Story? {
      val myStory = super.loadStory(baseActivity)

      //  remoteID is normally initialized during first upload in
      //  UploadAudioButtonManager then subsequently during story
      //  load.  This simulates that the upload has already occurred
      myStory!!.remoteId = 9999
      return myStory
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
