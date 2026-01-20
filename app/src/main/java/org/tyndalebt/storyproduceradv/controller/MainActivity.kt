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
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageAdapter
import org.tyndalebt.storyproduceradv.controller.storylist.StoryPageTab
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.Network.ConnectivityStatus
import org.tyndalebt.storyproduceradv.tools.Network.VolleySingleton
import org.tyndalebt.storyproduceradv.controller.JsonHelper
import com.fasterxml.jackson.databind.ObjectMapper
import dev.b3nedikt.restring.Restring
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
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
        
        // Load saved language and initialize translations if not already loaded
        val savedLanguage = Workspace.readFromFile(this)
        if (savedLanguage != null && savedLanguage.isNotEmpty()) {
            val langCode = Workspace.getLanguageCode(savedLanguage)
            if (langCode.isNotEmpty()) {
                // Load translations from JSON file
                loadLanguageTranslations(langCode)
            }
        }

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
        if (!Workspace.InternetConnection) {
            Toast.makeText(this,
                this.getString(R.string.remote_check_msg_no_connection),
                Toast.LENGTH_LONG).show()
        }
        supportActionBar?.setTitle(R.string.title_activity_story_templates)
    }
    
    private fun loadLanguageTranslations(language: String) {
        try {
            applicationContext.assets.open("$language/strings.json").use { inputStream ->
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                val jsonString = String(buffer, StandardCharsets.UTF_8)
                val listTypeJson: HashMap<String, String> = HashMap()
                JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                    "",
                    ObjectMapper().readTree(jsonString),
                    listTypeJson
                )
                listTypeJson.forEach {
                    org.tyndalebt.storyproduceradv.model.languageStringsMap[it.key] = it.value
                }
                Restring.locale = Locale(language)
                Restring.putStrings(Restring.locale, org.tyndalebt.storyproduceradv.model.languageStringsMap)
            }
        } catch (exception: IOException) {
            Log.e("MainActivity", "Error loading language translations: ${exception.message}")
        }
    }

    /**
     * move to the chosen story
     */
    fun switchToStory(story: Story) {
        // RK 11/13/23
        // Crashes were reported for the case that for some reason the SD card became
        // unwritable which caused problems for editing the story.  If we detect that
        // case, we will warn the user, encouraging him to investigate the cause before
        // proceeding.
        if (story.isWritable(this)) {

            Workspace.activeStory = story
            val intent = Intent(this.applicationContext, Workspace.activePhase.getTheClass())
            startActivity(intent)
            finish()
        }
        else if (!Workspace.isUnitTest){  // if not writable (and not unit test) warn the user
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.read_only_title))
            builder.setMessage(getString(R.string.read_only_message))
            builder.setPositiveButton(getString(R.string.ok), null)
            val dlg = builder.create()
            dlg.show()
        }
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

