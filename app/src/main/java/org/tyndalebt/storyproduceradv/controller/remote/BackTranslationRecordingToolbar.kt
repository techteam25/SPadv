package org.tyndalebt.storyproduceradv.controller.remote

import android.widget.Toast
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.dramatization.DramatizationRecordingToolbar
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.toolbar.MultiRecordRecordingToolbar

// For the ROCC case, if the slide has already been approved then
// disallow changes to the recording and notify the user that the
// slide has already been approved.
class BackTranslationRecordingToolbar: DramatizationRecordingToolbar() {

   protected override fun doRecordAudio() {
      var slide = Workspace.activeStory.slides[slideNum]
      if (!isAppendingOn && slide.isApproved) {
         Toast.makeText(context, R.string.already_approved, Toast.LENGTH_LONG).show()
      }
      else {
         super.doRecordAudio()
      }
   }

   protected override fun doAudioListDialog() {
      var slide = Workspace.activeStory.slides[slideNum]
      if (!isAppendingOn && slide.isApproved) {
         Toast.makeText(context, R.string.already_approved, Toast.LENGTH_LONG).show()
      }
      else {
         super.doAudioListDialog()
      }
   }
}
