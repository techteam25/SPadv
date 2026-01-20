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
import android.os.Handler
import android.os.Looper
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
        
        // Load saved language and initialize translations
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
        // Update menu after view is fully laid out to ensure translations are available
        findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.post {
            val navigationView: com.google.android.material.navigation.NavigationView? = findViewById(R.id.nav_view)
            navigationView?.let {
                updateMenuItems(it)
            }
        }
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
    
    override fun onResume() {
        super.onResume()
        // Refresh menu translations when activity resumes
        // Use post to ensure view is ready
        Handler(Looper.getMainLooper()).post {
            val navigationView: com.google.android.material.navigation.NavigationView? = findViewById(R.id.nav_view)
            navigationView?.let {
                updateMenuItems(it)
            }
        }
    }
    
    private fun updateMenuItems(navigationView: com.google.android.material.navigation.NavigationView) {
        val menu = navigationView.menu
        
        // Log to debug translation loading
        Log.d("MainActivity:updateMenuItems", "languageStringsMap size: ${org.tyndalebt.storyproduceradv.model.languageStringsMap.size}")
        Log.d("MainActivity:updateMenuItems", "video_share in map: ${org.tyndalebt.storyproduceradv.model.languageStringsMap.containsKey("video_share")}")
        Log.d("MainActivity:updateMenuItems", "video_share value: ${org.tyndalebt.storyproduceradv.model.languageStringsMap["video_share"]}")
        
        // Get translated strings directly from Restring/languageStringsMap
        val videoShare = org.tyndalebt.storyproduceradv.model.languageStringsMap["video_share"] ?: getString(R.string.video_share)
        val backupRestore = org.tyndalebt.storyproduceradv.model.languageStringsMap["backup_restore"] ?: getString(R.string.backup_restore)
        val helpMe = org.tyndalebt.storyproduceradv.model.languageStringsMap["help_me"] ?: getString(R.string.help_me)
        val createTemplateMode = org.tyndalebt.storyproduceradv.model.languageStringsMap["create_template_mode"] ?: getString(R.string.create_template_mode)
        val templatesCreated = org.tyndalebt.storyproduceradv.model.languageStringsMap["templates_created"] ?: getString(R.string.templates_created)
        
        Log.d("MainActivity:updateMenuItems", "Setting video_share to: $videoShare")
        Log.d("MainActivity:updateMenuItems", "Setting backup_restore to: $backupRestore")
        Log.d("MainActivity:updateMenuItems", "Setting help_me to: $helpMe")
        
        // Update each menu item with translated string
        menu.findItem(R.id.nav_stories)?.title = getString(R.string.title_activity_story_templates)
        menu.findItem(R.id.nav_registration)?.title = getString(R.string.update_registration)
        menu.findItem(R.id.nav_more_templates)?.title = getString(R.string.more_templates)
        menu.findItem(R.id.nav_word_link_list)?.title = getString(R.string.title_activity_wordlink_list)
        menu.findItem(R.id.nav_workspace)?.title = getString(R.string.update_workspace)
        menu.findItem(R.id.change_language)?.title = getString(R.string.change_language)
        menu.findItem(R.id.video_share)?.title = videoShare
        menu.findItem(R.id.backup_restore)?.title = backupRestore
        menu.findItem(R.id.help_me)?.title = helpMe
        menu.findItem(R.id.create_template_mode)?.title = createTemplateMode
        menu.findItem(R.id.templates_created)?.title = templatesCreated
        menu.findItem(R.id.nav_spadv_website)?.title = getString(R.string.spadv_website)
        menu.findItem(R.id.nav_about)?.title = getString(R.string.about)
        
        // Also try Reword as a fallback
        dev.b3nedikt.reword.Reword.reword(navigationView)
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
                Log.d("MainActivity", "Loaded ${listTypeJson.size} translations for language: $language")
                Log.d("MainActivity", "video_share: ${org.tyndalebt.storyproduceradv.model.languageStringsMap["video_share"]}")
                Log.d("MainActivity", "backup_restore: ${org.tyndalebt.storyproduceradv.model.languageStringsMap["backup_restore"]}")
                Log.d("MainActivity", "help_me: ${org.tyndalebt.storyproduceradv.model.languageStringsMap["help_me"]}")
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

