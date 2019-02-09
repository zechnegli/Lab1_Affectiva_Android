package com.example.lab1_affectiva_android;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraDetector.CameraEventListener, CameraDetector.ImageListener{
    ArrayList<Float> emotionJoyList = new ArrayList<Float>();
    ArrayList<EmtionWithTimeStamp> emotionJoyTimeStampList =  new ArrayList<EmtionWithTimeStamp>();
    int joyStandard = 10;
    //1
    SurfaceView cameraDetectorSurfaceView;
    CameraDetector cameraDetector;
    TextView SMADescribitonView;
    TextView WMALabel;
    TextView SMAtimestampsTextView;
    TextView EmojiTextView;
    MediaPlayer mediaPlayer;

    //2
    int maxProcessingRate = 10;
    float totalWeight = 0;
    int sampleNumber = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        totalWeight = totalWeight();
        //3
        cameraDetectorSurfaceView = (SurfaceView) findViewById(R.id.cameraDetectorSurfaceView);
        WMALabel = findViewById(R.id.WMALabel);
        SMAtimestampsTextView = findViewById(R.id.SMAtimestampsTextView);
        SMADescribitonView = findViewById(R.id.SMALabel);
        EmojiTextView = findViewById(R.id.EmojiTextView);
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        //4
        cameraDetector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraDetectorSurfaceView);

        //5
        cameraDetector.setMaxProcessRate(maxProcessingRate);

        //6
        cameraDetector.setImageListener(this);
        cameraDetector.setOnCameraEventListener(this);

        //7
        cameraDetector.setDetectAllEmotions(true);

        //8
        cameraDetector.start();
    }
    @Override
    public void onCameraSizeSelected(int cameraHeight, int cameraWidth, Frame.ROTATE rotation) {

        //1
        ViewGroup.LayoutParams params = cameraDetectorSurfaceView.getLayoutParams();

        //2
        params.height = cameraHeight;
        params.width = cameraWidth;

        //3
        cameraDetectorSurfaceView.setLayoutParams(params);
    }

    @Override
    public void onImageResults(List<Face> faces, Frame frame, float timeStamp) {
        //1
        if (faces == null)
            return; //frame was not processed

        //2
        if (faces.size() == 0)
            return; //no face found


        Face currentFace = faces.get(0);

        EmojiTextView.setText( "Your current face: " + getEmotionEmoji(currentFace.emotions.getJoy(), currentFace.emotions.getAnger(), currentFace.emotions.getFear(), currentFace.emotions.getSurprise()));
        addNewEmotionJoy(currentFace.emotions.getJoy());
        float lastRemovedStampValue = addNewEmotionJoy(timeStamp, currentFace.emotions.getJoy());

        System.out.println("------" + emotionJoyList.size());

        //When joy exceed the requirement, the music will play
       if (emotionJoyList.size() >= sampleNumber) {
            float meanValue = caculateMean();
            if (isJoy(meanValue)) {
                mediaPlayer.start();
                SMADescribitonView.setText("You have reached the threshold using SMA");
            } else {
                mediaPlayer.stop();
                SMADescribitonView.setText("You did not have reached the threshold using SMA");
            }

            float weightMeanVlaue = caculateWeightedAverage();
           if (isJoy(weightMeanVlaue)) {
               WMALabel.setText("You have reached the threshold using WMA");
           } else {
               WMALabel.setText("You did not have reached the threshold using WMA");
           }
           System.out.println("1: " + meanValue);
           System.out.println("2: " + weightMeanVlaue);


       }

        if ((emotionJoyTimeStampList.get(emotionJoyTimeStampList.size() - 1).timeStamp - lastRemovedStampValue) >= 10) {
            float meanValueInTimeStamp = caculateMeanBasedOnTimeStamp();
            if (isJoy(meanValueInTimeStamp)) {
                SMAtimestampsTextView.setText("You have reached the threshold using SMA using timestamps");
            } else {
                SMAtimestampsTextView.setText("You did not have reached the threshold using SMA using timestamps");
            }

        }


    }

    public String getEmotionEmoji(float joy, float anger, float fear, float surprise) {
        System.out.println("j " + joy + "a " + anger + " " + fear + " " + surprise);
        if (surprise > 1.9) {
            return "😍";
        } else if (anger > 1.1) {
            return "😈";
        } else if (fear > 1) {
            return "😱";
        } else if (joy > 30) {
            return "😀";
        }
        return "😌";
    }





    // if the total emotion joy is less than 100, add it, otherwise add new joy to the end and delete the first one
    public void addNewEmotionJoy(float emotionJoy) {
        if (emotionJoyList.size() < sampleNumber) {
            emotionJoyList.add(emotionJoy);
        } else {
            emotionJoyList.remove(0);
            emotionJoyList.add(emotionJoy);
        }
    }
    //cacualte mean for 100 data point
    public float caculateMean() {
        if (emotionJoyList.size() != sampleNumber) {
            return  0;
        }
        float total = 0;
        for (int index = 0; index < emotionJoyList.size(); index++) {
            total += emotionJoyList.get(index);
        }
        return total / sampleNumber;

    }

    public float caculateMeanBasedOnTimeStamp() {
        float total = 0;
        for (int index = 0; index < emotionJoyTimeStampList.size(); index++) {
            total += emotionJoyTimeStampList.get(index).emotionJoy;
        }

        return total / emotionJoyTimeStampList.size();
    }

    //caculate weigh value based on each index weight
    public float caculateWeightedAverage() {
        if (emotionJoyList.size() != sampleNumber) {
            return  0;
        }
        float averageWeight = 0;
        for (int index = 0; index < emotionJoyList.size(); index++) {
            averageWeight += emotionJoyList.get(index) * ((index+1) / totalWeight);
        }

        return averageWeight;
    }

    //add new joy based on the time stamp, remove previous time stamps that are 10s lesser than the new time stamp
    public float addNewEmotionJoy(float timeStamp, float emotionJoy) {
        float lowerBound = timeStamp - 10;
        emotionJoyTimeStampList.add(new EmtionWithTimeStamp(emotionJoy, timeStamp));
        float lastRemovedValue = emotionJoyTimeStampList.get(0).timeStamp;
       for (int index = 0; index < emotionJoyTimeStampList.size(); index++) {
           if (emotionJoyTimeStampList.get(index).timeStamp <= lowerBound) {
               lastRemovedValue = emotionJoyTimeStampList.get(index).timeStamp;
               emotionJoyTimeStampList.remove(index);

               index--;
           }
       }
       emotionJoyTimeStampList.add(new EmtionWithTimeStamp(emotionJoy, timeStamp));
       return lastRemovedValue;
    }

    //cacualte the total weight
    public int totalWeight() {
        int total = 0;
        for (int index = 1; index <= sampleNumber; index++) {
            total += index;
        }
        return total;
    }




    public boolean isJoy(float mean) {
        if (mean >= joyStandard) {
            return true;
        }
        return false;
    }
}
