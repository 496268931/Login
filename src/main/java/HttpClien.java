import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by Administrator on 2015/12/4.
 */
public class HttpClien extends Util{

    /**
     * 异步请求
     * @return
     */
    public List<HttpResponse> AsyncRequest(Object requests){
        try {
            CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().build();
            try {
                httpclient.start();
                List<HttpResponse> responses = new ArrayList<HttpResponse>();
                HttpUriRequest[] arr = requests instanceof HttpUriRequest[]?(HttpUriRequest[]) requests:null;
                @SuppressWarnings("unchecked")
                List<HttpUriRequest> list = requests instanceof HttpUriRequest[]?null:(List<HttpUriRequest>) requests;
                int count = requests instanceof HttpUriRequest[]?arr.length:list.size();
                for(int i=0;i<count;i++){
                    try {
                        responses.add(httpclient.execute(requests instanceof HttpUriRequest[]?arr[i]:list.get(i),null).get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return responses;
            } finally {
                httpclient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Http无代理请求
     * @param form
     * @param headers
     * @param url
     * @return
     */
    public HttpResponse Request(JSONObject form, JSONObject headers, String url){
        try {
            return Request(null, form, headers, url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Http代理请求
     * @param config
     * @param form
     * @param headers
     * @return
     */
    public HttpResponse Request(RequestConfig config,JSONObject form,JSONObject headers,String exeurl){
        try {
            CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
            try {
                httpclient.start();
                URL url = new URL(exeurl);
//                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
                Future<HttpResponse> future = httpclient.execute(getHttpUriRequest(config, form, headers, exeurl), null);
                HttpResponse response = future.get();
                //执行成功获取返回Cookie信息
                if(response!=null && headers!=null){
                    Header[] header = response.getHeaders("Set-Cookie");
                    if(header!=null && header.length>0){
                        JSONObject Cookie = headers.has("Cookie")? (JSONObject) (headers.get("Cookie") instanceof String ? getCookie(headers.getString("Cookie")) : headers.getJSONObject("Cookie")) :new JSONObject();
                        for(int i=0;i<header.length;i++){
                            String[] sp = header[i].getValue().split(";")[0].split("=");
                            if(sp.length>2){
                                Cookie.put(sp[0],header[i].getValue().split(";")[0].split(sp[0]+"=")[1]);
                            }else{
                                Cookie.put(sp[0],sp.length>1?sp[1]:"");
                            }
                        }
                        headers.put("Cookie",Cookie);
                    }
                }
                return response;
            }finally{
                httpclient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public HttpUriRequest getHttpUriRequest(RequestConfig config,JSONObject form,JSONObject headers,String url){
        try {
            config = config!=null?config:getConfig();
//            url = URLDecoder.decode(URLDecoder.decode(url, "UTF-8"), "UTF-8");
            HttpGet get = form==null?new HttpGet(url):null;
            HttpPost post = form==null?null:new HttpPost(url);
            (get==null?post:get).setConfig(config);
            //form不为Null,则POST请求
            if(form!=null){
                List<NameValuePair> params = getParams(form);
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params,"utf-8");
                post.setEntity(entity);
            }
            addHeaders(headers,get,post);
            return (get==null?post:get);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void addHeaders(JSONObject headers,HttpGet get,HttpPost post) {
        try{
            if(headers!=null){
                Iterator i = headers.keys();
                while(i.hasNext()){
                    try {
                        String key = ((String)i.next()).trim();
                        if(key.equals("Cookie")){
                            if(headers.get(key) instanceof String){
                                (get==null?post:get).addHeader("Cookie",headers.getString("Cookie").trim());
                            }else{
                                Iterator ic = headers.getJSONObject("Cookie").keys();
                                StringBuffer Cookies = new StringBuffer();
                                while(ic.hasNext()){
                                    key = ((String)ic.next()).trim();
                                    Cookies.append(key+"="+headers.getJSONObject("Cookie").get(key)+"; ");
                                }
                                if(Cookies.length()>0){
                                    String Cookie = Cookies.toString().substring(0,Cookies.toString().length()-2);
                                    (get==null?post:get).addHeader("Cookie",Cookie);
                                }
                            }
                        }else{
                            (get==null?post:get).addHeader(key,headers.getString(key));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){

            System.out.println("异常"+e);
//            error("异常",e);
        }
    }


    /**下载图片
     * @param imageUrl
     * @return
     */
    public byte[] getImageByte(final String imageUrl, JSONObject headers){
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            //设置Cookie信息
            if(headers!=null){
                Iterator i = headers.keys();
                while(i.hasNext()){
                    try {
                        String key = ((String)i.next()).trim();
                        if(key.equals("Cookie")){
                            if(headers.get(key) instanceof String){
                                con.setRequestProperty("Cookie",headers.getString("Cookie").trim());
                            }else{
                                Iterator ic = headers.getJSONObject("Cookie").keys();
                                StringBuffer Cookies = new StringBuffer();
                                while(ic.hasNext()){
                                    key = ((String)ic.next()).trim();
                                    Cookies.append(key+"="+headers.getJSONObject("Cookie").get(key)+"; ");
                                }
                                if(Cookies.length()>0){
                                    String Cookie = Cookies.toString().substring(0,Cookies.toString().length()-2);
                                    con.setRequestProperty("Cookie",Cookie);
                                }
                            }
                        }else{
                            con.setRequestProperty(key,headers.getString(key));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            //设置请求超时10s
            con.setConnectTimeout(10*1000);
            //执行成功获取返回Cookie信息
            if(con!=null && headers!=null && con.getHeaderFields().get("Set-Cookie")!=null){
                Iterator<String> it = con.getHeaderFields().get("Set-Cookie").iterator();
                while(it.hasNext()){
                    JSONObject Cookie = headers.has("Cookie")? (JSONObject) (headers.get("Cookie") instanceof String ? getCookie(headers.getString("Cookie")) : headers.getJSONObject("Cookie")) :new JSONObject();
                    String s = it.next().split(";")[0].trim();
                    String[] sp = s.split("; ")[0].split("=");
                    Cookie.put(sp[0],sp.length>1?sp[1]:"");
                    headers.put("Cookie",Cookie);
                }
            }

            InputStream is = con.getInputStream();
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = is.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            byte[] b = swapStream.toByteArray();
            swapStream.close();
            is.close();
            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 封装String类型的Cookie
     * @param Cookies
     * @return
     */
    public static Map<String, String> getCookie(String Cookies){
//    private JSONObject getCookie(String Cookies) {
        JSONObject Cookie = new JSONObject();
        try {
            String[] arr = Cookies.split(";");
            for(int i=0;i<arr.length;i++){
                Cookie.put(arr[i].split("=")[0].trim(),arr[i].split("=").length>1?arr[i].split("=")[1].trim():"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (Map<String, String>) Cookie;
//        return Cookie;
    }

    RequestConfig getConfig() {
        try {
            return RequestConfig.custom()
                    .setSocketTimeout(10000)
                    .setConnectTimeout(10000)
                    .setConnectionRequestTimeout(10000)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置代理
     * @param ips
     * @return
     */
    public RequestConfig getConfig(String ips) {
        try {
            HttpHost proxy = new HttpHost(ips.split(":")[0].trim(),Integer.parseInt(ips.split(":")[1].trim()));
            return RequestConfig.custom()
                    .setSocketTimeout(10000)
                    .setConnectTimeout(10000)
                    .setConnectionRequestTimeout(10000)
                    .setProxy(proxy)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getEntity(HttpResponse response){
        try {
            return getEntity(response, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getEntity(HttpResponse response,String CharSet){
        try {
            return EntityUtils.toString(response.getEntity(), CharSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**添加请求参数
     * @param form
     * @return
     */
    List<NameValuePair> getParams(JSONObject form) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        try {
            Iterator i = form.keys();
            while(i.hasNext()){
                try {
                    String key = ((String)i.next()).trim();
                    params.add(new BasicNameValuePair(key.trim(),form.getString(key).trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    public static String getLocation(HttpResponse response){
        try{
            Header h = response.getLastHeader("Location");
            if(h!=null){
                return h.getValue().trim();
            }
        }catch (Exception e){
            System.out.println("getLocation(HttpResponse response)异常"+e);
//            error("getLocation(HttpResponse response)异常", e);
        }
        return null;
    }

    public String PostWeiboImage(Object Cookie,byte[] data){
        try{
            CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
            try {
                httpclient.start();
                HttpPost post = new HttpPost("http://picupload.service.weibo.com/interface/pic_upload.php?app=miniblog&data=1&markpos=1&logo=1&marks=1&mime=image/jpeg");
                InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(data));
                post.setEntity(reqEntity);
                JSONObject headers = new JSONObject();
                headers.put("Cookie",Cookie);
                headers.put("Accept","*/*");
                headers.put("Content-Type","application/octet-stream");
                headers.put("X-Requested-With","ShockwaveFlash/18.0.0.232");
                headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.93 Safari/537.36");
                headers.put("Referer","http://js.t.sinajs.cn/t6/home/static/swf/MultiFilesUpload.swf?version=b0b4d2d947d7e5ca");
                addHeaders(headers,null,post);
                Future<HttpResponse> future = httpclient.execute(post,null);
                HttpResponse response = future.get();
                if(response!=null && response.getStatusLine().getStatusCode()==200){
                    String content = getEntity(response);
                    if(content!=null && content.contains("pid")){
                        JSONObject j = new JSONObject(content.split("\n")[1].trim()).getJSONObject("data").getJSONObject("pics").getJSONObject("pic_1");
                        return j.getString("pid");
                    }
                }
            }finally{
                httpclient.close();
            }
        }catch (Exception e){
            System.out.println("异常"+e);
//            error("异常",e);
        }
        return null;
    }
}
