package org.tyndalebt.storyproduceradv.test.controller

import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.export.VideoActivity
import org.tyndalebt.storyproduceradv.controller.export.VideoListHelper
import org.tyndalebt.storyproduceradv.controller.export.ExportedVideosAdapter
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestVideoActivity : BaseActivityTest() {

   var mVideoActivity : VideoActivity? = null
   var mCopyUri : Uri? = null

   //
   // Test: videoActivityTest
   //
   // Purpose:
   //    Tests functionality for the video copy activity
   //
   // Steps:
   //    1. Initialize the video activity screen
   //    2. Test the list of videos is properly populated
   //    3. select a video and copy it
   //    4. Check that the copy code detects an existing video file
   //    5. Test the proper display of the copy path, e.g.
   //       internal, sdcard, and external
   //
   // Author: Ray Kaestner 06-15-2023
   //

   @Test
   fun videoActivityTest() {

      // init environment
      initProjectFiles(false)
      mVideoActivity = startVideoActivity()
      try {
         setupCopyDir()
         initVideoList(false)

         val helper = mVideoActivity!!.getHelper()
         val adapter = helper.getVideosAdapter()
         val count = adapter.count
         Assert.assertEquals("Incorrect nunber of videos recognized", 4, count)

         // specify a copy directory and copy some files with the API
         var vidListView = helper.getVideosListView()
         val view = adapter.getView(0, vidListView!!, vidListView!!)
         val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
         checkbox.setChecked(true)
         helper.copyVideos(Workspace.videoCopyPath.uri)
         checkCopyDir(1)
         checkCopyFile(checkbox.text.toString())

         // check for detection of existing files
         checkbox.setChecked(true)  // make sure that it is still checked
         val bExists = helper.checkForExistingFiles(Workspace.videoCopyPath.uri)
         Assert.assertTrue("Did not find existing file: " + checkbox.text, bExists)

         // Do some testing on getUIPathText
         val copyDirPath = helper.getUIPathText(Workspace.videoCopyPath.uri)
         Assert.assertEquals("getUIPathText should return dir name.", copyDirPath, Workspace.videoCopyPath.uri.path)

         val bogusUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode("videosxxx"))
         val bogusDirPath = helper.getUIPathText(bogusUri)   // from sdcard
         Assert.assertNull("Bogus uri should return null UIPathTest: " + bogusUri.path, bogusDirPath)

         val sdcardPath = helper.getUIPathTextInternal(Workspace.videoCopyPath.uri, "/storage/1234-5678" + Workspace.videoCopyPath.uri.path)
         Assert.assertEquals("getUIPathText should return dir name.", sdcardPath, "[sdcard]" + Workspace.videoCopyPath.uri.path)
         val usbPath = helper.getUIPathTextInternal(Workspace.videoCopyPath.uri, "/dev/bus/usb/123/456" + Workspace.videoCopyPath.uri.path)
         Assert.assertEquals("getUIPathText should return dir name.", usbPath, "[external]" + Workspace.videoCopyPath.uri.path)
      }
      catch (ex : Exception) {
         ex.printStackTrace()
      }
      finally {
         cleanTempDirectories(mVideoActivity!!)
      }
   }

   //
   // Test: videoActivityCheckboxTest
   //
   // Purpose:
   //    Tests video activity checkboxes
   //
   // Steps:
   //    1. Initialize the video activity screen
   //    2. Check the mp4/3gp checkboxes and see
   //       that the proper videos are selected
   //    3. Deselect an mp4/3gp video and see that
   //       the main mp4/3gp checkboxes getunselected
   //    4. Select the remaining mp4/3gp video and
   //       see that the main mp4/3gp checkboxes
   //       get selected again.
   //
   // Author: Ray Kaestner 06-15-2023
   //

   @Test
   fun videoActivityCheckboxTest() {

      // init environment
      initProjectFiles(false)
      mVideoActivity = startVideoActivity()
      try {
         setupCopyDir()
         initVideoList(false)

         val helper = mVideoActivity!!.getHelper()
         val adapter = helper.getVideosAdapter()
         val count = adapter.count
         Assert.assertEquals("Incorrect nunber of videos recognized", 4, count)

         var gpBtn: CheckBox = mVideoActivity!!.findViewById(R.id.dumbphone_3gp)
         var mp4Btn: CheckBox = mVideoActivity!!.findViewById(R.id.smartphone_mp4)


         // Deselect/select individual video files and see the
         // mp4button and gp3button follow the selections
         var vidListView = helper.getVideosListView()

         // try checking indidual file checkboxes
         checkFileCheckBoxes(false)
         checkFileCheckBoxes(true)

         // test combinations of main radio buttons
         checkSelectedVideos(false, false)
         gpBtn.setChecked(true)
         checkSelectedVideos(false, true)

         gpBtn.setChecked(false)
         checkSelectedVideos(false, false)

         mp4Btn.setChecked(true)
         checkSelectedVideos(true, false)

         mp4Btn.setChecked(false)
         checkSelectedVideos(false, false)

         mp4Btn.setChecked(true)
         gpBtn.setChecked(true)
         checkSelectedVideos(true, true)
      }
      catch (ex : Exception) {
         ex.printStackTrace()
      }
      finally {
         cleanTempDirectories(mVideoActivity!!)
      }
   }

   fun startVideoActivity() : VideoActivity {
      registration.complete = true
      val videoActivity = Robolectric.buildActivity(VideoActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      return videoActivity
   }

   fun setupCopyDir() {

      val newUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testCopy"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)
      dir.mkdir()

      val df2 = androidx.documentfile.provider.DocumentFile.fromFile(dir)
      Workspace.videoCopyPath = df2
      mCopyUri = newUri;
   }

   fun checkCopyDir(expected : Int) {

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

   fun initVideoList(bSel : Boolean) {
      val helper = mVideoActivity!!.getHelper()
      val adapter = helper.getVideosAdapter()
      val count = adapter.count
      var vidListView = helper.getVideosListView()

      // create the list views
      for (ctr in 0 until count) {
         val view = adapter.getView(ctr, vidListView!!, vidListView!!)
      }

      // set the checkbox selected
      for (ctr in 0 until count) {
         val view = adapter.getView(ctr, vidListView!!, vidListView!!)
         val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
         checkbox.setChecked(bSel)
      }
   }

   fun checkFileCheckBoxes(bMP4 : Boolean) {
      val helper = mVideoActivity!!.getHelper()
      val adapter = helper.getVideosAdapter()
      var vidListView = helper.getVideosListView()
      var startIndex = 1  // for mp4
      if (!bMP4) {
         startIndex = 0
      }

      helper.clearSelection()  // clear all the checkboxes

      val view = adapter.getView(startIndex, vidListView!!, vidListView!!)
      val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
      checkbox.setChecked(true)
      checkMainChecks(false, false)

      val view2 = adapter.getView(startIndex+2, vidListView!!, vidListView!!)
      val checkbox2: CheckBox = view2.findViewById(R.id.video_title_cb)
      checkbox2.setChecked(true)
      checkMainChecks(bMP4, !bMP4)

      helper.clearSelection()
      checkMainChecks(false, false)
   }

   fun checkSelectedVideos(bMP4 : Boolean, b3GP : Boolean) {
      val helper = mVideoActivity!!.getHelper()
      val adapter = helper.getVideosAdapter()
      val count = adapter.count
      var vidListView = helper.getVideosListView()

      // set the checkbox selected
      for (ctr in 0 until count) {
         val view = adapter.getView(ctr, vidListView!!, vidListView!!)
         val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
         if (helper.isFileMp4(checkbox.text.toString())) {
            Assert.assertEquals("MP4 checkbox status is incorrect: " + checkbox.text.toString(),
                    checkbox.isChecked, bMP4)
         }
         if (helper.isFile3gp(checkbox.text.toString())) {
            Assert.assertEquals("3GP checkbox status is incorrect: " + checkbox.text.toString(),
                    checkbox.isChecked, b3GP)
         }
      }
   }

   fun checkMainChecks(bMP4 : Boolean, b3GP : Boolean) {

      var gpBtn: CheckBox = mVideoActivity!!.findViewById(R.id.dumbphone_3gp)
      var mp4Btn: CheckBox = mVideoActivity!!.findViewById(R.id.smartphone_mp4)

      Assert.assertEquals("Main 3GP checkbox status is incorrect" ,
              gpBtn.isChecked, b3GP)
      Assert.assertEquals("Main MP4 checkbox status is incorrect" ,
              mp4Btn.isChecked, bMP4)
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // copy the videos files for testing
      val srcUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode("videos"))
      val dstUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode("videos"))

      deleteDirectory(dstUri.path)

      copyDirectory(srcUri.path, dstUri.path)
   }

}
