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
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageFragment
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestMainActivity : BaseActivityTest() {

   val cProjectName2 = "003 One Lost Sheep"

   //
   // Test: storyListTest
   //
   // Purpose:
   //    Tests loading of Workspace Stories list and proper
   //    initialization of the MainActivity UI with the stories.
   //
   // Steps:
   //    1. Initialize the projects directories
   //    2. Open the MainActivity
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
      val mainActivity = startMainActivity()
      try {
         // readingTemplatesDialog is private
         //Assert.assertNull("mainActivity.readingTemplatesDialog should be null before load", mainActivity.readingTemplatesDialog)

         // Should this testing be moved to TestSplashScreenActivity?
         // At the end of loading the stories, it automatically activates
         // the MainActivity

         mainActivity.controller.updateStories()  // this is normally done in Activity.initWorkspace()
         //Assert.assertNotNull("mainActivity.readingTemplatesDialog should not be null during load", mainActivity.readingTemplatesDialog)

         java.util.concurrent.TimeUnit.SECONDS.sleep(5)  // pause a bit to allow async code to catch up
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()     // allow other threads to run, for the story loading
         java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // one last chance for other threads
         //Assert.assertNull("mainActivity.readingTemplatesDialog should be null after load", mainActivity.readingTemplatesDialog)

         val storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
         Assert.assertEquals("Incorrect nunber of stories", 2, storyCount)
         // could add some testing for story loading

         val itemCount = mainActivity.storyPageViewPager.adapter?.itemCount
         Assert.assertEquals("Incorrect number of available storyPageViePager.items", 3, itemCount)

         val storyPageAdapter = mainActivity.storyPageViewPager.adapter as org.tyndalebt.storyproduceradv.controller.storylist.StoryPageAdapter
         var frag = startStoryPageFragment(mainActivity, 0)
         var testFrag : StoryPageFragment? = null
         for (i in 1 until storyPageAdapter.itemCount) {
            testFrag = startStoryPageFragment(mainActivity, i)
            Assert.assertNotNull("Could not find fragment: " + i, testFrag)
         }
         checkFragment(frag, 0, 2, 2)

         // test filters
      }
      finally {
         cleanTempDirectories(mainActivity)
      }
   }

   //
   // Test: storySwitchTest
   //
   // Purpose:
   //    Tests the action of switching to a new story.
   //    If a story is not writable, should not be able to
   //    open the story.
   //
   // Steps:
   //    1. Initialize the projects directories
   //    2. Open the MainActivity
   //    3. Switch to a writable story
   //    4. Make a story unwritable and try to switch to it
   //
   // Author: Ray Kaestner 11-08-2023
   //

   @Test
   fun storySwitchTest() {

      // init environment
      initProjectFiles(false)
      initProjectFiles2()
      val mainActivity = startMainActivity()
      try {

         mainActivity.controller.updateStories()  // this is normally done in Activity.initWorkspace()

         java.util.concurrent.TimeUnit.SECONDS.sleep(5)  // pause a bit to allow async code to catch up
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()     // allow other threads to run, for the story loading
         java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // one last chance for other threads
         //Assert.assertNull("mainActivity.readingTemplatesDialog should be null after load", mainActivity.readingTemplatesDialog)

         val storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
         Assert.assertEquals("Incorrect nunber of stories", 2, storyCount)
         // could add some testing for story loading

         Assert.assertEquals("The initial active story should be null", Workspace.activeStory.title, "")

         mainActivity.switchToStory(Workspace.Stories[0])
         Assert.assertEquals("Switching to the story did not succeed", Workspace.activeStory.title, Workspace.Stories[0].title)

         val uri = Workspace.Stories[1].getStoryUri()
         val file = File(uri!!.path)
         file.setReadOnly()
         mainActivity.switchToStory(Workspace.Stories[1])
         Assert.assertEquals("Should not be able to switch to a read-only story", Workspace.activeStory.title, Workspace.Stories[0].title)

      }
      finally {
         cleanTempDirectories(mainActivity)
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
         fragment.adapter.getView(position, null, fragment.listView)
         val fileHolder = fragment.adapter.getFileHolderAt(0)
         Assert.assertNotNull("Unable to find fileHolder(0): " + position, fileHolder)
         Assert.assertNull("Should be no checkbox for mainActivity", fileHolder!!.checkBox)
         Assert.assertNotNull("Should be an icon for mainActivity", fileHolder!!.imgIcon)
      }
   }

   fun startMainActivity() : MainActivity {
      registration.complete = true
      val mainActivity = Robolectric.buildActivity(MainActivity::class.java).create().get()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return mainActivity
   }

   fun startStoryPageFragment(mainActivity : MainActivity, position : Int) : StoryPageFragment {
      val storyPageAdapter = mainActivity.storyPageViewPager.adapter as org.tyndalebt.storyproduceradv.controller.storylist.StoryPageAdapter
      var storyPageFrag = storyPageAdapter.createFragment(position) as StoryPageFragment

      val fragmentManager: FragmentManager = mainActivity.getSupportFragmentManager()
      val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
      fragmentTransaction.add(storyPageFrag, null)
      fragmentTransaction.commit()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

      storyPageFrag.onCreateView(storyPageFrag.onGetLayoutInflater(null), null, null)

      return storyPageFrag
   }

   fun initProjectFiles2()  {
      // adds a second project to the list
      copyStory(cProjectName2, cProjectName2, false)
   }
}
