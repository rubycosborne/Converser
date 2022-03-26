package uk.ac.aber.dcs.rco1.converser.view.home

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
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import uk.ac.aber.dcs.rco1.converser.R
import uk.ac.aber.dcs.rco1.converser.data.ConverserRepository
import uk.ac.aber.dcs.rco1.converser.databinding.FragmentHomeBinding
import uk.ac.aber.dcs.rco1.converser.model.home.PositionInConversation
import uk.ac.aber.dcs.rco1.converser.model.home.TranslationItem
import kotlin.collections.ArrayList

/**
 * TODO
 *
 */
class HomeFragment : Fragment(){

    private lateinit var homeFragmentBinding: FragmentHomeBinding

    //used instead of the depreciated startActivityForResult in speak method
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    //grab UI elements
    private lateinit var translateButton: ImageButton
    private lateinit var micFAB: ImageButton
    private lateinit var swapButton: ImageButton
    private lateinit var translationItemRecyclerView: RecyclerView
    private lateinit var inputText: EditText
    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner

    //adapter for the conversation recycler view
    private lateinit var conversationAdapter: ConversationAdapter
    //list of translations in a conversation
    private lateinit var translationItemList : ArrayList<TranslationItem>

    private var sourceLanguage: String = ""
    private var targetLanguage: String = ""
    private var stringToTranslate: String = ""

    //initialise positionInConversation codes to English by default
    var sourceLanguageCode = TranslateLanguage.ENGLISH
    var targetLanguageCode = TranslateLanguage.ENGLISH

    private lateinit var translator: Translator

    //used for identifying which positionInConversation is being translated
    private var positionInConversation: PositionInConversation = PositionInConversation.SECOND
    private lateinit var languageA: String
    private lateinit var languageB: String

    private val languageModelManager: RemoteModelManager = RemoteModelManager.getInstance()
    lateinit var downloadedModels: List<String>

    //TODO: change later as dont want in home fragment
    private lateinit var repository: ConverserRepository

    /**
     * TODO
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        homeFragmentBinding = FragmentHomeBinding.inflate(inflater, container, false)

        //set up positionInConversation selection spinners
        setupSpinners()

        //get the UI elements via the binding mechanism
        getUIElements()

        //get speech data and put into the edit text box
        displaySpeechToText()

        //find and configure conversation recycler view
        translationItemList = ArrayList()
        conversationAdapter = ConversationAdapter(requireContext(), translationItemList)
        translationItemRecyclerView.adapter = conversationAdapter
        //val conversation = homeFragmentBinding.conversationRecyclerView
        val conversationLayoutManager = LinearLayoutManager(context)
        //conversationLayoutManager.orientation = LinearLayoutManager.VERTICAL
        translationItemRecyclerView.layoutManager = conversationLayoutManager

        stringToTranslate = inputText.text.toString()

        //set up on click events
        setListenerForLanguageSwap()
        setListenerForTranslation()
        setListenerForSpeechRecording()

        repository = ConverserRepository(requireActivity().application)

        return homeFragmentBinding.root
    }

    private fun setListenerForSpeechRecording() {
        micFAB.setOnClickListener {
            speak(homeFragmentBinding.recordVoice)
        }
    }

    //get model for a positionInConversation
    private fun getLanguageModel(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }

    // download a positionInConversation model
    private fun downloadLanguage(languageCode: String) {
        val model = getLanguageModel(languageCode)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        languageModelManager.download(model, conditions)
    }

    // update list of downloaded models
    private fun getDownloadedModels() {
        languageModelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener {
                    models ->
                //TODO: sort alphabetically
                downloadedModels = models.map { it.language}
            }
    }

    // delete a model and update downloaded models
    private fun deleteLanguage(languageCode: String) {
        val model = getLanguageModel(languageCode)
        languageModelManager.deleteDownloadedModel(model)
            .addOnCompleteListener {
                //getDownloadedModels()
            }
    }

    private fun setListenerForTranslation() {
        translateButton.setOnClickListener {
            ////////////////////////////////////////////


            //create translator object with configurations for source and target languages
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build()

            //create a translator object with the target and source languages set
            translator = Translation.getClient(options)
            lifecycle.addObserver(translator)

            /*//set up conditions for downloading positionInConversation models
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()*/

            Toast.makeText(activity, "DEBUG: checking positionInConversation models", Toast.LENGTH_SHORT).show()

            //if text input is not empty
            if (!isEmpty(inputText.text)) {

                //download the positionInConversation models if they are not already downloaded
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        Log.i("TAG", "Downloaded model successfully")
                        Toast.makeText(
                            activity,
                            "DEBUG: Downloaded model successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Log.e("TAG", "Model could not be downloaded")
                        /*Toast.makeText(
                            activity,
                            "DEBUG: Model could not be downloaded",
                            Toast.LENGTH_SHORT
                        ).show()*/
                    }
                    //put translation in here as it works if positionInConversation model needs downloading
                    .continueWith { task ->

                        if (task.isSuccessful){
                            Toast.makeText(activity, "DEBUG: translating", Toast.LENGTH_SHORT).show()
                            //translate the input text using the translator model that was just created
                            translator.translate(homeFragmentBinding.textBox.text.toString())
                                .addOnSuccessListener { translatedText ->
                                    Log.i("TAG", "Translation is " + translatedText as String)

                                    //check if positionInConversation A (first item translated) or B
                                    when {
                                        conversationAdapter.itemCount == 0 -> {
                                            languageA = sourceLanguage
                                            languageB = targetLanguage
                                            positionInConversation = PositionInConversation.FIRST
                                        }
                                        (conversationAdapter.itemCount > 0) && (sourceLanguage == languageA)
                                                && (targetLanguage == languageB) -> positionInConversation = PositionInConversation.FIRST
                                        (conversationAdapter.itemCount > 0) && (sourceLanguage == languageB)
                                                && (targetLanguage == languageA) -> positionInConversation = PositionInConversation.SECOND
                                        else -> {
                                            //set new positionInConversation
                                            languageA = sourceLanguage
                                            languageB = targetLanguage
                                            positionInConversation = PositionInConversation.FIRST
                                            restartConversation()
                                        }
                                    }

                                    //add a message to the message list (original and translated)
                                    val originalTranslationItem = inputText.text.toString()
                                    val translationItemObject = TranslationItem(0, originalTranslationItem, translatedText, positionInConversation)
                                    translationItemList.add(translationItemObject)
                                    repository.insert(translationItemObject)

                                    //tell the adapter that a message has been added, so it can update the UI
                                    //TODO: use different mechanism to notify change
                                    conversationAdapter.notifyDataSetChanged()

                                    //autoscroll to bottom of message list
                                    translationItemRecyclerView.smoothScrollToPosition(translationItemList.size - 1)

                                    inputText.text.clear()

                                }
                                .addOnFailureListener {
                                    Log.e("TAG", "Translation failed")
                                }
                        }
                    }


            }
            // no input text to translate
            else {
                Toast.makeText(activity, "No text to translate", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets a listener when the swap button is pressed
     * Swaps the source and target languages around
     */
    private fun setListenerForLanguageSwap() {
        swapButton.setOnClickListener {
            //swap text in spinners
            //get source positionInConversation and put in temp target
            val tempTargetLanguage = sourceSpinner.selectedItemPosition
            //get target positionInConversation and put in source
            sourceSpinner.setSelection(targetSpinner.selectedItemPosition)
            //put temp target in target
            targetSpinner.setSelection(tempTargetLanguage)

            sourceLanguage = sourceSpinner.selectedItem.toString().uppercase()
            targetLanguage = targetSpinner.selectedItem.toString().uppercase()

        }
    }

    /**
     * Uses Intent mechanism to update the input text field with speech data when it is provided
     *
     */
    private fun displaySpeechToText() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            //if data is available, display data in edit text view
            if (result!!.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechData =
                    result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            //TODO: check the cast or use alternative mechanism
                            as ArrayList<Editable>
                inputText.text = speechData[0]
            }
        }
    }

    /**
     * Initialise the UI elements using the binding mechanism
     *
     */
    private fun getUIElements() {
        translationItemRecyclerView = homeFragmentBinding.conversationRecyclerView
        inputText = homeFragmentBinding.textBox
        translateButton = homeFragmentBinding.translateButton
        micFAB = homeFragmentBinding.recordVoice
        sourceSpinner = homeFragmentBinding.sourceLanguageSpinner
        targetSpinner = homeFragmentBinding.targetLanguageSpinner
        swapButton = homeFragmentBinding.swapLanguages
    }

    /**
     * Set up the source and target positionInConversation spinners using appropriate resources
     *
     */
    private fun setupSpinners(){
        setupSpinner(view,
            homeFragmentBinding.sourceLanguageSpinner,
            R.array.sourceLanguages)

        setupSpinner(view,
            homeFragmentBinding.targetLanguageSpinner,
            R.array.targetLanguages)
    }

    /**
     * Set up an individual spinner to provide a selection of languages
     *
     * @param view - the current view
     * @param spinner - The spinner being set up (source or target positionInConversation lists)
     * @param arrayResourceId - The array of languages stored as a strings array value
     */
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


        //define behaviour when an item is selected
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(spinner == sourceSpinner) {
                    //get positionInConversation selected in the spinners
                    sourceLanguage = sourceSpinner.selectedItem.toString().uppercase()
                    sourceLanguageCode = setLanguageCode(sourceSpinner)
                } else if (spinner == targetSpinner){
                    targetLanguageCode = setLanguageCode(targetSpinner)
                    targetLanguage = targetSpinner.selectedItem.toString().uppercase()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
    }

    /**
     * Set the positionInConversation code for a positionInConversation using the API positionInConversation code retrieval mechanism
     *
     * @param spinner - The source or target positionInConversation spinner where the positionInConversation has been selected
     * @return the positionInConversation code the positionInConversation e.g. en for English
     */
    private fun setLanguageCode(spinner: Spinner): String{
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
    }

    /**
     * Use speech recognizer intent mechanism to get speech input from the user
     * An alternative would be to use Speech Recognizer
     *
     * @param view
     */
    private fun speak(view: View) {
        //Starts an activity that will prompt the user for speech and send it through a speech recognizer.
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            //Informs the recognizer which speech model to prefer when performing
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            //Use a positionInConversation model based on free-form speech recognition.
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        //This tag informs the recognizer to perform speech recognition in a positionInConversation
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguageCode)
        //message to see in dialogue box when clicking speech button and waiting for speech input
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak now")

        try {
            //launch activity results mechanism to perform an action using intent
            activityResultLauncher.launch(speechRecognizerIntent)
            // Toast.makeText(this, "Speech recognition available", Toast.LENGTH_SHORT).show()
        } catch (exp: ActivityNotFoundException) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        repository = ConverserRepository(requireActivity().application)
        repository.deleteAll()
        super.onCreate(savedInstanceState)
    }

//TODO: fix so that conversation is deleted before closing app instead of whe you open it
    override fun onDestroy() {
        restartConversation()
        super.onDestroy()
    }

    private fun restartConversation(){
        translationItemList.clear()
        repository.deleteAll()
        translator.close()
        //TODO: move somewhere else and check if not in downloaded list
        deleteLanguage(sourceLanguage)
        deleteLanguage(targetLanguage)
        conversationAdapter.notifyDataSetChanged()
    }


}
