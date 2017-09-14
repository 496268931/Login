import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import java.util.*;

public class Httpclient{

    public static String getText() {
        String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int i1 = 5+new Random().nextInt(10);
        String text = "";
        for(int i=0;i<i1;i++){
            text += s.charAt(new Random().nextInt(s.length()));
        }
        return text;
    }

    /**无代理ip请求
     * @param map  请求参数
     * @param hmap  请求消息头
     * @param exeurl  请求地址
     * @return HttpResponse
     */
    public HttpResponse getHttpResponse(Map<String,String> map,Map<String,String> hmap,String exeurl){
        try {
            HttpClient httpclient = new DefaultHttpClient();
            //请求超时
            httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
            //读取超时
            //httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
            HttpPost post = null;
            HttpGet get = null;
            if(map!=null){//post请求
                post = new HttpPost(exeurl);
                List<NameValuePair> nvps = addDate(map);
                post.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));
            }else{//get请求
                get = new HttpGet(exeurl);
            }
            //添加消息头信息
            if(hmap!=null && hmap.size()>0){
                addHeader(post==null?get:post,hmap,post==null?"get":"post");
            }
            //请求
            HttpResponse res = httpclient.execute(post==null?get:post);
            //获取返回的Cookie信息，封装到hmap
            if(hmap!=null){
                getHeader(httpclient,hmap);
            }
            return res;
        } catch (Exception e) {
            System.out.println("无代理IP请求异常—>" + e);
        }
        return null;
    }

    /**代理ip请求
     * @param ips   代理IP
     * @param map  请求参数
     * @param hmap  请求消息头
     * @param exeurl  请求地址
     * @return HttpResponse
     */
    public HttpResponse getHttpResponse(String ips,Map<String,String> map,Map<String,String> hmap,String exeurl){
        try {
            String[] sp = ips.trim().split(":");
            String ip = sp[0].trim();
            int port = Integer.parseInt(sp[1].trim());
            HttpClient httpclient = getHttpClient(ip, port);
            //请求超时
            httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
            //读取超时
            //httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
            HttpPost post = null;
            HttpGet get = null;
            if(map!=null){//post请求
                post = new HttpPost(exeurl);
                List<NameValuePair> nvps = addDate(map);
                post.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));
            }else{//get请求
                get = new HttpGet(exeurl);
            }
            //添加消息头信息
            if(hmap!=null){
                addHeader(post==null?get:post,hmap,post==null?"get":"post");
            }
            //请求
            HttpResponse res = httpclient.execute(post==null?get:post);
            //获取返回的Cookie信息，封装到hmap
            if(hmap!=null){
                getHeader(httpclient,hmap);
            }
            return res;
        } catch (Exception e) {
            System.out.println("代理IP请求异常—>"+exeurl+"\n"+ips+e);
//            logger.error("代理IP请求异常—>"+exeurl+"\n"+ips,e);
        }
        return null;
    }



    /**添加消息头信息
     * @param http
     * @param hmap
     * @param type
     */
    private void addHeader(Object http, Map<String, String> hmap,String type) {
        if(hmap!=null && hmap.size()>0){
            try {
                HttpPost post = (HttpPost) (type.equals("post")?http:null);
                HttpGet get = (HttpGet) (type.equals("get")?http:null);
                Iterator i= hmap.entrySet().iterator();
                String Cookie = "";
                while(i.hasNext()){
                    Map.Entry e=(Map.Entry)i.next();
                    String s = e.getKey().toString().trim();
                    if (s.equals("Accept")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Accept-Encoding")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Accept-Language")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Cache-Control")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Connection")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Host")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Referer")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("User-Agent")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Content-Type")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else if (s.equals("Cookie")) {
                        (type.equals("get") ? get : post).addHeader(e.getKey().toString().trim(), e.getValue().toString().trim());

                    } else {
                        Cookie += e.getKey().toString().trim() + "=" + e.getValue().toString().trim() + ";";

                    }
                }
                if(Cookie.length()>1){
                    (type.equals("get")?get:post).addHeader("Cookie",Cookie);
                }
            } catch (Exception e) {
                System.out.println("异常"+e);
//                logger.error("异常",e);
            }
        }
    }

    /**设置请求代理
     * @param ip
     * @param port  端口
     * @return
     */
    public HttpClient getHttpClient(String ip, int port) {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(ip,port),new UsernamePasswordCredentials("",""));
            HttpHost proxy = new HttpHost(ip,port);
            httpclient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
            return httpclient;
        } catch (Exception e) {
            System.out.println("异常"+e);
//            logger.error("异常",e);
        }
        return null;
    }

    /**获取返回的消息头信息
     * @param httpclient
     * @param hmap
     * @return
     */
    public void getHeader(HttpClient httpclient,Map<String,String> hmap){
        try {
            List<Cookie> cookies = ((AbstractHttpClient)httpclient).getCookieStore().getCookies();
            if(!cookies.isEmpty()){
                for(int i=0;i<cookies.size();i++){
                    hmap.put(cookies.get(i).getName().trim(),cookies.get(i).getValue().trim());
                }
            }
        } catch (Exception e) {
            System.out.println("异常"+e);
//            logger.error("异常", e);
        }
    }

    /**添加参数到集合
     * @param map
     * @return
     */
    private List<NameValuePair> addDate(Map<String, String> map) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        try {
            Iterator i= map.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry e=(Map.Entry)i.next();
                nvps.add(new BasicNameValuePair(e.getKey().toString().trim(),e.getValue().toString().trim()));
            }
        } catch (Exception e) {
            System.out.println("异常"+e);
//            logger.error("异常",e);
        }
        return nvps;
    }
}