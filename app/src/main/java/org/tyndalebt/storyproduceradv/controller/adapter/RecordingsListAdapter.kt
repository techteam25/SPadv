package org.tyndalebt.storyproduceradv.controller.adapter

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.Modal
import org.tyndalebt.storyproduceradv.controller.remote.BackTranslationFrag
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksRecordingToolbar
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.UploadState
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.logging.saveLog
import org.tyndalebt.storyproduceradv.tools.file.*
import org.tyndalebt.storyproduceradv.tools.media.AudioPlayer
import org.tyndalebt.storyproduceradv.tools.toolbar.RecordingToolbar

class RecordingsListAdapter(val values: MutableList<String>?, private val listeners: ClickListeners) : RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    interface ClickListeners {
        fun onRowClick(pos: Int)
        fun onPlayClick(pos: Int, buttonClickedNow: ImageButton)
        fun onDeleteClick(name: String, pos: Int)
        fun onRenameClick(position: Int, newName: String)
    }

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val individualAudio = inflater.inflate(R.layout.audio_comment_list_item, parent, false)

        return ViewHolder(individualAudio)
    }

    override fun getItemCount(): Int = values?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioText = values?.get(position)
        if (audioText != null) {
            if (getChosenDisplayName().contains(audioText)) {
                val color = ContextCompat.getColor(holder.itemView.context, R.color.primary)
                holder.itemView.setBackgroundColor(color)
                selectedPos = holder.adapterPosition
            }
            else{
                val color = ContextCompat.getColor(holder.itemView.context, R.color.black)
                holder.itemView.setBackgroundColor(color)
            }
            holder.bindView(audioText)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(text: String) {
            itemView.setOnClickListener {
                notifyItemChanged(selectedPos)
                selectedPos = adapterPosition
                notifyItemChanged(selectedPos)
                listeners.onRowClick(adapterPosition)
            }
            itemView.setOnLongClickListener {
                showItemRenameDialog(adapterPosition)
                return@setOnLongClickListener true
            }
            val messageButton = itemView.findViewById<TextView>(R.id.audio_comment_title)
            messageButton.text = text

            val playButton = itemView.findViewById<ImageButton>(R.id.audio_comment_play_button)
            playButton.setOnClickListener {
                listeners.onPlayClick(adapterPosition, it as ImageButton)
            }

            val deleteButton = itemView.findViewById<ImageButton>(R.id.audio_comment_delete_button)
            deleteButton.setOnClickListener {
                showDeleteItemDialog(adapterPosition, text)
            }
        }

        private fun showDeleteItemDialog(position: Int, text: String) {
            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(itemView.context.getString(R.string.delete_audio_title))
                    .setMessage(itemView.context.getString(R.string.delete_audio_message))
                    .setNegativeButton(itemView.context.getString(R.string.no), null)
                    .setPositiveButton(itemView.context.getString(R.string.yes)) { _, _ ->
                        listeners.onDeleteClick(text, position)
                        notifyItemRemoved(position)
                    }
                    .create()

            dialog.show()
        }

        /**
         * Show to the user a dialog to rename the audio comment
         *
         * @param position the integer position of the comment the user "long-clicked"
         */
        private fun showItemRenameDialog(position: Int) {
            val newName = EditText(itemView.context)

            // Programmatically set layout properties for edit text field
            val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            // Apply layout properties
            newName.layoutParams = params

            // 11/13/2021 - DKH, Issue 606, Wordlinks quick fix for text back translation
            // This piece of software is used in multiple places in Story Publisher Adv
            // Grab the default instructions for data entry
            // This is something like: "Choose a new name"
            var title = itemView.context.getString(R.string.rename_title)

            // If this is Wordlinks, use instructions that are geared toward Wordlinks
            when(Workspace.activePhase.phaseType){
                PhaseType.WORD_LINKS -> {
                    title = itemView.context.getString(R.string.rename_title_wordlinks)
                }
            }

            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(title)
                    .setView(newName)
                    .setNegativeButton(itemView.context.getString(R.string.cancel), null)
                    .setPositiveButton(itemView.context.getString(R.string.save)) { _, _ ->
                        var testName = newName.text.toString()
                        if (testName.isEmpty()) {
                            // RK - 05/01/2024
                            // if for some reason the name was blank,
                            // go back to the default name.
                            // This code needs a name and causes problems if none results.
                            testName = createRecordingCombinedName()
                            testName = Story.getDisplayName(testName)
                        }
                        listeners.onRenameClick(position, testName)
                        notifyDataSetChanged()
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }
    }

    class RecordingsListModal(private val context: Context, private val toolbar: RecordingToolbar?) : ClickListeners, Modal {
        private var rootView: ViewGroup? = null
        private var dialog: AlertDialog? = null

        var displayNames: MutableList<String> = mutableListOf()
        var initialChosenComboName = ""

        internal var recyclerView: androidx.recyclerview.widget.RecyclerView? = null
        private val audioPlayer: AudioPlayer = AudioPlayer()
        private var currentPlayingButton: ImageButton? = null
        private var audioPos = -1
        private var embedded = false
        private var playbackListener: RecordingToolbar.ToolbarMediaListener? = null
        private var slideNum: Int = Workspace.activeSlideNum

        fun setSlideNum(mSlideNum:Int){
            slideNum = mSlideNum
        }

        fun setParentFragment(parentFragment: Fragment?){
            try{
                playbackListener = parentFragment as RecordingToolbar.ToolbarMediaListener
            }
            catch (e : ClassCastException){
                playbackListener = context as RecordingToolbar.ToolbarMediaListener
            }
            catch (e:Exception){}
        }

        fun embedList(view: ViewGroup) {
            rootView = view
            embedded = true
        }

        override fun show() {
            if (!embedded) {
                val inflater = LayoutInflater.from(context)
                rootView = inflater?.inflate(R.layout.recordings_list, rootView) as ViewGroup?
            }

            recyclerView = rootView?.findViewById(R.id.recordings_list)

            resetRecordingList()
            recyclerView?.adapter = RecordingsListAdapter(displayNames, this)
            recyclerView?.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))
            recyclerView?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            initialChosenComboName = getChosenCombName()

            if (!embedded) {
                val tb = rootView?.findViewById<Toolbar>(R.id.toolbar2)
                tb?.setTitle(R.string.recordings_title)

                val alertDialog = AlertDialog.Builder(context)
                alertDialog.setView(rootView)
                dialog = alertDialog.create()

                val exit = rootView?.findViewById<ImageButton>(R.id.exitButton)
                exit?.setOnClickListener {
                    dialog?.dismiss()
                    checkUpdateChosenAudio()
                }
                dialog?.setOnDismissListener {
                    if (audioPlayer.isAudioPlaying) {
                        audioPlayer.stopAudio()
                    }
                    val newNames = getRecordedDisplayNames(slideNum) ?:  mutableListOf()
                    if (newNames.size == 0) {
                        // all items were deleted.  Reset the buttons
                        toolbar!!.updateInheritedToolbarButtonVisibility()
                    }
                    checkUpdateChosenAudio()
                }
                dialog?.show()
            }
        }

        /**
         * Initializes the list of draft recordings at beginning of fragment creation and after externally driven list changes.
         */
        fun resetRecordingList() {
            //only update if there was a change.
            val newNames = getRecordedDisplayNames(slideNum) ?:  mutableListOf()
            if(!displayNames.equals(newNames)) {
                displayNames = newNames
                recyclerView?.adapter = RecordingsListAdapter(displayNames, this)
            }
        }

        override fun onRowClick(pos: Int) {
            setChosenFileIndex(pos)
        }

        override fun onPlayClick(pos: Int, buttonClickedNow: ImageButton) {
            if (audioPlayer.isAudioPlaying && audioPos == pos) {
                stopAudio()
            } else {
                stopAudio()
                audioPos = pos

                toolbar?.stopToolbarMedia()

                playbackListener?.onStartedToolbarMedia()

                currentPlayingButton = buttonClickedNow
                currentPlayingButton?.setImageResource(R.drawable.ic_stop_white_36dp)

                audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                    buttonClickedNow.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                    stopAudio()
                })

                if (storyRelPathExists(context, getRecordedAudioFiles()[pos])) {
                    audioPlayer.setStorySource(context, getRecordedAudioFiles()[pos])
                    audioPlayer.playAudio()
                    when (Workspace.activePhase.phaseType) {
                        PhaseType.TRANSLATE_REVISE -> saveLog(context.getString(R.string.DRAFT_PLAYBACK))
                        PhaseType.COMMUNITY_WORK -> saveLog(context.getString(R.string.COMMENT_PLAYBACK))
                        else -> {
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.recording_toolbar_no_recording), Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onDeleteClick(name: String, pos: Int){
            val deletedDisplayName = displayNames[pos]
            if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS) {
                deleteWLAudioFileFromList(context, pos)
            } else {
                deleteAudioFileFromList(context,pos)
            }
            displayNames.removeAt(pos)
            recyclerView?.adapter!!.notifyDataSetChanged()
            if (deletedDisplayName == getChosenDisplayName()) { // normally this is done be deleteAudioFiles
                if (displayNames.size > 0) {
                    onRowClick(displayNames.size-1)
                }
                else {
                    setChosenFileIndex(-1)
                }
            }

            // RK 12/29/23 - if no items left, update the display
            if (displayNames.size == 0) {
                toolbar?.updateInheritedToolbarButtonVisibility()
                if (Workspace.activePhase.phaseType != PhaseType.WORD_LINKS) {
                    dialog?.dismiss()  // dismiss the list dialog
                }
                else {
                    // RK 02/07/24   not a dialog, collapse the sheet
                    (toolbar as WordLinksRecordingToolbar).collapseBottomSheet()
                }

            }
        }

        override fun onRenameClick(position: Int, newName: String) {
            updateDisplayName(position, newName)
            setChosenFileIndex(position)
            displayNames[position] = newName
            recyclerView?.adapter!!.notifyDataSetChanged()
        }

        fun stopAudio() {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton?.setImageResource(R.drawable.ic_play_arrow_white_36dp)
                audioPlayer.stopAudio()
            }
        }

        // RK 12/28/23
        // Watches for updates in the chosen audio file list
        fun checkUpdateChosenAudio() {
            var chosenDisplay = getChosenDisplayName()
            var chosenComboName = getChosenCombName()
            if (initialChosenComboName != chosenComboName) {
                setUploadNeeded(true)
                initialChosenComboName = chosenComboName
            }
        }

        // RK 12/28/23
        // If BackTransFrag, it will update the state of the upload icon
        fun setUploadNeeded(bNeeded : Boolean) {

            if ((toolbar != null) && (toolbar.parentFragment != null) &&
                (toolbar.parentFragment is BackTranslationFrag)){
                val backTransFrag = toolbar.parentFragment as BackTranslationFrag
                backTransFrag.setBackTranslationUploadStateValue(UploadState.NOT_UPLOADED)
            }
        }
    }
}
