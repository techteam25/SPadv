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
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.file.goToURL

class ActivateTemplateActivity : MainBaseActivity() {

    //private var mDrawerLayout: DrawerLayout? = null
    //private lateinit var msgDialog: AlertDialog
    val selectOne: String = "Select one"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activate_temeplate)
        setListeners()
        setupDrawer()
        initActionBar()
        invalidateOptionsMenu()
    }

    private fun setListeners() {

        val activateTemplate = findViewById<TextView>(R.id.btn_activate_mode)
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

        val switch = findViewById<TextView>(R.id.sw_hide_show)
        val label = findViewById<TextView>(R.id.lbl_show_status)
        switch.setOnClickListener {
            var txtValue = ""
            if (label.text.toString() == getString(R.string.create_template_is_visible)) {
                txtValue = getString(R.string.create_template_is_hidden)
            } else {
                txtValue = getString(R.string.create_template_is_visible)
            }
            label.text = txtValue
            Workspace.registration.load(this)
            Workspace.registration.putString("createTemplate", txtValue)
            Workspace.registration.save(this)
        }
        if (Workspace.registration.getString("newLanguage") == "") {
            switch.visibility = View.GONE
            label.text = ""
        } else {
            switch.visibility = View.VISIBLE
            if (Workspace.registration.getString("createTemplate") == getString(R.string.create_template_is_hidden)) {
                label.text = Workspace.registration.getString("createTemplate")
            } else {
                label.text = getString(R.string.create_template_is_visible)
            }
        }
    }


    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId(): Int {
        return R.id.create_template_mode
    }

    override fun getTitleString(): String? {
        return getString(R.string.create_template_mode)
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


    // If this menu item is selected, do nothing
    // since this is the currently selected page.

    override fun getTitleColor2(): Int {
        return R.color.transparent
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
        if (mViewS.selectedItem.toString() == "" || mViewS.selectedItem.toString() == selectOne) {
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

        spinnerAdapter.add(selectOne)
        for (i in choiceStrings!!.indices) {
            if (choiceStrings[i] != "home") {
                spinnerAdapter.add(choiceStrings[i])
            }
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
                // starting folder should be folder that has all the folders of each language (plus "home" which we will ignore later, if present)
                var result: Boolean = false
                var fList = con.listNames()
                if (fList.isNotEmpty()) {
                    result = true
                }
                if (result) {
                    con.logout()
                    con.disconnect()
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
