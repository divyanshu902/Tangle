
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;
    public static int i =0;

    public static FriendlyMessage copy[] = new FriendlyMessage[1000];
    public static FriendlyMessage copy1[] = new FriendlyMessage[1000];

    public static int count;
    public static int count1;


    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    private TextToSpeech mTTs;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mUsername = ANONYMOUS;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        ListView mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.chat_activity);

        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.developer_activity:
                        startActivity(new Intent(getApplicationContext(), DeveloperActivity.class));
                        finish();
                        overridePendingTransition(0,0);
                        return;

                    case R.id.image_activity:
                        startActivity(new Intent(getApplicationContext(), ImageActivity.class));
                        finish();
                        overridePendingTransition(0,0);
                        return;

                    case R.id.chat_activity:

                }
            }
        });

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);            }
        });

        Button mButtonSpeak = findViewById(R.id.button_speak);
        mTTs = new android.speech.tts.TextToSpeech(this, new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    int result = mTTs.setLanguage(Locale.ENGLISH);
                    if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                            result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                }
                else{
                    Log.e("TTS", "Initialisation Failed");
                }
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});



        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear input box
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);
                mMessageEditText.setText("");

            }
        });



        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    onSignedInInitialize(user.getDisplayName());

                }
                else{
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }

            }
        };
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(this,"Signed In!", Toast.LENGTH_SHORT).show();
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this,"Sign in cancelled", Toast.LENGTH_SHORT).show();
                finish();

            }
        }else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            StorageReference photoRef =
                    mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!urlTask.isSuccessful());
                    Uri downloadUrl = urlTask.getResult();
                    FriendlyMessage friendlyMessage =
                            new FriendlyMessage(null, mUsername, downloadUrl.toString());
                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int lang=0;
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.english:
            {
                lang = 11;
                for(int j=0;j<=count;j++) {
                    identifylang(copy[j], lang);
                }
                i=0;
                return true;

            }

            case R.id.spanish:
            {

                lang = 13;

                for(int j=0;j<=count;j++) {


                    identifylang(copy[j], lang);
                }
                i=0;

                return true;

            }

            case R.id.german:
            {
                lang = 9;
                for(int j=0;j<=count;j++) {
                    identifylang(copy[j], lang);
                }
                i=0;
                return true;


            }

            case R.id.french:
            {
                lang = 17;
                for(int j=0;j<=count;j++) {
                    identifylang(copy[j], lang);
                }
                i=0;
                return true;


            }


            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected  void  onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void onSignedInInitialize(String username){
        mUsername = username;
        attachDatabaseReadListener();


    }

    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();

    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {

            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    final FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    copy[i] = friendlyMessage;
                    //identifylang(friendlyMessage);
                    count = i;
                    //System.out.println(count);
                    mMessageAdapter.add(friendlyMessage);// To display messages
                    i++;

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }
    public void identifylang(final FriendlyMessage friendlyMessage, final int code){
        FirebaseLanguageIdentification languageIdentifier =
                FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        if(friendlyMessage.getText()!=null) {
            languageIdentifier.identifyLanguage(friendlyMessage.getText())
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@Nullable String languageCode) {
                                    if (!(languageCode.equals("und"))) {
                                        Log.i(TAG, "Language: " + languageCode);
                                        translatelanguage(friendlyMessage, code, languageCode);

                                    } else {
                                        Log.i(TAG, "Can't identify language.");
                                        languageCode = "en";
                                        translatelanguage(friendlyMessage, code, languageCode);


                                        // mMessageAdapter.remove(friendlyMessage);
                                        // mMessageAdapter.add(friendlyMessage);

                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Model couldn’t be loaded or other internal error.
                                    // ...
                                }

                            });
        }
        else {
            String languageCode = "en";
            friendlyMessage.setText(".");
            translatelanguage(friendlyMessage, code, languageCode);


        }

    }
    public void translatelanguage(final FriendlyMessage friendlyMessage, int code,String languagecode){


            int langcode = FirebaseTranslateLanguage
                    .languageForLanguageCode(languagecode);

            FirebaseTranslatorOptions options =
                    new FirebaseTranslatorOptions.Builder()
                            .setSourceLanguage(langcode)
                            .setTargetLanguage(code)
                            .build();

            final FirebaseTranslator languageTranslator =
                    FirebaseNaturalLanguage.getInstance().getTranslator(options);
            FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build();
            languageTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(
                            new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void v) {
                                    // Model downloaded successfully. Okay to start translating.
                                    // (Set a flag, unhide the translation UI, etc.)
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Model couldn’t be downloaded or other internal error.
                                    // ...
                                }
                            });
            languageTranslator.translate(friendlyMessage.getText())
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@NonNull String translatedText) {
                                    // Translation successful.
                                    mMessageAdapter.remove(friendlyMessage);
                                    friendlyMessage.setText(translatedText);


                                    mMessageAdapter.add(friendlyMessage);

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Error.
                                    // ...
                                }
                            });


    }

    @Override
    public void onDestroy() {
        if(mTTs != null){
            mTTs.stop();
            mTTs.shutdown();
        }
        super.onDestroy();
    }

    public void speak(String text) {
        float pitch = 1.0f;
        float speed = 1.0f;

        mTTs.setPitch(pitch);
        mTTs.setSpeechRate(speed);
        mTTs.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
    }
}

