package org.tyndalebt.storyproduceradv.test.model

import android.os.Build
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tyndalebt.storyproduceradv.model.*


@RunWith(RobolectricTestRunner::class)

@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestParsePhotoStory : BaseActivityTest() {

   val cTranslatedContent = "In the beginning was the word!"
   val cCredits = "Contact: Bob\nTranslator: Larry"

   //
   // Test: loadSaveJsonTest
   //
   // Purpose:
   //    Tests loading, editing, and saving of story.json
   //
   // Steps:
   //    1. Initialize existing project files
   //    2. Open the existing project and check for accurate
   //       loading of the project.
   //    3. Modify the loaded project
   //    4. Save the changes to the project
   //    5. Re-open the project and check that the changes were
   //       properly persisted and loaded
   //
   // Author: Ray Kaestner 03-23-2023
   //

   @Test
   fun loadSaveJsonTest() {

      // init environment
      initProjectFiles(false)
      val learnActivity = startLearnActivity()

      // test loading

      val myStory = loadStory(learnActivity)
      Assert.assertNotNull("Unable to open story", myStory)
      checkStoryContents(myStory, false, false)

      // test edit and save
      modifyStoryContents(myStory)
      saveStory(learnActivity, myStory)

      val myStory2 = loadStory(learnActivity)
      checkStoryContents(myStory2, true, false)
   }

   //
   // Test: createStoryLoadBloomHtmlTest
   //
   // Purpose:
   //    Tests creating story object from the bloom html file,
   //    then saving and editing, story.json
   //
   // Steps:
   //    1. Initialize existing project files, ensure project directory removed
   //    2. Open the new project and check for accurate.  Since the story.json
   //       file has been removed, it should be loaded from the bloom html file
   //    3. Modify the loaded project
   //    4. Save the changes to the project
   //    5. Re-open the project and check that the changes were
   //       properly persisted and loaded
   //
   // Author: Ray Kaestner 03-23-2023
   //

   @Test
   fun createStoryLoadBloomHtmlTest() {

      // init environment
      initProjectFiles(true)
      val learnActivity = startLearnActivity()

      // test loading

      val myStory = loadStory(learnActivity)  // story will be loaded and saved
      Assert.assertNotNull("Unable to open story", myStory)
      checkStoryContents(myStory, false, true)

      // test edit and save
      modifyStoryContents(myStory)
      saveStory(learnActivity, myStory)

      val myStory2 = loadStory(learnActivity)
      checkStoryContents(myStory2, true, true)
   }


/////////////////////////////////////
/////////////////////////////////////

   fun modifyStoryContents(myStory : Story?) {
      // modified in PhaseBaseActivity.onPause()
      myStory!!.lastSlideNum++
      myStory.lastPhaseType = PhaseType.TRANSLATE_REVISE
       
      // Also modify the first slide a little bit 
      val slide = myStory.slides[0]
      slide.translatedContent = cTranslatedContent
             
      // Add some credits to our project
      myStory.localCredits = cCredits
        
   }

   protected var storyValuesDefault = arrayOf(

           arrayOf("isApproved", "false"),
           arrayOf("lastSlideNum", "0"),
           arrayOf("lastPhaseType", PhaseType.LEARN.toString()),
           arrayOf("localCredits", ""))

   protected var storyValuesEdit = arrayOf(

           arrayOf("isApproved", "false"),
           arrayOf("lastSlideNum", "1"),
           arrayOf("lastPhaseType", PhaseType.TRANSLATE_REVISE.toString()),
           arrayOf("localCredits", cCredits))

   protected var defaultSlideValues = arrayOf(
           arrayOf("imageFile", ""),
           arrayOf("textFile", ""),
           arrayOf("title", ""),
           arrayOf("subtitle", "Luke 15"),
           arrayOf("width", "-1"),
           arrayOf("height", "-1"),
           arrayOf("reference", "Luke 15:1-2, 15:8-10"),
           arrayOf("content", "Title Ideas:\nGod is happy when a person repents.\nThe Lost Coin.\nWhen God celebrates!"),
           arrayOf("narrationFile", "audio/5484e7a9-65ab-4108-8850-d6d240318254.mp3"),
           arrayOf("volume", "0.3"),
           arrayOf("crop", null),
           arrayOf("startMotion", "Rect(0, 0 - -1, -1)"),
           arrayOf("endMotion", "Rect(0, 0 - -1, -1)"),
           arrayOf("translatedContent", ""))

   protected var firstSlideValuesBloom = arrayOf(

           arrayOf("width", "0"),
           arrayOf("height", "0"),
           arrayOf("startMotion", "Rect(0, 0 - 0, 0)"),
           arrayOf("endMotion", "Rect(0, 0 - 0, 0)"))

   protected var firstSlideValuesEdit = arrayOf(

           arrayOf("translatedContent", "In the beginning was the word!"))

   protected var secondSlideValues = arrayOf(
           arrayOf("imageFile", "1.jpg"),
           arrayOf("textFile", ""),
           arrayOf("title", "Title Slide 1"),
           arrayOf("subtitle", "Subtitle Slide 1"),
           arrayOf("width", "1185"),
           arrayOf("height", "1005"),
           arrayOf("reference", "Luke 15:1-2"),
           arrayOf("volume", "0.0"),
           arrayOf("content", "One day, Jesus was teaching the people. Some of those people were critical that Jesus ate and spoke with others [other people] who did bad things. So Jesus told this story:"),
           arrayOf("narrationFile", "audio/narration1.mp3"),
           arrayOf("startMotion", "Rect(244, 0 - 1020, 657)"),
           arrayOf("endMotion", "Rect(70, 0 - 1112, 882)"))

   protected var secondSlideValuesBloom = arrayOf(
           arrayOf("title", ""),
           arrayOf("subtitle", ""))

   protected var secondSlideValuesEdit = arrayOf(
           arrayOf("subtitle", ""))

   private fun getExpectedSlideDefaultValueMap(): HashMap<String?, String?> {
      val slideValues: HashMap<String?, String?> = HashMap<String?, String?>()
      for (i in defaultSlideValues.indices) {
         slideValues[defaultSlideValues[i][0]] = defaultSlideValues[i][1]
      }
      return slideValues
   }
   
   private fun checkStoryContents(myStory: Story?, bModified : Boolean, bBloom : Boolean) {
      // Testing that the story contents have been properly loaded
      val storyValues = getExpectedStoryValueMap(bModified)     
      checkStoryContentsGeneral(myStory, storyValues);

      checkSlideContents(myStory!!.slides[0], getExpectedSlideValueMap(0, bModified, bBloom), 0)
      checkSlideContents(myStory.slides[1], getExpectedSlideValueMap(1, bModified, bBloom), 1)
   }

 
    private fun getExpectedStoryDefaultValueMap(): HashMap<String?, String?> {
      val storyValueMap: HashMap<String?, String?> = HashMap<String?, String?>()
      for (i in storyValuesDefault.indices) {
         storyValueMap[storyValuesDefault[i][0]] = storyValuesDefault[i][1]
      }
      return storyValueMap
   }

   private fun getExpectedStoryValueMap(bModified : Boolean) : HashMap<String?, String?> {
      val valueMap = getExpectedStoryDefaultValueMap()
      if (bModified) {
         addMapValues(valueMap, storyValuesEdit)
      }
      return valueMap
   }
   
   
   private fun getExpectedSlideValueMap(pageNo : Int, bModified : Boolean, bBloom : Boolean) : HashMap<String?, String?> {
      val slideValueMap = getExpectedSlideDefaultValueMap()
      if (pageNo == 1) {
         addMapValues(slideValueMap, secondSlideValues)
         if (bBloom) {
            addMapValues(slideValueMap, secondSlideValuesBloom)
         }
         //if (bModified) {
         //   addMapValues(slideValueMap, secondSlideValuesEdit)
         //}
      }
      else {
         //addMapValues(slideValueMap, firstSlideValues)
         if (bBloom) {
            addMapValues(slideValueMap, firstSlideValuesBloom)
         }
         if (bModified) {
            addMapValues(slideValueMap, firstSlideValuesEdit)
         }
      }
      return slideValueMap
   }
  
   /*

   @Test
   fun parsePhotoStoryXML_When_StoryHasNoSlides_Should_ReturnNull() {
      setupWorkspace()
      val storyPath = Mockito.mock(androidx.documentfile.provider.DocumentFile::class.java)
      Mockito.`when`(storyPath.name).thenReturn("StoryWithNoSlides")

      val result = parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), storyPath)

      Assert.assertNull(result)
   }
*/


}
