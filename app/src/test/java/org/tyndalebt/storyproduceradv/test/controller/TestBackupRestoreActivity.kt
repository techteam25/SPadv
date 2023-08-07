package org.tyndalebt.storyproduceradv.test.controller

import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.controller.storylist.BackupRestoreActivity
import org.tyndalebt.storyproduceradv.controller.storylist.BackupStoryPageFragment
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageFragment
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import org.tyndalebt.storyproduceradv.tools.file.UriUtils
import org.tyndalebt.storyproduceradv.tools.file.getFileType
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestBackupRestoreActivity : BaseActivityTest() {

   val cProjectName2 = "003 One Lost Sheep"
   var mBackupUri : Uri? = null
   var mStoryName : String? = null

   //
   // Test: storyListTest
   //
   // Purpose:
   //    Tests loading of Workspace Stories list and proper
   //    initialization of the BackupRestoreActivity UI with the stories.
   //
   // Steps:
   //    1. Initialize the projects directories
   //    2. Open the BackupRestoreActivity
   //    3. Use updateStories to populate the Worspace story list
   //    4. Test the stories are properly loaded
   //    5. Test the story list fragemnt that it is properly updated
   //
   // Author: Ray Kaestner 04-06-2023
   //

   @Test
   fun storyListTest() {

      // init environment
      initProjectFiles(false)
      initProjectFiles2()
      val backupRestoreActivity = startBackupRestoreActivity()
      try {

         // FIRST: Test the project setup
         backupRestoreActivity.controller.updateStories()  // this is normally done in Activity.initWorkspace()
         //Assert.assertNotNull("BackupRestoreActivity.readingTemplatesDialog should not be null during load", BackupRestoreActivity.readingTemplatesDialog)

         java.util.concurrent.TimeUnit.SECONDS.sleep(5)  // pause a bit to allow async code to catch up
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()     // allow other threads to run, for the story loading
         java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // one last chance for other threads
         //Assert.assertNull("BackupRestoreActivity.readingTemplatesDialog should be null after load", BackupRestoreActivity.readingTemplatesDialog)

         val storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
         Assert.assertEquals("Incorrect nunber of stories", 2, storyCount)
         // could add some testing for story loading

         val itemCount = backupRestoreActivity.storyPageViewPager.adapter?.itemCount
         Assert.assertEquals("Incorrect number of available storyPageViePager.items", 1, itemCount)

         val storyPageAdapter = backupRestoreActivity.storyPageViewPager.adapter as org.tyndalebt.storyproduceradv.controller.storylist.StoryPageAdapter
         var frag = startBackupStoryPageFragment(backupRestoreActivity)

         checkFragment(frag, 0, 2, 2)

         checkBackup(backupRestoreActivity, frag)
         checkDelete(backupRestoreActivity, frag)
         checkRestore(backupRestoreActivity, frag)

         // Test filters
         // test button enabling/disabling for backup/restore/delete
      }
      catch (ex : Throwable) {
         ex.printStackTrace()
         throw ex
      }
      finally {
         cleanTempDirectories(backupRestoreActivity)
      }
   }

   fun checkFragment (fragment : StoryPageFragment, position : Int, storyCount : Int, listCount : Int) {
      Assert.assertNotNull("Unable to find fragment: " + position, fragment)
      Assert.assertNotNull("Unable to find fragment listView: " + position, fragment.listView)
      Assert.assertNotNull("Unable to find fragment adapter: " + position, fragment.adapter)
      Assert.assertEquals("Current story list is incorrect size." , fragment.CurrentStoryList.size, storyCount)
      Assert.assertEquals("Incorrect number of stories in fragment.", fragment.adapter.stories.size, storyCount)

      if (listCount > 0) {
         // check a few more details
         fragment.adapter.getView(0, null, fragment.listView)
         val fileHolder = fragment.adapter.getFileHolderAt(0)
         Assert.assertNotNull("Unable to find fileHolder(0): " + position, fileHolder)
         Assert.assertNotNull("Should be a checkbox for mainActivity", fileHolder!!.checkBox)
         Assert.assertNull("Should be no icon for mainActivity", fileHolder!!.imgIcon)
         fragment.adapter.getView(1, null, fragment.listView)
      }
   }

   fun checkBackup(backupRestoreActivity : BackupRestoreActivity, frag : BackupStoryPageFragment) {

      // select item to back up
      var fileHolder = frag.adapter.getFileHolderAt(0)
      Assert.assertFalse("Selected flag should initially be false for row 0", fileHolder!!.selected)
      frag.selectRow(0, fileHolder)
      Assert.assertTrue("Selected flag should be set for row 0", fileHolder!!.selected)
      Assert.assertEquals("Number of selections should be 1", frag.getSelectionCount(), 1)
      mStoryName = frag.CurrentStoryList.get(0).title

      // check and set backup directory
      //Assert.assertEquals("Workspace.storyBackupPath should initially be null.", Workspace.storyBackupPath, "/")

      setupBackupDir()
      var backupDirPath = UriUtils.getUIPathText(backupRestoreActivity, Workspace.storyBackupPath.uri)
      Assert.assertNotNull("getUIPathText() for real Dir should not be null.", backupDirPath)

      val bogusUri = Uri.parse(Workspace.storyBackupPath.uri.toString() + Uri.encode("/XXXX"))
      backupDirPath = UriUtils.getUIPathText(backupRestoreActivity, bogusUri)
      Assert.assertNull("getUIPathText() for bogusDir should be null.", backupDirPath)

      backupDirPath = UriUtils.getUIPathText(backupRestoreActivity, Workspace.storyBackupPath.uri)
      Assert.assertNotNull("getUIPathText() for real Dir should not be null.", backupDirPath)

      // test successful backup
      var msg = frag.checkForExistingBackupFiles(Workspace.storyBackupPath.uri)
      Assert.assertNull("No existing file should be detected initially.", msg)
      frag.backupStories()

      val backupDir = File(Workspace.storyBackupPath.uri.path!!)
      Assert.assertTrue("BackupDir should exist", backupDir.exists())
      val fileList = backupDir.listFiles()
      Assert.assertEquals("Backup directory should have one child.", fileList.size, 1)
   }

   fun checkDelete(backupRestoreActivity : BackupRestoreActivity, frag : BackupStoryPageFragment) {

      // test file exists from previous backup
      var fileHolder = frag.adapter.getFileHolderAt(0)
      Assert.assertFalse("Selected flag should initially be false for row 0", fileHolder!!.selected)
      frag.selectRow(0, fileHolder)
      var msg = frag.checkForExistingBackupFiles(Workspace.storyBackupPath.uri)
      Assert.assertNotNull("An existing file should be detected after the backup.", msg)

      var storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
      Assert.assertEquals("Incorrect number of stories before delete", 2, storyCount)
      Assert.assertEquals("Current story list is incorrect size before delete." , frag.CurrentStoryList.size, storyCount)
      Assert.assertEquals("Incorrect number of stories in fragment before delete.", frag.adapter.stories.size, storyCount)

      // test delete of story
      frag.deleteStories(Workspace.workdocfile.uri)
      frag.updateStoriesAfterDelete()

      // test that story list is updated
      storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
      Assert.assertEquals("Incorrect number of stories after delete", 1, storyCount)
      Assert.assertEquals("Current story list is incorrect size after delete." , frag.CurrentStoryList.size, storyCount)
      Assert.assertEquals("Incorrect number of stories in fragment after delete.", frag.adapter.stories.size, storyCount)
   }

   fun checkRestore(backupRestoreActivity : BackupRestoreActivity, frag : BackupStoryPageFragment) {

      // test story project does not exist
      var backupDirName = Workspace.storyBackupPath.uri.path + "/" + mStoryName
      var storyDirName = Workspace.workdocfile.uri.path + "/" + mStoryName
      var backupDir : File = File(backupDirName)
      var storyDir : File = File(storyDirName)
      Assert.assertTrue("BackupDir should exist", backupDir.exists())
      Assert.assertFalse("Story should not exist before restore", storyDir.exists())

      val srcUri = Uri.parse(Workspace.storyBackupPath.uri.toString() + Uri.encode("/$mStoryName"))
      val dstUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$mStoryName"))
      Assert.assertNotNull ("srcUri does not exist for restore", getFileType(backupRestoreActivity, srcUri))
      Assert.assertNull ("dstUri should not exist before restore", getFileType(backupRestoreActivity, dstUri))

      frag.doRestoreStory(srcUri, mStoryName!!)

      // Test that copy occurred
      Assert.assertTrue("Story should exist after restore", storyDir.exists())
      Assert.assertNotNull ("dstUri should exist after restore", getFileType(backupRestoreActivity, dstUri))

      // test that story list is updated
      var storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
      Assert.assertEquals("Incorrect number of stories after delete", 2, storyCount)
      Assert.assertEquals("Current story list is incorrect size after delete." , frag.CurrentStoryList.size, storyCount)
      Assert.assertEquals("Incorrect number of stories in fragment after delete.", frag.adapter.stories.size, storyCount)
   }


   fun startBackupRestoreActivity() : BackupRestoreActivity {
      registration.complete = true
      val BackupRestoreActivity = Robolectric.buildActivity(BackupRestoreActivity::class.java).create().get()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return BackupRestoreActivity
   }

   fun startBackupStoryPageFragment(BackupRestoreActivity : BackupRestoreActivity) : BackupStoryPageFragment {
      val storyPageAdapter = BackupRestoreActivity.storyPageViewPager.adapter as org.tyndalebt.storyproduceradv.controller.storylist.BackupStoryAdapter
      var storyPageFrag = storyPageAdapter.createFragment(0) as BackupStoryPageFragment

      val fragmentManager: FragmentManager = BackupRestoreActivity.getSupportFragmentManager()
      val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
      fragmentTransaction.add(storyPageFrag, null)
      fragmentTransaction.commit()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      storyPageFrag.onCreateView(storyPageFrag.onGetLayoutInflater(null), null, null)

      //var storyPageFrag2 = Robolectric.buildFragment(BackupStoryPageFragment::class.java).create().get()

      return storyPageFrag
   }

   fun initProjectFiles2()  {
      // adds a second project to the list
      copyStory(cProjectName2, cProjectName2, false)
   }

   fun setupBackupDir() {

      val newUri = Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/testBackup"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)
      dir.mkdir()

      val df2 = androidx.documentfile.provider.DocumentFile.fromFile(dir)
      Workspace.storyBackupPath = df2
      mBackupUri = newUri;
   }
}
