package com.example.smartmedicinereminder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public class AIAssistantActivity extends AppCompatActivity {
    private EditText etQuestion;
    private Button btnSendQuestion;
    private RecyclerView recyclerViewChat;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private AIService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        aiService = new AIService();
        addWelcomeMessage();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        etQuestion = findViewById(R.id.etQuestion);
        btnSendQuestion = findViewById(R.id.btnSendQuestion);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnSendQuestion.setOnClickListener(v -> sendQuestion());
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
                "Hello! I'm your smart medication assistant. You can ask me any questions about medications, such as:\n\n" +
                        "• Medication side effects\n" +
                        "• Medication precautions\n" +
                        "• Drug interactions\n" +
                        "• Medication timing recommendations\n\n" +
                        "Please enter your question and I'll do my best to help you.",
                false
        );
        chatMessages.add(welcomeMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }

    private void sendQuestion() {
        String question = etQuestion.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage userMessage = new ChatMessage(question, true);
        chatMessages.add(userMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerViewChat.scrollToPosition(chatMessages.size() - 1);

        etQuestion.setText("");
        showLoading(true);

        aiService.askQuestion(question, new AIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    ChatMessage aiMessage = new ChatMessage(response, false);
                    chatMessages.add(aiMessage);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    ChatMessage errorMessage = new ChatMessage(
                            "Sorry, I can't answer your question right now. Please try again later.\nError: " + error,
                            false
                    );
                    chatMessages.add(errorMessage);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendQuestion.setEnabled(!show);
    }
}