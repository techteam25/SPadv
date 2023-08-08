package org.tyndalebt.storyproduceradv.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.controller.export.SelectCopyFolderController
import org.tyndalebt.storyproduceradv.controller.storylist.SelectBackupFolderController
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageAdapter
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageTab
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.Network.ConnectivityStatus
import org.tyndalebt.storyproduceradv.tools.Network.VolleySingleton
import org.tyndalebt.storyproduceradv.view.BaseActivityView
import java.io.Serializable

class MainActivity : MainBaseActivity(), Serializable {

    //private var mDrawerLayout: DrawerLayout? = null
    lateinit var storyPageViewPager : ViewPager2
    lateinit var storyPageTabLayout : TabLayout

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!ConnectivityStatus.isConnected(context)) {
                Log.i("Connection Change", "no connection")

                VolleySingleton.getInstance(context).stopQueue()
            } else {
                Log.i("Connection Change", "Connected")

                VolleySingleton.getInstance(context).startQueue()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setupDrawer()
        setupStoryListTabPages()

// Only do this in one place.  SplashScreenActivity
//        if (!Workspace.isInitialized) {
//            initWorkspace()
//        }

        if (Workspace.showMoreTemplates) {
            Workspace.startDownLoadMoreTemplatesActivity(this)
        }
        else if (Workspace.showRegistration) {
            // DKH - 05/12/2021
            // Issue #573: SP will hang/crash when submitting registration
            // This flag indicates that MainActivity should create the
            // RegistrationActivity and show the registration screen.
            // This is set in BaseController function onStoriesUpdated()
            Workspace.showRegistration = false

            // When starting the RegistrationActivity from the MainActivity, specify that
            // finish should not be called on the MainActivity.
            // This is done by setting executeFinishActivity to false.
            // After the RegistrationActivity is complete, MainActivity will then display
            // the story template list
            showRegistration(false)
        }
        // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
        // Updated while integrating pull request #561 into current sillsdev baseline
        // This was deleted in pull request #561.
        // It was added back in because it monitors the network connection for VolleySingleton
        // and is used by  for support of RemoteCheckFrag.java,
        // AudioUpload.java & BackTranslationUpload.java
        GlobalScope.launch {
            runOnUiThread {
                this@MainActivity.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }
        if (Workspace.InternetConnection == false) {
            Toast.makeText(this,
                this.getString(R.string.remote_check_msg_no_connection),
                Toast.LENGTH_LONG).show()
        }
        supportActionBar?.setTitle(R.string.title_activity_story_templates)
    }

    /**
     * move to the chosen story
     */
    fun switchToStory(story: Story) {
        Workspace.activeStory = story
        val intent = Intent(this.applicationContext, Workspace.activePhase.getTheClass())
        startActivity(intent)
        finish()
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

    private fun setupStoryListTabPages() {
        storyPageViewPager = findViewById(R.id.storyPageViewPager)
        storyPageViewPager.offscreenPageLimit = StoryPageTab.values().size
        storyPageTabLayout = findViewById(R.id.tabLayout)

        val storyPageAdapter = StoryPageAdapter(this, StoryPageTab.values().size)
        storyPageViewPager.adapter = storyPageAdapter

        storyPageViewPager.registerOnPageChangeCallback(storyPageChangeCallback)

        // Sets the Tab Names from the list of StoryPageTabs
        TabLayoutMediator(storyPageTabLayout, storyPageViewPager) { tab, position ->
            tab.text = getString(StoryPageTab.values()[position].nameId)
        }.attach()
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId() : Int {
      return R.id.nav_stories
    }

    override fun getTitleString() : String? {
        return getString(R.string.title_activity_story_templates)
    }
}

