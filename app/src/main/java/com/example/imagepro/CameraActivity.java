package com.example.imagepro;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
//Second activity where we detect face ,
// make automatic captures of face when it is detected
//and save them on a folder called ImagePro

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";

    private Mat mRgba;
    private Mat mGray;
    private ImageView flip_camera;
    private int mCameraId = 0;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier CascadeClassifier;
    private int take_image = 0;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface
                        .SUCCESS: {
                    Log.i(TAG, "OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default: {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA = 0;

        // ask for permission on device if it is not given
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);

        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }


        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        flip_camera = findViewById(R.id.flip_camera);
        flip_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapCamera();
            }
        });


        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);//creating a folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");//creating a file on that folder
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            //writing that file from raw folder
            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);

            }

            is.close();
            os.close();

            //load file from cascade folder created above
            CascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());

            //model is loaded

        } catch (IOException e) {
            Log.i(TAG, "cascade file not found");

        }

    }

    private void swapCamera() {
        mCameraId = mCameraId ^ 1;
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {

            //if opencv loaded successfully
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {

            //if not loaded
            Log.d(TAG, "Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if (mCameraId == 1) {
            Core.flip(mRgba, mRgba, -1);
            Core.flip(mGray, mGray, -1);

        }
        mRgba = CascadeRec(mRgba);

        return mRgba;

    }

    private Mat CascadeRec(Mat mRgba) {
        Core.flip(mRgba.t(), mRgba, 1);
        Mat mRbg = new Mat();
        Imgproc.cvtColor(mRgba, mRbg, Imgproc.COLOR_RGBA2BGR);
        int height = mRbg.height();
        int absoluteFaceSize = (int) (height * 0.1);
        MatOfRect faces = new MatOfRect();
        if (CascadeClassifier != null) {
            CascadeClassifier.detectMultiScale(mRbg, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());

        }
        //Draw a rectangle
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            Rect roi = new Rect((int) facesArray[i].tl().x, (int) facesArray[i].tl().y, (int) facesArray[i].br().x - (int) facesArray[i].tl().x, (int) facesArray[i].br().y - (int) facesArray[i].tl().y);
            Mat cropped = new Mat(mRgba, roi);
            take_picture_function_rgb(cropped);

        }
        Core.flip(mRgba.t(), mRgba, 0);

        return mRgba;
    }

    //make capture if face detected
    private void take_picture_function_rgb(Mat mRgba) {
        Mat save_mat = new Mat();
        Imgproc.cvtColor(mRgba, save_mat, Imgproc.COLOR_RGBA2BGRA);
        //Create a folder where we save the captures
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/ImagePro");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateAndTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() + "/ImagePro/" + currentDateAndTime + ".jpg";
        //  save_mat
        Imgcodecs.imwrite(fileName, save_mat);

        File dir = new File(Environment.getExternalStorageDirectory() + "/ImagePro");
        File[] files = dir.listFiles();
        if (files != null) {
            int numberOfFiles = files.length;





            Intent intent = new Intent(CameraActivity.this, ScanIDStart.class);
            //if we get 10 face capture on the folder,we move to the next activity
            if(numberOfFiles == 10){
                startActivity(intent);
                finish();
            }

        }
    }

    }








