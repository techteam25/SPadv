package org.tyndalebt.storyproduceradv.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import dev.b3nedikt.reword.Reword
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.tools.file.getWorkspaceUri
import org.tyndalebt.storyproduceradv.tools.file.goToURL
import java.io.InputStream

open class MainBaseActivity : BaseActivity() {

    protected var mDrawerLayout: DrawerLayout? = null
    protected lateinit var msgDialog: AlertDialog

    //override fun onCreate(savedInstanceState: Bundle?) {
    //    super.onCreate(savedInstanceState)
    //    initActionBar()
    //}

    fun initActionBar() {
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
        
        // Update menu items with translations using Reword
        Reword.reword(navigationView)
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    open fun getMenuItemId() : Int {
      return -1
    }

    open fun getTitleString() : String? {
        return null
    }

    open fun getTitleColor2() : Int {
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
                            startActivity(Intent(this@MainBaseActivity, MainActivity::class.java))
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

                if (!Workspace.checkForInternet(this)) {
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
            R.id.backup_restore -> {
                showBackupRestore()
            }
            R.id.help_me -> {
                helpMe()
            }
            R.id.create_template_mode -> {
                createTemplateMode()
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

    // This method would probably be better served moving it to UriUtils (see UriUtils.getUIPathText)
    // but Regex code seems to be happier in kotlin than in java.
    fun getUIPathTextInternal(uriStr: String, replaceStr : String): String {

        // At this point the videoFileUriStr will look something like this: /storage/emulated/0/
        // This is the actual path. However, it needs be changed to the SD Card (/sdcard/)
        // which is a symbolic link to the emulated storage path.
        // sdcard/: Is a symlink to...
        //      /storage/sdcard0 (Android 4.0+)
        // In Story Publisher Adv, the version will never be less than Android 4.0
        // We will instead show it as an optional [sdcard]
        // The below code will change: /storage/emulated/0/ to /storage/[sdcard]/
        var retVal = uriStr.replace(Regex("(/storage\\/emulated\\/)\\d+"), replaceStr)

        // Also, the SD-Card could show up as /storage/####-####/ where # is a hexidecimal value
        retVal = retVal.replace(Regex("(/storage)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"), replaceStr)

        // Also, the SD-Card could show up as /mnt/media_rw/####-####/ where # is a hexidecimal value for earlier android releases
        retVal = retVal.replace(Regex("(/mnt/media_rw)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"), replaceStr)

        // Also, this is for a usb memory stick
        retVal = retVal.replace(Regex("(/dev/bus/usb)\\/[0-9]{3}\\/[0-9]{3}"), replaceStr)
        return retVal
    }

    fun goForIt(pFileName: String) : Boolean {
        // if upload is successful, also send a blank file to indicate in the root level of that user, what files were uploaded
        val lang = Workspace.registration.getString("newLanguage")
        if (goForItInternal(NEW_TEMPLATES_DIR, "$pFileName.zip", lang, "$pFileName.zip")) {
            // Usage
            val tempFileName = "emptyFile.txt"
            val fileUri = getWorkspaceUri(tempFileName)
            try {
                DocumentsContract.createDocument(this.contentResolver, Workspace.workdocfile.uri, "*/*", tempFileName)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return goForItInternal(".", tempFileName, "", "$lang - $pFileName")
        } else {
            return false
        }
    }
        // Transfer file from pSourcePath/pSourceFilename via FTP to pDestPath/pDestFileName
    private fun goForItInternal(pSourcePath: String, pSourceFileName: String, pDestPath: String, pDestFileName: String) : Boolean {

        val sourceFilePath = "$pSourcePath/$pSourceFileName"
        var con: FTPClient? = null

        val user: String = "ftpstory"
        val pwd: String = "StoryProducer"
        val host: String = "rocc.ttapps.org"
        try {
            con = FTPClient()
            con.autodetectUTF8 = true
            con.controlEncoding = "UTF-8"
            con.connect(host)
            if (con.login(user, pwd)) {
                con.enterLocalPassiveMode() // important!
                con.setFileType(FTP.BINARY_FILE_TYPE)
                //val lang = Workspace.registration.getString("newLanguage")
                // directory created by server admin (Robin/TECH/etc) should always exist since choice list is based on it
                if (pDestPath.isNotEmpty()) {
                    if (!con.changeWorkingDirectory(pDestPath)) {
                        con.logout()
                        con.disconnect()
                        return false
                    }
                }
                val resolver = this.contentResolver
                val fileUri = getWorkspaceUri(sourceFilePath)
                val `in` = resolver.openInputStream(fileUri!!)
                val result = con.storeFile(pDestFileName, `in`)
                `in`!!.close()
                if (result) {
                    con.logout()
                    con.disconnect()
                    // Delete zip file, indicating success and not try to upload again
                    val deleteUri = getWorkspaceUri(sourceFilePath)
                    org.tyndalebt.storyproduceradv.tools.file.deleteFile(this, deleteUri!!)
                    Log.v("upload result", "succeeded")
                    msgDialog.dismiss()
                    return true
                } else {
                    con.logout()
                    con.disconnect()
                    // Failed, show message that we will try again later
                    msgDialog.dismiss()
                    msgDialog = AlertDialog.Builder(this)
                            .setTitle(R.string.template_upload_failed)
                            .setMessage(R.string.template_upload_retry)
                            .setPositiveButton(R.string.ok) { _, _ -> }
                            .setCancelable(false)
                            .create()

                    msgDialog?.show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            msgDialog.dismiss()
            msgDialog = AlertDialog.Builder(this)
                    .setTitle(e.localizedMessage)
                    .setMessage(R.string.template_upload_retry)
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .setCancelable(false)
                    .create()

            msgDialog?.show()
        }
        return false
    }
}
