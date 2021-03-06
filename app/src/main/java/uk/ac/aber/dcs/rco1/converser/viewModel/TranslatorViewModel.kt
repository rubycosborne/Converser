package uk.ac.aber.dcs.rco1.converser.viewModel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Spinner
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import uk.ac.aber.dcs.rco1.converser.model.ConverserRepository
import uk.ac.aber.dcs.rco1.converser.model.home.TranslationItem

/**
 * View model class for the translator fragment
 * Handles the logic of the fragment class (Currently only database communication)
 * Implements AndroidViewModel - Need to use application context to instantiate a repository
 * @constructor
 *
 * @param application - application context required for AndroidViewModel
 *
 * @author Ruby Osborne (rco1)
 * @version 1.0 (release)
 */
class TranslatorViewModel(application: Application) :
    AndroidViewModel(application) {

    //repository to use for communication with the database
    private val repository: ConverserRepository = ConverserRepository(application)

    //list of translation items in the conversation
    var translationItems: LiveData<List<TranslationItem>> = getAllTranslationItems()
        //public get method which can only be set privately (read only from home fragment)
        private set


    /**
     * Gets all translation items in the conversation from the database
     *
     * @return a list of translation items as live data
     */
    private fun getAllTranslationItems(): LiveData<List<TranslationItem>> {
        return repository.getAllTranslationItems()
    }

    /**
     * Adds a translation item into the conversation saved in the database
     *
     * @param translationItem - the item to insert into a conversation
     */
    fun addTranslationItemToConversation(translationItem: TranslationItem) {
        repository.insert(translationItem)
    }

    /**
     * Removes all entries in the database
     *
     */
    private fun deleteConversation() {
        repository.deleteAll()
    }

    /**
     * Set the positionInConversation code for a positionInConversation using the API
     * positionInConversation code retrieval mechanism
     *
     * @param spinner - The source or target positionInConversation spinner where the
     * positionInConversation has been selected
     * @return the positionInConversation code the positionInConversation e.g. en for English
     */
    fun setLanguageCode(spinner: Spinner): String {
        return when (spinner.selectedItem.toString()) {
            "Afrikaans" -> TranslateLanguage.AFRIKAANS
            "Arabic" -> TranslateLanguage.ARABIC
            "Belarusian" -> TranslateLanguage.BELARUSIAN
            "Bulgarian" -> TranslateLanguage.BULGARIAN
            "Bengali" -> TranslateLanguage.BENGALI
            "Catalan" -> TranslateLanguage.CATALAN
            "Czech" -> TranslateLanguage.CZECH
            "Welsh" -> TranslateLanguage.WELSH
            "Danish" -> TranslateLanguage.DANISH
            "German" -> TranslateLanguage.GERMAN
            "Greek" -> TranslateLanguage.GREEK
            "English" -> TranslateLanguage.ENGLISH
            "Esperanto" -> TranslateLanguage.ESPERANTO
            "Spanish" -> TranslateLanguage.SPANISH
            "Estonian" -> TranslateLanguage.ESTONIAN
            "Persian" -> TranslateLanguage.PERSIAN
            "Finnish" -> TranslateLanguage.FINNISH
            "French" -> TranslateLanguage.FRENCH
            "Irish" -> TranslateLanguage.IRISH
            "Galician" -> TranslateLanguage.GALICIAN
            "Gujarati" -> TranslateLanguage.GUJARATI
            "Hebrew" -> TranslateLanguage.HEBREW
            "Hindi" -> TranslateLanguage.HINDI
            "Croatian" -> TranslateLanguage.CROATIAN
            "Haitian" -> TranslateLanguage.HAITIAN_CREOLE
            "Hungarian" -> TranslateLanguage.HUNGARIAN
            "Indonesian" -> TranslateLanguage.INDONESIAN
            "Icelandic" -> TranslateLanguage.ICELANDIC
            "Italian" -> TranslateLanguage.ITALIAN
            "Japanese" -> TranslateLanguage.JAPANESE
            "Georgian" -> TranslateLanguage.GEORGIAN
            "Kannada" -> TranslateLanguage.KANNADA
            "Korean" -> TranslateLanguage.KOREAN
            "Lithuanian" -> TranslateLanguage.LITHUANIAN
            "Latvian" -> TranslateLanguage.LATVIAN
            "Macedonian" -> TranslateLanguage.MACEDONIAN
            "Marathi" -> TranslateLanguage.MARATHI
            "Malay" -> TranslateLanguage.MALAY
            "Maltese" -> TranslateLanguage.MALTESE
            "Dutch" -> TranslateLanguage.DUTCH
            "Norwegian" -> TranslateLanguage.NORWEGIAN
            "Polish" -> TranslateLanguage.POLISH
            "Portuguese" -> TranslateLanguage.PORTUGUESE
            "Romanian" -> TranslateLanguage.ROMANIAN
            "Russian" -> TranslateLanguage.RUSSIAN
            "Slovak" -> TranslateLanguage.SLOVAK
            "Slovenian" -> TranslateLanguage.SLOVENIAN
            "Albanian" -> TranslateLanguage.ALBANIAN
            "Swedish" -> TranslateLanguage.SWEDISH
            "Swahili" -> TranslateLanguage.SWAHILI
            "Tamil" -> TranslateLanguage.TAMIL
            "Telugu" -> TranslateLanguage.TELUGU
            "Thai" -> TranslateLanguage.THAI
            "Tagalog" -> TranslateLanguage.TAGALOG
            "Turkish" -> TranslateLanguage.TURKISH
            "Ukrainian" -> TranslateLanguage.UKRAINIAN
            "Urdu" -> TranslateLanguage.URDU
            "Vietnamese" -> TranslateLanguage.VIETNAMESE
            "Chinese" -> TranslateLanguage.CHINESE
            //default to english
            else -> TranslateLanguage.ENGLISH
        }
    }


}