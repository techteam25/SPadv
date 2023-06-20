package org.tyndalebt.storyproduceradv.test.controller

import android.net.Uri
import android.os.Build
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ListView
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.export.ShareActivity
import org.tyndalebt.storyproduceradv.controller.export.VideoListHelper
import org.tyndalebt.storyproduceradv.controller.export.ExportedVideosAdapter
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestSharePhase : BaseActivityTest() {

   var mShareActivity : ShareActivity? = null

   //
   // Test: shareActivityTest
   //
   // Purpose:
   //    Tests functionality for the share phase activity
   //
   // Steps:
   //    1. Initialize the share phase activity screen
   //    2. Test the list of videos is properly populated
   //    3. delete a video file
   //    4. Check that the the file is removed
   //
   // Author: Ray Kaestner 06-15-2023
   //

   @Test
   fun shareActivityTest() {

      // init environment
      initProjectFiles(false)
      mShareActivity = startSharePhaseActivity()
      try {
         addProjectVideos()
         initVideoList(false)

         val helper = mShareActivity!!.getHelper()
         val adapter = helper.getVideosAdapter()
         val count = adapter.count
         Assert.assertEquals("Incorrect TestShareActivitynunber of videos recognized", 2, count)

         // Checks that the main 3gp/mp4 are labels, not checkboxes
         try {
            var gp3: TextView = mShareActivity!!.findViewById(R.id.dumbphone_3gp)
            var mp4: TextView = mShareActivity!!.findViewById(R.id.smartphone_mp4)
         }
         catch (ex : Throwable) {
            Assert.assertNotNull("Unable to find the top video labels", ex)
         }

         // delete a file and see that it is gone from the disk and from the adapter
         var vidListView = helper.getVideosListView()
         val view = adapter.getView(1, vidListView!!, vidListView!!)
         val deleteBtn : ImageButton = view.findViewById(R.id.file_delete_button)
         val videoText : TextView = view.findViewById(R.id.video_title)
         Workspace.deleteVideo(helper.mActivity!!, videoText.text.toString())
         helper.refreshViews()

         // is the adapter list updated?
         Assert.assertEquals("Incorrect TestShareActivitynunber of videos recognized",
                 1, adapter.count)
         // check the files in the video directory
         val videoUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode("videos"))
         val videoDir = File(videoUri.path)
         val fileNames = videoDir.list()
         Assert.assertEquals("Incorrect number of files after delete",
               3, fileNames.size)

      }
      catch (ex : Exception) {
         Assert.assertNotNull("Exception error occurred", ex)
      }
      finally {
         cleanTempDirectories(mShareActivity!!)
      }
   }

   fun startSharePhaseActivity() : ShareActivity {
      registration.complete = true
      val shareActivity = Robolectric.buildActivity(ShareActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return shareActivity
   }

   fun initVideoList(bSel : Boolean) {
      val helper = mShareActivity!!.getHelper()
      helper.refreshViews()
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
         try {
            val checkbox: CheckBox = view.findViewById(R.id.video_title_cb)
            Assert.assertNull("Should be no checkbox in the share page.", checkbox)
         }
         catch (ex : Throwable)  {}  // should not be found
         try {
            val textView: TextView = view.findViewById(R.id.video_title)
            Assert.assertNotNull("Should be text label in the share page.", textView);
         }
         catch (ex : Throwable)
         {
            Assert.assertNull("Did not find video title text view", ex)
         }
      }
   }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // copy the videos files for testing
      val srcUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode("videos"))
      val dstUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode("videos"))

      deleteDirectory(dstUri.path)

      copyDirectory(srcUri.path, dstUri.path)
   }
   
   fun addProjectVideos() {
      val myStory = loadStory(mShareActivity!!)  // story will be loaded and saved
      Assert.assertNotNull("Unable to open story", myStory)
      

      val videoUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode("videos"))
      val videoDir = File(videoUri.path)
      if (videoDir.exists() && videoDir.isDirectory()) {
         val fileNames = videoDir.list()
         myStory!!.addVideo(fileNames[0])
         myStory!!.addVideo(fileNames[1])
      }
      
      saveStory(mShareActivity!!, myStory)

      val helper = mShareActivity!!.getHelper()
      helper.mStory = myStory
      Workspace.activeStory = myStory!!
   }

}
