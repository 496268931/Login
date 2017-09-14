import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Created by wiseweb on 2017/2/22.
 */
public class HttpTools {
     /**
      * 正常GET方式HTTP请求
      * @param client
      * @param url
      * @return
      * @throws ClientProtocolException
      * @throws IOException
      */
     public static String getRequest(HttpClient client, String url) throws ClientProtocolException, IOException {
         HttpGet get = new HttpGet(url);
         get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
         get.addHeader("Referer", "http://weibo.com");
         HttpResponse response = client.execute(get);
         HttpEntity entity = response.getEntity();
         String content = getEntity(response);
         EntityUtils.consume(entity);
         return content;
     }

    public static JSONObject getData(HttpClient client,String url) throws ClientProtocolException, IOException {
        JSONObject Cookie = new JSONObject();
        JSONObject form = new JSONObject();
        JSONObject h = new JSONObject();
        h.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
        h.put("Referer", "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)");
        HttpResponse response = new HttpClien().Request(form,h,url);
        Header[] header = response.getHeaders("Set-Cookie");
        JSONObject headers = new JSONObject();
        if (header != null && header.length > 0) {
            for (int i = 0; i < header.length; i++) {
                String[] sp = header[i].getValue().split(";")[0].split("=");
                if (sp.length > 2) {
                    Cookie.put(sp[0], header[i].getValue().split(";")[0].split(sp[0] + "=")[1]);
                } else {
                    Cookie.put(sp[0], sp.length > 1 ? sp[1] : "");
                }
            }
//            Cookie.put("time", new Date().getTime() + 4 * 30 * 24 * 60 * 60 * 1000);
            Cookie.remove("SRT");
            Cookie.remove("SRF");
            Cookie.remove("SCF");
            Cookie.remove("ALF");
            Cookie.remove("SUHB");
        }
        return Cookie;
    }

    /**
     * 获取实体
     * @param response
     * @return
     */
    public static String getEntity(HttpResponse response){
        try {
            return getEntity(response, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 实体类转换
     * @param response
     * @param CharSet
     * @return
     */
    public static String getEntity(HttpResponse response,String CharSet){
        try {
            return EntityUtils.toString(response.getEntity(), CharSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 正常POST方式HTTP请求
     * @param client
     * @param url
     * @param parms
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String postRequest(HttpClient client, String url, List<NameValuePair> parms) throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(url);
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.addHeader("Referer", "http://weibo.com/");
        UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(parms, "UTF-8");
        post.setEntity(postEntity);
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity, "GBK");
        return content;
    }
}
