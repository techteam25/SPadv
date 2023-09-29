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
class TestTranslateRevisePhase : BaseMultiRecordPhaseTest() {

   //
   // Test: TranslateRevisePhaseTest
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
   fun TranslateRevisePhaseTest() {

      // init environment
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN
         var frag = startPagerFragment(0)
         var fragView = startPagerFragmentView(frag)
         checkTranslateReviseContent(frag, fragView, 0)

         modifyTextAndPicture(frag, fragView, 0)

         // switch to next slide and double check
         Workspace.activeSlideNum = 1  // from CircularViewPageHandler.onPageSelected()
         var frag2 = startPagerFragment(Workspace.activeSlideNum)
         var fragView2 = startPagerFragmentView(frag2)
         checkTranslateReviseContent(frag2, fragView2, Workspace.activeSlideNum)

         modifyTextAndPicture(frag2, fragView2, 1)

         // switch to final slide and double check
         Workspace.activeSlideNum = mStory!!.slides.count()-2  // final slide is not visible, copyright slide
         var frag3 = startPagerFragment(Workspace.activeSlideNum)
         var fragView3 = startPagerFragmentView(frag3)
         checkTranslateReviseContent(frag3, fragView3, Workspace.activeSlideNum)

         modifyTextAndPicture(frag3, fragView3, Workspace.activeSlideNum)
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
   // Test: TranslateReviseAudioTest
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

   fun checkTranslateReviseContent(frag : MultiRecordFrag, fragView : View?, slideNum : Int) {

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

      val scriptureText = fragView.findViewById(R.id.fragment_scripture_text) as TextView?
      Assert.assertTrue("Incorrect scripture scripture text", scriptureText!!.text.indexOf(slide.content) == 0)

      val scriptureReference = fragView.findViewById(R.id.fragment_reference_text) as TextView?
      Assert.assertEquals("Incorrect scripture reference", scriptureReference!!.text, slide.reference)

      // Button visibility varies based upon the type of page.
      // Check for proper button bisibility
      val restoreFab = fragView.findViewById<View>(R.id.restore_image_view) as ImageView?
      if (slideType == SlideType.NUMBEREDPAGE) {
         Assert.assertTrue("Restore Image view should be visible",
            restoreFab!!.visibility == View.VISIBLE)
      }
      else {
         Assert.assertFalse("Restore Image view should be invisible",
            restoreFab!!.visibility == View.VISIBLE)
      }

      val imageFab = fragView.findViewById<View>(R.id.insert_image_view) as ImageView?
      Assert.assertTrue("Image view should be visible", imageFab!!.visibility == View.VISIBLE)

      val editBox = fragView.findViewById<View>(R.id.fragment_dramatization_edit_text) as EditText?
      if (editBox != null) {
         Assert.assertTrue("Edit box should be invisible", editBox.visibility == View.INVISIBLE)
      }

      val editFab = fragView.findViewById<View>(R.id.edit_text_view) as ImageView?
      if (slideType == SlideType.NUMBEREDPAGE) {
         Assert.assertFalse("Edit view should be invisible", editFab!!.visibility == View.VISIBLE)
      }
      else {
         Assert.assertTrue("Edit view should be visible", editFab!!.visibility == View.VISIBLE)
      }
   }

   fun modifyTextAndPicture(frag : MultiRecordFrag, fragView : View?, slideNum : Int) {

      val slide = Workspace.activeStory.slides[slideNum]
      val slideType: SlideType = slide.slideType

      if (slideType != SlideType.NUMBEREDPAGE) {
         // modify the text, see MultiRecordFrag,setupCameraAndEditButton handler
         // but this is only available on certain pages
         // See that the UI is updated
         slide.translatedContent = "Hello LarryBoy!"
         frag.setPic(fragView!!.findViewById(R.id.fragment_image_view) as ImageView)
         checkTranslateReviseContent(frag, fragView, Workspace.activeSlideNum)
      }

      // now select a new picture and verify that it gets properly copied to the right location
      var imageFileName = "$PROJECT_DIR/${slideNum}${slide.localSlideExtension}"
      var defaultImageName = "${slideNum}.jpg"
      if (slideType != SlideType.NUMBEREDPAGE) {
         defaultImageName = ""
      }
      Assert.assertEquals("Before mods, imagefilename is incorrect", slide.imageFile, defaultImageName)

      checkPictureFile(imageFileName, false)

      val uri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy/" + TestPictureName))
      frag.onPictureSelected(uri)
      checkTranslateReviseContent(frag, fragView, Workspace.activeSlideNum)

      Assert.assertEquals ("After mods, imagefile not correct", slide.imageFile, imageFileName)
      checkPictureFile(imageFileName, true)

      if (slideType == SlideType.NUMBEREDPAGE) {  // reset button only on NUMBEREDPAGEs
         // reset the image name.
         // See the click listener for the AlertDialog in MultiRecordFrag.setupCameraAndEditButton
         // Only available on the numbered pages
         slide.imageFile = defaultImageName  // reverts back to default
         frag.setPic(fragView!!.findViewById(R.id.fragment_image_view) as ImageView)

         // previous picture not erased
         checkPictureFile(imageFileName, true)
      }
   }
}
