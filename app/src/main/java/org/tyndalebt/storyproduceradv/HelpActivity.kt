package org.tyndalebt.storyproduceradv

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.core.view.GravityCompat
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity

class HelpActivity : MainBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setListeners()
        setupDrawer()
        initActionBar()
        invalidateOptionsMenu()
    }

    private fun setListeners() {
        val genUsage = findViewById<TextView>(R.id.btn_general_usage)

        genUsage.setOnClickListener {
            sendEmail("General Usage issue")
        }
        val createTemplate = findViewById<TextView>(R.id.btn_create_new_templates)
        createTemplate.setOnClickListener {
            sendEmail("Want to create new templates")
        }
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId(): Int {
        return R.id.help_me
    }

    override fun getTitleString(): String? {
        return getString(R.string.help_me)
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

    private fun sendEmail(pSubject: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        val toAddress = getText(R.string.registration_robin_rempel_email)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(toAddress.toString()))
        //emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("dharding@arkweb.org"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, pSubject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, "I need some assistance")
        emailIntent.type = "text/html"

        startActivity(emailIntent)
    }
}
