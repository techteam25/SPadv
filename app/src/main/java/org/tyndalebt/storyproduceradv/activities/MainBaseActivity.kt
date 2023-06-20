package org.tyndalebt.storyproduceradv.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.Phase
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.goToURL
import java.io.InputStream

open class MainBaseActivity : BaseActivity() {

    protected var mDrawerLayout: DrawerLayout? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_with_help, menu)
        return true
    }

    open fun openHelpFile() : InputStream {
        return  Phase.openHelpDocFile(PhaseType.STORY_LIST, Workspace.activeStory.language,this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout!!.openDrawer(GravityCompat.START)
                true
            }
            R.id.helpButton -> {

                val wv = WebView(this)
                val iStream = openHelpFile()
                //val iStream = Phase.openHelpDocFile(PhaseType.STORY_LIST, Workspace.activeStory.language,this)

                val text = iStream.reader().use {
                        it.readText() }

                wv.loadDataWithBaseURL(null,text,"text/html", null,null)
                val dialog = AlertDialog.Builder(this)
//                    .setTitle("${resources.getString(R.string.title_activity_story_templates)} ${resources.getString(R.string.help)}\n")
                    .setView(wv)
                    .setNegativeButton("Close") { dialog, _ ->
                        dialog!!.dismiss()
                    }
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * initializes the items that the drawer needs
     */
    protected fun setupDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(::onNavigationItemSelected)
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    open fun getMenuItemId() : Int {
      return -1
    }
    
    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        mDrawerLayout?.closeDrawers()
   
        if (menuItem.itemId == getMenuItemId()) {
            return true  // do nothing, current page is displayed
         }
      
        when (menuItem.itemId) {
            R.id.nav_workspace -> {
                showSelectTemplatesFolderDialog()
            }
            R.id.nav_word_link_list -> {
                showWordLinksList()
            }
            R.id.nav_more_templates -> {
                // DKH - 01/15/2022 Issue #571: Add a menu item for accessing templates from Google Drive
                // A new menu item was added that opens a URL for the user to download templates.
                // If we get here, the user wants to browse for more templates, so,
                // open the URL in a new activity

                if (Workspace.checkForInternet(this) == false) {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setTitle(R.string.more_templates)
                        .setMessage(R.string.remote_check_msg_no_connection)
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this@MainBaseActivity, MainBaseActivity::class.java))
                            finish()
                        }.create()
                        .show()
                }
                else {
                    Workspace.startDownLoadMoreTemplatesActivity(this)
                }

            }
            R.id.nav_stories -> {
                showMain()
            }
            R.id.nav_registration -> {
                // DKH - 05/10/2021 Issue 573: SP will hang/crash when submitting registration
                // The MainBaseActivity thread is responsible for displaying  story templates
                // and allowing the user to select  a registration update via this menu option.
                // So, when calling the RegistrationActivity from the MainBaseActivity, specify that
                // finish should not be called.  This is done by setting executeFinishActivity to false.
                // After the RegistrationActivity is complete, MainBaseActivity will then display
                // the story template list

                if (Workspace.checkForInternet(this) == false) {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setTitle(R.string.registration_title)
                        .setMessage(R.string.remote_check_msg_no_connection)
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this@MainBaseActivity, MainBaseActivity::class.java))
                            finish()
                        }.create()
                        .show()
                }
                else {
                    showRegistration(false)
                }
            }
            R.id.change_language -> {
                showChooseLanguage()
            }
            R.id.video_share -> {
                showVideos()
            }
            //R.id.backup_sync -> {
            //    showBackup()
            //}
            R.id.nav_spadv_website -> {
                goToURL(this, Workspace.URL_FOR_WEBSITE)
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
        }

        return true
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.exit_application))
                .setMessage(getString(R.string.quit_question))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                }.create()
        dialog.show()
    }
}

