package com.nelsonpantaleon.blogreader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainListActivity extends ListActivity {

    public static final int NUMBER_OF_POSTS = 20;
    public static final String TAG = MainListActivity.class.getSimpleName();
    protected JSONObject mBlogData;
    protected ProgressBar mProgressBar;

    private final String KEY_TITLE = "title";
    private final String KEY_AUTHOR = "author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        if (isNetworkAvailable()) {
            // show progressbar
            mProgressBar.setVisibility(View.VISIBLE);
            // start working
            GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
            getBlogPostsTask.execute();
        } else {
            Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Get the clicked list item as a JSONObject and retrive its URL
        JSONArray jsonPosts = null;
        try {
            jsonPosts = mBlogData.getJSONArray("posts");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String blogUrl = jsonPost.getString("url");

            // open the blog post URL using an explicit intent
            Intent intent = new Intent(this, BlogWebViewActivity.class);
            intent.setData(Uri.parse(blogUrl));
            startActivity(intent);
        } catch (JSONException e) {
            Log.e(TAG, "Exception caught (JSONException):" + e);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void handleBlogResponse() {
        // hide progress bar
        mProgressBar.setVisibility(View.INVISIBLE);

        if (mBlogData == null) {
            updateDisplayForError();
        } else {
            try {
                // Get the JSONArray of posts from the JSONObject
                JSONArray jsonPosts = mBlogData.getJSONArray("posts");
                // Create an arraylist of hashmaps in which to store posts in
                ArrayList<HashMap<String, String>> blogPosts = new ArrayList<>();
                // show every post
                for (int i = 0; i < jsonPosts.length(); i++) {
                    JSONObject jsonPost = jsonPosts.getJSONObject(i);
                    // get post title
                    String title = jsonPost.getString(KEY_TITLE);
                    title = Html.fromHtml(title).toString();
                    // get post author
                    String author = jsonPost.getString(KEY_AUTHOR);
                    author = Html.fromHtml(author).toString();

                    // Save data as a hashmap (The post)
                    HashMap<String,String> blogPost = new HashMap<>();
                    blogPost.put(KEY_TITLE, title);
                    blogPost.put(KEY_AUTHOR, author);
                    // add it to the ArrayList (List of Posts)
                    blogPosts.add(blogPost);

                }
                // setup adapter
                String[] keys = { KEY_TITLE, KEY_AUTHOR }; // the column names on the arraylist
                int[] ids = { android.R.id.text1, android.R.id.text2 };  // the TextViews to display the data in
                SimpleAdapter adapter = new SimpleAdapter(this, blogPosts,
                        android.R.layout.simple_list_item_2, keys, ids);
                setListAdapter(adapter);

            } catch (JSONException e) {
                Log.e(TAG, "Exception caught(JSON): " + e);
            }
        }
    }

    private void updateDisplayForError() {
        // setup dialog properties using a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error_title))
               .setMessage(getString(R.string.error_message));
        builder.setPositiveButton(android.R.string.ok, null); // null -> no event listener when clicked

        // Build dialog using builder and display it
        AlertDialog dialog = builder.create();
        dialog.show();

        // Get empty textview associated with the list view
        TextView emptyTextView = (TextView) getListView().getEmptyView();
        emptyTextView.setText(getString(R.string.no_items));
    }

    private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject> {

        int responseCode = -1;
        JSONObject jsonResponse = null;

        @Override
        protected JSONObject doInBackground(Object... object0) {
            try {
                URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
                HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
                connection.connect();

                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {// code == 200 OK
                    // Get ready to read data
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);

                    // Read the data and save it in a String variable (responseData)
                    int nextCharacter; // read() returns an int, we cast it to char later
                    String responseData = "";

                    while (true) { // loops until there are no more characters to read
                        nextCharacter = reader.read(); // read() without parameters returns one character
                        if (nextCharacter == -1) // A return value of -1 means that we reached the end
                            break;
                        responseData += (char) nextCharacter; // Append the character to the end of the string
                    }

                    // Parse JSON
                    jsonResponse = new JSONObject(responseData);

                } else {
                    Log.i(TAG, "Unsuccessful HTTP Response Code " + responseCode);
                }
                Log.i(TAG, "Code: " + responseCode);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Exception1 caught: ", e);
            } catch (IOException e) {
                Log.e(TAG, "Exception2 caught: ", e);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException: ", e);
            }
            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            mBlogData = result;
            handleBlogResponse();
        }
    }
}
