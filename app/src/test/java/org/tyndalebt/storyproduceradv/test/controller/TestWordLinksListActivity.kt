package org.tyndalebt.storyproduceradv.test.controller

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksActivity
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksListActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.tools.file.deleteWLAudioFileFromList
import org.tyndalebt.storyproduceradv.tools.toolbar.RecordingToolbar
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestWordLinksListActivity : BaseMultiRecordPhaseTest() {

   var mWLLActivity : BaseActivity? = null
   var mWLActivity : WordLinksActivity? = null
   var mLastTerm : String? = null

   override fun getAudioFiles(slide : Slide) : MutableList<String> {
      return slide.communityWorkAudioFiles
   }

   override fun getPhaseType() : PhaseType {
      return PhaseType.WORD_LINKS
   }

   override fun getActivity() : BaseActivity {
      return mWLLActivity!!
   }

   override fun getDestinationAudioFileName(fileName : String) : String {
      val dstName = Workspace.workdocfile.uri.path + "/" + WORD_LINKS_DIR + '/' + fileName
      return dstName
   }
   //
   // Test: WordLinksListActivityTest
   //
   // Purpose:
   //    Tests functionality for the WordLink activity
   //
   // Steps:
   //    1. Initialize the Wordlink list activity screen
   //    2. Switch to a wordlink activity screen
   //    3. Check the contents of the fragment for proper display
   //    4. Switch wordlink and test for proper display
   //
   // Author: Ray Kaestner 11/13/2023
   //

   @Test
   fun WordLinksListActivityTest() {

      // init environment
      initProjectFiles(false)
      var activity = startWordLinksListActivity()
      try {
         mStory = loadStory(activity!!)
         Workspace.activeStory = mStory!!
         checkWordLinksListContent()

         // open up the info on a wordlink using WordLinksActivity
         var wlActivity = startWordLinksActivity("Abram")
         checkWordLinksContent("Abram")

         // switch to a different wordlink and check for proper updates
         wlActivity.replaceActivityWordLink("Aaron")
         checkWordLinksContent("Aaron")

      } catch (ex: Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      } finally {
         cleanTempDirectories(activity!!)
      }
   }

   //
   // Test: RecordAudioTest
   //
   // Purpose:
   //    Tests functionality for the Wordlink audio files
   //
   // Steps:
   //    1. Initialize the Wordlinks list screen
   //    2. Switch to a wordlink activity
   //    3. Check the contents of the wordlink audio comment data
   //    4. Simulate the buttons to record an audio file and check that the
   //       wordlinkactivity screen is properly updated
   //    6. Test the audio file back translation function
   //    7. Test the audio file delete function
   //
   // Author: Ray Kaestner 11-13-2023
   //

   @Test
   fun RecordAudioTest() {
      // init environment
      initProjectFiles(false)
      var activity = startWordLinksListActivity()
      try {
         mStory = loadStory(activity!!)
         Workspace.activeStory = mStory!!  // switches activePhase back to LEARN

         // open up the info on a wordlink using WordLinksActivity
         mLastTerm = "Aaron"
         var wlActivity = startWordLinksActivity(mLastTerm!!)

         var toolbarView = startRecordingToolbarView()
         checkAudioRecordAudioContent(toolbarView, mLastTerm!!,  true, false)
         var toolbar = mWLActivity!!.getWLRecordingToolbar()
         modifyAudioFiles(toolbar!!, toolbarView, mLastTerm!!, false)
      }
      catch (ex : Throwable) {
         ex.printStackTrace()
         Assert.assertTrue("Exception occurred. " + ex.message, false)
      }
      finally {
         cleanTempDirectories(getActivity())
      }
   }

    fun checkWordLinksListContent() {

      var recyclerView = getActivity().findViewById<RecyclerView>(R.id.wordlink_list) as RecyclerView
      Assert.assertNotNull("Unable to find recycler view", recyclerView)
      var adapter = recyclerView!!.getAdapter()
      Assert.assertNotNull("Adapter not initialized.", adapter)
      Assert.assertTrue("Adapter is empty.", adapter!!.itemCount > 0)
      Assert.assertTrue("Adapter is partially empty.", adapter.itemCount > 100)
      Assert.assertNotNull("Workspace wordlink list is empty.", Workspace.termToWordLinkMap)
      //Assert.assertTrue("Workspace wordlink map is empty.", Workspace.termToWordLinkMap > 0)
   }

   fun checkWordLinksContent(value : String) {
      Assert.assertNotNull("Unable to find active Word Link", Workspace.activeWordLink)
      Assert.assertEquals("Incorrect active word link.", Workspace.activeWordLink.term, value)

      var wordLink = Workspace.termToWordLinkMap.get(value)
      Assert.assertNotNull("Unable to find the wordlink in the map", wordLink)
      Assert.assertEquals("Incorrect word link title.", wordLink!!.term, value)

      val explanationView = mWLActivity!!.findViewById<TextView>(R.id.explanation_text)
      Assert.assertEquals("Incorrect Explanation text.", explanationView.text, wordLink!!.explanation)

      val relatedTermsView = mWLActivity!!.findViewById<TextView>(R.id.related_terms_text)
      if (wordLink.relatedTerms.isEmpty()) {
         Assert.assertEquals("Related terms text should be empty.",relatedTermsView.text.toString(), "None")
      } else {
         val relatedTerms = wordLink.relatedTerms.fold(SpannableStringBuilder()) {
               result, relatedTerm -> result.append(stringToWordLink(relatedTerm, null)).append("   ")
         }

         Assert.assertEquals("Incorrect Related Terms text.", relatedTermsView.text.toString(), relatedTerms.toString())
      }

      val alternateRenderingsView = mWLActivity!!.findViewById<TextView>(R.id.alternate_renderings_text)
      val alternateRenderings = wordLink.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
      }.removeSuffix("\n")
      Assert.assertEquals("Incorrect Related Terms text.", alternateRenderingsView.text.toString(), alternateRenderings.toString())
   }

   fun checkAudioRecordAudioContent(toolbarFragView : View?, term : String, bList : Boolean, bAudioExists : Boolean) {

      var audioFiles = getAudioFiles(term)
      if (audioFiles != null) {
         val bFiles = audioFiles.size > 0
         Assert.assertEquals("Audio files existence incorrect", bFiles, bAudioExists)
      }
      checkRecordingToolbarButtons(toolbarFragView, bList, bAudioExists)
   }

    fun getAudioFiles(term : String) : MutableList<WordLinkRecording> {
       var wordLink = Workspace.termToWordLinkMap.get(term)
       Assert.assertNotNull("Unable to find the wordlink in the map", wordLink)
       return wordLink!!.wordLinkRecordings
   }

   override fun getAudioFilesSize(slideNum : Int) : Int {
      return getAudioFiles(mLastTerm!!).size
   }

   fun modifyAudioFiles(toolBar : RecordingToolbar, toolbarFragView : View?, term : String, bList : Boolean) {
      // add an audio file and check for proper operation
      val slideNum = 0
      var toolbar = mWLActivity!!.getWLRecordingToolbar()
      doAddAudioFile(toolbarFragView, toolbar!!, slideNum, bList)
      checkAudioRecordAudioContent(toolbarFragView, term, bList, true)

      doAddAudioFile(toolbarFragView, toolbar!!, slideNum, bList)
      checkAudioRecordAudioContent(toolbarFragView, term, bList, true)

      doAddAudioFile(toolbarFragView, toolbar!!, slideNum, bList)
      checkAudioRecordAudioContent( toolbarFragView, term, bList, true)

      doAddAudioFile(toolbarFragView, toolbar!!, slideNum, bList)
      checkAudioRecordAudioContent(toolbarFragView, term, bList, true)


      // check the operations of the list files buttons in the toolbar
      checkAudioFilesList(toolbarFragView, term, bList, true)
   }

   // this method tests the operations that occur when you hit the files list button and
   // do the various operations.
   fun checkAudioFilesList(toolbarFragView : View?, term : String, bList : Boolean, bFileExists : Boolean) {
      // val slide = Workspace.activeStory.slides[slideNum]

      // first check the starting number of files in the list
      var wordLink = Workspace.termToWordLinkMap.get(term)
      var recordings = wordLink!!.wordLinkRecordings
      var numFiles = 4
      Assert.assertNotNull("Could not find recordings", recordings)
      Assert.assertEquals("Incorrect number of recordings", recordings!!.size, numFiles)

      var wordLinkRecording = recordings.get(0)
      var combinedFileName = wordLinkRecording.audioRecordingFilename  // returns combined name
      var backTranslation = wordLinkRecording.textBackTranslation
      Assert.assertNotNull("No wordlink filename", combinedFileName)
      var fileName = Story.getFilename(combinedFileName)
      var displayName = Story.getDisplayName(combinedFileName)
      wordLinkRecording.textBackTranslation = term + " Back Translation"

      // test that file exists
      var fullFileName = getDestinationAudioFileName(fileName)
      var file = File(fullFileName)
      Assert.assertTrue("Audio file does not exist.", file.exists())

      // test that textBackTranslation is persisted

      // finally delete the selected file
      deleteFileTest(term)

      // delete all the rest of the files, too
      deleteFileTest(term)
      deleteFileTest(term)
      deleteFileTest(term)

      // check they are all gone
      recordings = wordLink!!.wordLinkRecordings
      Assert.assertEquals(
         "Display names should be empty after delete all",
         recordings!!.size,
         0
      )
   }

   fun deleteFileTest(term : String) {
      // See RecordingsListAdapter for the delete functionality
      var wordLink = Workspace.termToWordLinkMap.get(term)
      var recordings = wordLink!!.wordLinkRecordings
      val recordingsSize = recordings!!.size

      var wordLinkRecording = recordings.get(0)
      var combinedFileName = wordLinkRecording.audioRecordingFilename  // returns combined name
      var fileName = Story.getFilename(combinedFileName)
      var fullFileName = getDestinationAudioFileName(fileName)

      // var file = File(fullFileName)
      // Assert.assertTrue("The audio file exists before delete", file.exists())

      deleteWLAudioFileFromList(getActivity(), 0)

      // check that the lists are properly updated
      recordings = wordLink!!.wordLinkRecordings
      Assert.assertEquals("Filenames size does not change after delete", recordings.size, recordingsSize-1)

      // check that the file is actually gone.
      var file = File(fullFileName)
      Assert.assertFalse("The audio file has not been deleted", file.exists())
   }

   fun startRecordingToolbarView() : View? {
      return  mWLActivity!!.getWLRecordingToolbar()!!.onCreateView(mWLActivity!!.getWLRecordingToolbar()!!.onGetLayoutInflater(null), null, null)
   }

   fun startWordLinksListActivity() : WordLinksListActivity {
      registration.complete = true
      val wllActivity = Robolectric.buildActivity(WordLinksListActivity::class.java).create().get()
      
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      Workspace.initFirebaseAnalytics(wllActivity)
      mWLLActivity = wllActivity
      return wllActivity
  }
  
  fun startWordLinksActivity(term : String) : WordLinksActivity {
     val intent = Intent(getActivity() , WordLinksActivity::class.java)
     intent.putExtra(PHASE, PhaseType.WORD_LINKS)
     intent.putExtra(WORD_LINKS_CLICKED_TERM, term)

     val wlActivity = Robolectric.buildActivity(WordLinksActivity::class.java, intent).create().get();

      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      java.util.concurrent.TimeUnit.SECONDS.sleep(5)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // videoFile.onPostExecute
      mWLActivity = wlActivity
      return wlActivity
  }

   override fun initProjectFiles(bCreateStory : Boolean) {
      super.initProjectFiles(bCreateStory)

      // set up the word links directory
      val srcUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode(WORD_LINKS_DIR))
      val dstUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode(WORD_LINKS_DIR))
      deleteDirectory(dstUri.path)
      copyDirectory(srcUri.path, dstUri.path)

      var splashScreenActivity = startSplashScreenActivity()
      Workspace.importWordLinks(splashScreenActivity)  // inits the wordlinks map in the workspace
   }

   fun startSplashScreenActivity() : SplashScreenActivity {
      registration.complete = false
      val splashScreenActivity = Robolectric.buildActivity(
         SplashScreenActivity::class.java
      ).create().get()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return splashScreenActivity
   }

}
