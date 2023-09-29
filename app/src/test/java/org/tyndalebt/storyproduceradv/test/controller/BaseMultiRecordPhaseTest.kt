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
open class BaseMultiRecordPhaseTest : BaseActivityTest() {

   var mActivity : PagerBaseActivity? = null
   var mCopyUri : Uri? = null
   var mStory : Story? = null
   val TestAudioFileName = "TestAudio.m4a"
   val TestPictureName = "YosemiteFalls.jpg"

   open fun getAudioFiles(slide : Slide) : MutableList<String> {
      return slide.translateReviseAudioFiles
   }

   open fun getPhaseType() : PhaseType {
      return PhaseType.TRANSLATE_REVISE
   }

   //
   // Test: AudioRecordAudioTest
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

   //@Test
   fun doRecordAudioTest(bList : Boolean) {

      // init environment
      initProjectFiles(false)
      mActivity = startAudioRecordActivity()
      try {
         mStory = loadStory(mActivity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN

         Workspace.activeSlideNum = 1
         var frag = startPagerFragment(Workspace.activeSlideNum)
         var fragView = startPagerFragmentView(frag)

         // The following gives a chance for first page to init the toolbar
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

         var toolbarFragView = startRecordingToolbarFragmentView(frag)
         doCheckAudioRecordAudioContent(frag, fragView, toolbarFragView, Workspace.activeSlideNum,  bList, false)
         doModifyAudioFiles(frag, fragView, toolbarFragView, Workspace.activeSlideNum, bList)
      }
      catch (ex : Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(mActivity!!)
      }
   }

   fun doCheckAudioRecordAudioContent(frag : MultiRecordFrag, fragView : View?, toolbarFragView : View?, slideNum : Int, bList : Boolean, bAudioExists : Boolean) {

      val slide = Workspace.activeStory.slides[slideNum]

      // check the default name of the original naration file
      var narrationFileName = slide.narrationFile
      val testName = "audio/narration" + slideNum + ".mp3"
      Assert.assertEquals("Incorrect narration file name", narrationFileName, testName)

      var audioFiles = getAudioFiles(slide) 
      getAudioFiles(slide)
      if (audioFiles != null) {
         val bFiles = audioFiles.size > 0
         Assert.assertTrue("Audio translate files existence incorrect", bFiles == bAudioExists)
      }

      // check the toolbar buttons for visibility
      // play and list only show up if there are existing audio draft files
      val micButton = toolbarFragView!!.findViewById<View>(R.id.start_recording_button) as ImageButton?
      val playButton = toolbarFragView.findViewById<View>(R.id.play_recording_button) as ImageButton?
      val listButton = toolbarFragView.findViewById<View>(R.id.list_recordings_button) as ImageButton?
      Assert.assertNotNull("Mic button does not exist", micButton)
      if (bList) {
         Assert.assertNotNull("Play button does not exist", playButton)
         Assert.assertNotNull("List button does not exist", listButton)
         Assert.assertTrue("Mic button should be visible", micButton!!.visibility == View.VISIBLE)
         Assert.assertTrue(
            "Play button visibility is incorrect",
            (playButton!!.visibility == View.VISIBLE) == bAudioExists
         )
         Assert.assertTrue(
            "List button visibility is incorrect",
            (listButton!!.visibility == View.VISIBLE) == bAudioExists
         )
      }
   }

   fun doModifyAudioFiles(frag : MultiRecordFrag, fragView : View?, toolbarFragView : View?, slideNum : Int, bList : Boolean) {
      // add an audio file and check for proper operation
      doAddAudioFile(frag, fragView, toolbarFragView, slideNum, bList)

      // add 3 more audio files to check that the code properly respects
      // the maximum number of files to keep
      doAddAudioFile(frag, fragView, toolbarFragView, slideNum, bList)
      doAddAudioFile(frag, fragView, toolbarFragView, slideNum, bList)
      doAddAudioFile(frag, fragView, toolbarFragView, slideNum, bList)

      // check the operations of the list files buttons in the toolbar
      doCheckAudioFilesList(frag, fragView, toolbarFragView, slideNum, bList, true)
   }

   // this method tests the operations that occur when you hit the record button and
   // add a new audio file to the story project
   fun doAddAudioFile(frag : MultiRecordFrag, fragView : View?, toolbarFragView : View?, slideNum : Int, bList : Boolean) {
      val slide = Workspace.activeStory.slides[slideNum]

      // get the name of the audio file that would be created by the audio recorder
      // and add it to the story object
      val startFilesSize = getAudioFiles(slide).size
      val fileName = assignNewAudioRelPath()
      val addedFilesSize = getAudioFiles(slide).size
      Assert.assertEquals("File was not added during create", addedFilesSize, startFilesSize + 1)

      // Copying the test audio file to the location given in assignNewAudioRelPath
      // simulates what the audio recorder.  It will take the recorded audio and
      // store it in the file with the desired name
      val srcName = Workspace.workdocfile.uri.path + "/testCopy/" + TestAudioFileName
      val dstName = Workspace.workdocfile.uri.path + "/" + mStory!!.title + '/' + fileName
      copyFile(srcName, dstName)

      // cleanupOlder files is called after the file has been created to
      // ensure that there is only the maximum amount of draft files (3)
      // that are being stored on the drive
      frag.getRecordToolbar().getRecorder()!!.cleanupOlderFiles()
      frag.getRecordToolbar().updateInheritedToolbarButtonVisibility()
      val doneFiles = getAudioFiles(slide)
      if ((addedFilesSize > 3) && bList) {
         Assert.assertEquals("File list should not exceed the max of 3", doneFiles.size, 3)
      }
      else {
         Assert.assertEquals("Cleanup should not remove any file", doneFiles.size, addedFilesSize)
      }

      // test AudioFiles.kt = see the selected audio file and its display name
      if (bList) {  // no chosen name if no list
         val combinedName = getChosenCombName(slideNum)
         Assert.assertNotNull("Unable to find combinedName", combinedName)
         Assert.assertTrue(
            "Chosen comb name should be the original file name",
            combinedName.indexOf(fileName) >= 0
         )
         val displayName = getChosenDisplayName(slideNum)
         Assert.assertNotNull("Unable to find displayName", displayName)
         var displayPrefix = Workspace.activePhase.getDirectorySafeName()
         Assert.assertEquals(
            "Incorrect display name",
            displayName,
            displayPrefix + " " + addedFilesSize
         )
      }
      doCheckAudioRecordAudioContent(frag, fragView, toolbarFragView, Workspace.activeSlideNum, bList, true)
   }

   // this method tests the operations that occur when you hit the files list button and
   // do the various operations.  This assumes it starts with the maximumum number of files
   // in the file list (i.e. 3).  See RecordingsListAdapter for this functionality
   fun doCheckAudioFilesList(frag : MultiRecordFrag, fragView : View?, toolbarFragView : View?, slideNum : Int, bList : Boolean, bFileExists : Boolean) {
      val slide = Workspace.activeStory.slides[slideNum]

      // first check the starting number of files in the list
      var displayNames = getRecordedDisplayNames(slideNum)
      var fileNames =  getRecordedAudioFiles(slideNum)
      var numFiles = 3
      if (!bList) {
         numFiles = 4
      }
      Assert.assertNotNull("Could not find displayNames", displayNames)
      Assert.assertNotNull("Could not find displayNames", displayNames)
      Assert.assertEquals("Incorrect number of displayNames", displayNames!!.size, numFiles)
      Assert.assertEquals("Incorrect number of file Names", fileNames!!.size, numFiles)

      if (bList) {  // no chosen name if no list function
         // next set a different index for the chosen file (simulates a single click on the file)
         var combinedName = getChosenCombName(slideNum)
         Assert.assertNotNull("Unable to find combinedName", combinedName)
         var displayName = getChosenDisplayName(slideNum)
         Assert.assertNotNull("Unable to find displayName", displayName)
         val displayIndex = displayNames!!.indexOf(displayName)
         //val fileIndex = fileNames.indexOf(combinedName)
         //Assert.assertEquals("displayIndex should match fileIndex", displayIndex, fileIndex)

         if (displayNames!!.size > 1) {  // if only one, then you cannot change it
            setChosenFileIndex(displayNames!!.size - 1, slideNum)

            val combinedName2 = getChosenCombName(slideNum)
            Assert.assertNotNull("Unable to find combinedName", combinedName2)
            val displayName2 = getChosenDisplayName(slideNum)
            Assert.assertNotNull("Unable to find displayName", displayName2)
         }

         // now modify the file name for the chosen file (simulates a long click on the file)

         setChosenFileIndex(0, slideNum)
         combinedName = getChosenCombName(slideNum)
         displayName = getChosenDisplayName(slideNum)
         fileNames = getRecordedAudioFiles(slideNum)
         var fileName = fileNames[0]
         val newFileName = "TestDisplayName"

         updateDisplayName(0, newFileName)
         setChosenFileIndex(0, slideNum)

         combinedName = getChosenCombName(slideNum)
         displayName = getChosenDisplayName(slideNum)
         fileNames = getRecordedAudioFiles(slideNum)
         val fileName2 = fileNames[0]
         Assert.assertEquals("Display name not updated", displayName, newFileName)
         Assert.assertTrue(
            "Combo name should include the new display name",
            combinedName.indexOf(newFileName) >= 0
         )
         Assert.assertEquals(
            "File name should not change, even if display name changes",
            fileName,
            fileName2
         )

         // finally delete the selected file
         doDeleteFileTest(slideNum)

         // delete all the rest of the files, too
         doDeleteFileTest(slideNum)
         doDeleteFileTest(slideNum)

         // check they are all gone
         displayNames = getRecordedDisplayNames(slideNum)
         Assert.assertEquals(
            "Display names should be empty after delete all",
            displayNames!!.size,
            0
         )
      }
   }

   fun doDeleteFileTest(slideNum : Int) {
      // See RecordingsListAdapter for the delete functionality
      var displayNames = getRecordedDisplayNames(slideNum)
      var fileNames =  getRecordedAudioFiles(slideNum)
      val displayNamesSize = displayNames!!.size
      val fileNamesSize = fileNames.size
      val combinedName = getChosenCombName(slideNum)
      val displayName = getChosenDisplayName(slideNum)

      var fileName = Workspace.workdocfile.uri.path + '/' + Workspace.activeStory.title + '/' + fileNames[0]
      // var file = File(fileName)
      // Assert.assertTrue("The audio file exists before delete", file.exists())

      deleteAudioFileFromList(mActivity!!, 0)

      // check that the lists are properly updated
      displayNames = getRecordedDisplayNames(slideNum)
      fileNames =  getRecordedAudioFiles(slideNum)
      Assert.assertEquals("Filenames size does not change after delete", fileNames.size, fileNamesSize-1)
      Assert.assertEquals("Display names size does not change after delete", displayNames!!.size, displayNamesSize-1)

      val combinedName2 = getChosenCombName(slideNum)
      val displayName2 = getChosenDisplayName(slideNum)
      Assert.assertNotEquals("Selected combo name not modified by delete", combinedName2, combinedName)
      Assert.assertNotEquals("Selected display name not modified by delete", displayName2, displayName)

      if (displayNames!!.size == 0) {
         Assert.assertEquals("Last display name should be null after last delete", displayName2, "")
         Assert.assertEquals("Last combo name should be null after last delete", combinedName2, "")
      }

      // check that the file is actually gone.
      var file = File(fileName)
      Assert.assertFalse("The audio file has not been deleted", file.exists())

   }

   fun startAudioRecordActivity() : PagerBaseActivity {
      registration.complete = true
      Workspace.activePhase = Phase(getPhaseType())
      Workspace.activeSlideNum = 0
      val pagerBaseActivity = Robolectric.buildActivity(PagerBaseActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      Workspace.initFirebaseAnalytics(pagerBaseActivity)
      return pagerBaseActivity
   }

   fun startPagerFragment(position : Int) : MultiRecordFrag {
      Workspace.activePhase = Phase(getPhaseType())
      val mViewPager: ViewPager = mActivity!!.findViewById<ViewPager>(R.id.pager)
      val multiRecordAdapter = mViewPager.adapter as org.tyndalebt.storyproduceradv.controller.pager.PagerAdapter
      var multiRecordFrag = multiRecordAdapter.getItem(position) as MultiRecordFrag

      val fragmentManager: FragmentManager = mActivity!!.supportFragmentManager
      val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
      fragmentTransaction.add(multiRecordFrag, null)
      fragmentTransaction.commit()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      multiRecordFrag.onCreateView(multiRecordFrag.onGetLayoutInflater(null), null, null)
      return multiRecordFrag
   }

   fun startPagerFragmentView(frag : MultiRecordFrag) : View? {
      return frag.onCreateView(frag.onGetLayoutInflater(null), null, null)
   }

   fun startRecordingToolbarFragmentView(frag : MultiRecordFrag) : View? {
      return frag.getRecordToolbar().onCreateView(frag.getRecordToolbar().onGetLayoutInflater(null), null, null)
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // init temp dir
      val newUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)
      dir.mkdir()

      // copy the files for testing
      initTestFile(TestAudioFileName)
      initTestFile(TestPictureName)
   }

   fun initTestFile(fileName : String) {
      val srcName = baseDocUri!!.path + "/files/" + fileName
      val dstName = Workspace.workdocfile.uri.path + "/testCopy/" + fileName
      copyFile(srcName, dstName)
   }

   fun copyFile(srcName : String, dstName : String) {

      val dstPath : Path = Paths.get(dstName)
      val srcPath: Path = Paths.get(srcName)
      Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING)
   }

   fun checkPictureFile(imageFileName : String, bExists : Boolean) {
      // check that the picture got copied to the location in the project directory
      val path =
         Workspace.workdocfile.uri.path + "/" + Workspace.activeStory.title + "/" + imageFileName
      val file = File(path)
      Assert.assertEquals("File exist failure: " + imageFileName, file.exists(), bExists)
   }
}
