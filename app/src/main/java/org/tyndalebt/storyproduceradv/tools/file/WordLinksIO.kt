package org.tyndalebt.storyproduceradv.tools.file

import android.content.Context
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.adapter
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.WORD_LINKS_DIR
import org.tyndalebt.storyproduceradv.model.WORD_LINKS_JSON_FILE
import kotlin.jvm.Volatile
import java.io.OutputStream

//fun WordLinkList.toJson(context: Context){    val moshi = Moshi
//    .Builder()
//    .add(KotlinJsonAdapterFactory()) // Essential for Kotlin data classes
//    .build()
//
//    val adapter = WordLinkList.jsonAdapter(moshi)
//    val oStream = getWordLinksChildOutputStream(context,
//            WORD_LINKS_JSON_FILE,"")
//    if(oStream != null) {
//        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
//        oStream.close()
//    }
//}

fun WordLinkList.toJson(context: Context){
    val gson = GsonBuilder().create()
    val jsonString = gson.toJson(this)
    val oStream: OutputStream? = getWordLinksChildOutputStream(context,
        WORD_LINKS_JSON_FILE,"")

    if(oStream != null) {
        oStream.write(jsonString.toByteArray(Charsets.UTF_8))
        oStream.close()
    }
}

/**
 * Retrieves the list of all the word links from wordlinks.json
 */
//fun wordLinkListFromJson(context: Context): WordLinkList? {
//    val moshi = Moshi
//            .Builder()
//            .add(UriAdapter())
//        .add(KotlinJsonAdapterFactory()) // Essential for Kotlin data classes
//            .build()
//    val adapter = WordLinkList.jsonAdapter(moshi)
//    val fileContents = getStoryText(context, WORD_LINKS_JSON_FILE, WORD_LINKS_DIR) ?: return null
//    return adapter.fromJson(fileContents)
//}

fun wordLinkListFromJson(context: Context): WordLinkList? {
    val gson = GsonBuilder().create() // Add .setLenient() or .setPrettyPrinting() if needed
    val fileContents = getStoryText(context, WORD_LINKS_JSON_FILE, WORD_LINKS_DIR) ?: return null
    return try {
        gson.fromJson(fileContents, WordLinkList::class.java)
    } catch (e: Exception) {
        // Handle parsing errors, e.g., log them
        e.printStackTrace()
        null
    }
}