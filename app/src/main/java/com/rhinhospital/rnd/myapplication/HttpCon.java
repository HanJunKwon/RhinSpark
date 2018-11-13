package com.rhinhospital.rnd.myapplication;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public class HttpCon  {
    Context context;
    String strUrl;

    /**
     *
     * @param context
     * @param url
     */
    HttpCon(Context context, String url){
        this.context = context;
        this.strUrl = url;
    }

    public String connectUrl(String text){
        String tempname = null;
        try{
            String msg = URLEncoder.encode(text, "UTF-8");
            URL url = new URL(strUrl);

            HttpURLConnection con = (HttpURLConnection)url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", StaticUtil.naver_ai_api_client_id);
            con.setRequestProperty("X-NCP-APIGW-API-KEY", StaticUtil.naver_ai_api_client_secret);

            // post request
            String postParams = "speaker=mijin&speed=0&text="+text;
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode == 200){
                // 정상 호출
                InputStream is = con.getInputStream();
                int read = 0;

                byte[] bytes = new byte[1024];

                tempname = Long.valueOf(new Date().getTime()).toString();

                File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+tempname+".mp3");
                f.createNewFile();
                OutputStream outputStream = new FileOutputStream(f);
                while((read = is.read(bytes)) != -1){
                    outputStream.write(bytes, 0, read);
                }
                is.close();

                // 바로 반환
                return tempname;
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
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempname;
    }
}
