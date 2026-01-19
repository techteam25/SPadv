package org.tyndalebt.storyproduceradv.tools.file

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.WORD_LINKS_DIR
import org.tyndalebt.storyproduceradv.model.WORD_LINKS_JSON_FILE

fun WordLinkList.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    val adapter = moshi.adapter(WordLinkList::class.java)
    val oStream = getWordLinksChildOutputStream(context,
            WORD_LINKS_JSON_FILE,"")
    if(oStream != null) {
        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_16))
        oStream.close()
    }
}

/**
 * Retrieves the list of all the word links from wordlinks.json
 */
fun wordLinkListFromJson(context: Context): WordLinkList? {
    val moshi = Moshi
            .Builder()
            .add(UriAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    val adapter = moshi.adapter(WordLinkList::class.java)
    val fileContents = getStoryText(context, WORD_LINKS_JSON_FILE, WORD_LINKS_DIR) ?: return null
    return adapter.fromJson(fileContents)
}