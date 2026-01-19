package org.tyndalebt.storyproduceradv.activities

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.fasterxml.jackson.databind.ObjectMapper
import dev.b3nedikt.restring.Restring
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.JsonHelper
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.controller.adapter.ChooseLangAdapter
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

internal const val CHOOSE_LANGUAGE_FILE = "language.csv"

class ChooseLangActivity : BaseActivity() {
    var mDrawerLayout: DrawerLayout? = null
    var pView: ListView? = null
    var pDownloadImage: ImageView? = null
    var itemArray = arrayOf<String>()
    var tagArray = arrayOf<String>()
    private var bloomFileContents: String = ""
    var languageStringsMap: HashMap<String, String> = HashMap()

    var initialSetup: Boolean = false
    var chosenLanguage: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chosenLanguage = Workspace.readFromFile(this)
        if (chosenLanguage != "" && !Workspace.isInitialized) {
            setLanguage(chosenLanguage!!)
            goToNextStep()
        }
        else {
            setContentView(R.layout.activity_download)
            parseLangFile()

            buildLanguageList(itemArray, tagArray)
        }
    }

    fun buildLanguageList(pList: Array<String>, pURL: Array<String>) {
        setContentView(R.layout.bloom_list_container)

        val mActionBarToolbar = findViewById<Toolbar>(R.id.toolbarMoreTemplates)
        mActionBarToolbar.visibility = View.INVISIBLE

        mDrawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_bloom)
        //Lock from opening with left swipe
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        pView = findViewById<View>(R.id.bloom_list_view) as ListView
        pDownloadImage = findViewById<View>(R.id.image_download) as ImageView

        val arrayList = ArrayList<DownloadDS?>()
        var idx: Int
        var tmp: String?

        idx = 0
        while (idx < pList.size) {
            arrayList.add(DownloadDS(pList[idx], pURL[idx], false))
            idx++
        }

        val arrayAdapter = ChooseLangAdapter(arrayList, this)
        pView!!.setAdapter(arrayAdapter)

        pDownloadImage!!.setImageBitmap(BitmapFactory.decodeResource(this.resources, R.drawable.language))
    }

    fun parseLangFile(): Boolean {
        var result = ""

        try {
            assets.open(CHOOSE_LANGUAGE_FILE).use { inputStream ->
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                // CSV file is now UTF-16, read it as such
                result = String(buffer, StandardCharsets.UTF_16)
            }
        } catch (e: Exception) {
            Log.d("ChooseLangActivity:parseLangFile", e.toString())
            val mDisplayAlert = Intent(this, DisplayAlert::class.java)
            mDisplayAlert.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mDisplayAlert.putExtra("title", getString(R.string.more_templates))
            mDisplayAlert.putExtra(
                "body",
                getString(R.string.remote_check_msg_no_connection)
            )
            startActivity(mDisplayAlert)
            return false
        }
        bloomFileContents = result

        //val lines = result.split("\\r\\n")
        val lines = result.lines()
        var itemString = ""
        var tagString = ""
        var idx: Int
        idx = 0
        while (idx < lines.size) {
            val line = lines[idx].trim()
            if (line.isEmpty()) {
                idx++
                continue
            }
            val lang = line.split(",").toTypedArray()
            if (lang.size < 2) {
                idx++
                continue
            }
            if (itemString != "") {
                itemString = "$itemString|"
                tagString = "$tagString|"
            }
            // Language names are already in UTF-8 from the CSV, use them directly
            // Convert to UTF-16 for internal use if needed
            val languageName = lang[1]
            itemString = itemString + languageName
            tagString = tagString + lang[0]
            idx++
        }

        itemArray = itemString.split("|").toTypedArray()
        tagArray = tagString.split("|").toTypedArray()
        return true
    }

    fun setLanguage(pChosenLanguage: String) {
        val langCode: String = Workspace.getLanguageCode(pChosenLanguage)
        if (langCode != "") {
            updateAppLanguage(langCode)
        }
        writeToFile(pChosenLanguage, this)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun updateStringsHashmap(language: String) {
        getLocalStringJsonHashmap(language).forEach {
            languageStringsMap[it.key] = it.value
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getLocalStringJsonHashmap(language: String): HashMap<String, String> {
        val listTypeJson: HashMap<String, String> = HashMap()
        try {
            applicationContext.assets.open("$language/strings.json").use { inputStream ->
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                // Try UTF-16LE first (for Nepali support - file has BOM ff fe), then UTF-16BE, then UTF-8
                var jsonString: String? = null
                try {
                    // Check for BOM and use appropriate encoding (bytes are signed, so use and 0xFF to get unsigned)
                    val firstByte = buffer[0].toInt() and 0xFF
                    val secondByte = if (buffer.size > 1) buffer[1].toInt() and 0xFF else 0
                    
                    if (buffer.size >= 2 && firstByte == 0xFF && secondByte == 0xFE) {
                        // UTF-16 LE BOM detected, skip BOM and decode
                        jsonString = String(buffer, 2, buffer.size - 2, StandardCharsets.UTF_16LE)
                    } else if (buffer.size >= 2 && firstByte == 0xFE && secondByte == 0xFF) {
                        // UTF-16 BE BOM detected, skip BOM and decode
                        jsonString = String(buffer, 2, buffer.size - 2, StandardCharsets.UTF_16BE)
                    } else {
                        // No BOM, try UTF-16LE (default for Windows)
                        jsonString = String(buffer, StandardCharsets.UTF_16LE)
                    }
                    // Remove BOM character (U+FEFF) if present at the start of the string
                    if (jsonString.isNotEmpty() && jsonString[0] == '\uFEFF') {
                        jsonString = jsonString.substring(1)
                    }
                    // Trim whitespace and remove any leading/trailing BOM characters
                    jsonString = jsonString.trim()
                    // Remove BOM character from start again after trim
                    if (jsonString.isNotEmpty() && jsonString[0] == '\uFEFF') {
                        jsonString = jsonString.substring(1)
                    }
                    // Log first few characters to debug
                    Log.d("ChooseLangActivity", "UTF-16 decoded string length: ${jsonString.length}, first 50 chars: ${jsonString.take(50)}")
                    // Check for any BOM or invalid characters at the start
                    if (jsonString.isNotEmpty()) {
                        val firstChar = jsonString[0]
                        val firstCharCode = firstChar.code
                        Log.d("ChooseLangActivity", "First character: '$firstChar' (code: $firstCharCode, 0x${firstCharCode.toString(16)})")
                        if (firstCharCode == 0xFEFF) {
                            Log.d("ChooseLangActivity", "BOM character still present, removing...")
                            jsonString = jsonString.substring(1)
                        }
                    }
                    // Ensure string starts with { and doesn't have BOM
                    var cleanJsonString = jsonString.trim()
                    while (cleanJsonString.isNotEmpty() && (cleanJsonString[0] == '\uFEFF' || cleanJsonString[0].isWhitespace())) {
                        cleanJsonString = cleanJsonString.substring(1).trimStart()
                    }
                    if (!cleanJsonString.startsWith("{")) {
                        Log.e("ChooseLangActivity", "JSON doesn't start with {, first char: '${cleanJsonString.take(10)}'")
                        throw Exception("Invalid JSON format")
                    }
                    // Try to parse to verify it's valid JSON - parse once and reuse
                    val jsonNode = ObjectMapper().readTree(cleanJsonString)
                    // Use the already parsed JSON node
                    JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                        "",
                        jsonNode,
                        listTypeJson
                    )
                    // Success - return early
                    return listTypeJson
                } catch (e: Exception) {
                    // If UTF-16 fails, try UTF-8 (standard JSON encoding)
                    Log.d("ChooseLangActivity", "UTF-16 read failed for $language/strings.json, trying UTF-8: ${e.message}")
                    Log.d("ChooseLangActivity", "UTF-16 exception type: ${e.javaClass.simpleName}, at line: ${e.stackTrace.firstOrNull()?.lineNumber}")
                    try {
                        jsonString = String(buffer, StandardCharsets.UTF_8)
                        // Trim and remove BOM from UTF-8 version too
                        jsonString = jsonString.trim()
                        if (jsonString.isNotEmpty() && jsonString[0] == '\uFEFF') {
                            jsonString = jsonString.substring(1)
                        }
                        // Try to parse UTF-8 version - parse once and reuse
                        val jsonNode = ObjectMapper().readTree(jsonString)
                        // Use the already parsed JSON node
                        JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                            "",
                            jsonNode,
                            listTypeJson
                        )
                        // Success - return early
                        return listTypeJson
                    } catch (e2: Exception) {
                        Log.e("ChooseLangActivity", "Both UTF-16 and UTF-8 failed for $language/strings.json", e2)
                        Log.e("ChooseLangActivity", "First 200 chars of decoded string: ${jsonString?.take(200)}")
                        throw e2
                    }
                }
            }
        } catch (exception: IOException) {
            Log.e("ChooseLangActivity", "Error reading strings.json for language: $language", exception)
        }
        return listTypeJson
    }

    fun updateAppLanguage(language: String) {
        updateStringsHashmap(language)
        Restring.locale = Locale(language)
        Restring.putStrings(Restring.locale, languageStringsMap)
    }

    public fun goToNextStep() {
        if (Workspace.isInitialized) {
            showMain()
        } else {
            startActivity(Intent(this, SplashScreenActivity::class.java))
            finish()
        }
    }

    private fun writeToFile(data: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput("config.txt", MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }
}
