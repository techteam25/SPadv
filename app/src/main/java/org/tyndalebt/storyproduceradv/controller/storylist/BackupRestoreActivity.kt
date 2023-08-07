package org.tyndalebt.storyproduceradv.controller.storylist

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.model.Phase
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.InputStream


class BackupRestoreActivity : MainBaseActivity()  {

    lateinit var storyPageViewPager : ViewPager2
    lateinit var backupController: SelectBackupFolderController

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_backup_restore)
            setupDrawer()
            setupStoryListTabPages()
            initActionBar()
            invalidateOptionsMenu()
        }
        catch (ex : Throwable) {
            ex.printStackTrace()
        }
    }

    private fun setupStoryListTabPages() {

        storyPageViewPager = findViewById(R.id.storyPageViewPager2)
        storyPageViewPager.offscreenPageLimit = 1  // StoryPageTab.values().size

        val storyPageAdapter = BackupStoryAdapter(this, 1)   // StoryPageTab.values().size)
        storyPageViewPager.adapter = storyPageAdapter

        storyPageViewPager.registerOnPageChangeCallback(storyPageChangeCallback)

    }


    override fun onDestroy() {
        super.onDestroy()
        storyPageViewPager.unregisterOnPageChangeCallback(storyPageChangeCallback)
    }

    var storyPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Log.i("MainActivity Story Page", "Selected Tab: $position")
        }
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId() : Int {
      return R.id.backup_restore
    }

    override fun getTitleString() : String? {
        return getString(R.string.backup_restore)
    }

    override fun getTitleColor2() : Int {
        return R.color.gray
    }

    override fun openHelpFile() : InputStream {
        // TODO - replace help file
        return  Phase.openHelpDocFile(PhaseType.COPY_VIDEOS, Workspace.activeStory.language,this)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if ((backupController != null) && SelectBackupFolderController.SELECT_BACKUP_FOLDER_REQUEST_CODES.contains(request)) {
            backupController.onFolderSelected(request, result, data)
        }
    }
}
