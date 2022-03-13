package uk.ac.aber.dcs.rco1.converser.ui.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextUtils.isEmpty
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import uk.ac.aber.dcs.rco1.converser.R
import uk.ac.aber.dcs.rco1.converser.databinding.FragmentHomeBinding
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : Fragment(){

    private lateinit var homeFragmentBinding: FragmentHomeBinding

    //startActivityForResult is deprecated so need to use this instead
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    //UI elements
    private lateinit var translateButton: ImageButton
    private lateinit var micFAB: ImageButton
    private lateinit var swapButton: ImageButton
   // private lateinit var originalTextView: TextView
   // private lateinit var translatedTextView: TextView
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var inputText: EditText
    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var messageList : ArrayList<Message>

    private var sourceLanguage: String = ""
    private var targetLanguage: String = ""
    private var stringToTranslate: String = ""

    var sourceLanguageCode = TranslateLanguage.ENGLISH
    var targetLanguageCode = TranslateLanguage.ENGLISH

    private lateinit var translator: Translator

    private var language: Char = 'B'
    private lateinit var languageA: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        homeFragmentBinding = FragmentHomeBinding.inflate(inflater, container, false)

        //set up language selection spinners
        setupSpinners()

        //get the UI elements via the binding mechanism
        getUIElements()

        //auto scroll to bottom of conversation scrolable area
        val scrollView =  homeFragmentBinding.conversationBackground

        //get speech data and put into the edit text box
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            //if data is available, display data in edit text view
            if (result!!.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechData =
                    result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            //TODO THIS IS BAD AND DISGUSTING - MUST CHANGE OR CHECK
                            as ArrayList<Editable>
                inputText.text = speechData[0]
            }
        }

        //find and configure conversation recycler view
        messageList = ArrayList()
        conversationAdapter = ConversationAdapter(requireContext(), messageList)
        messageRecyclerView.adapter = conversationAdapter
        //val conversation = homeFragmentBinding.conversationRecyclerView
        val conversationLayoutManager = LinearLayoutManager(context)
        //conversationLayoutManager.orientation = LinearLayoutManager.VERTICAL
        messageRecyclerView.layoutManager = conversationLayoutManager



        stringToTranslate = inputText.text.toString()

        swapButton.setOnClickListener{
            //swap text in spinners
            //get source language and put in temp target
            val tempTargetLanguage = sourceSpinner.selectedItemPosition
            //get target language and put in source
            sourceSpinner.setSelection(targetSpinner.selectedItemPosition)
            //put temp target in target
            targetSpinner.setSelection(tempTargetLanguage)

            sourceLanguage = sourceSpinner.selectedItem.toString().uppercase()
            targetLanguage = targetSpinner.selectedItem.toString().uppercase()
        }

        translateButton.setOnClickListener{
            //create translator object with configurations for source and target languages
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build()

            translator = Translation.getClient(options)
           // lifecycle.addObserver(translator)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            //TODO edit to use recycler view
           // translatedTextView.text = "... translating ..."
            Toast.makeText(activity, "... translating...", Toast.LENGTH_SHORT).show()

            if (!isEmpty(inputText.text)){

                //val message: Message? = null
                //message!!.originalMessage = inputText.text.toString()

                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.i("TAG", "Downloaded model successfully")
                        Toast.makeText(activity, "DEBUG: Downloaded model successfully", Toast.LENGTH_SHORT).show()
                    }

                    .addOnFailureListener {
                        Log.e("TAG", "Model could not be downloaded")
                        Toast.makeText(activity, "DEBUG: Model could not be downloaded", Toast.LENGTH_SHORT).show()
                    }

                    translator.translate(homeFragmentBinding.textBox.text.toString())
                    .addOnSuccessListener { translatedText ->
                        //TODO edit to use recycler view
                       // translatedTextView.text = translatedText
                        Log.i("TAG", "Translation is " + translatedText as String)

                        //check if first item / if language a or b
                        when {
                            conversationAdapter.itemCount == 0 -> {
                                languageA = sourceLanguage
                                language = 'A'
                            }
                            (conversationAdapter.itemCount > 0) && (sourceLanguage != languageA) -> {
                                language = 'B'
                            }
                            else -> {
                                language = 'A'
                            }
                        }

                        val originalMessage = inputText.text.toString()
                        val messageObject = Message(originalMessage, translatedText, language)
                        messageList.add(messageObject)
                        conversationAdapter.notifyDataSetChanged()

                        scrollView.post {
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }

                    .addOnFailureListener {
                        Log.e("TAG", "Translation failed")
                    }

            } else{
                //TODO edit to use recycler view
                //translatedTextView.text = "no text to translate"
                Toast.makeText(activity, "No text to translate", Toast.LENGTH_SHORT).show()
            }
        }

        micFAB.setOnClickListener{
            speak(homeFragmentBinding.recordVoice)
        }


        return homeFragmentBinding.root
    }

    private fun getUIElements() {
        //TODO edit to use recycler view
       // originalTextView = homeFragmentBinding
        //TODO edit to use recycler view
       // translatedTextView = homeFragmentBinding.
        messageRecyclerView = homeFragmentBinding.conversationRecyclerView
        inputText = homeFragmentBinding.textBox
        translateButton = homeFragmentBinding.translateButton
        micFAB = homeFragmentBinding.recordVoice
        sourceSpinner = homeFragmentBinding.sourceLanguageSpinner
        targetSpinner = homeFragmentBinding.targetLanguageSpinner
        swapButton = homeFragmentBinding.swapLanguages
    }


    private fun setupSpinners(){
        setupSpinner(view,
            homeFragmentBinding.sourceLanguageSpinner,
            R.array.sourceLanguages)

        setupSpinner(view,
            homeFragmentBinding.targetLanguageSpinner,
            R.array.targetLanguages)
    }

    private fun setupSpinner(view: View?, spinner: Spinner, arrayResourceId: Int){
        //default value first item in string array
        spinner.setSelection(1)

        //use predefined layout for spinner
        val adapter =
            ArrayAdapter.createFromResource(
                requireContext(),
                arrayResourceId,
                android.R.layout.simple_spinner_item
            )

        //use predefined layout for spinner dropdown list
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        //assign adapter to spinner
        spinner.adapter = adapter

        //behaviour when an item is selected
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(spinner == sourceSpinner) {
                    //get language selected in the spinners
                    sourceLanguage = sourceSpinner.selectedItem.toString().uppercase()
                    sourceLanguageCode = setLanguageCode(sourceSpinner)
                   // setLanguageCode(sourceLanguageCode, sourceSpinner)
                } else if (spinner == targetSpinner){
                    targetLanguageCode = setLanguageCode(targetSpinner)
                    targetLanguage = targetSpinner.selectedItem.toString().uppercase()
                   // setLanguageCode(targetLanguageCode, targetSpinner)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
    }

    private fun setLanguageCode(spinner: Spinner): String{
        //val languageCode: String =
        return when (spinner.selectedItem.toString()){
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
       // return languageCode
    }

    private fun speak(view: View) {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        //message to see in dialogue box when clicking speech button and waiting for speech input
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "... listening ...")

        try {
            activityResultLauncher.launch(speechRecognizerIntent)
            // Toast.makeText(this, "Speech recognition available", Toast.LENGTH_SHORT).show()
        } catch (exp: ActivityNotFoundException) {

        }

    }


}
