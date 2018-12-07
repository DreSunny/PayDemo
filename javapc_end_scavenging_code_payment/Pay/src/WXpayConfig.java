package com.andata.conf;

public class WXpayConfig {
    public static String APPID = "wx830c4d2c6a666923";//微信公众号APPID
    public static String WXPAYMENTACCOUNT = "1497867872";//微信公众号的商户号
    public static String APIKEY = "0F1DBAD6B1E7D6A23E49B938152D58DE";//微信公众号的商户支付密钥
    public static String basePath = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    public static String notify_url = "http://www.andata.com.cn/wxPayCallBack.do";
}
