package com.samcodeman

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var tokenizer: BpeTokenizer
    private lateinit var onnxModel: OnnxModel
    private lateinit var markwon: Markwon
    private val inferenceScope = CoroutineScope(Dispatchers.IO)
    private var inferenceJob: Job? = null

    private val END_TOKEN_IDS = setOf(151643, 151645)
    private val skipTokenIdsQwen3 = setOf(151667, 151668)

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear -> {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.DarkAlertDialog))
                    .setTitle("Clear Chat?")
                    .setMessage("This will remove all messages.")
                    .setPositiveButton("Yes") { _, _ ->
                        messages.clear()
                        chatAdapter.clearAll()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_main)

        markwon = Markwon.create(this)
        tokenizer = BpeTokenizer(this)

        val thinkingToggle: CheckBox = findViewById(R.id.thinkingToggle)
        val inputEditText: EditText = findViewById(R.id.userInput)
        
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        inputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                chatRecyclerView.postDelayed({
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }, 300)
            }
        }

        val sendButton = findViewById<android.widget.ImageButton>(R.id.sendButton)
        val stopButton = findViewById<android.widget.ImageButton>(R.id.stopButton)

        chatRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                chatRecyclerView.post {
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }

        fun toggleGenerating(isGenerating: Boolean) {
            if (isGenerating) {
                sendButton.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
                stopButton.bringToFront()
                stopButton.requestLayout()
            } else {
                stopButton.visibility = View.GONE
                sendButton.visibility = View.VISIBLE
                sendButton.bringToFront()
                sendButton.requestLayout()
            }
        }

        // === Define token IDs for prompt formatting ===
        val roleTokens = RoleTokenIds(
            systemStart = listOf(tokenizer.getTokenId("<|im_start|>"), tokenizer.getTokenId("system"), tokenizer.getTokenId("Ċ")),
            userStart = listOf(tokenizer.getTokenId("<|im_start|>"), tokenizer.getTokenId("user"), tokenizer.getTokenId("Ċ")),
            assistantStart = listOf(tokenizer.getTokenId("<|im_start|>"), tokenizer.getTokenId("assistant"), tokenizer.getTokenId("Ċ")),
            endToken = tokenizer.getTokenId("<|im_end|>")
        )

        // === Model configurations ===
        val modelconfigqwen25 = ModelConfig(
            modelName = "Qwen2_5",
            promptStyle = PromptStyle.QWEN2_5,
            modelPath = "model.onnx",
            eosTokenIds = END_TOKEN_IDS,
            numLayers = 24,
            numKvHeads = 2,
            headDim = 64,
            batchSize = 1,
            defaultSystemPrompt = "You are Qwen, created by Alibaba Cloud. You are a helpful assistant.",
            roleTokenIds = roleTokens,
            scalarPosId = false
        )

        val modelconfigqwen3 = ModelConfig(
            modelName = "Qwen3",
            promptStyle = PromptStyle.QWEN3,
            modelPath = "model.onnx",
            eosTokenIds = END_TOKEN_IDS,
            numLayers = 28,
            numKvHeads = 8,
            headDim = 128,
            batchSize = 1,
            defaultSystemPrompt = "You are PrometheusAI, an advanced offline Local LLM developed by SamCodeMan. Your purpose is to provide secure, private, and intelligent assistance entirely on-device. You do not rely on internet connectivity, and no user data is ever sent to external servers or used for training.",
            roleTokenIds = roleTokens,
            scalarPosId = true,
            dtype = "float32",
            IsThinkingModeAvailable = true
        )

        // ---------------------------------------------------------------------
        // SELECT WHICH MODEL TO RUN
        // ---------------------------------------------------------------------
        val config = modelconfigqwen3

        // Show thinking toggle if available
        if (config.IsThinkingModeAvailable) {
            thinkingToggle.visibility = View.VISIBLE
            thinkingToggle.isChecked = true // Default to true
        } else {
            thinkingToggle.visibility = View.GONE
        }

        // Remove default title bar since we have a custom one
        supportActionBar?.hide()

        val promptBuilder = PromptBuilder(tokenizer, config)
        val mapper = TokenDisplayMapper(this, config.modelName)

        toggleGenerating(false)
        messages.add(Message("⏳ Please wait, the model is loading.", isUser = false))
        chatAdapter.notifyItemInserted(messages.size - 1)

        inferenceScope.launch {
            onnxModel = OnnxModel(this@MainActivity, config)
            withContext(Dispatchers.Main) {
                messages.add(Message("✅ Model is ready.", isUser = false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                chatRecyclerView.scrollToPosition(messages.size - 1)
                toggleGenerating(false)
            }
        }

        sendButton.setOnClickListener {
            val userPrompt = inputEditText.text.toString().trim()
            if (userPrompt.isEmpty() || !::onnxModel.isInitialized) return@setOnClickListener

            messages.add(Message(userPrompt, isUser = true))
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.scrollToPosition(messages.size - 1)
            inputEditText.text.clear()

            // Use different system prompt based on Thinking Mode toggle (only for Qwen3)
            val systemPrompt = if (config.modelName.equals("qwen3", ignoreCase = true) && config.IsThinkingModeAvailable) {
                if (thinkingToggle.isChecked) config.defaultSystemPrompt
                else "${config.defaultSystemPrompt} /no_think"
            } else {
                config.defaultSystemPrompt
            }

            val intent = PromptIntent.QA(systemPrompt)
            
            // Limit context to last 2 interactions (4 messages) + current user message = 5 messages
            val recentMessages = if (messages.size > 5) messages.takeLast(5) else messages
            val inputIds = promptBuilder.buildPromptTokens(recentMessages, intent, maxTokens = 500)

            toggleGenerating(true)

            val botTokenIds = mutableListOf<Int>()
            val botMessage = Message("", isUser = false)
            messages.add(botMessage)
            val botIndex = messages.lastIndex
            chatAdapter.notifyItemInserted(botIndex)
            chatRecyclerView.scrollToPosition(botIndex)
            var tokenCounter = 0

            inferenceJob = inferenceScope.launch {
                try {
                    onnxModel.runInferenceStreamingWithPastKV(
                        inputIds = inputIds,
                        endTokenIds = END_TOKEN_IDS,
                        shouldStop = { inferenceJob?.isActive != true },
                        onTokenGenerated = { tokenId ->
                            // Skip first 4 tokens if Qwen3
                            if (!(config.modelName.equals("Qwen3", ignoreCase = true) && tokenCounter < 4)) {
                                botTokenIds.add(tokenId)
                                val fullText = tokenizer.decode(botTokenIds.toIntArray())

                                runOnUiThread {
                                    messages[botIndex] = botMessage.copy(text = fullText)
                                    chatAdapter.notifyItemChanged(botIndex)
                                    chatRecyclerView.scrollToPosition(botIndex)
                                }
                            }
                            tokenCounter++
                        }
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        messages.add(Message("❌ Error: ${e.message ?: "Unknown error."}", isUser = false))
                        chatAdapter.notifyItemInserted(messages.size - 1)
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        sendButton.visibility = View.VISIBLE
                        stopButton.visibility = View.GONE
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            inferenceJob?.cancel()
            messages.add(Message("⛔ Generation stopped.", isUser = false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.scrollToPosition(messages.size - 1)
            toggleGenerating(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inferenceScope.cancel()
    }
}