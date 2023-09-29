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
import org.tyndalebt.storyproduceradv.activities.BaseActivity
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
class TestVoiceStudioPhase : BaseMultiRecordPhaseTest() {

   override fun getAudioFiles(slide : Slide) : MutableList<String> {
      return slide.voiceStudioAudioFiles
   }

   override fun getPhaseType() : PhaseType {
      return PhaseType.VOICE_STUDIO
   }

   //
   // Test: VoiceStudioPhaseTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity
   //
   // Steps:
   //    1. Initialize the Translate/Revise activity screen
   //    2. Init display for Slide 0
   //    3. Check the contents of the fragment for proper display
   //    4. Simulate the buttons to modify tesxt, modify the picture, restore the picture
   //    5. Test that slide has been properly updated
   //    6. Test also for slide 1 and final slide
   //
   // Author: Ray Kaestner 09-15-2023
   //

   @Test
   fun VoiceStudioPhaseTest() {

      // init environment
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         var frag = startPagerFragment(0)
         var fragView = startPagerFragmentView(frag)
         checkVoiceStudioContent(frag, fragView, 0)

         modifyTextAndPicture(frag, fragView, 0)

         // switch to next slide and double check
         Workspace.activeSlideNum = 1  // from CircularViewPageHandler.onPageSelected()
         var frag2 = startPagerFragment(Workspace.activeSlideNum)
         var fragView2 = startPagerFragmentView(frag2)
         checkVoiceStudioContent(frag2, fragView2, Workspace.activeSlideNum)

         modifyTextAndPicture(frag2, fragView2, 1)
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
   // Test: VoiceStudioAudioTest
   //
   // Purpose:
   //    Tests functionality for the Translate/Revise activity, audio files
   //
   // Steps:
   //    1. Initialize the Translate/Revise activity screen
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


   @Test
   fun RecordAudioTest() {
      doRecordAudioTest(true)
   }

   override fun loadStory(baseActivity : BaseActivity) : Story? {
      val myStory = super.loadStory(baseActivity)
      myStory!!.isApproved = true // approval is needed to enter this phase
      return myStory
   }

   fun checkVoiceStudioContent(frag : MultiRecordFrag, fragView : View?, slideNum : Int) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType : SlideType = slide.slideType

      val imageFab = fragView!!.findViewById<View>(R.id.insert_image_view) as ImageView?
      if (slideType != SlideType.NUMBEREDPAGE) {
         Assert.assertTrue("Image view should be visible", imageFab!!.visibility == View.VISIBLE)
      }
      else {
         Assert.assertFalse("Image view should be invisible", imageFab!!.visibility == View.VISIBLE)
      }

      val slideTextEdit = fragView!!.findViewById(R.id.fragment_dramatization_edit_text) as EditText?
      Assert.assertNotNull("Unable to find edit slide text", slideTextEdit)
      if (slideType != SlideType.NUMBEREDPAGE) {
         Assert.assertFalse("Edit box should be invisible", slideTextEdit!!.visibility == View.VISIBLE)
      }
      else {
         Assert.assertTrue("Edit box should be visible", slideTextEdit!!.visibility == View.VISIBLE)
      }
   }

   fun modifyTextAndPicture(frag : MultiRecordFrag, fragView : View?, slideNum : Int) {

      // from VoiceStudioFrag.closeKeyboard()
      val slide = Workspace.activeStory.slides[slideNum]
      val slideType: SlideType = slide.slideType

      //if (slideType == SlideType.NUMBEREDPAGE) {
         // from VoiceStudioFrag.closeKeyboard()
         // modify the text, see MultiRecordFrag,setupCameraAndEditButton handler
         // but this is only available on certain pages
         // See that the UI is updated
         slide.translatedContent = "Hello LarryBoy!"
         frag.setPic(fragView!!.findViewById(R.id.fragment_image_view) as ImageView)
         checkVoiceStudioContent(frag, fragView, Workspace.activeSlideNum)
      //}

      // now select a new picture and verify that it gets properly copied to the right location
      if (slideType != SlideType.NUMBEREDPAGE) {
         var imageFileName = "$PROJECT_DIR/${slideNum}${slide.localSlideExtension}"
         var defaultImageName = "${slideNum}.jpg"
         if (slideType != SlideType.NUMBEREDPAGE) {
            defaultImageName = ""
         }
         Assert.assertEquals(
            "Before mods, imagefilename is incorrect",
            slide.imageFile,
            defaultImageName
         )

         checkPictureFile(imageFileName, false)


         val uri =
            Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy/" + TestPictureName))
         frag.onPictureSelected(uri)
         checkVoiceStudioContent(frag, fragView, Workspace.activeSlideNum)

         Assert.assertEquals("After mods, imagefile not correct", slide.imageFile, imageFileName)
         checkPictureFile(imageFileName, true)
      }

      //if (slideType == SlideType.NUMBEREDPAGE) {  // reset button only on NUMBEREDPAGEs
      //   // reset the image name.
      //   // See the click listener for the AlertDialog in MultiRecordFrag.setupCameraAndEditButton
      //   // Only available on the numbered pages
      //   slide.imageFile = defaultImageName  // reverts back to default
      //   frag.setPic(fragView!!.findViewById(R.id.fragment_image_view) as ImageView)

      //   // previous picture not erased
      //   checkPictureFile(imageFileName, true)
      //}
   }
}
