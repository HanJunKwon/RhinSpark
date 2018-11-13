﻿# RhinSpark
네이버 Clova 테스트

추후 작업 내용
1. STT, TTS 기능 분리
2. 환자 정보 입력할 수 있는 뷰 생성
3. Firebase 또는 Naver 클라우드 사용하여 데이터 저장<br>
    3-1. Firebase를 사용하게 되는 경우 아시아 서버쪽에 아직 서버가 없기 때문에 네트워크 속도가 느린 경우가 생길 수 있다. 이런 경우를 대비해서 
    Retrofit2 를 사용하여 네트워크 속도 개선 필요함.
   
4. STT 세션 연결 개선(스레드쪽 봐야 됨)
5. STT의 경우 pcm으로 저장되기 때문에 mp3로 변환해주는 기능이 필요함.
6. 소스코드 정리( TTS 부분 작업하면서 스레드 문제인 줄 알고 Handler, Thread, AsyncTask 다 사용해서 코드가 지저분함)