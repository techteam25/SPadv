package org.tyndalebt.storyproduceradv.tools.toolbar

import android.view.View
import android.widget.ImageButton
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MultiRecordFrag
import org.tyndalebt.storyproduceradv.controller.adapter.RecordingsListAdapter
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace

/**
 * A class responsible for listing recorded audio files from a recording toolbar.
 *
 * This class extends both recording and playback functionality of its base classes. A third button
 * is added that can bring up a modal listing the audio recording created with this toolbar.
 */
open class MultiRecordRecordingToolbar: PlayBackRecordingToolbar() {
    protected lateinit var multiRecordButton: ImageButton

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        multiRecordButton = toolbarButton(R.drawable.ic_playlist_play_white_48dp, R.id.list_recordings_button)
        rootView?.addView(multiRecordButton)
        
        rootView?.addView(toolbarButtonSpace())

        commentIcon = toolbarButton(R.drawable.ic_comment_present_on_community_phase, R.id.comment_icon);
        commentIcon.visibility = View.VISIBLE;
//        checks the presence/absence of recorded comments on the community work phase and adds an icon if present
        when (MultiRecordFrag.slideNumHolder != null) {
            true -> if (Workspace.activeStory.slides[MultiRecordFrag.slideNumHolder!!].communityWorkAudioFiles.isNotEmpty() &&
                Workspace.activePhase.phaseType != PhaseType.COMMUNITY_WORK) {
                rootView?.addView(commentIcon);
            }
            // TM  needed else - not sure if return is right but needs something
            else -> return;
        }
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        multiRecordButton.visibility = View.VISIBLE
    }

    override fun hideInheritedToolbarButtons() {
        super.hideInheritedToolbarButtons()

        multiRecordButton.visibility = View.INVISIBLE
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

        multiRecordButton.setOnClickListener(multiRecordButtonOnClickListener())
    }

    protected open fun multiRecordButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
            doAudioListDialog()
         }
    }

    protected open fun doAudioListDialog() {
        toolbarMediaListener.onStartedToolbarMedia()
// TM replace activity!! with requireactivity
        RecordingsListAdapter.RecordingsListModal(requireActivity(), this).show()
    }
}