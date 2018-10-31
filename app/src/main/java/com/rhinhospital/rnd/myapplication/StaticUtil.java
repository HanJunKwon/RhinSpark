package com.rhinhospital.rnd.myapplication;

public interface StaticUtil {
    final static String clova_speech_recognition_url =""; // CSR 기능 사용 시 호출할 url
    final static String clova_speech_synthesis_url="https://naveropenapi.apigw.ntruss.com/voice/v1/tts"; // CSS 기능 사용 시 호출할 url(POST)

    // 네이버 AI API 사용 시에 인증을 위해서 client ID 와 client secret을 함께 key와 value로 전송한다.
    final static String naver_ai_api_client_id = "3wbn8ctz00";
    final static String naver_ai_api_client_secret = "Qgf4gwMFPn9T0TNee0nSZJsW68sG6tdrk4cnSJb8";



}
