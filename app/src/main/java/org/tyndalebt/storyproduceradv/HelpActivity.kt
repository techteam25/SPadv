package org.tyndalebt.storyproduceradv

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.apache.commons.net.ftp.FTPClient
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.tools.file.goToURL

class HelpActivity : BaseActivity() {

    private var mDrawerLayout: DrawerLayout? = null
    private lateinit var msgDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setListeners()
        setupDrawer()
        initActionBar()
        invalidateOptionsMenu()
    }

    private fun setListeners() {
        val genUsage = findViewById<TextView>(R.id.general_usage)

        genUsage.setOnClickListener {
            sendEmail("General Usage issue")
        }
        val createTemplate = findViewById<TextView>(R.id.create_new_templates)
        createTemplate.setOnClickListener {
            sendEmail("Want to create new templates")
        }
        val activateTemplate = findViewById<TextView>(R.id.activate_new_templates)
        activateTemplate.setOnClickListener {
            buildSpinner(R.id.new_language)
            toggleVisibility()
        }
        val cancel = findViewById<TextView>(R.id.cancel_action)
        cancel.setOnClickListener {
            toggleVisibility()
        }
        val ok = findViewById<TextView>(R.id.ok_action)
        ok.setOnClickListener {
            validateSave()
        }
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    private fun getMenuItemId(): Int {
        return R.id.help_me
    }

    private fun getTitleString(): String? {
        return getString(R.string.help_me)
    }

    private fun initActionBar() {
        var title = this.getTitleString()
        if (title != null) {
            supportActionBar?.setTitle(title)
        }
        var titleColor = getTitleColor2()
        if (titleColor != R.color.transparent) {

            supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                    titleColor, null)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val hsv: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
                Color.colorToHSV(ContextCompat.getColor(this, titleColor), hsv)
                hsv[2] *= 0.8f
                window.statusBarColor = Color.HSVToColor(hsv)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//  This is a help screen, so no additional help option needed
//        menuInflater.inflate(R.menu.menu_with_help, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout!!.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * initializes the items that the drawer needs
     */
    private fun setupDrawer() {
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

    open fun getTitleColor2(): Int {
        return R.color.transparent
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

                if (!Workspace.checkForInternet(this)) {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setTitle(R.string.more_templates)
                            .setMessage(R.string.remote_check_msg_no_connection)
                            .setPositiveButton("OK") { _, _ ->
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }.create()
                            .show()
                } else {
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

                if (!Workspace.checkForInternet(this)) {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setTitle(R.string.registration_title)
                            .setMessage(R.string.remote_check_msg_no_connection)
                            .setPositiveButton("OK") { _, _ ->
                                startActivity(Intent(this, BaseActivity::class.java))
                                finish()
                            }.create()
                            .show()
                } else {
                    showRegistration(false)
                }
            }
            R.id.change_language -> {
                showChooseLanguage()
            }
            R.id.video_share -> {
                showVideos()
            }
            R.id.backup_restore -> {
                showBackupRestore()
            }
            R.id.help_me -> {
                helpMe()
            }
            R.id.templates_created -> {
                templatesCreated()
            }
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

    private fun sendEmail(pSubject: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.setDataAndType(Uri.parse("mailto:"), "text/plain")
        val toAddress = getText(R.string.registration_robin_rempel_email)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(toAddress.toString()))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, pSubject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, "I need some assistance")

        try {
            this.startActivity(Intent.createChooser(emailIntent, "Sending email..."))
            // DKH - 8/26/2021  Log.i needs second argument for print out
            Log.i("Finished sending email", "Mail Sent")
        } catch (ex: android.content.ActivityNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            Toast.makeText(this,
                    "There is no email client installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleVisibility() {
        var mView = findViewById<TextView>(R.id.password)
        var mVisible = View.GONE
        if (mView.visibility == View.INVISIBLE || mView.visibility == View.GONE) {
            mVisible = View.VISIBLE
        }
        mView.visibility = mVisible

        mView = findViewById<TextView>(R.id.password_text_field)
        mView.visibility = mVisible

        mView = findViewById<TextView>(R.id.enter_new_language)
        mView.visibility = mVisible

        val mSpinner = findViewById<Spinner>(R.id.new_language)
        mSpinner.visibility = mVisible

        mView = findViewById<TextView>(R.id.ok_action)
        mView.visibility = mVisible

        mView = findViewById<TextView>(R.id.cancel_action)
        mView.visibility = mVisible

    }

    private fun validateSave() {
        var mView = findViewById<TextView>(R.id.password_text_field)

        val pass: String = mView.text.toString().toLowerCase()
        if (pass != "sp") {
            incorrectPassword()
            return
        }
        // Valid, save the language path selection
        val mViewS = findViewById<Spinner>(R.id.new_language)
        if (mViewS.selectedItem.toString() == "") {
            languageNotChosen()
            return
        }
        Workspace.registration.load(this)
        Workspace.registration.putString("newLanguage", mViewS.selectedItem.toString())
        Workspace.registration.save(this)
        createTemplateEnabled()
        toggleVisibility()
    }

    private fun incorrectPassword() {
        val dialog = AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(getString(R.string.incorrect_password))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                }.create()
        dialog.show()
    }

    private fun createTemplateEnabled() {
        val dialog = AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(getString(R.string.allow_create_template))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                }.create()
        dialog.show()
    }

    private fun languageNotChosen() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setMessage("")
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                }.create()
        dialog.show()
    }

    private fun setSpinnerValues(pSpinner: Spinner) {
        var choiceStrings: Array<out String>? = arrayOf()
        choiceStrings = getServerFolderList()
        val spinnerAdapter =
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pSpinner.adapter = spinnerAdapter

        for (i in choiceStrings!!.indices) {
            spinnerAdapter.add(choiceStrings[i])
        }
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun buildSpinner(id: Int) {
        val mSpinner = findViewById<Spinner>(id)
        var idList: Int = 0

        if (id == R.id.new_language) {
            setSpinnerValues(mSpinner)
        }
    }

    private fun getServerFolderList(): Array<out String>? {
        var choiceStrings: Array<out String>? = arrayOf()
        if (Build.VERSION.SDK_INT > 8) {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            choiceStrings = getServerFolders()
        }
        return choiceStrings
    }

    private fun getServerFolders() : Array<out String>? {
        var con: FTPClient? = null
        var fList: Array<out String>? = arrayOf()

        val user: String = "ftpstory"
        val pwd: String = "StoryProducer"
        val basePath: String = "/var/www/html/Files/newtemplates"
        val host: String = "rocc.ttapps.org"
        try {
            con = FTPClient()
            con.connect(host)
            if (con.login(user, pwd)) {
                con.enterLocalPassiveMode() // important!
                if (!con.changeWorkingDirectory("$basePath/files")) {
                    con.logout()
                    con.disconnect()
                }
                var result: Boolean = false
                var fList = con.listNames()
                if (fList.isNotEmpty()) {
                    result = true
                }
                if (result) {
                    con.logout()
                    con.disconnect()
                    // Delete zip file, indicating success and not try to upload again
                    Log.v("list result", "succeeded")
                    return fList
                } else {
                    con.logout()
                    con.disconnect()
                    // Failed, show message that we will try again later
                    msgDialog = AlertDialog.Builder(this)
                            .setTitle("")
                            .setMessage(R.string.remote_check_msg_no_connection)
                            .setPositiveButton(R.string.ok) { _, _ -> }
                            .setCancelable(false)
                            .create()

                    msgDialog?.show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            msgDialog = AlertDialog.Builder(this)
                    .setTitle(e.localizedMessage)
                    .setMessage(R.string.remote_check_msg_no_connection)
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .setCancelable(false)
                    .create()

            msgDialog?.show()
        }
        return fList
    }

}