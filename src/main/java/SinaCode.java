import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.UUID;


/**
 * Created by jiacb on 16/8/14.
 */
public class SinaCode{


    public static String postFile(File f) throws ClientProtocolException, IOException, JSONException {
        String result = null;
        if (f != null) {
//            HttpPost httpPost = new HttpPost("http://bbb4.hyslt.com/api.php?mod=php&act=upload");
            HttpPost httpPost = new HttpPost("http://v1-http-api.jsdama.com/api.php?mod=php&act=upload");
            httpPost.setHeader("User-Agent", "SOHUWapRebot");
            httpPost.setHeader("Accept-Language", "zh-cn,zh;q=0.5");
            httpPost.setHeader("Accept-Charset", "GBK,utf-8;q=0.7,*;q=0.7");
            httpPost.setHeader("Connection", "keep-alive");

            MultipartEntity mutiEntity = new MultipartEntity();
            mutiEntity.addPart("user_name", new StringBody("shengzing", Charset.forName("utf-8")));
            mutiEntity.addPart("user_pw", new StringBody("Wi$eR00t", Charset.forName("utf-8")));
            mutiEntity.addPart("zztool_token",new StringBody("shengzing", Charset.forName("utf-8")));
            mutiEntity.addPart("upload", new FileBody(f));


            HttpClient httpClient = new DefaultHttpClient();

            httpPost.setEntity(mutiEntity);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            String content = EntityUtils.toString(httpEntity);
            JSONObject re = new JSONObject(content);
            System.out.println(re);
            if (re.has("result")) {
                if (re.getBoolean("result") && re.has("data")) {
                    result = re.getJSONObject("data").getString("val").toLowerCase();
                    return result;
                }
            }
            return result;

        } else {
            return result;
        }
    }

    public File getCode(String url) throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        File file = new File(UUID.randomUUID() + ".png");
        try {
            FileOutputStream fout = new FileOutputStream(file);
            int l = -1;
            byte[] tmp = new byte[1024];
            while ((l = in.read(tmp)) != -1) {
                fout.write(tmp, 0, l);
                // 注意这里如果用OutputStream.write(buff)的话，图片会失真，大家可以试试
            }
            fout.flush();
            fout.close();
        } finally {
            // 关闭低层流。
            in.close();
        }
        httpclient.close();

        return file;

    }
    public static String getCodeFromImg(final String imgStr) throws IOException {

        File file = base64ImageToFile(imgStr);
        return postFile(file);
//        return imgStr;
    }
    /**
     * 识别base64
     *
     * @param imgStr
     * @return
     */
    public static File base64ImageToFile(String imgStr) {
        if (imgStr == null) //图像数据为空
            return null;
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            //Base64解码
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {//调整异常数据
                    b[i] += 256;
                }
            }
            File file = new File(UUID.randomUUID() + ".png");
            OutputStream out = new FileOutputStream(file);
            out.write(b);
            out.flush();
            out.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }


}