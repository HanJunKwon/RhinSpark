package com.rhinhospital.rnd.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.service.autofill.CharSequenceTransformation;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.speech.clientapi.SpeechConfig;
import com.naver.speech.clientapi.SpeechConfig.EndPointDetectType;
import com.naver.speech.clientapi.SpeechConfig.LanguageType;
import com.naver.speech.clientapi.SpeechRecognitionException;
import com.naver.speech.clientapi.SpeechRecognitionListener;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.naver.speech.clientapi.SpeechRecognizer;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import retrofit2.Retrofit;


public class MainActivity extends AppCompatActivity implements StaticUtil {
    private final String TAG = MainActivity.class.getSimpleName();
    private final String CLIENT_ID = StaticUtil.naver_ai_api_client_id; // "내 애플리케이션"에서 Client ID
    private RecognitionHandler handler; // 녹음한 내용올 보낸 후에 텍스트로 결과를 받는 핸들러
    private SynthesisHandler CSShandler; // 문자열을 보낸 후에 음성으로 변환된 결과를 받는 핸들러
    private NaverRecognizer naverRecognizer;
    private NaverSynthesis naverSynthesis;
    private Button btnStart, btnSend; // 녹음시작
    private EditText edtText; // 음성으로 변환할 텍스트입력란
    private String mResult; // 음성 파일 텍스트로 분석한 결과 저장
    private TextView txtRecognizeResult; // 음성 결과를 보여주는 뷰
    private AudioWriterPCM audioWriterPCM; // 네이버에서 제공하는 sdk 클래스
    private HttpCon httpCon; // CSS 연결라인을 담는 객체
    private String fileName = null; // 텍스트를 네이버 API로 전달하여 음성으로 변환된 mp3 데이터의 파일명
    private MediaPlayer record;
    private SQLiteManager sqLiteManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 권한(스토리즈, 오디오, 인터넷) 체크
        checkPermission();

        sqLiteManager = new SQLiteManager(this);
        sqLiteManager.onCreate();

        txtRecognizeResult = (TextView) findViewById(R.id.txtRecognizeResult);
        btnStart = (Button) findViewById(R.id.btnStart);
        edtText = (EditText) findViewById(R.id.editText);
        handler = new RecognitionHandler(this);
        CSShandler = new SynthesisHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
        naverSynthesis = new NaverSynthesis(this, CSShandler);


        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    mResult = "";
                    txtRecognizeResult.setText(R.string.record_stop);
                    naverRecognizer.recognize();
                }
                else{
                    btnStart.setEnabled(false);
                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 백그라운드 스레드로 네트워크 연결하여 TTS 처리 요청.
                new NaverSynthesisAsyncTask().execute();
            }
        });

    }

    private void checkPermission() {
        int writePermissionResult = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int audioPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if(writePermissionResult == PackageManager.PERMISSION_DENIED || readPermissionResult == PackageManager.PERMISSION_DENIED || audioPermissionResult == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, StaticUtil.write_permission);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResult ="";
        txtRecognizeResult.setText("");
        btnStart.setText(R.string.record_start);
        btnStart.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop(); // 음성인식 서버 연결 종료
        naverRecognizer.getSpeechRecognizer().release();
    }

    // RecognitionHandler객체로 부터 전달된 메세지를 처리하는 부분
    private void handleMessage(Message msg){
        switch(msg.what){
            case R.id.clientReady: // 네이버 API 연결(준비단계)
                audioWriterPCM = new AudioWriterPCM(Environment.getExternalStorageDirectory().getAbsolutePath() + R.string.rhin_record_path);
                Toast.makeText(this, R.string.naver_api_connecte_successed, Toast.LENGTH_SHORT).show();
                break;
            case R.id.audioRecording: // 음성 녹음 시작
                Toast.makeText(this, R.string.started_record, Toast.LENGTH_SHORT).show();
                break;
            case R.id.partialResult: // 음성인식 서버로부터 인식 중간 결과를 받으면 호출됩니다. 중간 결과는 없거나 여러번 있을 수 있습니다.
                Toast.makeText(this, R.string.partial_result_request, Toast.LENGTH_SHORT).show();
                break;
            case R.id.finalResult: // 최종 인식 결과
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults(); // 우선순위가 높은 순으로 최대 5개의 리스트를 가져온다.
                StringBuilder strBuf = new StringBuilder(); // StringBuilder는 다른 스레드들이 접근이 가능하다. StringBuffer는 멀티 스레드 환경에서 수정이 불가능함.

                for(String result : results) {
                    strBuf.append(result);
                    strBuf.append("\n");
                }

                mResult = strBuf.toString();
                txtRecognizeResult.setText(mResult); // 환자 음성 분석한 결과를 textview에 보여준다.
                break;
            case R.id.recognitionError: // 인식 에러
                if(audioWriterPCM != null)
                    audioWriterPCM.close();

                mResult = "Error code" + msg.obj.toString();
                txtRecognizeResult.setText(mResult);

                btnStart.setText("녹음 시작");
                btnStart.setEnabled(true);
                break;
            case R.id.clientInactive:
                if( audioWriterPCM!= null)
                    audioWriterPCM.close();

                btnStart.setText("녹음 시작");
                btnStart.setEnabled(true);
                break;

            case R.id.SynthesisConnect:
                Log.e("","SynthesisConnect");
                httpCon = new HttpCon(this, StaticUtil.clova_speech_synthesis_url);
                naverSynthesis.sendText();
                break;
            case R.id.SendText:
                Log.e("", "sendText");
                httpCon.connectUrl(edtText.getText().toString());
                naverSynthesis.startRecord();
                break;
            case R.id.ResultRecordStart:
                Log.e("", "ResultRecordStart");
                break;
        }
    }

    //
    private class NaverSynthesisAsyncTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            try{
                String text = URLEncoder.encode(edtText.getText().toString(), "UTF-8");
                URL url = new URL(StaticUtil.clova_speech_synthesis_url);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST"); // POST 방식으로 전환이 안 됨.

                con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", StaticUtil.naver_ai_api_client_id);
                con.setRequestProperty("X-NCP-APIGW-API-KEY", StaticUtil.naver_ai_api_client_secret);

                String postParams = "speaker=mijin&speed=0&text="+text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                BufferedReader br;

                if(responseCode == 200){
                    // 정상호출
                    InputStream is = con.getInputStream();
                    int read = 0;

                    byte[] bytes = new byte[1024];

                    fileName = Long.valueOf(new Date().getTime()).toString();

                    // 변환된 음성파일 저장
                    File f = new File(StaticUtil.record_folder_name + fileName+".mp3");

                    // 저장할 디렉토리가 존재하지 않으면 생성함.
                    if(!StaticUtil.record_folder_name.mkdirs()){
                        StaticUtil.record_folder_name.mkdirs(); // 음성파일을 저장할 폴더를 생성함.
                        f.createNewFile(); // 생성된 폴더에 음성 파일 저장
                    }
                    else{
                        f.createNewFile(); // 음성 파일 저장
                    }

                    OutputStream outputStream = new FileOutputStream(f);
                    while((read = is.read(bytes)) != -1){
                        outputStream.write(bytes, 0, read);
                    }
                    is.close();
                }else{
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while((inputLine = br.readLine()) != null){
                        response.append(inputLine);
                    }
                    br.close();
                    System.out.print(response.toString());
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (ProtocolException e1) {
                e1.printStackTrace();
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            // R.raw에 파일을 저장한게 아니기 때문에 내부 Uri를 통해서 파일에 접근후에 재생해야 된다.
            /*
            R.raw, 내부 URI, 외부 URL을 통해서 파일을 재생하는 방법
            http://unikys.tistory.com/350
             */
            Uri fileUri = Uri.fromFile(new File(StaticUtil.record_folder_name+fileName+".mp3"));
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(getApplicationContext(), fileUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    static class RecognitionHandler extends Handler {
        /*
         WeakReference는 메모리가 충분하면 GC에 의해서 수거되지 않고 메모리에 여유가 없다면 GC(가비지 컬레터)에 수거된다.
         GC가 발생하면 해당 메모리 영역은 무조건 회수 되기 때문에 짧게 자주 사용하는 객체에 사용하는게 바람직하다.
        */

        /*
        안드로이드 메모리 누수가 발생하는 경우의 대부분은 Activity context에 관한 참조를 오랫동안 유지하기 때문이다.
        따라서 OOM이 발생하지 않도록 WeakRefence 객체를 사용하여 액티비티에 대한 Content를 짧게 갖게한다.
        Handler는 non-static 클래스이므로 외부 클래스 Activity 의 레퍼런스를 갖고 있기 때문에 애플리케이션이 종료되어도
        GC되지 않아 메모리 누수가 발생할 수 있다.
        자바에서 non static inner 클래스의 경우 outer class에 대한 reference를 참조하게 되면
         */
        private final WeakReference<MainActivity> mActivity;
        RecognitionHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null){
                activity.handleMessage(msg);
            }
        }
    }

    static class SynthesisHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;
        SynthesisHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null){
                activity.handleMessage(msg);
            }
        }
    }
}


