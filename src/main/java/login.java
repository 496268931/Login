import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.commons.codec.binary.Base64;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.*;
import org.json.JSONObject;

/**
 * Created by wiseweb on 2017/2/22.
 */
public class login {

    private static HttpClient client;
    private String username;     //登录帐号(明文)
    private String password;     //登录密码(明文)
    private String su;            //登录帐号(Base64加密)
    private String sp;            //登录密码(各种参数RSA加密后的密文)
    private long servertime;    //初始登录时，服务器返回的时间戳,用以密码加密以及登录用
    private String nonce;        //初始登录时，服务器返回的一串字符，用以密码加密以及登录用
    private String rsakv;        //初始登录时，服务器返回的一串字符，用以密码加密以及登录用
    private String pubkey;       //初始登录时，服务器返回的RSA公钥
    private String errInfo;      //登录失败时的错误信息
    private String location;     //登录成功后的跳转连接
    private String pcid;          //登陆验证码图片ID
    private String door;          //登陆验证码
    private JSONObject Cookie;    //登陆成功返回Cookie


    /**
     * 构造函数
     */
    public login(){}

    /**
     * 构造函数(带参数)
     * @param username
     * @param password
     */
    public login(String username, String password) {
        client = new DefaultHttpClient();
        this.username = username;
        this.password = password;
    }


    /**
     * 初始登录信息&lt;br&gt;
     * 返回false说明初始失败
     *
     * @return
     */
    public boolean preLogin() {
        boolean flag = false;
        try {
            su = new String(Base64.encodeBase64(URLEncoder.encode(username, "UTF-8").getBytes()));
            String url = "http://login.sina.com.cn/sso/prelogin.php?entry=weibo&rsakt=mod&checkpin=1&" +
                    "client=ssologin.js(v1.4.18)&_=" + getTimestamp();
            url += "&su=" + su;
            String content;
            content = HttpTools.getRequest(client, url);
            JSONObject json = new JSONObject(content);
            pcid = json.getString("pcid");
            if (json.getInt("showpin") == 1) {
                String imgUrl = "http://login.sina.com.cn/cgi/pin.php?r=" + Math.floor(Math.random() * 100000000) + "&s=0&p=" + pcid;
                HttpResponse res = new HttpClien().Request(null, null, imgUrl);
                String fileName = UUID.randomUUID() + ".png";
                File file = new File(fileName);
                OutputStream out = new FileOutputStream(file);
                InputStream inputStream = res.getEntity().getContent();
                byte b[] = new byte[32 * 1024];
                int j = 0;
                while ((j = inputStream.read(b)) != -1) {
                    out.write(b, 0, j);
                }
                if (out != null) {
                    out.close();
                }
                door = SinaCode.postFile(file);
                if(door.length()!=5){
                    door = SinaCode.postFile(file);
                }
                file.delete();
            }
            servertime = json.getLong("servertime");
            nonce = json.getString("nonce");
            rsakv = json.getString("rsakv");
            pubkey = json.getString("pubkey");
            flag = encodePwd();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 登录（备用）
     *
     * @return true:登录成功
     */
    public boolean login2() {
        if (preLogin()) {
            String url = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)";
            List<NameValuePair> parms = new ArrayList<NameValuePair>();
            parms.add(new BasicNameValuePair("entry", "weibo"));
            parms.add(new BasicNameValuePair("gateway", "1"));
            parms.add(new BasicNameValuePair("from", ""));
            parms.add(new BasicNameValuePair("savestate", "7"));
            parms.add(new BasicNameValuePair("useticket", "1"));
            parms.add(new BasicNameValuePair("pagerefer", "http://login.sina.com.cn/sso/logout.php?entry=miniblog&r=http%3A%2F%2Fweibo.com%2Flogout.php%3Fbackurl%3D%2F"));
            if (door != null) {
                parms.add(new BasicNameValuePair("door", door));
                parms.add(new BasicNameValuePair("pcid", pcid));
            }
            parms.add(new BasicNameValuePair("vsnf", "1"));
            parms.add(new BasicNameValuePair("su", su));
            parms.add(new BasicNameValuePair("service", "miniblog"));
            parms.add(new BasicNameValuePair("servertime", servertime + ""));
            parms.add(new BasicNameValuePair("nonce", nonce));
            parms.add(new BasicNameValuePair("pwencode", "rsa2"));
            parms.add(new BasicNameValuePair("rsakv", rsakv));
            parms.add(new BasicNameValuePair("sp", sp));
            parms.add(new BasicNameValuePair("encoding", "UTF-8"));
            parms.add(new BasicNameValuePair("prelt", "182"));
            parms.add(new BasicNameValuePair("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
            parms.add(new BasicNameValuePair("returntype", "META"));
            try {
                String content = HttpTools.postRequest(client, url, parms);
                String regex = "location.replace\\('([\\s\\S]*?)'\\);";
                System.out.println(regex);
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(content);
                if (m.find()) {
                    location = m.group(1);
                    if (location.contains("reason=")) {
                        errInfo = location.substring(location.indexOf("reason=") + 7);
                        errInfo = URLDecoder.decode(errInfo, "GBK");
                    } else {
                        System.out.println("location = "+location);
                        String result = HttpTools.getRequest(client, location);//.substring(2, location.length()-2)
                        int beginIndex = result.indexOf("(");
                        int endIndex = result.lastIndexOf(")");
                        result = result.substring(beginIndex+1, endIndex);//截取括号里面的json字符串
                        //content = URLDecoder.decode(content, "UTF-8");
                        JSONObject jsonObject = new JSONObject(result);//转换为json
                        //获取uniqueid+userdomain用于访问时带的参数
                        String uniqueid = jsonObject.getJSONObject("userinfo").getString("uniqueid");
                        String userdomain = jsonObject.getJSONObject("userinfo").getString("userdomain");
                        System.out.println("result--------------" + result);
                        return true;
                    }
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
      --授权登录
      --@return

      定义了一个list，该list的数据类型是NameValuePair（简单名称值对节点类型），
      这个代码多处用于Java像url发送Post请求。在发送post请求时用该list来存放参数。
    发送请求的大致过程如下：
    String url="http://www.baidu.com";
    HttpPost httppost=new HttpPost(url); //建立HttpPost对象
    List<NameValuePair> params=new ArrayList<NameValuePair>();
    //建立一个NameValuePair数组，用于存储欲传送的参数
    params.add(new BasicNameValuePair("pwd","2544"));
    //添加参数
    httppost.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
    //设置编码
    HttpResponse response=new DefaultHttpClient().execute(httppost);
    //发送Post,并返回一个HttpResponse对象
     */
    public String authLogin() {
        String ticket = "";
        if (preLogin()) {
            String url = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)";
            List<NameValuePair> parms = new ArrayList<NameValuePair>();
            parms.add(new BasicNameValuePair("entry", "openapi"));
            parms.add(new BasicNameValuePair("gateway", "1"));
            parms.add(new BasicNameValuePair("from", ""));
            parms.add(new BasicNameValuePair("savestate", "0"));
            parms.add(new BasicNameValuePair("useticket", "1"));
            parms.add(new BasicNameValuePair("pagerefer", ""));
            if (door != null) {
                parms.add(new BasicNameValuePair("door", door));
                parms.add(new BasicNameValuePair("pcid", pcid));
            }
            parms.add(new BasicNameValuePair("vsnf", "1"));
            parms.add(new BasicNameValuePair("su", su));
            parms.add(new BasicNameValuePair("service", "miniblog"));
            parms.add(new BasicNameValuePair("servertime", servertime + ""));
            parms.add(new BasicNameValuePair("nonce", nonce));
            parms.add(new BasicNameValuePair("pwencode", "rsa2"));
            parms.add(new BasicNameValuePair("rsakv", rsakv));
            parms.add(new BasicNameValuePair("sp", sp));
            parms.add(new BasicNameValuePair("encoding", "UTF-8"));
            parms.add(new BasicNameValuePair("prelt", "617"));
            parms.add(new BasicNameValuePair("returntype", "TEXT"));
            parms.add(new BasicNameValuePair("domain","weibo.com"));
            parms.add(new BasicNameValuePair("appkey","2F6IC3"));

            try {
                String content = HttpTools.postRequest(client, url, parms);
                JSONObject json = new JSONObject(content);
                if(json.getString("retcode").equals("0")){
                    ticket = json.getString("ticket");
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ticket;
    }


    /**
     * 新浪微博登陆
     * @return true 登陆成功
     */
    public JSONObject login() {
        JSONObject form = new JSONObject();
        JSONObject headers = new JSONObject();
        if (preLogin()) {
            //登录网址
            String url = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)";
            form.put("entry", "weibo");
            form.put("gateway", "1");
            form.put("from", "");
            form.put("savestate", "7");
            form.put("useticket", "0");
            //登出
            form.put("pagerefer", "http://login.sina.com.cn/sso/logout.php?entry=miniblog&r=http%3A%2F%2Fweibo.com%2Flogout.php%3Fbackurl%3D%2F");
            if (door != null) {
                form.put("door", door);
                form.put("pcid", pcid);
            }
            form.put("vsnf", "1");
            form.put("su", su);
            form.put("service", "miniblog");
            form.put("servertime", servertime + "");
            form.put("nonce", nonce);
            form.put("pwencode", "rsa2");
            form.put("rsakv", rsakv);
            form.put("sp", sp);
            form.put("encoding", "UTF-8");
            form.put("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack");
            form.put("returntype", "META");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Referer", "http://weibo.com/");
            HttpResponse response = new HttpClien().Request(form, headers, url);
            String content = new HttpClien().getEntity(response);
            String regex = "location.replace\\('([\\s\\S]*?)'\\);";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(content);
            JSONObject err = new JSONObject();
            err.put("err","err");
            if (m.find()) {
                location = m.group(1);

                System.out.println(location);

                if (location.contains("reason=")) {
                    errInfo = location.substring(location.indexOf("reason=") + 7);
                    try {
                        errInfo = URLDecoder.decode(errInfo, "GBK");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return err;
                } else {
                    try {
                        Cookie = HttpTools.getData(client,location);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Cookie;
                }
            }else{
                return err;
            }
        }
        return null;
    }
    /**
     * 新浪微博登陆
     * @return true 登陆成功
     */
    public JSONObject loginWY() {
        JSONObject form = new JSONObject();
        JSONObject headers = new JSONObject();
        if (preLogin()) {
            //登录网址
            String url = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.18)";
            form.put("entry", "weibo");
            form.put("gateway", "1");
            form.put("from", "");
            form.put("savestate", "7");
            form.put("useticket", "0");
            //登出
            form.put("pagerefer", "http://login.sina.com.cn/sso/logout.php?entry=miniblog&r=http%3A%2F%2Fweibo.com%2Flogout.php%3Fbackurl%3D%2F");
            if (door != null) {
                form.put("door", door);
                form.put("pcid", pcid);
            }
            form.put("vsnf", "1");
            form.put("su", su);
            form.put("service", "miniblog");
            form.put("servertime", servertime + "");
            form.put("nonce", nonce);
            form.put("pwencode", "rsa2");
            form.put("rsakv", rsakv);
            form.put("sp", sp);
            form.put("encoding", "UTF-8");
            form.put("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack");
            form.put("returntype", "META");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Referer", "http://weibo.com/");
            HttpResponse response = new HttpClien().Request(form, headers, url);
            String content = new HttpClien().getEntity(response);
            String regex = "location.replace\\('([\\s\\S]*?)'\\);";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(content);
            JSONObject err = new JSONObject();
            err.put("err","err");
            if (m.find()) {
                location = m.group(1);

                System.out.println(location);


                if (location.contains("reason=")) {
                    errInfo = location.substring(location.indexOf("reason=") + 7);
                    try {
                        errInfo = URLDecoder.decode(errInfo, "GBK");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return err;
                } else {
                    try {
                        Cookie = HttpTools.getData(client,location);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Cookie;
                }
            }else{
                return err;
            }
        }
        return null;
    }


    //加密用js
    private static String sina_js = "var sinaSSOEncoder=sinaSSOEncoder||{};(function(){var hexcase=0;var chrsz=8;this.hex_sha1=function(s){return binb2hex(core_sha1(str2binb(s),s.length*chrsz));};var core_sha1=function(x,len){x[len>>5]|=0x80<<(24-len%32);x[((len+64>>9)<<4)+15]=len;var w=Array(80);var a=1732584193;var b=-271733879;var c=-1732584194;var d=271733878;var e=-1009589776;for(var i=0;i<x.length;i+=16){var olda=a;var oldb=b;var oldc=c;var oldd=d;var olde=e;for(var j=0;j<80;j++){if(j<16)w[j]=x[i+j];else w[j]=rol(w[j-3]^w[j-8]^w[j-14]^w[j-16],1);var t=safe_add(safe_add(rol(a,5),sha1_ft(j,b,c,d)),safe_add(safe_add(e,w[j]),sha1_kt(j)));e=d;d=c;c=rol(b,30);b=a;a=t;}a=safe_add(a,olda);b=safe_add(b,oldb);c=safe_add(c,oldc);d=safe_add(d,oldd);e=safe_add(e,olde);}return Array(a,b,c,d,e);};var sha1_ft=function(t,b,c,d){if(t<20)return(b&c)|((~b)&d);if(t<40)return b^c^d;if(t<60)return(b&c)|(b&d)|(c&d);return b^c^d;};var sha1_kt=function(t){return(t<20)?1518500249:(t<40)?1859775393:(t<60)?-1894007588:-899497514;};var safe_add=function(x,y){var lsw=(x&0xFFFF)+(y&0xFFFF);var msw=(x>>16)+(y>>16)+(lsw>>16);return(msw<<16)|(lsw&0xFFFF);};var rol=function(num,cnt){return(num<<cnt)|(num>>>(32-cnt));};var str2binb=function(str){var bin=Array();var mask=(1<<chrsz)-1;for(var i=0;i<str.length*chrsz;i+=chrsz)bin[i>>5]|=(str.charCodeAt(i/chrsz)&mask)<<(24-i%32);return bin;};var binb2hex=function(binarray){var hex_tab=hexcase?'0123456789ABCDEF':'0123456789abcdef';var str='';for(var i=0;i<binarray.length*4;i++){str+=hex_tab.charAt((binarray[i>>2]>>((3-i%4)*8+4))&0xF)+hex_tab.charAt((binarray[i>>2]>>((3-i%4)*8))&0xF);}return str;};this.base64={encode:function(input){input=''+input;if(input=='')return '';var output='';var chr1,chr2,chr3='';var enc1,enc2,enc3,enc4='';var i=0;do{chr1=input.charCodeAt(i++);chr2=input.charCodeAt(i++);chr3=input.charCodeAt(i++);enc1=chr1>>2;enc2=((chr1&3)<<4)|(chr2>>4);enc3=((chr2&15)<<2)|(chr3>>6);enc4=chr3&63;if(isNaN(chr2)){enc3=enc4=64;}else if(isNaN(chr3)){enc4=64;}output=output+this._keys.charAt(enc1)+this._keys.charAt(enc2)+this._keys.charAt(enc3)+this._keys.charAt(enc4);chr1=chr2=chr3='';enc1=enc2=enc3=enc4='';}while(i<input.length);return output;},_keys:'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='};}).call(sinaSSOEncoder);;(function(){var dbits;var canary=0xdeadbeefcafe;var j_lm=((canary&0xffffff)==0xefcafe);function BigInteger(a,b,c){if(a!=null)if('number'==typeof a)this.fromNumber(a,b,c);else if(b==null && 'string' !=typeof a)this.fromString(a,256);else this.fromString(a,b);}function nbi(){return new BigInteger(null);}function am1(i,x,w,j,c,n){while(--n>=0){var v=x*this[i++]+w[j]+c;c=Math.floor(v/0x4000000);w[j++]=v&0x3ffffff;}return c;}function am2(i,x,w,j,c,n){var xl=x&0x7fff,xh=x>>15;while(--n>=0){var l=this[i]&0x7fff;var h=this[i++]>>15;var m=xh*l+h*xl;l=xl*l+((m&0x7fff)<<15)+w[j]+(c&0x3fffffff);c=(l>>>30)+(m>>>15)+xh*h+(c>>>30);w[j++]=l&0x3fffffff;}return c;}function am3(i,x,w,j,c,n){var xl=x&0x3fff,xh=x>>14;while(--n>=0){var l=this[i]&0x3fff;var h=this[i++]>>14;var m=xh*l+h*xl;l=xl*l+((m&0x3fff)<<14)+w[j]+c;c=(l>>28)+(m>>14)+xh*h;w[j++]=l&0xfffffff;}return c;}BigInteger.prototype.am=am3;dbits=28;BigInteger.prototype.DB=dbits;BigInteger.prototype.DM=((1<<dbits)-1);BigInteger.prototype.DV=(1<<dbits);var BI_FP=52;BigInteger.prototype.FV=Math.pow(2,BI_FP);BigInteger.prototype.F1=BI_FP-dbits;BigInteger.prototype.F2=2*dbits-BI_FP;var BI_RM='0123456789abcdefghijklmnopqrstuvwxyz';var BI_RC=new Array();var rr,vv;rr='0'.charCodeAt(0);for(vv=0;vv<=9;++vv)BI_RC[rr++]=vv;rr='a'.charCodeAt(0);for(vv=10;vv<36;++vv)BI_RC[rr++]=vv;rr='A'.charCodeAt(0);for(vv=10;vv<36;++vv)BI_RC[rr++]=vv;function int2char(n){return BI_RM.charAt(n);}function intAt(s,i){var c=BI_RC[s.charCodeAt(i)];return(c==null)?-1:c;}function bnpCopyTo(r){for(var i=this.t-1;i>=0;--i)r[i]=this[i];r.t=this.t;r.s=this.s;}function bnpFromInt(x){this.t=1;this.s=(x<0)?-1:0;if(x>0)this[0]=x;else if(x<-1)this[0]=x+DV;else this.t=0;}function nbv(i){var r=nbi();r.fromInt(i);return r;}function bnpFromString(s,b){var k;if(b==16)k=4;else if(b==8)k=3;else if(b==256)k=8;else if(b==2)k=1;else if(b==32)k=5;else if(b==4)k=2;else{this.fromRadix(s,b);return;}this.t=0;this.s=0;var i=s.length,mi=false,sh=0;while(--i>=0){var x=(k==8)?s[i]&0xff:intAt(s,i);if(x<0){if(s.charAt(i)=='-')mi=true;continue;}mi=false;if(sh==0)this[this.t++]=x;else if(sh+k>this.DB){this[this.t-1]|=(x&((1<<(this.DB-sh))-1))<<sh;this[this.t++]=(x>>(this.DB-sh));}else  this[this.t-1]|=x<<sh;sh+=k;if(sh>=this.DB)sh-=this.DB;}if(k==8&&(s[0]&0x80)!=0){this.s=-1;if(sh>0)this[this.t-1]|=((1<<(this.DB-sh))-1)<<sh;}this.clamp();if(mi)BigInteger.ZERO.subTo(this,this);}function bnpClamp(){var c=this.s&this.DM;while(this.t>0&&this[this.t-1]==c)--this.t;}function bnToString(b){if(this.s<0)return '-'+this.negate().toString(b);var k;if(b==16)k=4;else if(b==8)k=3;else if(b==2)k=1;else if(b==32)k=5;else if(b==4)k=2;else return this.toRadix(b);var km=(1<<k)-1,d,m=false,r='',i=this.t;var p=this.DB-(i*this.DB)%k;if(i-->0){if(p<this.DB&&(d=this[i]>>p)>0){m=true;r=int2char(d);}while(i>=0){if(p<k){d=(this[i]&((1<<p)-1))<<(k-p);d|=this[--i]>>(p+=this.DB-k);}else{d=(this[i]>>(p-=k))&km;if(p<=0){p+=this.DB;--i;}}if(d>0)m=true;if(m)r+=int2char(d);}}return m?r:'0';}function bnNegate(){var r=nbi();BigInteger.ZERO.subTo(this,r);return r;}function bnAbs(){return(this.s<0)?this.negate():this;}function bnCompareTo(a){var r=this.s-a.s;if(r!=0)return r;var i=this.t;r=i-a.t;if(r!=0)return r;while(--i>=0)if((r=this[i]-a[i])!=0)return r;return 0;}function nbits(x){var r=1,t;if((t=x>>>16)!=0){x=t;r+=16;}if((t=x>>8)!=0){x=t;r+=8;}if((t=x>>4)!=0){x=t;r+=4;}if((t=x>>2)!=0){x=t;r+=2;}if((t=x>>1)!=0){x=t;r+=1;}return r;}function bnBitLength(){if(this.t<=0)return 0;return this.DB*(this.t-1)+nbits(this[this.t-1]^(this.s&this.DM));}function bnpDLShiftTo(n,r){var i;for(i=this.t-1;i>=0;--i)r[i+n]=this[i];for(i=n-1;i>=0;--i)r[i]=0;r.t=this.t+n;r.s=this.s;}function bnpDRShiftTo(n,r){for(var i=n;i<this.t;++i)r[i-n]=this[i];r.t=Math.max(this.t-n,0);r.s=this.s;}function bnpLShiftTo(n,r){var bs=n%this.DB;var cbs=this.DB-bs;var bm=(1<<cbs)-1;var ds=Math.floor(n/this.DB),c=(this.s<<bs)&this.DM,i;for(i=this.t-1;i>=0;--i){r[i+ds+1]=(this[i]>>cbs)|c;c=(this[i]&bm)<<bs;}for(i=ds-1;i>=0;--i)r[i]=0;r[ds]=c;r.t=this.t+ds+1;r.s=this.s;r.clamp();}function bnpRShiftTo(n,r){r.s=this.s;var ds=Math.floor(n/this.DB);if(ds>=this.t){r.t=0;return;}var bs=n%this.DB;var cbs=this.DB-bs;var bm=(1<<bs)-1;r[0]=this[ds]>>bs;for(var i=ds+1;i<this.t;++i){r[i-ds-1]|=(this[i]&bm)<<cbs;r[i-ds]=this[i]>>bs;}if(bs>0)r[this.t-ds-1]|=(this.s&bm)<<cbs;r.t=this.t-ds;r.clamp();}function bnpSubTo(a,r){var i=0,c=0,m=Math.min(a.t,this.t);while(i<m){c+=this[i]-a[i];r[i++]=c&this.DM;c>>=this.DB;}if(a.t<this.t){c-=a.s;while(i<this.t){c+=this[i];r[i++]=c&this.DM;c>>=this.DB;}c+=this.s;}else{c+=this.s;while(i<a.t){c-=a[i];r[i++]=c&this.DM;c>>=this.DB;}c-=a.s;}r.s=(c<0)?-1:0;if(c<-1)r[i++]=this.DV+c;else if(c>0)r[i++]=c;r.t=i;r.clamp();}function bnpMultiplyTo(a,r){var x=this.abs(),y=a.abs();var i=x.t;r.t=i+y.t;while(--i>=0)r[i]=0;for(i=0;i<y.t;++i)r[i+x.t]=x.am(0,y[i],r,i,0,x.t);r.s=0;r.clamp();if(this.s!=a.s)BigInteger.ZERO.subTo(r,r);}function bnpSquareTo(r){var x=this.abs();var i=r.t=2*x.t;while(--i>=0)r[i]=0;for(i=0;i<x.t-1;++i){var c=x.am(i,x[i],r,2*i,0,1);if((r[i+x.t]+=x.am(i+1,2*x[i],r,2*i+1,c,x.t-i-1))>=x.DV){r[i+x.t]-=x.DV;r[i+x.t+1]=1;}}if(r.t>0)r[r.t-1]+=x.am(i,x[i],r,2*i,0,1);r.s=0;r.clamp();}function bnpDivRemTo(m,q,r){var pm=m.abs();if(pm.t<=0)return;var pt=this.abs();if(pt.t<pm.t){if(q!=null)q.fromInt(0);if(r!=null)this.copyTo(r);return;}if(r==null)r=nbi();var y=nbi(),ts=this.s,ms=m.s;var nsh=this.DB-nbits(pm[pm.t-1]);if(nsh>0){pm.lShiftTo(nsh,y);pt.lShiftTo(nsh,r);}else{pm.copyTo(y);pt.copyTo(r);}var ys=y.t;var y0=y[ys-1];if(y0==0)return;var yt=y0*(1<<this.F1)+((ys>1)?y[ys-2]>>this.F2:0);var d1=this.FV/yt,d2=(1<<this.F1)/yt,e=1<<this.F2;var i=r.t,j=i-ys,t=(q==null)?nbi():q;y.dlShiftTo(j,t);if(r.compareTo(t)>=0){r[r.t++]=1;r.subTo(t,r);}BigInteger.ONE.dlShiftTo(ys,t);t.subTo(y,y);while(y.t<ys)y[y.t++]=0;while(--j>=0){var qd=(r[--i]==y0)?this.DM:Math.floor(r[i]*d1+(r[i-1]+e)*d2);if((r[i]+=y.am(0,qd,r,j,0,ys))<qd){y.dlShiftTo(j,t);r.subTo(t,r);while(r[i]<--qd)r.subTo(t,r);}}if(q!=null){r.drShiftTo(ys,q);if(ts!=ms)BigInteger.ZERO.subTo(q,q);}r.t=ys;r.clamp();if(nsh>0)r.rShiftTo(nsh,r);if(ts<0)BigInteger.ZERO.subTo(r,r);}function bnMod(a){var r=nbi();this.abs().divRemTo(a,null,r);if(this.s<0&&r.compareTo(BigInteger.ZERO)>0)a.subTo(r,r);return r;}function Classic(m){this.m=m;}function cConvert(x){if(x.s<0||x.compareTo(this.m)>=0)return x.mod(this.m);else return x;}function cRevert(x){return x;}function cReduce(x){x.divRemTo(this.m,null,x);}function cMulTo(x,y,r){x.multiplyTo(y,r);this.reduce(r);}function cSqrTo(x,r){x.squareTo(r);this.reduce(r);}Classic.prototype.convert=cConvert;Classic.prototype.revert=cRevert;Classic.prototype.reduce=cReduce;Classic.prototype.mulTo=cMulTo;Classic.prototype.sqrTo=cSqrTo;function bnpInvDigit(){if(this.t<1)return 0;var x=this[0];if((x&1)==0)return 0;var y=x&3;y=(y*(2-(x&0xf)*y))&0xf;y=(y*(2-(x&0xff)*y))&0xff;y=(y*(2-(((x&0xffff)*y)&0xffff)))&0xffff;y=(y*(2-x*y%this.DV))%this.DV;return(y>0)?this.DV-y:-y;}function Montgomery(m){this.m=m;this.mp=m.invDigit();this.mpl=this.mp&0x7fff;this.mph=this.mp>>15;this.um=(1<<(m.DB-15))-1;this.mt2=2*m.t;}function montConvert(x){var r=nbi();x.abs().dlShiftTo(this.m.t,r);r.divRemTo(this.m,null,r);if(x.s<0&&r.compareTo(BigInteger.ZERO)>0)this.m.subTo(r,r);return r;}function montRevert(x){var r=nbi();x.copyTo(r);this.reduce(r);return r;}function montReduce(x){while(x.t<=this.mt2)x[x.t++]=0;for(var i=0;i<this.m.t;++i){var j=x[i]&0x7fff;var u0=(j*this.mpl+(((j*this.mph+(x[i]>>15)*this.mpl)&this.um)<<15))&x.DM;j=i+this.m.t;x[j]+=this.m.am(0,u0,x,i,0,this.m.t);while(x[j]>=x.DV){x[j]-=x.DV;x[++j]++;}}x.clamp();x.drShiftTo(this.m.t,x);if(x.compareTo(this.m)>=0)x.subTo(this.m,x);}function montSqrTo(x,r){x.squareTo(r);this.reduce(r);}function montMulTo(x,y,r){x.multiplyTo(y,r);this.reduce(r);}Montgomery.prototype.convert=montConvert;Montgomery.prototype.revert=montRevert;Montgomery.prototype.reduce=montReduce;Montgomery.prototype.mulTo=montMulTo;Montgomery.prototype.sqrTo=montSqrTo;function bnpIsEven(){return((this.t>0)?(this[0]&1):this.s)==0;}function bnpExp(e,z){if(e>0xffffffff||e<1)return BigInteger.ONE;var r=nbi(),r2=nbi(),g=z.convert(this),i=nbits(e)-1;g.copyTo(r);while(--i>=0){z.sqrTo(r,r2);if((e&(1<<i))>0)z.mulTo(r2,g,r);else{var t=r;r=r2;r2=t;}}return z.revert(r);}function bnModPowInt(e,m){var z;if(e<256||m.isEven())z=new Classic(m);else z=new Montgomery(m);return this.exp(e,z);}BigInteger.prototype.copyTo=bnpCopyTo;BigInteger.prototype.fromInt=bnpFromInt;BigInteger.prototype.fromString=bnpFromString;BigInteger.prototype.clamp=bnpClamp;BigInteger.prototype.dlShiftTo=bnpDLShiftTo;BigInteger.prototype.drShiftTo=bnpDRShiftTo;BigInteger.prototype.lShiftTo=bnpLShiftTo;BigInteger.prototype.rShiftTo=bnpRShiftTo;BigInteger.prototype.subTo=bnpSubTo;BigInteger.prototype.multiplyTo=bnpMultiplyTo;BigInteger.prototype.squareTo=bnpSquareTo;BigInteger.prototype.divRemTo=bnpDivRemTo;BigInteger.prototype.invDigit=bnpInvDigit;BigInteger.prototype.isEven=bnpIsEven;BigInteger.prototype.exp=bnpExp;BigInteger.prototype.toString=bnToString;BigInteger.prototype.negate=bnNegate;BigInteger.prototype.abs=bnAbs;BigInteger.prototype.compareTo=bnCompareTo;BigInteger.prototype.bitLength=bnBitLength;BigInteger.prototype.mod=bnMod;BigInteger.prototype.modPowInt=bnModPowInt;BigInteger.ZERO=nbv(0);BigInteger.ONE=nbv(1);function Arcfour(){this.i=0;this.j=0;this.S=new Array();}function ARC4init(key){var i,j,t;for(i=0;i<256;++i)this.S[i]=i;j=0;for(i=0;i<256;++i){j=(j+this.S[i]+key[i%key.length])&255;t=this.S[i];this.S[i]=this.S[j];this.S[j]=t;}this.i=0;this.j=0;}function ARC4next(){var t;this.i=(this.i+1)&255;this.j=(this.j+this.S[this.i])&255;t=this.S[this.i];this.S[this.i]=this.S[this.j];this.S[this.j]=t;return this.S[(t+this.S[this.i])&255];}Arcfour.prototype.init=ARC4init;Arcfour.prototype.next=ARC4next;function prng_newstate(){return new Arcfour();}var rng_psize=256;var rng_state;var rng_pool;var rng_pptr;function rng_seed_int(x){rng_pool[rng_pptr++]^=x&255;rng_pool[rng_pptr++]^=(x>>8)&255;rng_pool[rng_pptr++]^=(x>>16)&255;rng_pool[rng_pptr++]^=(x>>24)&255;if(rng_pptr>=rng_psize)rng_pptr-=rng_psize;}function rng_seed_time(){rng_seed_int(new Date().getTime());}if(rng_pool==null){rng_pool=new Array();rng_pptr=0;var t;while(rng_pptr<rng_psize){t=Math.floor(65536*Math.random());rng_pool[rng_pptr++]=t>>>8;rng_pool[rng_pptr++]=t&255;}rng_pptr=0;rng_seed_time();}function rng_get_byte(){if(rng_state==null){rng_seed_time();rng_state=prng_newstate();rng_state.init(rng_pool);for(rng_pptr=0;rng_pptr<rng_pool.length;++rng_pptr)rng_pool[rng_pptr]=0;rng_pptr=0;}return rng_state.next();}function rng_get_bytes(ba){var i;for(i=0;i<ba.length;++i)ba[i]=rng_get_byte();}function SecureRandom(){}SecureRandom.prototype.nextBytes=rng_get_bytes;function parseBigInt(str,r){return new BigInteger(str,r);}function linebrk(s,n){var ret='';var i=0;while(i+n<s.length){ret+=s.substring(i,i+n)+'\\n';i+=n;}return ret+s.substring(i,s.length);}function byte2Hex(b){if(b<0x10)return '0'+b.toString(16);else  return b.toString(16);}function pkcs1pad2(s,n){if(n<s.length+11){return null;}var ba=new Array();var i=s.length-1;while(i>=0&&n>0){var c=s.charCodeAt(i--);if(c<128){ba[--n]=c;}else if((c>127)&&(c<2048)){ba[--n]=(c&63)|128;ba[--n]=(c>>6)|192;}else{ba[--n]=(c&63)|128;ba[--n]=((c>>6)&63)|128;ba[--n]=(c>>12)|224;}}ba[--n]=0;var rng=new SecureRandom();var x=new Array();while(n>2){x[0]=0;while(x[0]==0)rng.nextBytes(x);ba[--n]=x[0];}ba[--n]=2;ba[--n]=0;return new BigInteger(ba);}function RSAKey(){this.n=null;this.e=0;this.d=null;this.p=null;this.q=null;this.dmp1=null;this.dmq1=null;this.coeff=null;}function RSASetPublic(N,E){if(N!=null&&E!=null&&N.length>0&&E.length>0){this.n=parseBigInt(N,16);this.e=parseInt(E,16);}else alert('Invalid RSA public key');}function RSADoPublic(x){return x.modPowInt(this.e,this.n);}function RSAEncrypt(text){var m=pkcs1pad2(text,(this.n.bitLength()+7)>>3);if(m==null)return null;var c=this.doPublic(m);if(c==null)return null;var h=c.toString(16);if((h.length&1)==0)return h;else return '0'+h;}RSAKey.prototype.doPublic=RSADoPublic;RSAKey.prototype.setPublic=RSASetPublic;RSAKey.prototype.encrypt=RSAEncrypt;this.RSAKey=RSAKey;}).call(sinaSSOEncoder);function getpass(pwd,servicetime,nonce,rsaPubkey){var RSAKey=new sinaSSOEncoder.RSAKey();RSAKey.setPublic(rsaPubkey,'10001');var password=RSAKey.encrypt([servicetime,nonce].join('\\t')+'\\n'+pwd);return password;}";

    /**
     * 密码加密
     * @return
     */
    private boolean encodePwd() {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("javascript");
        try {
            //用js加密密码,RSA,调用js内方法 我这里使用的是字符串 也可以直接放入文件中然后读取，如下面注释部分。
            se.eval(sina_js);
            //调用js内部函数用于加密
            if (se instanceof Invocable) {
                Invocable iv = (Invocable) se;
                sp = (String) iv.invokeFunction("getpass", this.password, this.servertime, this.nonce,
                        this.pubkey);
            }
            return true;
        } catch (ScriptException e) {
            //TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            //TODO Auto-generated catch block
            e.printStackTrace();
        }
        errInfo = "密码加密失败!";
        return false;
    }

    /**
     * 获取时间戳
     *
     * @return
     */
    private long getTimestamp() {
        Date now = new Date();
        return now.getTime();
    }



    /**
     * main方法
     * @param args
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void    main(String[] args) throws ClientProtocolException, IOException {
        login weibo = new login("dcu1234947@sina.cn", "vadjnrwa1701u");
        //login weibo = new login("rlp390660@sina.cn", "4B1ivdq92b30");
        //login weibo = new login("kva84723526@sina.cn", "EO1iRkiNLBPy");
        //login weibo = new login("15233615992", "rxs1031109920");
        JSONObject cookie = weibo.login();
        System.out.println(cookie);
        //JSONObject res = new WeiboWebHttpClient().weiboAdd(cookie, "Good Morning!", null);
        //JSONObject res = new WeiboWebHttpClient().commentSupport(cookie,"4080843733461489");
        //System.out.println(res);

        //login weibo = new login("rlp390660@sina.cn", "4B1ivdq92b30");
        //System.out.println(weibo.auth1());
        //System.out.println(weibo.authLogin());//获取ticket

       /* JSONObject l1 = weibo.loginWY();
        System.out.println(l1);
        System.out.println("!!!!!");

        boolean l2 = weibo.login2();
        System.out.println(l2);*/


    }

    public String auth1() {
        String ticket =  authLogin();
        System.out.println(ticket);


        String url = "https://api.weibo.com/oauth2/authorize?client_id=1651601463&redirect_uri=http%3A%2F%2Fsns.whalecloud.com%2Fsina%2Fcallback%3Fimei%3Dc1cbc99be4cf339b14869bb91d963cf%26appkey%3D541cf68dfd98c51895027d3c%26key%3D1651601463%26secret%3D84502c2a408c8de5a1c6b7f76d574a84%26pcv%3D2.0&display=default&forcelogin=true&fc=umeng&response_type=code&scope=all&with_offical_account=1&pcv=2.0";//橘子娱乐登录平台
        //String url = "https://api.weibo.cn/oauth2/authorize";
        //String url = "https://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.15)";


        JSONObject headers = new JSONObject();
        JSONObject form  = new JSONObject();


        //headers.put("Host","api.weibo.com");
        //headers.put("Origin:","https://api.weibo.com");
        headers.put("User-Agent","Mozilla/5.0 (Linux; Android 6.0.1; SM919 Build/MXB48T; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.11 Mobile Safari/537.36");
        //headers.put("Referer",url);//橘子娱乐登录微博地址
        //headers.put("Content-Type","application/x-www-form-urlencoded");


        //form.put("action","login");
        form.put("action","submit");
        form.put("display","default");//form.put("display","mobile");
        form.put("wisthOfficeFlag","0");
        form.put("quick_auth","null");
        form.put("withOfficalAccount","");
        form.put("scope","");
        form.put("ticket",ticket);
        form.put("isLoginSina","");
        form.put("response_type","code");

        //form.put("quick_auth","false");


        //form.put("regCallback","https%3A%2F%2Fapi.weibo.com%2F2%2Foauth2%2Fauthorize%3Fclient_id%3D1651601463%26response_type%3Dcode%26display%3Dmobile%26redirect_uri%3Dhttp%253A%252F%252Fsns.whalecloud.com%252Fsina%252Fcallback%253Fimei%253Dc1cbc99be4cf339b14869bb91d963cf%2526amp%253Bappkey%253D541cf68dfd98c51895027d3c%2526amp%253Bkey%253D1651601463%2526amp%253Bsecret%253D84502c2a408c8de5a1c6b7f76d574a84%2526amp%253Bpcv%253D2.0%26from%3D%26with_cookie%3D");
        form.put("regCallback","https://api.weibo.com/2/oauth2/authorize?client_id=541cf68dfd98c51895027d3c&response_type=code&display=default&redirect_uri=http://sns.whalecloud.com/sina/callback?imei=c1cbc99be4cf339b14869bb91d963cf&appkey=541cf68dfd98c51895027d3c&key=1651601463&secret=84502c2a408c8de5a1c6b7f76d574a84&pcv=2.0&from=&with_cookie=");
        form.put("redirect_uri","http://sns.whalecloud.com/sina/callback?imei=c1cbc99be4cf339b14869bb91d963cf&appkey=541cf68dfd98c51895027d3c&key=1651601463&secret=84502c2a408c8de5a1c6b7f76d574a84&pcv=2.0");

        form.put("client_ID","1651601463");
        form.put("appkey62","2F6IC3");
        form.put("state","");
        form.put("verifyToken","null");
        form.put("from","");
        //form.put("switchLogin",0);
        form.put("userId","rlp390660@sina.cn");
        form.put("passwd","4B1ivdq92b30");


        //form.put("redirect_uri","https://api.weibo.com/oauth2/default.html");



        //form.put("client_ID","3187206780");
        //form.put("client_SERCRET","ea71f3b5ff7061e0ce9fc3a3b3037556");
        //form.put("client_SERCRET","84502c2a408c8de5a1c6b7f76d574a84");





        //form.put("offcialMobile","null");

        //form.put("version","");
        //form.put("sso_type","");
        try {
            //String content = new HttpClientUtil().httpPostRequest(url,form,headers);
            //System.out.println(content);
            String post_url="https://api.weibo.com/oauth2/authorize";
            HttpResponse res = new HttpClien().Request(headers,form,url);


            //System.out.println(res);
            //System.out.println(headers);
            //System.out.println(form);

            //String location=res.getLastHeader("Location").toString();
            //System.out.println(location);





            String content = Util.getContent(res);

            System.out.println(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String auth() {
        String ticket =  authLogin();
        String url = "https://api.weibo.cn/oauth2/authorize";
        JSONObject headers = new JSONObject();
        JSONObject form  = new JSONObject();
        headers.put("Host","api.weibo.com");
        headers.put("Origin:","https://api.weibo.com");
        headers.put("User-Agent","Mozilla/5.0 (Linux; Android 6.0.1; SM919 Build/MXB48T; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.11 Mobile Safari/537.36");
        headers.put("Referer","https://api.weibo.com/oauth2/authorize?client_id=1651601463&redirect_uri=http%3A%2F%2Fsns.whalecloud.com%2Fsina%2Fcallback%3Fimei%3Dc1cbc99be4cf339b14869bb91d963cf%26appkey%3D541cf68dfd98c51895027d3c%26key%3D1651601463%26secret%3D84502c2a408c8de5a1c6b7f76d574a84%26pcv%3D2.0&display=mobile&forcelogin=true&fc=umeng&response_type=code&scope=all&with_offical_account=1&pcv=2.0");//橘子娱乐登录微博地址

        form.put("display","mobile");//form.put("display","client");我写的
        form.put("action","login");
        form.put("ticket",ticket);
        form.put("scope","all");
        form.put("isLoginSina","");
        form.put("wisthOfficeFlag","0");
        form.put("quick_auth","false");
        form.put("withOfficalAccount","checked");
        form.put("response_type","code");
        form.put("regCallback","https%3A%2F%2Fapi.weibo.com%2F2%2Foauth2%2Fauthorize%3Fclient_id%3D1651601463%26response_type%3Dcode%26display%3Dmobile%26redirect_uri%3Dhttp%253A%252F%252Fsns.whalecloud.com%252Fsina%252Fcallback%253Fimei%253Dc1cbc99be4cf339b14869bb91d963cf%2526amp%253Bappkey%253D541cf68dfd98c51895027d3c%2526amp%253Bkey%253D1651601463%2526amp%253Bsecret%253D84502c2a408c8de5a1c6b7f76d574a84%2526amp%253Bpcv%253D2.0%26from%3D%26with_cookie%3D");

        form.put("redirect_uri","https://api.weibo.com/oauth2/default.html");
        //form.put("redirect_uri","http://sns.whalecloud.com/sina/callback?imei=c1cbc99be4cf339b14869bb91d963cf&appkey=541cf68dfd98c51895027d3c&key=1651601463&secret=84502c2a408c8de5a1c6b7f76d574a84&pcv=2.0");

        //form.put("client_id","1651601463");
        form.put("client_ID","3187206780");
        form.put("client_SERCRET","ea71f3b5ff7061e0ce9fc3a3b3037556");
        form.put("appkey62","2F6IC3");
        form.put("state","");
        form.put("from","");
        form.put("offcialMobile","null");
        form.put("verifyToken","null");
        form.put("version","");
        form.put("sso_type","");
        try {
            //String content = new HttpClientUtil().httpPostRequest(url,form,headers);
            //System.out.println(content);
            HttpResponse res = new HttpClien().Request(headers,form,url);


            //System.out.println(res);
            //System.out.println(headers);
            //System.out.println(form);


            String content = Util.getContent(res);
            System.out.println(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public void loginApp(){
        boolean flag = preLogin();
        String url = "http://api.weibo.cn/2/account/login?uicode=10000058&c=android&p=jjZWmebZN7aZ821mz8w3t1HtDntkCHz76Q%2F9W1iwg5zmONXgLgFs6GwB6i0i3onIp4iSFhBqhqwJuyrMYAKKqMC%2FaeEj3OVHvHy8sBvFMxAcnbUvoymIlP2HrAP%2BZcjMLPk8egSEb%2FQoaFu5p6Goy4vKa6sTgVZtNkxXfmjAqvw%3D&s=148cf864&u=15233615992";
        Map headers = new HashMap();
        Map form = new HashMap();
//        form.put("s","148cf864");
        try {
            String content = new HttpClientUtil().httpPostRequest(url,form,headers);
            System.out.println(content);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}

