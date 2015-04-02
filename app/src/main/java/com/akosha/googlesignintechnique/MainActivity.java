package com.akosha.googlesignintechnique;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener {
    public static final Scope SCOPE_PLUS_WRITE_STREAM = new Scope("https://www.googleapis.com/auth/plus.stream.write");

    private static final int RC_SIGN_IN = 10;
    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 3;
    private GoogleApiClient googleApiClient;
    private boolean signInClicked;
    private ConnectionResult connectionResult;
    private boolean intentInProgress;
    private AccountManager accountManager;

    private ListView accountListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Plus.PlusOptions plusOptions = new Plus.PlusOptions.Builder()
                .addActivityTypes("http://schema.org/AddActivity", "http://schema.org/ReviewActivity",
                        "http://schema.org/MusicRecording", "http://schema.org/ListenAction")
                .build();


        // Initialize google api client to use login and moment feature of google plush.
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getAccountNames());

        setContentView(R.layout.activity_main);

        accountListView = (ListView) findViewById(R.id.account_list_view);
        accountListView.setOnItemClickListener(this);
        accountListView.setAdapter(itemsAdapter);

        if (googleApiClient != null && (!googleApiClient.isConnected() || !googleApiClient.isConnecting())) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
        }
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

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (connectionResult.hasResolution()) {
            try {
                intentInProgress = true;
                startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                intentInProgress = false;
                googleApiClient.connect();
            }
        }
    }

    private String[] getAccountNames() {
        accountManager = AccountManager.get(this);
        Account[] accounts = accountManager
                .getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];

        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
        }
        return names;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Main", "--- Connected ---");
        show("--- Connected ---");


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                String scopes = "oauth2:" + "https://www.googleapis.com/auth/plus.login" + " https://www.googleapis.com/auth/plus.me";
                String scopes = "oauth2:server:client_id:23076989397-cut5f09l9aoehn9kd4n4u6d91u6a81s8.apps.googleusercontent.com:api_scope:" + Scopes.PLUS_LOGIN + " " + Scopes.PLUS_ME;


                try {
                    String token = GoogleAuthUtil.getToken(MainActivity.this, Plus.AccountApi.getAccountName(googleApiClient), scopes);
                    Log.d(TAG, "Token: " + token);
                    Log.d(TAG, "UserID: " + Plus.PeopleApi.getCurrentPerson(googleApiClient).getId());

                    GoogleAuthUtil.invalidateToken(MainActivity.this, token);

                } catch (IOException e) {

                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    if (e instanceof UserRecoverableAuthException) {
                        Intent intent = ((UserRecoverableAuthException) e).getIntent();
                        startActivityForResult(intent,
                                REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
                    } else if (e instanceof GoogleAuthException) {
                    }

                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    private volatile boolean isAuthenticated = false;

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Main", "--- Connection failed ---");
        show("--- Connection failed ---");

        if (connectionResult.hasResolution()) {
            try {
                startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update SERVICE_ACCOUNT_EMAIL with the email address of the service account for the client ID
     *  created in the developer console.
     */
    private static final String SERVICE_ACCOUNT_EMAIL = "23076989397-cut5f09l9aoehn9kd4n4u6d91u6a81s8@developer.gserviceaccount.com";

    /**
     * Update SERVICE_ACCOUNT_PKCS12_FILE_PATH with the file path to the private key file downloaded
     *  from the developer console.
     */
    private static final String SERVICE_ACCOUNT_PKCS12_FILE_PATH =
            "file://///android_asset/JhakasApplication677b8abdda87.p12";

    /**
     * Update USER_EMAIL with the email address of the user within your domain that you would like
     *  to act on behalf of.
     */
    private static final String USER_EMAIL = "user@mydomain.com";


    /**
     * plus.me and plus.stream.write are the scopes required to perform the tasks in this quickstart.
     *  For a full list of available scopes and their uses, please see the documentation.
     */
    private static final List<String> SCOPE = Arrays.asList(
            "https://www.googleapis.com/auth/plus.me",
            "https://www.googleapis.com/auth/plus.stream.write");



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Adapter adapter = parent.getAdapter();
        String accountName = (String) adapter.getItem(position);
        Plus.PlusOptions plusOptions = new Plus.PlusOptions.Builder()
                .addActivityTypes("http://schema.org/ReviewActivity", "http://schema.org/ReviewActivity",
                        "http://schema.org/MusicRecording", "http://schema.org/ListenAction")
                .build();

        Scope scope = new Scope("https://www.googleapis.com/auth/userinfo.profile");

        // Initialize google api client to use login and moment feature of google plush.
        /*googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, plusOptions)
                .addApi(Plus.API)
                .setAccountName(accountName)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addScope(SCOPE_PLUS_WRITE_STREAM)
                .addScope(scope)
                .build();


        googleApiClient.connect();*/
        googleApiClient.disconnect();

        googleApiClient.connect();
    }


    public void show(String textMessage) {
        Toast.makeText(this, textMessage, Toast.LENGTH_SHORT).show();
    }
}
