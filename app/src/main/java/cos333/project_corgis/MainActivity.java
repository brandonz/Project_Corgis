package cos333.project_corgis;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.facebook.Profile;
import com.facebook.AccessToken;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private String body_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        Spinner gender_spinner = (Spinner) findViewById(R.id.gender_spinner);
        ArrayAdapter<CharSequence> gender_adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_choices, android.R.layout.simple_spinner_dropdown_item);
        gender_spinner.setAdapter(gender_adapter);
        gender_spinner.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sendInfo(View view) throws Exception {
        Intent intent = new Intent(this, DrinkLogActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_weight);
        String weight = editText.getText().toString();
        if (weight.isEmpty() || Integer.parseInt(weight) == 0 || Integer.parseInt(weight) > 1000) {
            AlertDialog.Builder builder  = new AlertDialog.Builder(this);

            builder.setMessage(R.string.enter_valid_weight);
            builder.setTitle(R.string.error_message);
            builder.setCancelable(true);

            builder.setPositiveButton(
                    R.string.okay,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();

            return;
        }

        // This format string is hardcoded for now because I had problems with the & symbol in the
        // strings resources file lol. TODO: put it in the resources
        String formatString = "fbid=%s&fname=%s&lname=%s&weight=%s&gender=%s";
        String id = AccessToken.getCurrentAccessToken().getUserId();
        Profile prof = Profile.getCurrentProfile();
        String firstName = prof.getFirstName();
        String lastName = prof.getLastName();
        String urlParameters = String.format(formatString, id, firstName, lastName, weight,
                toMF(body_type));
        new PostAsyncTask().execute(getResources().getString(R.string.server), urlParameters);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("weight", Integer.parseInt(weight));
        editor.putString("gender", body_type);
        editor.commit();

        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        body_type = (String) parent.getItemAtPosition(position);
    }

    private String toMF(String gender) {
        String genders[] = getResources().getStringArray(R.array.gender_choices);
        if (gender.equals(genders[0]))
            return "M";
        else
            return "F";
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //Async task for post
    private class PostAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            return RestClient.Post(url[0], url[1]);
        }
    }
}
