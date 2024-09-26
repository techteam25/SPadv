package org.tyndalebt.storyproduceradv.model

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.fasterxml.jackson.databind.ObjectMapper
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.BuildConfig
import org.tyndalebt.storyproduceradv.controller.JsonHelper
import org.tyndalebt.storyproduceradv.tools.file.getChildDocuments
import org.tyndalebt.storyproduceradv.tools.file.getStoryFileDescriptor
import org.tyndalebt.storyproduceradv.tools.file.getText
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

var languageStringsMap: HashMap<String, String> = HashMap()

val prompt_en = "Use this slide to compose and record a song for the Bible story! Or ask questions to help people think about the story!  You can choose to include it or not include it when you make a video."
val prompt_es = "Utilice esta diapositiva para componer y grabar una canción para la historia bíblica. ¡O haga preguntas para ayudar a la gente a pensar en la historia!  Pueda optar por incluirla o no incluirla cuando haga un vídeo."
val prompt_fr = "Utilisez cette diapositive pour composer et enregistrer une chanson pour le récit biblique ! Ou posez des questions pour aider les gens à réfléchir à l'histoire ! Vous pouvez choisir de l'inclure ou non lorsque vous créez une vidéo."
val prompt_hi = "बाइबिल कहानी के लिए एक गीत लिखने और रिकॉर्ड करने के लिए इस स्लाइड का उपयोग करें! या लोगों को कहानी के बारे में सोचने में मदद करने के लिए प्रश्न पूछें! जब आप वीडियो बनाते हैं तो आप इसे शामिल करना या न करना चुन सकते हैं।"
val prompt_id = "Gunakan slide ini untuk membuat dan merekam lagu untuk cerita Alkitab! Atau ajukan pertanyaan untuk membantu orang berpikir tentang ceritanya! Anda dapat memilih untuk memasukkannya atau tidak memasukkannya saat Anda membuat video."
val prompt_po = "Use este slide para compor e gravar uma música para a história da Bíblia! Ou faça perguntas para ajudar as pessoas a pensar sobre a história! Você pode optar por incluí-lo ou não ao fazer um vídeo."
val prompt_sw = "Katika ukurasa huu, tunga na urekodi wimbo kwa ajili ya hadithi ya Biblia! Au, uliza maswali ya majadiliano kuhusu hadithi. Unaweza kuchagua kujumuisha au kutenga ukurasa huu unapokamilisha video."
val prompt_tp = "Kamapim nupela song long poromanim stori!  Bihain, taim yu pinisim wok na kamapim vidio, yu ken skruim singsing long stori vidio o yu ken larim song i stap nating."

val la_English = "English"
val la_French = "French"
val la_Hindi = "Hindi"
val la_Indonesian = "Indonesian"
val la_Portuguese = "Portuguese"
val la_Spanish = "Spanish"
val la_Swahili = "Swahili"
val la_TokPisin = "Tok Pisin"
val la_Bislama = "Bislama"
val la_Khmer = "Khmer"
val la_Nepali = "Nepali"
val la_Telugu = "Telugu"

fun parseBloomHTML(context: Context, storyPath: DocumentFile): Story? {
    //See if there is a BLOOM html file there
    val childDocs = getChildDocuments(context, storyPath.name!!)
    var html_name = ""

    languageStringsMap[la_English] = prompt_en
    languageStringsMap[la_French] = prompt_fr
    languageStringsMap[la_Hindi] = prompt_hi
    languageStringsMap[la_Indonesian] = prompt_id
    languageStringsMap[la_Portuguese] = prompt_po
    languageStringsMap[la_Spanish] = prompt_es
    languageStringsMap[la_Swahili] = prompt_sw
    languageStringsMap[la_TokPisin] = prompt_tp
        // These do not have translations currently - so we will set them to english
    languageStringsMap[la_Bislama] = prompt_en
    languageStringsMap[la_Khmer] = prompt_en
    languageStringsMap[la_Nepali] = prompt_en
    languageStringsMap[la_Telugu] = prompt_en

    for (f in childDocs) {
        if (f.endsWith(".html") || f.endsWith(".htm")){
            html_name = f
            continue
        }
    }
    if(html_name == "") return null
    val htmlText = getText(context,"${storyPath.name}/$html_name") ?: return null
    //The file "index.html" is there, it is a Bloom project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val story = Story(storyPath.name!!, slides)
    story.importAppVersion = BuildConfig.VERSION_NAME
    story.language = Workspace.parseLanguage
    val soup = Jsoup.parse(htmlText)

    //add the title slide
    val frontCoverSlideBuilder = BloomFrontCoverSlideBuilder()
    frontCoverSlideBuilder.build(context, storyPath, soup)?.also {
        slides.add(it)
    } ?: return null

    val lang = frontCoverSlideBuilder.lang

    var slide = Slide()
    val pages = soup.getElementsByAttributeValueContaining("class","numberedPage")
    if(pages.size <= 2) return null
    for (page in pages) {
        if (page.attr("class").contains("numberedPage")) {
            NumberedPageSlideBuilder().build(context, storyPath, page, lang)?.also {
                slides.add(it)
            }
        }
    }

    //Add the song slide
    slide = Slide()
    slide.slideType = SlideType.LOCALSONG
//    slide.content = context.getString(R.string.LS_prompt)
    slide.content = languageStringsMap[Workspace.parseLanguage].toString()
    slide.musicFile = MUSIC_NONE
    slides.add(slide)

    //Before the first page is the bloomDataDiv stuff.  Get the originalAcknowledgments.
    //If they are there, append to the end of the slides.
    val mOrgAckns = soup.getElementsByAttributeValueMatching("class","(?=.*bloom-translationGroup)(?=.*originalAcknowledgments)")
    if(mOrgAckns.size >= 1){
        val mOrgAckn = mOrgAckns[0]
        slide = Slide()
        slide.slideType = SlideType.COPYRIGHT
        val mOAParts = mOrgAckn.getElementsByAttributeValueContaining("class","bloom-editable")
        slide.content = ""
        for(p in mOAParts){
            slide.content += p.wholeText()
        }
        //cleanup whitespace
        slide.content = slide.content.trim().replace("\\s*\\n\\s*".toRegex(),"\n")
        slide.translatedContent = slide.content
        slide.musicFile = MUSIC_NONE
        slides.add(slide)
    }

    return story
}

//Image and transition pattern
val reRect = "([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+)".toRegex()

fun parsePage(context: Context, frontCoverGraphicProvided: Boolean, page: Element, slide: Slide, storyPath: DocumentFile): Boolean {
    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true

    val audios = page.getElementsByAttributeValueContaining("class", "audio-sentence")

    //narration
    if (slide.narrationFile.isEmpty()) {
        if (audios.size >= 1) {
            slide.narrationFile = "audio/${audios[0].id()}.mp3"
        } else {
            return false
        }
    }

    if (!slide.isFrontCover() && !slide.isNumberedPage()) {
        slide.content = ""
        for (a in audios) {
            slide.content += a.wholeText()
        }
        slide.content = slide.content.trim().replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    //soundtrack
    val soundtrack = page.getElementsByAttribute("data-backgroundaudio")
    if(soundtrack.size >= 1){
        slide.musicFile = "audio/${soundtrack[0].attr("data-backgroundaudio")}"
        // DKH - 07/23/2021
        // Issue #585: SP fails to read new templates made with Story Publisher Adv Template Maker
        // The attr method on class Node (ie, soundtrack[0] object) does not return a null but either
        // the attribute string or an empty sting
        // The following method throws this exception: "java.lang.NumberFormatException: empty String",
        // because it tries to do a ".toFloat()" on an empty string
        // slide.volume = (soundtrack[0].attr("data-backgroundaudiovolume") ?: "0.25").toFloat()
        // Replace the previous line of code with the following, which checks for string length
        // to determine if we have an empty string

        // grab the node attribute
        val slideVolume = soundtrack[0].attr("data-backgroundaudiovolume")
        if(slideVolume.length == 0) { // if the attribute length is zero, we have an empty string
            slide.volume = 0.25F     // assign a default volume
        }else {
            // convert slideVolume string to a float
            slide.volume = slideVolume.toFloat()
        }
    }

    //image
    val images = page.getElementsByAttributeValueContaining("class","bloom-imageContainer")
    if(images.size >= 1){
        val image = images[0]
        if (!slide.isFrontCover() || frontCoverGraphicProvided) {
            slide.imageFile = image.attr("src")
            if (slide.imageFile == "") {
                //bloomd books store the image in a different location
                slide.imageFile = image.attr("style")
                //typical format: background-image:url('1.jpg')
                slide.imageFile = slide.imageFile.substringAfter("'").substringBefore("'")
            }
            if (slide.imageFile == "") {
                val src = image.getElementsByAttribute("src")
                if (src.size >= 1) slide.imageFile = src[0].attr("src")
            }
        }

        // RK 04/13/2023
        // see TestDownloadActivity.downloadListTest
        // The unit test fail for the coverslide and an empty imagefile
        // Do we need to call the BitmapFactory code in the case of
        // an empty image file at all?   Note: bmOptions content may be affected
        if (!(Workspace.isUnitTest && slide.imageFile == "")) {
            BitmapFactory.decodeFileDescriptor(getStoryFileDescriptor(context, slide.imageFile, "image/*", "r", storyPath.name!!), null, bmOptions)
        }

        slide.height = bmOptions.outHeight
        slide.width = bmOptions.outWidth
        slide.startMotion = Rect(0, 0, slide.width, slide.height)
        slide.endMotion = Rect(0, 0, slide.width, slide.height)

        val mSR = reRect.find(image.attr("data-initialrect"))
        if(mSR != null) {
            val x = mSR.groupValues[1].toDouble()*slide.width
            val y = mSR.groupValues[2].toDouble()*slide.height
            val w = mSR.groupValues[3].toDouble()*slide.width
            val h = mSR.groupValues[4].toDouble()*slide.height
            slide.startMotion = Rect((x).toInt(), //left
                    (y).toInt(),  //top
                    (x+w).toInt(),   //right
                    (y+h).toInt())  //bottom
        }
        val mER = reRect.find(image.attr("data-finalrect"))
        if(mER != null) {
            val x = mER.groupValues[1].toDouble()*slide.width
            val y = mER.groupValues[2].toDouble()*slide.height
            val w = mER.groupValues[3].toDouble()*slide.width
            val h = mER.groupValues[4].toDouble()*slide.height
            slide.endMotion = Rect((x).toInt(), //left
                    (y).toInt(),  //top
                    (x+w).toInt(),   //right
                    (y+h).toInt())  //bottom
        }
    }
    return true
}

