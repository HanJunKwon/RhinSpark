package com.rhinhospital.rnd.myapplication;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;



public class SQLiteManager extends SQLiteOpenHelper{
    SQLiteManager(Context context){
        super(context, StaticUtil.DATABASE_NAME , null, StaticUtil.CURR_DB_VERSION);
    }

    // 안드로이드 앱이 실행되면 미리 생성된 이미지명와 텍스트를 매칭해놓는다.
    private void image_text_matching(){
        
    }

    // 앱이 실행될 때 한 번 실행됨.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        try{
            // 이미지-텍스트 매칭 테이블 생성
            sqLiteDatabase.execSQL("create table img_text_match_tb(" +
                    "img_text_match_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "img_name VARCHAR(20) NOT NULL," + // 로컬에 저장된 이미지 파일의 이름
                    "text VARCHAR(50) NOT NULL," + // 해당 이미지와 매칭되는 텍스트
                    "item_difficulty INTEGER NOT NULL DEFAULT 1," + // 텍스트의 발음 난이도
                    "percent_current REAL NOT NULL DEFAULT 0.0" + // 텍스트 발음의 정확도(정확하게 발음한 개수 / 총 발음 개수)
                    ")");

            // STT 결과 전 오디오 파일 경로 저장
            sqLiteDatabase.execSQL("create table stt_record_file_tb("+
                    "sst_result_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                    "file_path VARCHAR(100) NOT NULL," + // 사용자가 녹음한 파일의 저장 경로
                    "file_name VARCHAR(50) NOT NULL," + // 사용자가 녹음한 파일명
                    "created_at DATE" +
                    ")");

            // STT 결과 저장
            sqLiteDatabase.execSQL("create table stt_result_text_tb(" +
                    "stt_record_file_id INTEGER," +
                    "trns_text VARCHAR(100)," + // 반환된 결과값 중 하나
                    "text_order INTEGER," + // 결과값 중 정확도 순위
                    "FOREIGN KEY(sst_record_file_id) REFERENCES stt_record_file_tb(sst_result_id)" +
                    ")");

            // 환자가 제출한 답안을 저장하는 테이블 -> 난이도 분석에 쓰임
            sqLiteDatabase.execSQL("create table answer_result_tb (" +
                    "sst_record_file_id INTEGER," +
                    "img_text_match_id INTEGER," +
                    "created_at DATE," + // 제출 일자
                    "is_current BOOLEAN DEFAULT 1," + // 정답 여부
                    "FOREIGN KEY(sst_record_file_id) REFERENCES sst_record_file_tb(sst_result_id)," +
                    "FOREIGN KEY(img_text_match_id) REFERENCES img_text_match_tb(img_text_match_id)"+ // 환자가 제출한 답안의 정답 여부
                    ")");

            // TTS 결과를 저장하는 테이블
            sqLiteDatabase.execSQL("create table tts_result_tb(" +
                    "tts_result_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "src_text VARCHAR(150) NOT NULL," + // 사용자가 입력한 텍스트
                    "file_path VARCHAR(100) NOT NULL," + // 네이버에서 보낸 결과 파일이 저장된 폴더까지 경로
                    "file_name VARCHAR(50) NOT NULL," + // 저장된 파일명
                    "created_at DATE" + // 생성 일자
                    ")");

            // 환자 정보를 입력하는 테이블
            sqLiteDatabase.execSQL("create table patient_info_tb(" +
                    "patient_info_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name VARCHAR(30) NOT NULL," + // 환자 이름
                    "birth VARCHAR(8) NOT NULL," + // 환자 생년월일 ex) 19941121
                    "gender VARCHAR(1) NOT NULL," + // 환자 성별 남자 = 'M' , 여자 = 'W'
                    "symptom TEXT," + // 환자 증상
                    "created_at DATE" + // 정보 입력 테이블
                    ")");

        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param sqLiteDatabase : 스키마 선택
     * @param oldVersion : 이전 데이터베이스 버전
     * @param newVersion : 현재 데이터베이스 버전
     *                   oldVersion < newVersion 이 되면 실행되는 메서드
     *                   업데이트마다 내용을 지우고 추가 및 수정되는 내용을 작성하면 됨.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        /*
        과거 변경이력까지 모두 갖고 있으려면 아래와 같이 코드를 작성하면 된다.
        switch(oldVersion){
            case 1:
                // 버전이 1에서 2로 올라갈 때 필요한 로직 작성
            case 2:
                // 버전이 2에서 3으로 올라갈 때 필요한 로직 작성
            case 3:
        }
         */
    }
}
