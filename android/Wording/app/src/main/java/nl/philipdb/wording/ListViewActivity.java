/*
 * Wording is a project by PhiliPdB
 *
 * Copyright (c) 2015.
 */

package nl.philipdb.wording;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;

public class ListViewActivity extends AppCompatActivity {

    private GetListTask mGetListTask = null;

    private List mList;

    private ProgressBar mProgressBar;
    private LinearLayout mLinearLayout;

    public int askedLanguage = 1;
    public boolean caseSensitive = true;
    public boolean cancelled = false;
    protected final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressBar = (ProgressBar) findViewById(R.id.get_list_progress);
        mLinearLayout = (LinearLayout) findViewById(R.id.list_view_layout);

        // Load List from Intent
        mList = (List) getIntent().getSerializableExtra("list");
        if (MainActivity.isNetworkAvailable(this)) getList();
        else {
            // Try to read from cache
            try {
                mList = CacheHandler.readList(this, mList.mName);
            } catch (IOException e) {
                Log.d("Cache", "Something went wrong with the IO: " + e);
            } catch (JSONException e) {
                Log.d("Cache", "Something went wrong with the JSON: " + e);
            }
            if (mList.getTotalWords() == 0) {
                Snackbar.make(mLinearLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();
                finish(); // No words data
            }
            else setWordsTable();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_practice) {
            // Create custom AlertDialog
            View view = getLayoutInflater().inflate(R.layout.content_practice_options, null);
            ((TextView) view.findViewById(R.id.ask_language_1)).setText(List.getLanguageName(this, mList.mLanguage1));
            ((TextView) view.findViewById(R.id.ask_language_2)).setText(List.getLanguageName(this, mList.mLanguage2));
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog).setTitle(getString(R.string.practice_options))
                    .setCancelable(true).setView(view);
            // Set option buttons
            final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group_asked_language);
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.case_sensitive_check_box);
            // Setup start and cancel buttons
            builder.setPositiveButton(R.string.start_practice, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Get user inputs
                    switch (radioGroup.getCheckedRadioButtonId()) {
                        case R.id.ask_language_1:
                            askedLanguage = 1;
                            break;
                        case R.id.ask_language_2:
                            askedLanguage = 2;
                            break;
                        case R.id.ask_both:
                            askedLanguage = 0;
                            break;
                    }
                    caseSensitive = checkBox.isChecked();

                    // Create and launch new intent
                    Intent newIntent = new Intent(mContext, PracticeActivity.class);
                    newIntent.putExtra("list", mList);
                    newIntent.putExtra("askedLanguage", askedLanguage);
                    newIntent.putExtra("caseSensitive", caseSensitive);
                    startActivity(newIntent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelled = true;
                    dialog.cancel();
                }
            });
            // Create and show dialog
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            return !cancelled;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setWordsTable() {
        // Set title and languages
        getSupportActionBar().setTitle(mList.mName);
        ((TextView) findViewById(R.id.language_1)).setText(List.getLanguageName(this, mList.mLanguage1));
        ((TextView) findViewById(R.id.language_2)).setText(List.getLanguageName(this, mList.mLanguage2));

        // Set words
        TableLayout table = (TableLayout) findViewById(R.id.word_table);
        for (int i = 0; i < mList.getTotalWords(); i++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            tableRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView word1 = new TextView(this);
            word1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            word1.setText(mList.mLanguage1Words.get(i));
            tableRow.addView(word1);

            TextView word2 = new TextView(this);
            word2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            word2.setText(mList.mLanguage2Words.get(i));
            tableRow.addView(word2);

            table.addView(tableRow);
        }
    }

    private void getList() {
        if (mGetListTask != null) {
            return;
        }

        showProgress(true);
        mGetListTask = new GetListTask(mList.mName, MainActivity.username);
        mGetListTask.execute((Void) null);
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLinearLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        mLinearLayout.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLinearLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public class GetListTask extends NetworkCaller {

        private final String mListName;
        private final String mUsername;

        GetListTask(String listName, String username) {
            mListName = listName;
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mList = getList(mUsername, mListName);
                return mList != null;
            } catch (IOException e) {
                Log.d("IOException", "Something bad happened on the IO");
            } catch (JSONException e) {
                Log.d("JSONException", "The JSON fails");
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mGetListTask = null;
            showProgress(false);

            if (success) {
                setWordsTable();
                // Write list to cache
                try {
                    CacheHandler.writeList(mContext, mList);
                } catch (IOException e) {
                    Log.d("IO", "Something bad with the IO: " + e);
                } catch (JSONException e) {
                    Log.d("JSON", "Something bad with the JSON: " + e);
                }
            } else {
                // Try to read from cache
                try {
                    mList = CacheHandler.readList(mContext, mList.mName);
                } catch (IOException e) {
                    Log.d("Cache", "Something went wrong with the IO: " + e);
                } catch (JSONException e) {
                    Log.d("Cache", "Something went wrong with the JSON: " + e);
                }
                if (mList.getTotalWords() == 0) {
                    Snackbar.make(mLinearLayout, getString(R.string.error_no_connection), Snackbar.LENGTH_LONG).show();
                    finish(); // No words data
                }
                else setWordsTable();
            }
        }

        @Override
        protected void onCancelled() {
            mGetListTask = null;
            showProgress(false);
        }

    }

}
