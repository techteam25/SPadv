package org.tyndalebt.storyproduceradv.test.controller

import android.os.Build
import android.view.View
import android.widget.ListView
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.DownloadActivity
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadAdapter
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.test.model.BaseActivityTest


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestDownloadActivity : BaseActivityTest() {


   //
   // Test: downloadListTest
   //
   // Purpose:
   //    Tests loading of Workspace Stories list and proper
   //    initialization of the MainActivity UI with the stories.
   //
   // Steps:
   //    1. Initialize the projects directories and DownloadActivity
   //    2. Check the bloomfile list for contents
   //    3. check the language list for contents
   //    4. Check the templates list for contents for the selected language
   //    5. Pick a template and download it and check it
   //    6. Refresh the template list and see that the downloaded template
   ///      is filtered out
   //
   // Author: Ray Kaestner 04-06-2023
   //

   @Test
   fun downloadListTest() {

      // init environment
      initProjectFiles(false)
      val downloadActivity = startDownloadActivity()
      try {
         updateWorkspaceStoryList(downloadActivity)

         var storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
         Assert.assertEquals("Incorrect nunber of stories", 1, storyCount)

         val indexEnglish = checkLanguageList(downloadActivity)

         // pick a language and see what the list looks like
         selectLanguageItem(downloadActivity, indexEnglish)
         val index004 = checkTemplateList(downloadActivity)

         // download a template, see that it is there.
         //selectTemplateItem(downloadActivity, index004)
         // The following code simulates DownloadActivity.clickListener for download button
         val urlList = BuildURLList(downloadActivity, index004)
         Assert.assertNotNull("Download urllist is empty", urlList)
         Assert.assertTrue("Download urllist should have one item", urlList!!.size == 1)
         downloadActivity.at.execute(*urlList)

         // the act of downloading the bloom file can take a while
         // the following delay times may need to be adjusteded based
         // upon network speed and processor speed.
         // see DownloadFileFromURL.doInBackground()
         // also see DownloadFileFromURL.postExecute()
         // StoryIO.unzipIfZipped() will unzip the  file
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
         java.util.concurrent.TimeUnit.SECONDS.sleep(60)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
         java.util.concurrent.TimeUnit.SECONDS.sleep(30)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // downloadFile.onPostExecute
         java.util.concurrent.TimeUnit.SECONDS.sleep(30)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // downloadFile.onPostExecute
         java.util.concurrent.TimeUnit.SECONDS.sleep(30)   // pause just a bit more
         ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // downloadFile.onPostExecute

         // check the template see and see that the selected template is missing
         storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
         Assert.assertEquals("Incorrect nunber of stories after download", 2, storyCount)
      }
      catch (ex : Exception) {
         ex.printStackTrace()
         throw ex
      }
      finally {
         cleanTempDirectories(downloadActivity)
      }
   }

   // Modified and adapted from DownloadActivity
   fun BuildURLList(downloadActivity : DownloadActivity, index : Int): Array<String?>? {

      val listView = downloadActivity.findViewById<View>(R.id.bloom_list_view) as ListView
      val listAdapter = listView.adapter as DownloadAdapter
      val dataModel = listAdapter.getItem(index) as DownloadDS
      var pURLs = dataModel.url
      return pURLs.split("\\|").toTypedArray()
   }

   fun startDownloadActivity() : DownloadActivity {
      registration.complete = true
      val downloadActivity = Robolectric.buildActivity(DownloadActivity::class.java).create().get()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // downloadFile.onPostExecute
      return downloadActivity
   }

   fun checkLanguageList(downloadActivity : DownloadActivity) : Int{
      // do some preliminary bloomFileContents checking
      Assert.assertNotNull("bloomFileContents not initialized.", downloadActivity.bloomFileContents)
      val index004 = downloadActivity.bloomFileContents.indexOf("004")
      Assert.assertTrue("004 bloom file not found", index004 > 0)
      //val lines: Array<String> = downloadActivity.bloomFileContents.split("\\r?\\n").toTypedArray()
      //Assert.assertTrue("Number of lines should be greater that one.", lines.size > 1)

      // do some preliminary available languages check
      val listView = downloadActivity.findViewById<View>(R.id.bloom_list_view) as ListView
      val listAdapter = listView.adapter as DownloadAdapter
      Assert.assertTrue("Languages list count is empty.", listAdapter.count > 0)
      //Assert.assertTrue("Data items should be for language.",
      //        listAdapter.getArray()[0].URL.equals("Language"))

      var englishIndex = getItemIndex(listAdapter, "English")
      Assert.assertTrue("English not found in language list.", englishIndex > 0)
      return englishIndex
   }

   fun selectLanguageItem(downloadActivity : DownloadActivity, index : Int) {
      val listView = downloadActivity.findViewById<View>(R.id.bloom_list_view) as ListView
      val listAdapter = listView.adapter as DownloadAdapter

      val dlDS = listAdapter.getItem(index) as DownloadDS
      downloadActivity.chosenLanguage = dlDS.getName()
      downloadActivity.copyFile(DownloadActivity.BLOOM_LIST_FILE)

      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
   }

   //fun selectTemplateItem(downloadActivity : DownloadActivity, index : Int) {
   //   val listView = downloadActivity.findViewById<View>(R.id.bloom_list_view) as ListView
   //   val listAdapter = listView.adapter as DownloadAdapter
   //   for (i in 0..4) {
   //      listAdapter.getView(i, null, listView) // init some items
   //   }

   //   val viewItem = listView.get(index)
   //   listAdapter.onClick(viewItem)
   //   java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
   //   ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
   //}

   fun checkTemplateList(downloadActivity : DownloadActivity) : Int {
      // do some preliminary bloomFileContents checking
      Assert.assertNotNull("bloomFileContents not initialized.", downloadActivity.bloomFileContents)
      val idx004 = downloadActivity.bloomFileContents.indexOf("004")
      Assert.assertTrue("004 bloom file not found", idx004 > 0)
      //val lines: Array<String> = downloadActivity.bloomFileContents.split("\\r?\\n").toTypedArray()
      //Assert.assertTrue("Number of lines should be greater that one.", lines.size > 1)

      // do some preliminary available languages check
      val listView = downloadActivity.findViewById<View>(R.id.bloom_list_view) as ListView
      val listAdapter = listView.adapter as DownloadAdapter
      Assert.assertTrue("Languages list count is empty.", listAdapter.count > 0)
      //Assert.assertFalse("Data items should be for language.",
      //        listAdapter.getArray()[0].URL.equals("Language"))

      var index001 = getItemIndex(listAdapter, "001")
      Assert.assertTrue("001 Template not found in template list.", index001 >= 0)

      var index004 = getItemIndex(listAdapter, "004")
      Assert.assertTrue("004 Template not found in tempalte list.", index004 > 0)
      return index004
   }

   fun getItemIndex(listAdapter : DownloadAdapter, stringId : String) : Int {
      var index = -1
      for (i in listAdapter.getArray().indices) {
         val item = listAdapter.getArray()[i]
         if ((item.name != null) && item.name.indexOf(stringId) == 0) {
            index = i
         }
      }
      return index
   }

}
