package org.tyndalebt.storyproduceradv.test.model

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.FirebaseApp
import org.junit.Assert
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.controller.learn.LearnActivity
import org.tyndalebt.storyproduceradv.model.Slide
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import org.tyndalebt.storyproduceradv.model.toJson
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


//
// RK - 3/31/2023
// This is the base class for tests that need to operate on
// Photo Stories, which are the heart of SPadv.
// This includes methods necessary to init a basic
// story project that can be operated on as part of the
// normal test.

open class BaseActivityTest {

   val cProjectNameOrig = "002 Lost Coin"
   val cProjectName = cProjectNameOrig + " Test"
   val cProjectBaseDir = "sampledata"
   val cProjectDir = "app/" + cProjectBaseDir
   val cTestDir = cProjectDir + "/test"
   var baseDocUri : Uri? = null

   fun loadStory(baseActivity : BaseActivity) : Story? {
      val storyUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode(cProjectName))
      var df2 = androidx.documentfile.provider.DocumentFile.fromFile(File(storyUri.path!!))
      val myStory = Workspace.buildStory(baseActivity, df2)
      return myStory
   }

   fun saveStory(baseActivity : BaseActivity, myStory : Story?) {
      FirebaseApp.initializeApp(baseActivity)
      myStory!!.toJson(baseActivity)
   }
    

   fun checkStoryContentsGeneral(myStory: Story?,  storyValues : HashMap<String?, String?>) {

      Assert.assertTrue("Expected number of slides should be 8.", myStory!!.slides.size.toInt() == 8)

      try {
         val clazz = Class.forName("org.tyndalebt.storyproduceradv.model.Story")
         val iter = storyValues.keys.iterator()
         while(iter.hasNext()) {
            val fieldName = iter.next()

            val field: Field = clazz.getDeclaredField(fieldName!!)
            field.setAccessible(true)
            val testValue = field.get(myStory)
            var value : String? = null
            if (testValue != null) {
               value = testValue.toString()
            }
            checkStoryFieldValue(storyValues, fieldName, value)
         }
      }
      catch (e: java.lang.Exception) {
         e.printStackTrace()
         throw e
      }
   }


   private fun checkStoryFieldValue(values : HashMap<String?, String?>, key : String?, value : String?) {
      val expected = values.get(key)
      Assert.assertEquals("Story value is incorrect. Field: \"" + key + "\" value: \"" + value,
              expected, value)
   }

   private fun checkSlideFieldValue(values : HashMap<String?, String?>, key : String?, value : String?, pageNo : Int) {
      val expected = values.get(key)
      Assert.assertEquals("Slide value is incorrect. Field: \"" + key + "\" value: \"" + value + "\" Slide: " + pageNo,
              expected, value)
   }

   fun checkSlideContents(slide : Slide?, slideValues : HashMap<String?, String?>, pageNo : Int) {
      var fieldName : String? = null
      try {
         val clazz = Class.forName("org.tyndalebt.storyproduceradv.model.Slide")
         val iter = slideValues.keys.iterator()
         while(iter.hasNext()) {
            fieldName = iter.next()

            val field: Field = clazz.getDeclaredField(fieldName!!)
            field.setAccessible(true)
            val testValue = field.get(slide)
            var value : String? = null
            if (testValue != null) {
               value = testValue.toString()
            }
            checkSlideFieldValue(slideValues, fieldName, value, pageNo)
         }
      }
      catch (e: java.lang.Exception) {
         e.printStackTrace()
         throw e
      }
   }


   fun addMapValues(valueMap: HashMap<String?, String?>, values: Array<Array<String>>) {
      for (i in values.indices) {
         valueMap[values[i][0]] = values[i][1]
      }
   }


   fun startLearnActivity() : LearnActivity {
      registration.complete = true
      val learnActivity = Robolectric.buildActivity(LearnActivity::class.java).create().get()
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
      return learnActivity
   }
    
   open fun initProjectFiles(bCreateStory : Boolean) {
      setupWorkspace()
      copyStory(cProjectNameOrig, cProjectName, bCreateStory)
   }

   fun copyStory(origStoryName : String, storyName : String, bCreateStory : Boolean) {

      // RK 03/24/2023
      //  Note: The story in the original project directory is being
      //  copied to a new directory and the testing will occur in the
      //  new directory. This allows the test to modify and save changes
      //  to the story while leaving the base project intact for the next test.
      //
      // Also note that all names in the original directory are set up to
      // point to the new directory names so that the project will be able
      // to operate properly in the new directory location.
      //
      // If a previous test has been run, then the test directory will have
      // been pre-populated and we need to clean up the previous test results
      // before running the new test
      //
      val srcUri = Uri.parse(baseDocUri?.toString() + "/" + Uri.encode(origStoryName))
      val dstUri =  Uri.parse(Workspace.workdocfile.uri.toString() + "/" + Uri.encode(storyName))

      deleteDirectory(dstUri.path)

      copyDirectory(srcUri.path, dstUri.path)
      if (bCreateStory) {
         val dstProjectUri =  Uri.parse(dstUri.toString() + "/" + "/project")
         deleteDirectory(dstProjectUri.path)
      }
   }

   fun setupWorkspace() {

      var baseDocFile = androidx.documentfile.provider.DocumentFile.fromFile(File(cProjectDir))
      if (!baseDocFile.isDirectory()) {
         baseDocFile = androidx.documentfile.provider.DocumentFile.fromFile(File(cProjectBaseDir))
      }
      baseDocUri = baseDocFile.uri

      // always start with a clean directory
      val newUri = Uri.parse(baseDocUri?.toString() + Uri.encode("/test"))
      deleteDirectory(newUri.path)
      val dir = File(newUri.path!!)
      dir.mkdir()

      val df2 = androidx.documentfile.provider.DocumentFile.fromFile(dir)
      Workspace.workdocfile = df2
      Workspace.parseLanguage = ""
      Workspace.isUnitTest = true
   }

   fun copyDirectory(srcDirLoc: String?, dstDirLoc: String?) {
      try {
         Files.walk(Paths.get(srcDirLoc))
              .forEach { source ->
                 val destination: Path = Paths.get(dstDirLoc, source.toString()
                         .substring(srcDirLoc!!.length))
                    Files.copy(source, destination)
              }
      }
      catch (e: Exception) {
         e.printStackTrace()
      }
   }

   fun deleteDirectory(dirLoc: String?) {
      val dstDir = File(dirLoc!!)
      if (dstDir.exists() && dstDir.isDirectory()) {
         deleteDir(dstDir)
      }
   }

   fun deleteDir(dir: File): Boolean {
      val allContents = dir.listFiles()
      if (allContents != null) {
         for (file in allContents) {
            deleteDir(file)
         }
      }
      return dir.delete()
   }
   
   fun updateWorkspaceStoryList(activity : BaseActivity) {
      activity.controller.updateStories()  // this is normally done in Activity.initWorkspace()

      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()     // allow other threads to run, for the story loading
      java.util.concurrent.TimeUnit.SECONDS.sleep(20)  // pause a bit to allow async code to catch up
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()     // allow other threads to run, for the story loading
      java.util.concurrent.TimeUnit.SECONDS.sleep(20)   // pause just a bit more
      ShadowLooper.runUiThreadTasksIncludingDelayedTasks()      // one last chance for other threads

      val storyCount = Workspace.Stories.size  // Why do we not read 2 stories instead of just 1?
      Assert.assertTrue("Workspace storycount should be greater than 0", storyCount > 0)

   }

   fun cleanTempDirectories(activity : org.tyndalebt.storyproduceradv.activities.BaseActivity) {
      val dir = activity.getFilesDir()
      if (dir.exists() && dir.isDirectory()) {
         deleteDir(dir)
      }
   }
}
