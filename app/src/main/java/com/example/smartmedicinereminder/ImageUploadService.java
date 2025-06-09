package com.example.smartmedicinereminder;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class ImageUploadService {
    private static final String TAG = "ImageUploadService";

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    public static void uploadImage(String localFilePath, UploadCallback callback) {
        Log.d(TAG, "Starting image upload: " + localFilePath);

        if (localFilePath == null || localFilePath.isEmpty()) {
            Log.e(TAG, "Image path is empty");
            callback.onError("Image path is empty");
            return;
        }

        File file = new File(localFilePath);
        if (!file.exists()) {
            Log.e(TAG, "Image file does not exist: " + localFilePath);
            callback.onError("Image file does not exist");
            return;
        }

        Log.d(TAG, "Image file size: " + file.length() + " bytes");
        new UploadImageTask(file, callback).execute();
    }

    public static void deleteImage(String imageUrl, UploadCallback callback) {
        Log.d(TAG, "Starting image deletion: " + imageUrl);

        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onError("Image URL is empty");
            return;
        }

        String fileName = extractFileNameFromUrl(imageUrl);
        new DeleteImageTask(fileName, callback).execute();
    }

    private static String extractFileNameFromUrl(String url) {
        try {
            if (url.contains("/storage/v1/object/public/" + SupabaseConfig.BUCKET_NAME + "/")) {
                String[] parts = url.split("/storage/v1/object/public/" + SupabaseConfig.BUCKET_NAME + "/");
                if (parts.length > 1) {
                    String fileName = parts[1];
                    Log.d(TAG, "Extracted file name from URL: " + fileName);
                    return fileName;
                }
            }

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            Log.d(TAG, "Backup method extracted file name: " + fileName);
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract file name: " + url, e);
            return url.substring(url.lastIndexOf("/") + 1);
        }
    }

    private static class UploadImageTask extends AsyncTask<Void, Void, String> {
        private File file;
        private UploadCallback callback;
        private String error;

        public UploadImageTask(File file, UploadCallback callback) {
            this.file = file;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String fileName = "prescription_" + UUID.randomUUID().toString() + ".jpg";
                String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;

                Log.d(TAG, "Upload URL: " + uploadUrl);
                Log.d(TAG, "File name: " + fileName);

                URL url = new URL(uploadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setRequestProperty("x-upsert", "true");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                Log.d(TAG, "Preparing to read file...");
                byte[] fileBytes = readFileToBytes(file);
                Log.d(TAG, "File reading completed, size: " + fileBytes.length + " bytes");

                Log.d(TAG, "Starting upload...");
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(fileBytes);
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;
                    Log.d(TAG, "Image upload successful: " + publicUrl);
                    return publicUrl;
                } else {
                    String errorResponse = readErrorResponse(connection);
                    Log.e(TAG, "Upload failed, response code: " + responseCode + ", error: " + errorResponse);
                    error = "Upload failed, response code: " + responseCode + ", error: " + errorResponse;
                }
            } catch (Exception e) {
                Log.e(TAG, "Image upload exception", e);
                error = "Upload failed: " + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d(TAG, "Upload success callback");
                callback.onSuccess(result);
            } else {
                Log.e(TAG, "Upload failed callback: " + error);
                callback.onError(error != null ? error : "Unknown error");
            }
        }
    }

    private static class DeleteImageTask extends AsyncTask<Void, Void, Boolean> {
        private String fileName;
        private UploadCallback callback;
        private String error;

        public DeleteImageTask(String fileName, UploadCallback callback) {
            this.fileName = fileName;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String deleteUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + SupabaseConfig.BUCKET_NAME + "/" + fileName;
                Log.d(TAG, "Delete URL: " + deleteUrl);
                Log.d(TAG, "File to delete: " + fileName);

                URL url = new URL(deleteUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Delete response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK ||
                        responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                        responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.d(TAG, "Image deletion successful: " + fileName);
                    return true;
                } else {
                    String errorResponse = readErrorResponse(connection);
                    error = "Deletion failed, response code: " + responseCode + ", error: " + errorResponse;
                    Log.e(TAG, error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Image deletion exception: " + fileName, e);
                error = "Deletion failed: " + e.getMessage();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                callback.onSuccess("Deletion successful");
            } else {
                callback.onError(error != null ? error : "Unknown error");
            }
        }
    }

    private static byte[] readFileToBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        int totalRead = 0;
        int bytesRead;

        while (totalRead < bytes.length) {
            bytesRead = fis.read(bytes, totalRead, bytes.length - totalRead);
            if (bytesRead == -1) break;
            totalRead += bytesRead;
        }

        fis.close();
        Log.d(TAG, "Actual bytes read: " + totalRead);
        return bytes;
    }

    private static String readErrorResponse(HttpURLConnection connection) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read error response", e);
        }
        return "Unable to read error response";
    }
}