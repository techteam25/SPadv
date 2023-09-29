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
import org.tyndalebt.storyproduceradv.controller.pager.PagerBaseActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
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
class TestCommunityPhase : BaseMultiRecordPhaseTest() {

   override fun getAudioFiles(slide : Slide) : MutableList<String> {
      return slide.communityWorkAudioFiles
   }

   override fun getPhaseType() : PhaseType {
      return PhaseType.COMMUNITY_WORK
   }

   //
   // Test: CommunityPhaseTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity
   //
   // Steps:
   //    1. Initialize the Community phase screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the fragment for proper display
   //    4. Test also for slide 1 and final slide
   //
   // Author: Ray Kaestner 09-15-2023
   //

   @Test
   fun CommunityPhaseTest() {

      // init environment
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         var frag = startPagerFragment(0)
         var fragView = startPagerFragmentView(frag)
         checkCommunityContent(frag, fragView, 0)

         // switch to next slide and double check
         Workspace.activeSlideNum = 1  // from CircularViewPageHandler.onPageSelected()
         var frag2 = startPagerFragment(Workspace.activeSlideNum)
         var fragView2 = startPagerFragmentView(frag2)
         checkCommunityContent(frag2, fragView2, Workspace.activeSlideNum)

         // switch to final slide and double check
         Workspace.activeSlideNum =
            mStory!!.slides.count() - 2  // final slide is not visible, copyright slide
         var frag3 = startPagerFragment(Workspace.activeSlideNum)
         var fragView3 = startPagerFragmentView(frag3)
         checkCommunityContent(frag3, fragView3, Workspace.activeSlideNum)
      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   //
   // Test: CommunityAudioTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the Community phase screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the silde audio comment data
   //    4. Simulate the buttons to record an audio file and check that the
   //       slide is properly updated
   //    5. Add more audio comments and check that there is no maximum
   //    6. Test the audio file rename function
   //    7. Test the audio file delete function
   //
   // Author: Ray Kaestner 09-15-2023
   //

   @Test
   fun RecordAudioTest() {
      doRecordAudioTest(false)
   }

    fun checkCommunityContent(frag: MultiRecordFrag, fragView: View?, slideNum: Int) {

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

      val recordingView = fragView!!.findViewById<View>(R.id.recordings_list) as View?
      val toolbarView = fragView!!.findViewById<View>(R.id.toolbar_for_recording_toolbar) as View?
      Assert.assertNotNull("Unable to find recordingsListView", recordingView)
      Assert.assertNotNull("Unable to find recording list view", toolbarView)
   }
}
