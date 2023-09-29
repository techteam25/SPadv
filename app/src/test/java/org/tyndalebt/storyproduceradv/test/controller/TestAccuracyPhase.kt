package org.tyndalebt.storyproduceradv.test.controller

import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MultiRecordFrag
import org.tyndalebt.storyproduceradv.controller.accuracycheck.AccuracyCheckFrag
import org.tyndalebt.storyproduceradv.controller.logging.LogListAdapter
import org.tyndalebt.storyproduceradv.controller.pager.PagerBaseActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.model.logging.LogEntry
import org.tyndalebt.storyproduceradv.model.logging.saveLearnLog
import org.tyndalebt.storyproduceradv.model.logging.saveLog
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import org.tyndalebt.storyproduceradv.tools.file.assignNewAudioRelPath
import org.tyndalebt.storyproduceradv.tools.file.deleteAudioFileFromList
import org.tyndalebt.storyproduceradv.tools.file.getChosenCombName
import org.tyndalebt.storyproduceradv.tools.file.getChosenDisplayName
import org.tyndalebt.storyproduceradv.tools.file.getRecordedAudioFiles
import org.tyndalebt.storyproduceradv.tools.file.getRecordedDisplayNames
import org.tyndalebt.storyproduceradv.tools.file.setChosenFileIndex
import org.tyndalebt.storyproduceradv.tools.file.updateDisplayName
import org.tyndalebt.storyproduceradv.viewmodel.SlideViewModelBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestAccuracyPhase : BaseActivityTest() {

   var mActivity : PagerBaseActivity? = null
   var mCopyUri : Uri? = null
   var mStory : Story? = null
   val TestAudioFileName = "TestAudio.m4a"

   //
   // Test: AccuracyPhaseTest
   //
   // Purpose:
   //    Tests functionality for the Accuracy activity
   //
   // Steps:
   //    1. Initialize the Accuracy activity screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the fragment for proper display
   //    4. Simulate the buttons to modify tesxt, modify the picture, restore the picture
   //    5. Test that slide has been properly updated
   //    6. Test also for slide 1 and final slide
   //
   // Author: Ray Kaestner 09-15-2023
   //

   @Test
   fun AccuracyPhaseTest() {

      // init environment
      initProjectFiles(false)
      mActivity = startAccuracyActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         var frag = startPagerFragment(0)
         var fragView = startPagerFragmentView(frag)  // AccuracyCheckFrag
         checkAccuracyContent(frag, fragView, 0)

         accuracyCheckTest(frag, fragView, 0)
      }
      catch (ex : Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   //
   // Test: AccuracyLogsTest
   //
   // Purpose:
   //    Tests functionality for the Accuracy activity, audio files
   //
   // Steps:
   //    1. Initialize the Accuracy activity screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the silde audio data
   //    4. Simulate the buttons to record an audio file and check that the
   //       slide is properly updated
   //    5. Add more pictures and check that only the maximum number of audio files are added
   //    6. Test the audio file rename function
   //    7. Test the audio file delete function
   //
   // Author: Ray Kaestner 09-15-2023
   //

      // LogEntry, LogListAdapter
   @Test
   fun AccuracyLogsTest() {

      // init environment
      initProjectFiles(false)
      mActivity = startAccuracyActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN

         Workspace.activeSlideNum = 1
         var frag = startPagerFragment(Workspace.activeSlideNum)
         var fragView = startPagerFragmentView(frag)

         populateLogList()

         // The following gives a chance for first page to init the toolbar
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

         checkLogListAdapter()
      }
      catch (ex : Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   fun populateLogList() {
      var initPhase = Workspace.activePhase
      var translatePhase = Phase(PhaseType.TRANSLATE_REVISE)
      var learnPhase = Phase(PhaseType.LEARN)
      var communityPhase = Phase(PhaseType.COMMUNITY_WORK)
      Workspace.activePhase = learnPhase
      saveLearnLog(mActivity!!, 1, 2, 2000, true)
      saveLearnLog(mActivity!!, 1, 2, 2000, false)

      Workspace.activePhase = translatePhase
      saveLog("Draft Recording")   // normally from RecordingToolbar
      saveLog("Draft Playback")    // normally from RecordingToolbar, RecordingsListAdapter
      saveLog("LWC Playback")      // normally from SLiderPhaseFrag

      Workspace.activePhase = communityPhase
      saveLog("Comment Recording") // normally from RecordingToolbar
      saveLog("Comment Plaback")   // normally from RecordingToolbar, RecordingsListAdapter
      saveLog("Draft Playback")    // normally from SliderPhaseFrag
      saveLog("Extra Comment")     // extra comment for testing

      Workspace.activePhase = initPhase
   }

   fun checkLogListAdapter() {
      var logAdapter = LogListAdapter(mActivity!!, 1)
      var logAdapter2 = LogListAdapter(mActivity!!, 2)
      var logAdapter3 = LogListAdapter(mActivity!!, 3)

      // check message counts for all phases
      logAdapter.updateList(true, true, true)
      logAdapter2.updateList(true, true, true)
      logAdapter3.updateList(true, true, true)

      Assert.assertEquals("Page 1: message count all types should be 8", logAdapter.count, 9)
      Assert.assertEquals("Page 2: message count all types should be 2", logAdapter2.count, 2)
      Assert.assertEquals("Page 1: message count all types should be 0", logAdapter3.count, 0)

      // check message counts for learn phase
      logAdapter.updateList(true, false, false)
      logAdapter2.updateList(true, false, false)
      logAdapter3.updateList(true, false, false)

      Assert.assertEquals("Page 1: message count learn phase should be 2", logAdapter.count, 2)
      Assert.assertEquals("Page 2: message count learn phase should be 2", logAdapter2.count, 2)
      Assert.assertEquals("Page 1: message count learn phase should be 0", logAdapter3.count, 0)

      // check message counts for translate phase
      logAdapter.updateList(false, true, false)
      logAdapter2.updateList(false, true, false)
      logAdapter3.updateList(false, true, false)

      Assert.assertEquals("Page 1: message count translate phase should be 3", logAdapter.count, 3)
      Assert.assertEquals("Page 2: message count translate phase should be 0", logAdapter2.count, 0)
      Assert.assertEquals("Page 1: message count translate phase should be 0", logAdapter3.count, 0)

      // check message counts for community phase
      logAdapter.updateList(false, false, true)
      logAdapter2.updateList(false, false, true)
      logAdapter3.updateList(false, false, true)

      Assert.assertEquals("Page 1: message count community phase should be 4", logAdapter.count, 4)
      Assert.assertEquals("Page 2: message count community phase should be 0", logAdapter2.count, 0)
      Assert.assertEquals("Page 1: message count community phase should be 0", logAdapter3.count, 0)

      // check message counts for no phases
      logAdapter.updateList(false, false, false)
      logAdapter2.updateList(false, false, false)
      logAdapter3.updateList(false, false, false)

      Assert.assertEquals("Page 1: message count no phase should be 0", logAdapter.count, 0)
      Assert.assertEquals("Page 2: message count no phase should be 0", logAdapter2.count, 0)
      Assert.assertEquals("Page 1: message count no phase should be 0", logAdapter3.count, 0)

      // Now test the log entries
      logAdapter.updateList(true, true, true)
      var logEntry = logAdapter.getItem(0)
      Assert.assertEquals("LogEntry 0 incorrect Phase", logEntry.phase.phaseType, PhaseType.LEARN)
      Assert.assertEquals("LogEntry 0 incorrect description", logEntry.description, "Record Slides 1-2 (2 sec)")

      logEntry = logAdapter.getItem(2)
      Assert.assertEquals("LogEntry 0 incorrect Phase", logEntry.phase.phaseType, PhaseType.TRANSLATE_REVISE)
      Assert.assertEquals("LogEntry 0 incorrect description", logEntry.description, "Draft Recording")

      logEntry = logAdapter.getItem(5)
      Assert.assertEquals("LogEntry 0 incorrect Phase", logEntry.phase.phaseType, PhaseType.COMMUNITY_WORK)
      Assert.assertEquals("LogEntry 0 incorrect description", logEntry.description, "Comment Recording")

      logEntry = logAdapter.getItem(8)
      Assert.assertEquals("LogEntry 0 incorrect Phase", logEntry.phase.phaseType, PhaseType.COMMUNITY_WORK)
      Assert.assertEquals("LogEntry 0 incorrect description", logEntry.description, "Extra Comment")

   }

   fun checkAccuracyContent(frag : AccuracyCheckFrag, fragView : View?, slideNum : Int) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType : SlideType = slide.slideType

      // checks for proper text values in the UI
      val slideNumberText = fragView!!.findViewById<TextView>(R.id.slide_number_text)
      Assert.assertEquals("Incorrect slide text.", slideNumberText?.text, slideNum.toString())

      val slideViewModel = SlideViewModelBuilder(Workspace.activeStory.slides[slideNum]).build()
      val tOverlay = slideViewModel.overlayText
      if (slideType == SlideType.NUMBEREDPAGE) {
         Assert.assertNull("Overlay should be null for regular pages", tOverlay)
      }
      else {
         var text = slide.content
         if ((slide.translatedContent != null) && slide.translatedContent.length > 0) {
            text = slide.translatedContent
         }
         Assert.assertTrue(
            "Incorrect text overlay value",
            text.indexOf(tOverlay!!.getText()) >= 0
         )
      }

      val scriptureText = fragView!!.findViewById(R.id.fragment_scripture_text) as TextView?
      Assert.assertTrue("Incorrect scripture scripture text", scriptureText!!.text.indexOf(slide.content) == 0)

      val scriptureReference = fragView!!.findViewById(R.id.fragment_reference_text) as TextView?
      Assert.assertEquals("Incorrect scripture reference", scriptureReference!!.text, slide.reference)

      val checkButton = fragView!!.findViewById<View>(R.id.concheck_checkmark_button) as ImageButton?
      val logButton = fragView!!.findViewById<View>(R.id.concheck_logs_button) as ImageButton?
      val commentIcon = fragView!!.findViewById<View>(R.id.comment_present_accuracy_check_indicator) as ImageView?
      Assert.assertNotNull("Unable to find check button", checkButton)
      Assert.assertNotNull("Unable to find logs button", logButton)
      Assert.assertNotNull("Unable to find comment icon", commentIcon)
      Assert.assertEquals("checkButton should be visible", checkButton!!.visibility, View.VISIBLE)
      Assert.assertEquals("logButton should be visible", logButton!!.visibility, View.VISIBLE)
      Assert.assertEquals("commentIcon should not be visible", commentIcon!!.visibility, View.INVISIBLE)

      // Button visibility varies based upon the type of page.
      // Check for proper button bisibility
      val restoreFab = fragView!!.findViewById<View>(R.id.restore_image_view) as ImageView?
      Assert.assertNull("Restore Image view should be invisible",
            restoreFab)

      val imageFab = fragView!!.findViewById<View>(R.id.insert_image_view) as ImageView?
      Assert.assertNull("Image view should not be visible", imageFab)

      val editBox = fragView!!.findViewById<View>(R.id.fragment_dramatization_edit_text) as EditText?
      Assert.assertNull("Edit box should be invisible", editBox)

      val editFab = fragView!!.findViewById<View>(R.id.edit_text_view) as ImageView?
      Assert.assertNull("Edit view should be invisible", editFab)
   }

   fun accuracyCheckTest(frag : AccuracyCheckFrag, fragView : View?, slideNum : Int) {
      val checkButton = fragView!!.findViewById<View>(R.id.concheck_checkmark_button) as ImageButton?
      Assert.assertFalse("Workspace should not be approved.", Workspace.activeStory.isApproved)

      // test the button toggle
      var slidex = Workspace.activeStory.slides[slideNum]
      Assert.assertFalse("First slide should be unchecked.", slidex.isChecked)
      frag.checkButtonClicked(checkButton!!)
      Assert.assertTrue("First slide After click should be checked.", slidex.isChecked)
      frag.checkButtonClicked(checkButton!!)
      Assert.assertFalse("First slide second click should be unchecked.", slidex.isChecked)
      
      // now check all the buttons
      for ((slideNum, slide) in Workspace.activeStory.slides.withIndex()) {
         if (slide.slideType == SlideType.COPYRIGHT) {  // copyright needs no approval
            break
         }
         Assert.assertFalse(
            "AllChecked should be false: slide: " + slideNum,
            frag.checkAllMarked()
         )
         frag.setSlideNumber(slideNum)
         Assert.assertFalse("Initially slide should be unchecked.  Slide: " + slideNum, slide.isChecked)
         frag.checkButtonClicked(checkButton!!)
         Assert.assertTrue("After click slide should be checked.  Slide: " + slideNum, slide.isChecked)
      }
      Assert.assertTrue("AllChecked should now be true", frag.checkAllMarked())
      Assert.assertTrue("Workspace should now be approved.", Workspace.activeStory.isApproved)

      // unable to change check state after story is approved
      frag.setSlideNumber(1)
      slidex = Workspace.activeStory.slides[1]
      Assert.assertTrue("After check all should be checked.", slidex.isChecked)
      frag.checkButtonClicked(checkButton!!)
      Assert.assertTrue("After check all, click slide should not change check state", slidex.isChecked)

      // check the comment icon visibility
      val commentIcon = fragView!!.findViewById<View>(R.id.comment_present_accuracy_check_indicator) as ImageView?
      Assert.assertEquals("Initially, commentIcon should not be visible", commentIcon!!.visibility, View.INVISIBLE)

      var initPhase = Workspace.activePhase
      var communityPhase = Phase(PhaseType.COMMUNITY_WORK)
      Workspace.activePhase = communityPhase
      val testFileName = assignNewAudioRelPath()  // even if we don't have a real file, this gives the appearance of one
      Workspace.activePhase = initPhase
      frag.setSlideNumber(1)  // should reset the icon visibility
      Assert.assertEquals("After add comment, commentIcon should be visible", commentIcon!!.visibility, View.VISIBLE)

   }

   fun startAccuracyActivity() : PagerBaseActivity {
      registration.complete = true
      Workspace.activePhase = Phase(PhaseType.ACCURACY_CHECK)
      Workspace.activeSlideNum = 0
      val pagerBaseActivity = Robolectric.buildActivity(PagerBaseActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      Workspace.initFirebaseAnalytics(pagerBaseActivity)
      return pagerBaseActivity
   }

   fun startPagerFragment(position : Int) : AccuracyCheckFrag {
      Workspace.activePhase = Phase(PhaseType.ACCURACY_CHECK)
      val mViewPager: ViewPager = mActivity!!.findViewById<ViewPager>(R.id.pager)
      val multiRecordAdapter = mViewPager.adapter as org.tyndalebt.storyproduceradv.controller.pager.PagerAdapter
      var accuracyFrag = multiRecordAdapter.getItem(position) as AccuracyCheckFrag

      val fragmentManager: FragmentManager = mActivity!!.supportFragmentManager
      val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
      fragmentTransaction.add(accuracyFrag, null)
      fragmentTransaction.commit()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      accuracyFrag.onCreateView(accuracyFrag.onGetLayoutInflater(null), null, null)
      return accuracyFrag
   }

   fun startPagerFragmentView(frag : AccuracyCheckFrag) : View? {
      return frag.onCreateView(frag.onGetLayoutInflater(null), null, null)
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // init temp dir
      val newUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)
      dir.mkdir()
   }
}
