package com.echo.reflection.echovoicerecognition;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.*;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;

import android.media.ToneGenerator;

import org.json.JSONArray;

/**
 * Created by sumee_000 on 11/3/2014.
 */
public class VoiceCommandService extends Service implements RecognitionListener {
    public static final String INPUT = "input";
    public static final String RESULT = "result";
    public boolean triggerWordSpoken = false;

    SpeechRecognizer speechRecognizer;
    AudioManager audioManager;
    Intent speechRecognizerIntent;
    TextToSpeech tts;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       // audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
     //   audioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        //audioManager.setStreamMute(AudioManager.STREAM_RING, true);
        //audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, (new Long(2500)));
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        }


        if (speechRecognizer != null)
            speechRecognizer.startListening(speechRecognizerIntent);

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }



    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(getClass().getName(), "onReadyForSpeech(..) called.");
        //TODO notify activity?
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(getClass().getName(), "onBeginningOfSpeech(..) called.");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(getClass().getName(), "onEndOfSpeech(..) called.");
    }

    @Override
    public void onError(int error) {
        Log.d(getClass().getName(), "onError(..) called (" + error + ")");
       if (error != 5) {
            speechRecognizer.stopListening();
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(getClass().getName(), "onResults(..) called.");
        Log.d(getClass().getName(), results.toString());

        String wordStr;
        String[] words = new String[5];
        String firstWord = null;
        String secondWord = null;


        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (data.size() > 0) {
                speechRecognizer.stopListening();

                String command = null;

                wordStr = data.get(0);
          if (wordStr.contains("echo"))
            {
                firstWord = "echo";
            }

            else {
              if(wordStr.contains(" ")) {
                  words = wordStr.split(" ");
                  firstWord = words[0];
                  secondWord = words[1];
              }
              else{
                  words[0]= wordStr;
                  firstWord = words[0];
              }
            }

            if (triggerWordSpoken) {
                    if (firstWord.equals("open")&& secondWord!=null) {
                        command = "Open";
                    }
                    else if (firstWord.equals("close")) {
                       command = "Close";
                    }
                } else {

                    if ((firstWord.equals("echo")))
                    {
                        triggerWordSpoken = true;
                    }
                }


            if (triggerWordSpoken && command != null) {
                if (command.equals("Open")) {
                    openRequestedApplication(secondWord);
                }
                if (command.equals("Close")) {
                    closeRequestApplication(secondWord);
                }
                triggerWordSpoken = false;
            } else if (triggerWordSpoken && command == null) {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                Toast.makeText(getApplicationContext(), "Echo is listening :)", Toast.LENGTH_SHORT).show();
                final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, 200);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);

            } else {
                triggerWordSpoken = false;
            }

            speechRecognizer.startListening(speechRecognizerIntent);

        } else {
            speechRecognizer.stopListening();
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void closeRequestApplication(String secondWord) {

    }


    private void openRequestedApplication( String secondWord) {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packs = packageManager
                .getInstalledPackages(0);
        int size = packs.size();

        for (int v = 0; v < size; v++) {
            PackageInfo p = packs.get(v);
            String tmpAppName = p.applicationInfo.loadLabel(
                    packageManager).toString();
            String pname = p.packageName;
            tmpAppName = tmpAppName.toLowerCase();
            if (tmpAppName.trim().toLowerCase().
                    equals(secondWord.trim().toLowerCase())) {

                PackageManager pm = this.getPackageManager();
                Intent appStartIntent = pm.getLaunchIntentForPackage(pname);
                if (null != appStartIntent) {
                    try {
                        this.startActivity(appStartIntent);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }



    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(getClass().getName(), "onPartialResults(..) called.");
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(getClass().getName(), "onEvent(..) called.");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
