package org.tyndalebt.storyproduceradv.test.controller

import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.CheckBox
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.export.FinalizeActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import org.tyndalebt.storyproduceradv.tools.file.UriUtils
import org.tyndalebt.storyproduceradv.tools.media.story.AutoStoryMaker
import org.tyndalebt.storyproduceradv.tools.sqlite.DatabaseHelper
import org.tyndalebt.storyproduceradv.tools.sqlite.EventTable
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestFinalizePhase : BaseActivityTest() {

   var mFinalizeActivity : FinalizeActivity? = null
   var mCopyUri : Uri? = null

   //
   // Test: FinalizeActivityTest
   //
   // Purpose:
   //    Tests functionality for the video copy activity
   //
   // Steps:
   //    1. Initialize the video activity screen
   //    2. Initialize the storymaker object
   //    3. create the video
   //    4. Test the video is properly created
   //
   // Author: Ray Kaestner 09-15-2023
   //

   // This would be a great test.  The code below is taken from
   // FinalizeActivity. startExport nad it creates the video file.
   // But unfortunately, the act of stub class for Robolectric is
   // incomplete and video creation throws an NPE
/*
   @Test
   fun FinalizeActivityTest() {

      // init environment
      initProjectFiles(false)
      mFinalizeActivity = startFinalizeActivity()
      try {
         val myStory = loadStory(mFinalizeActivity!!)
         Workspace.activeStory = myStory!!

         var storyMaker = AutoStoryMaker(this)

         storyMaker!!.mIncludeBackgroundMusic = false
         storyMaker!!.mIncludePictures = false  // prevents 3gp from being saved
         storyMaker!!.mIncludeText = true
         storyMaker!!.mIncludeKBFX = true
         storyMaker!!.mIncludeSong = false

         storyMaker!!.videoRelPath = mOutputPath
         storyMaker!!.start()
      }
      catch (ex : Exception) {
         ex.printStackTrace()
      }
      finally {
         cleanTempDirectories(mFinalizeActivity!!)
      }
   }
*/

   //
   // Test: FinalizeActivityTest
   //
   // Purpose:
   //    Tests functionality for the video copy activity
   //
   // Steps:
   // Steps:
   //    1. Initialize the video activity screen
   //    2. Initialize the storymaker object
   //    3. Set up the temporary video file as if
   //       it had been created by storymaker
   //    4. Call storymaker.created() to see proper
   //       handling of the video files after they have 
   //       been created.  Files are copied, temp files
   //       removed.  Names are proper.  Messages are proper
   //
   // Author: Ray Kaestner 09-15-2023
   //
   
   @Test
   fun FinalizeActivityTest() {

      // init environment
      initProjectFiles(false)
      mFinalizeActivity = startFinalizeActivity()
      try {
         val myStory = loadStory(mFinalizeActivity!!)
         Workspace.activeStory = myStory!!
         doVideoTest(myStory.title + "_VideoFile.mp4")
         doVideoTest(myStory.title + "_VideoFile2.mp4")
      }
      catch (ex : Exception) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(mFinalizeActivity!!)
      }
   }

   fun doVideoTest(videoFileName : String) {
      var storyMaker = AutoStoryMaker(mFinalizeActivity!!)
      storyMaker!!.mIncludeBackgroundMusic = false
      storyMaker!!.mIncludePictures = false  // prevents 3gp from being saved
      storyMaker!!.mIncludeText = true
      storyMaker!!.mIncludeKBFX = true
      storyMaker!!.mIncludeSong = false

      storyMaker!!.videoRelPath = videoFileName
      val startFile = File(mCopyUri!!.path!! + "/000_MyStoryTest_PxMv.mp4")
      val videoFile = File(Workspace.workdocfile.uri.path + "/videos/" + storyMaker!!.videoRelPath)
      startFile.copyTo(storyMaker!!.videoTempFile)  // simulate file being created

      // RK 09/29 - re-enable if we store events in db instead of to Firebase
      //val dbHelper: DatabaseHelper = DatabaseHelper(mFinalizeActivity!!)
      //var eventList = dbHelper.viewEvent()

      Assert.assertTrue("Temp video file does not exist", storyMaker!!.videoTempFile.exists())
      Assert.assertFalse("Video file should not exist before create", videoFile.exists())
      storyMaker!!.videoCreated()

      // check that temp file is gone
      // check that video file exists in video directory
      Assert.assertFalse("Temp video file should be deleted", storyMaker!!.videoTempFile.exists())
      Assert.assertTrue("Video file should exist after create", videoFile.exists())

      // RK 9/29 Re-enable this if we store events to db instead of Firebase
      //val dbHelper: DatabaseHelper = DatabaseHelper(mFinalizeActivity!!)
      //var eventList2 = dbHelper.viewEvent()
      //Assert.assertEquals("We should find at least one event.", eventList2.size, eventList.size+1)
      //Assert.assertNotNull("Returned video name should not be null.", eventList2[0].video_name)
      //Assert.assertEquals("Returned video name is incorrect.", eventList2[eventList2.size-1].video_name, videoFileName)

   }
   fun startFinalizeActivity() : FinalizeActivity {
      registration.complete = true
      val FinalizeActivity = Robolectric.buildActivity(FinalizeActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      Workspace.initFirebaseAnalytics(FinalizeActivity)
      return FinalizeActivity
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // init temp dir
      val newUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)

      val df2 = androidx.documentfile.provider.DocumentFile.fromFile(dir)
      Workspace.videoCopyPath = df2
      mCopyUri = newUri;

      // copy the videos files for testing
      val srcUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode("videos"))
      copyDirectory(srcUri.path, newUri.path)

      // clear out video directory???
   }

   fun checkVideoDir(expected : Int) {

      val dir = File(mCopyUri!!.path!!)
      Assert.assertTrue("dir not a directory.", dir.isDirectory())
      val fileList = dir.listFiles()
      var found = 0
      if (expected > 0) {
         Assert.assertNotNull("No files found in the copy directory", fileList)
      }
      if (fileList != null) {
         found = fileList.size
      }
      Assert.assertEquals("Incorrect number of copied files found", found, expected)
   }

   fun checkCopyFile(fileName : String) {
      val path = mCopyUri!!.path!! + "/" + fileName
      val file = File(path)
      Assert.assertTrue("File does not exist: " + fileName, file.exists())
   }
}
