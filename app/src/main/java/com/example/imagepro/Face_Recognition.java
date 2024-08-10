package com.example.imagepro;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.imagepro.realm.Students_List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;

public class Face_Recognition {

    private Interpreter interpreter;
    private int INPUT_SIZE;
    private int height = 0;
    private int width = 0;
    private GpuDelegate gpuDelegate = null;
    private CascadeClassifier cascadeClassifier;
    public float read_face = 0;
    private List<String> namesArray = new ArrayList<>();

    Face_Recognition(AssetManager assetManager, Context context, String modelPath, int input_size) throws Exception {
        INPUT_SIZE = input_size;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
//        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        interpreter = new Interpreter(loadmodel(assetManager, modelPath), options);
        Log.d("Face_Recognition", "Face_Recognition: Model is Loaded");

        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt");
            FileOutputStream outputStream = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            outputStream.close();

            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.d("Face_recognition", "Classifier is loaded");
            if (cascadeClassifier == null) {
                Log.d("Face_recognition", "Failed to load classifier");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Mat recognizeImage(Mat mat_image) {

        Core.flip(mat_image.t(), mat_image, 1);
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
        height = grayscaleImage.height();
        width = grayscaleImage.width();
        int absoluteFaceSize = (int) (height * 0.1);
        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }
        Rect[] faceArray = faces.toArray();
        for (int i = 0; i < faceArray.length; i++) {
            Imgproc.rectangle(mat_image, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            Rect roi = new Rect((int) faceArray[i].tl().x, (int) faceArray[i].tl().y,
                    ((int) faceArray[i].br().x) - ((int) faceArray[i].tl().x),
                    ((int) faceArray[i].br().y) - ((int) faceArray[i].tl().y));
            Mat cropped_rgb = new Mat(mat_image, roi);
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(cropped_rgb.cols(), cropped_rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgb, bitmap);
            Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaleBitmap);
            float[][] face_value = new float[1][1];
            interpreter.run(byteBuffer, face_value);
            Log.d("Face Value: ", Arrays.deepToString(face_value));
            Log.d("Face_recognition", "out: " + Array.get(Array.get(face_value, 0), 0));

            read_face = (float) Array.get(Array.get(face_value, 0), 0);
            String face_name = get_face_name(read_face);
            Imgproc.putText(mat_image, "" + face_name,
                    new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                    1, 1.5, new Scalar(255, 255, 255, 150), 2);
        }
        Core.flip(mat_image.t(), mat_image, 0);
        return mat_image;
    }

//    private String get_face_name(float readFace) {
//        String val = "";
//
//        if (readFace >= 0 && readFace < 1) {
//            val = "Courteney_Cox";
//        } else if (readFace >= 1 && readFace < 2) {
//            val = "hardik_pandya";
//        } else if (readFace >= 2 && readFace < 3) {
//            val = "David_Schwimmer";
//        } else if (readFace >= 3 && readFace < 4) {
//            val = "Pho";
//        } else if (readFace >= 4 && readFace < 5) {
//            val = "Matt_LeBlanc";
//        } else if (readFace >= 5 && readFace < 6) {
//            val = "Simon_Helberg";
//        } else if (readFace >= 6 && readFace < 7) {
//            val = "scarlett_johansson";
//        } else if (readFace >= 7 && readFace < 8) {
//            val = "Pankaj_Tripathi";
//        } else if (readFace >= 8 && readFace < 9) {
//            val = "Mai";
//        } else if (readFace >= 9 && readFace < 10) {
//            val = "Matthew_Perry";
//        } else if (readFace >= 10 && readFace < 11.5) {
//            val = "messi";
////        } else if (readFace >= 11 && readFace < 12) {
////            val = "messi"; sylvester_stallone
//        } else if (readFace >= 11.5 && readFace < 12.5) {
//            val = "Jim_Parsons";
//        } else if (readFace >= 12.5 && readFace < 14) {
//            val = "Lisa_Kudrow";
//        } else if (readFace >= 14 && readFace < 15) {
//            val = "HoangPho";
//        } else if (readFace >= 15 && readFace < 16) {
//            val = "brad_pitt";
//        } else if (readFace >= 16 && readFace < 17) {
//            val = "ronaldo";
//        } else if (readFace >= 17 && readFace < 18) {
//            val = "virat_kohli";
//        } else if (readFace >= 18 && readFace < 19) {
//            val = "angelina_jolie";
//        } else if (readFace >= 19 && readFace < 20) {
//            val = "Tinh";
//        } else if (readFace >= 20 && readFace < 21) {
//            val = "Sachin_Tendulka";
//        } else if (readFace >= 21 && readFace < 22) {
//            val = "Jennifer_Aniston";
//        } else if (readFace >= 22 && readFace < 23) {
//            val = "manoj_bajpayee";
//        } else if (readFace >= 23 && readFace < 24) {
//            val = "dhoni";
//        } else if (readFace >= 24 && readFace < 25) {
//            val = "pewdiepie";
//        } else if (readFace >= 25 && readFace < 26) {
//            val = "Johnny_Galeck";
//        } else if (readFace >= 26 && readFace < 27) {
//            val = "aishwarya_rai";
//        } else if (readFace >= 27 && readFace < 28) {
//            val = "ROHIT_SHARMA";
//        } else if (readFace >= 28 && readFace < 29) {
//            val = "suresh_raina";
//        }
//
//        return val;
//    }

    private String get_face_name(float read_face) {
        String val = "";

        if (read_face >= -1 && read_face < 1) {
            val = "Courteney_Cox";
        } else if (read_face >= 1 && read_face < 2) {
            val = "scarlett_johansson";
        } else if (read_face >= 2 && read_face < 3.5) {
            val = "HoangPho";
//        } else if (read_face >= 3 && read_face < 4) {
//            return "Mai";
        } else if (read_face >= 4 && read_face < 5.7) {
            val = "VanTinh";
//        } else if (read_face >= 5 && read_face < 6) {
//            return "Jim_Parsons";
        } else if (read_face >= 5.7 && read_face < 6.3) {
            val = "ronaldo";
        } else if (read_face >= 6.3 && read_face < 8) {
            val = "angelina_jolie";
//        } else if (read_face >= 8 && read_face < 9) {
//            val = "Messi";
        } else if (read_face >= 8 && read_face < 9) {
            val = "Jennifer_Aniston";
        } else{
            val = "Unknown";
        }
        return val;
    }

    public void get_name(Context context) {
        String name = get_face_name(read_face);
        String stdID = getStdIDFromName(name);
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
//        Log.d("Test_get_name", ""+name+stdID);
        editor.putString(name, stdID);
        editor.apply();
    }

    public String getStdIDFromName(String studentName) {
        Realm realm = Realm.getDefaultInstance();
        Students_List student = realm.where(Students_List.class)
                .equalTo("name_student", studentName)
                .findFirst();
        if (student != null) {
            return student.getRegNo_student();
        }
        return null;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaleBitmap) {
        ByteBuffer byteBuffer;
        int input_size = INPUT_SIZE;
        byteBuffer = ByteBuffer.allocateDirect(4 * 1 * input_size * input_size * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[input_size * input_size];
        scaleBitmap.getPixels(intValues, 0, scaleBitmap.getWidth(), 0, 0, scaleBitmap.getWidth(), scaleBitmap.getHeight());
        int pixels = 0;
        for (int i = 0; i < input_size; ++i) {
            for (int j = 0; j < input_size; ++j) {
                final int val = intValues[pixels++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                byteBuffer.putFloat(((val & 0xFF)) / 255.0f);

            }
        }
        return byteBuffer;
    }

    private MappedByteBuffer loadmodel(AssetManager assetManager, String modelPath) throws Exception {
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
