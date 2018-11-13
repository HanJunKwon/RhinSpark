package com.rhinhospital.rnd.myapplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioWriterPCM {
    String path;
    String filename;
    FileOutputStream speechFile;

    public AudioWriterPCM(String path){
        this.path = path; // 저장 경로 지정ㄴ
    }

    // Naver 서버로부터 전송받은 SessionId를 사용하여 파일명에 적용 생성
    public void open(String sessionId){
        File directory = new File(path);
        if(!directory.exists()){
            directory.mkdirs(); // 디렉토리 생성
        }

        // PCM(펄스 부호 변조)
        filename = directory + "/" + sessionId +".pcm"; //
        try {
            speechFile = new FileOutputStream(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.println("Can't open file : " + filename);
            speechFile = null;
            e.printStackTrace();
        }
    }

    // 녹음 종료
    public void close(){
        if(speechFile == null)
            return;

        try{
            speechFile.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // 파일 전송 시에 파일 형태 및 전송방식을 서버와 동기화하기 위해서 전송 방식 및 저장 방식 지정
    public void write(short[] data){
        if(speechFile == null)
            return;

        // 받은 데이터의 길이 * 2를 하는 이유
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 2);
        // 바이트를 배열하는 방식(ByteOrder)는 리틀 엔디안을 사용하여 저장한다.
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0; i< data.length; i++){
            buffer.putShort(data[i]);
        }

        buffer.flip();

        try{
            speechFile.write(buffer.array());
        } catch (IOException e) {
            System.err.println(e);
        }

    }
}
