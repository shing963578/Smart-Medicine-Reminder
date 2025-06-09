package com.example.smartmedicinereminder;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIService {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = "sk-1dfb5873aa8444149a1abf5a5dcc1f74";

    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void askQuestion(String question, AICallback callback) {
        new AITask(callback).execute(question);
    }

    private static class AITask extends AsyncTask<String, Void, String> {
        private AICallback callback;
        private String errorMessage;

        public AITask(AICallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... params) {
            String question = params[0];

            try {
                String medicineContext = "You are a professional medication consultation assistant, specializing in providing medication-related advice and information for elderly people. Please answer in English, keeping the language simple and easy to understand for elderly users. Provide accurate and safe medication information, but also remind users to consult professional healthcare providers.";
                String fullPrompt = medicineContext + "\n\nUser question: " + question;

                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "deepseek-chat");

                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", fullPrompt);
                messages.put(message);

                requestBody.put("messages", messages);
                requestBody.put("max_tokens", 500);
                requestBody.put("temperature", 0.7);

                OutputStream os = connection.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject responseJson = new JSONObject(response.toString());
                    String content = responseJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    return content.trim();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorMessage = "API request failed, status code: " + responseCode + ", error: " + errorResponse.toString();
                    return null;
                }

            } catch (IOException | JSONException e) {
                errorMessage = "Network connection error: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                if (errorMessage == null) {
                    errorMessage = "Sorry, I can't connect to the server right now. Please check your network connection and try again later.\n\nAs your medication assistant, I recommend:\n\n1. If you have urgent medication questions, please contact your doctor immediately\n2. Take medications on time, don't stop taking them arbitrarily\n3. If you experience medication side effects, please consult professional healthcare providers\n4. Store medications in a cool, dry place\n5. Regularly check medication expiration dates";
                }
                callback.onError(errorMessage);
            }
        }
    }
}