package com.example.akila.booklisting;

/**
 * Created by Akila on 5/21/17.
 */

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> bookList = new ArrayList<String>();
    private ListView bookListView;
    private ArrayAdapter<String> arrayAdapter;
    private EditText booksearchTF;
    private Button findBookBTN;
    private TextView instructionTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
        {

            bookList = savedInstanceState.getStringArrayList("headerList");
        }

        bookListView = (ListView) findViewById(R.id.bookListView);
        booksearchTF = (EditText) findViewById(R.id.booksearchTF);
        findBookBTN = (Button) findViewById(R.id.findBookBTN);

        instructionTV = (TextView)findViewById(R.id.instructionTV);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bookList);
        bookListView.setAdapter(arrayAdapter);

        if(!bookList.isEmpty()){
            instructionTV.setText("");
        }

        findBookBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bookList.clear();
                String searchText = booksearchTF.getText().toString();
                System.out.println("Search Text " + searchText);


                ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = manager.getActiveNetworkInfo();

                boolean isAvailable = false;
                if (networkInfo != null && networkInfo.isConnected()) {
                    isAvailable = true;
                }

                if(isAvailable){
                    DownloadTask task = new DownloadTask();
                    String urlEndPoint = "https://www.googleapis.com/books/v1/volumes?q="+searchText+"&maxResults=20";
                    urlEndPoint = urlEndPoint.replaceAll("\\s+","");
                    task.execute(urlEndPoint);
                    booksearchTF.setText(" ");
                } else{
                    Toast.makeText(getApplicationContext(), "Check your internet connection and try again.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("headerList",  bookList);
        super.onSaveInstanceState(outState);
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            String result = "";
            URL url;
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    result = readStream(in);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
            return result;
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    Integer totalItems = jsonObject.getInt("totalItems");
                    if (totalItems > 0) {
                        String books = jsonObject.getString("items");
                        JSONArray arr = new JSONArray(books);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject jsonPart = arr.getJSONObject(i);
                            JSONObject volumeInfo = jsonPart.getJSONObject("volumeInfo");
                            String title = volumeInfo.getString("title");
                            String authors = "";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorArr = volumeInfo.getJSONArray("authors");
                                for (int x = 0; x < authorArr.length(); x++) {
                                    System.out.println(authorArr.get(x));
                                    authors += authorArr.get(x) + "    ";
                                }
                            } else {
                                authors += "Author N/A";
                            }
                            System.out.println(title);
                            String book = title + " by " + authors;
                            bookList.add(book);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bookListView.setAdapter(arrayAdapter);
                if (bookList.isEmpty()) {
                    instructionTV.setText(" Try again. Type in a keyword (ie: peace) and click the 'Search' button to find results from Google Books");
                    instructionTV.setTextColor(Color.parseColor("#512DA8"));
                }else{
                    instructionTV.setText("Type in a keyword and click the 'Search' button to find results from Google Books");
                }
            }
        }
    }

    private String readStream(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
