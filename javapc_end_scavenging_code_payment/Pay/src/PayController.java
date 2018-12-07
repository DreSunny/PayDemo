package com.andata.controller;
import com.andata.pojo.ProductOrders;
import com.andata.pojo.Products;
import com.andata.pojo.UnifiedOrderRequest;
import com.andata.service.CorrectService;
import com.andata.service.ProOrdersService;
import com.andata.service.ProductsService;
import com.andata.util.QRCodeUtil;
import com.andata.util.testPay;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConfigImpl;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.wxpay.sdk.WXPayUtil.generateUUID;
import static com.github.wxpay.sdk.WXPayUtil.xmlToMap;

/**
 * 与支付无关的逻辑
 * */
@Controller
public class PayController {
    @Resource
    private ProOrdersService proOrdersService;
    /**
     * 调用接口 生成业务订单信息保存在数据库，并返回订单号
     *
     * @param filetxt
     * @return ordernum
     */
    @RequestMapping(value = "getOrder.do")
    @ResponseBody
    public Map getorder(HttpServletRequest request, @Param("filetxt") String filetxt) {
        //获取当前用户
        String username = (String) request.getSession().getAttribute("username");
        ProductOrders productOrders = new ProductOrders();
        productOrders.setUserId(username);//用户
        productOrders.setOrdernumber(getOutTradeNo());//订单号
        productOrders.setProductId("XB001");//商品
        int wordnum = filetxt.trim().length();//字数
        productOrders.setQuantity(wordnum);//数量
        Integer pay1 = testPay.getPay1(wordnum);//计算价格
        productOrders.setTotalPrice(pay1);//总价
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String format = df.format(new Date());//日期格式转换
        productOrders.setOrdertime(format);
        productOrders.setOrderDetails(filetxt);//文章内容
        productOrders.setStatus(0);
        //设置订单详情格式
        try {
            int insert = proOrdersService.insert(productOrders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> map = new HashMap<>();
        //封装返回值
        map.put("orderid", productOrders.getOrdernumber());//订单号
        return map;
    }
    /**
     * 查询订单信息
     *
     * @param orderid
     * @return filetxt
     */
    @RequestMapping(value = "selectOrder.do")
    @ResponseBody
    public Map selectOrder(@Param("orderid") String orderid) {
        ProductOrders productOrders = this.proOrdersService.selectByOrderId(orderid);
        Map<String, Object> map = new HashMap<>();
        map.put("wordnum", productOrders.getQuantity());
        map.put("totelprice", productOrders.getTotalPrice());
        map.put("filetxt", productOrders.getOrderDetails());
        return map;
    }
    /**
     * 验证支付状态
     *
     * @Param orderid
     */
    @RequestMapping(value = "OrderStatus.do")
    @ResponseBody
    public Map SelectOrderStatus(HttpServletRequest request, @Param("orderid") String orderid) {
        Map<String, Object> map = new HashMap<>();
        int i = this.proOrdersService.selectOrderStatus(orderid);
        if (i == 1)//支付成功
        {
            map.put("type", "SUCCESS");
            return map;
        }
        map.put("type", "FAIL");
        return map;
    }
    /**
     * 生成16位随机订单号
     *
     * @return key
     */
    private static String getOutTradeNo() {
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss", Locale.getDefault());
        Date date = new Date();
        String key = format.format(date);
        Random r = new Random();
        key = key + r.nextInt();
        key = key.replaceAll("-", "").substring(0, 15);
        return key;
    }
//    private static String APPID="wx830c4d2c6a666923";//微信公众号APPID
//    private static String WXPAYMENTACCOUNT="1497867872";//微信公众号的商户号
//    private static String APIKEY="0F1DBAD6B1E7D6A23E49B938152D58DE";//微信公众号的商户支付密钥
//    private static String basePath="https://api.mch.weixin.qq.com/pay/unifiedorder";
//    private static String notify_url="http://www.andata.com.cn/wxPayCallBack.do";
//    @Resource
//    private ProOrdersService proOrdersService;
//    @Resource
//    private ProductsService productsService;
//    @Resource
//    private CorrectService correctService;
//    private WXPayConfigImpl config;
//    private WXPay wxpay=null;
//    public PayController () throws Exception {
//        config = WXPayConfigImpl.getInstance();
//        wxpay = new WXPay(config);
//    }
//    /**
//     * 查询订单信息
//     * @param orderid
//     * @return wordnum
//     * @return totelprice
//     * */
//    @RequestMapping(value = "selectOrder.do")
//    @ResponseBody
//    public Map selectOrder(@Param("orderid")String orderid)
//    {
//        ProductOrders productOrders = this.proOrdersService.selectByOrderId(orderid);
//        Map<String,Object> map=new HashMap<>();
//        map.put("wordnum",productOrders.getQuantity());
//        map.put("totelprice",productOrders.getTotalPrice());
//        map.put("filetxt",productOrders.getOrderDetails());
//        return map;
//    }
//    /**
//     * 调用接口 生成业务订单信息保存在数据库，并返回订单号
//     * @Param username
//     * @return ordernum
//     * */
//    @RequestMapping(value = "getOrder.do")
//    @ResponseBody
//    public Map getorder(HttpServletRequest request,@Param("filetxt")String filetxt)
//    {
//        //获取当前用户
//        String username =(String) request.getSession().getAttribute("username");
//        ProductOrders productOrders=new ProductOrders();
//        productOrders.setUserId(username);//用户
//        productOrders.setOrdernumber(getOutTradeNo());//订单号16位随机数字
//        productOrders.setProductId("XB001");//商品
//        int wordnum = filetxt.trim().length();//字数
//        productOrders.setQuantity(wordnum);//数量
//        Integer pay1 = testPay.getPay1(wordnum);//计算价格
//        productOrders.setTotalPrice(pay1);//总价
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
//        String format = df.format(new Date());//日期格式转换
//        productOrders.setOrdertime(format);
//        productOrders.setOrderDetails(filetxt);//文章内容
//        productOrders.setStatus(0);
//        //设置订单详情格式
//        try{
//            int insert = proOrdersService.insert(productOrders);
//        }catch (Exception e)
//        {
//            System.out.println(e);
//        }
//        Map<String,Object> map=new HashMap<>();
//        //封装返回值
//        map.put("orderid",productOrders.getOrdernumber());//订单号
//        return map;
//    }
//
//    /**
//     * 生成16位随机订单号
//     * @return key
//     * */
//    private static String getOutTradeNo() {
//        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss", Locale.getDefault());
//        Date date = new Date();
//        String key = format.format(date);
//        Random r = new Random();
//        key = key + r.nextInt();
//        key = key.replaceAll("-","").substring(0, 15);
//        return key;
//    }
//    /**
//     * 生成二维码
//     * */
//    @RequestMapping(value = "/pay")
//    @ResponseBody
//    public void payMain(HttpServletRequest request,HttpServletResponse response)
//    {
//        String ordernum = request.getParameter("orderid");//订单号
//        String url_code = createOrderInfo(ordernum);
//        String code_url = url_code.split(",")[1].replace("code_url=","");
//        ServletOutputStream sos = null;
//        try {
//            sos = response.getOutputStream();
//            QRCodeUtil.encode(code_url, sos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * 验证支付
//     * @Param ordernum
//     * */
//    @RequestMapping(value = "OrderStatus.do")
//    @ResponseBody
//    public Map SelectOrder(HttpServletRequest request,@Param("orderid") String orderid)
//    {
//        Map<String,Object> map=new HashMap<>();
//        int i = this.proOrdersService.selectOrderStatus(orderid);
//        if (i==1)//支付成功
//        {
//            map.put("type","SUCCESS");
//            return map;
//        }
//        map.put("type","FAIL");
//        return map;
//    }
//
//    /**
//     * 微信回调函数
//     * 支付成功后微信服务器会调用此方法，修改数据库订单状态
//     * */
//    @RequestMapping(value="/wxPayCallBack.do")
//    @ResponseBody
//    public String wxPayCallBack(HttpServletRequest request, HttpServletResponse response) {
//        System.out.println("回调成功");
//        try {
//            InputStream inStream = request.getInputStream();
//            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
//            byte[] buffer = new byte[1024];
//            int len = 0;
//            while ((len = inStream.read(buffer)) != -1) {
//                outSteam.write(buffer, 0, len);
//            }
//            outSteam.close();
//            inStream.close();
//            String result = new String(outSteam.toByteArray(), "utf-8");// 获取微信调用我们notify_url的返回信息
//            Map<String, String> map = xmlToMap(result);
//            if (map.get("result_code").equalsIgnoreCase("SUCCESS")) {
//                //返回成功后修改订单状态
//                String out_trade_no = map.get("out_trade_no");
//                this.proOrdersService.updateByOrderId(out_trade_no);
//            }
//        } catch (Exception e) {
//
//        }
//        return "SUCCESS";
//    }
//    /**
//     * 调起支付接口
//     * */
//    private  Map<String,String> OrderToMap(UnifiedOrderRequest unifiedOrderRequest){
//        Map<String,String> map=new HashMap<>();
//        map.put("body", unifiedOrderRequest.getBody());
//        map.put("out_trade_no", unifiedOrderRequest.getOut_trade_no());//订单号
//        map.put("total_fee", unifiedOrderRequest.getTotal_fee());//总金额
//        map.put("spbill_create_ip", unifiedOrderRequest.getSpbill_create_ip());//终端ip
//        map.put("notify_url", unifiedOrderRequest.getNotify_url());//通知地址
//        map.put("trade_type", unifiedOrderRequest.getTrade_type());//交易类型
//        Map<String, String> returnmap =null;
//        try {
//            returnmap = wxpay.unifiedOrder(map);//调用统一下单api，向Wxpay请求数据
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return returnmap;
//    }
//    /**
//     * 生成订单
//     * 一、以上为业务需求的订单信息，就是发起支付前的订单信息，业务系统自行创建存储；
//     * 二、本方法为满足统一下单API要求的订单信息，
//     * @param orderId
//     * @auther sunny
//     * */
//    public  String createOrderInfo(String orderId)  {
//        //获取小编商品对象
//        Products products = productsService.selectByPrimaryKey(Long.valueOf(1));
//        //生成订单对象
//        UnifiedOrderRequest unifiedOrderRequest = new UnifiedOrderRequest();
//        unifiedOrderRequest.setAppid(APPID);//公众账号ID
//        unifiedOrderRequest.setMch_id(WXPAYMENTACCOUNT);//商户号
//        unifiedOrderRequest.setNonce_str(generateUUID());//随机字符串,调用微信sdk自带uuid方法
//        unifiedOrderRequest.setBody(products.getProductdetails());//商品描述
//        unifiedOrderRequest.setOut_trade_no(orderId);//商户订单号
//        //从数据库根据订单号查询金额
//        ProductOrders productOrders = this.proOrdersService.selectByOrderId(orderId);
//        unifiedOrderRequest.setTotal_fee(String.valueOf(productOrders.getTotalPrice()));//金额需要扩大100倍:1代表支付时是0.01
//        unifiedOrderRequest.setSpbill_create_ip("192.168.1.103");//用户终端IP
//        unifiedOrderRequest.setNotify_url(notify_url);//回调方法
//        unifiedOrderRequest.setTrade_type("NATIVE");//JSAPI--公众号支付、NATIVE--原生扫码支付、APP--app支付
//        unifiedOrderRequest.setSign(createSign(unifiedOrderRequest));//生成签名
//        Map<String, String> map = OrderToMap(unifiedOrderRequest);
//        return map.toString();
//    }
//    /**
//     * 生成签名
//     * @auther sunny
//     */
//    private String createSign(UnifiedOrderRequest unifiedOrderRequest) {
//        //根据规则创建可排序的map集合
//        SortedMap<String, String> packageParams = new TreeMap<String, String>();
//        packageParams.put("appid", unifiedOrderRequest.getAppid());
//        packageParams.put("mch_id", unifiedOrderRequest.getMch_id());
//        packageParams.put("body", unifiedOrderRequest.getBody());
//        packageParams.put("nonce_str", unifiedOrderRequest.getNonce_str());
//        packageParams.put("notify_url", unifiedOrderRequest.getNotify_url());
//        packageParams.put("out_trade_no", unifiedOrderRequest.getOut_trade_no());
//        packageParams.put("spbill_create_ip", unifiedOrderRequest.getSpbill_create_ip());
//        packageParams.put("trade_type", unifiedOrderRequest.getTrade_type());
//        packageParams.put("total_fee", unifiedOrderRequest.getTotal_fee());
//        StringBuffer sb = new StringBuffer();
//        Set es = packageParams.entrySet();//字典序
//        Iterator it = es.iterator();
//        while (it.hasNext()) {
//            Map.Entry entry = (Map.Entry) it.next();
//            String k = (String) entry.getKey();
//            String v = (String) entry.getValue();
//            //为空不参与签名、参数名区分大小写
//            if (null != v && !"".equals(v) && !"sign".equals(k)
//                    && !"key".equals(k)) {
//                sb.append(k + "=" + v + "&");
//            }
//        }
//        //第二步拼接key，key设置路径：微信商户平台(pay.weixin.qq.com)-->账户设置-->API安全-->密钥设置
//        sb.append("key=" +APIKEY);
//        String sign = null;
//        try {
//            sign = WXPayUtil.MD5(sb.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return sign;
//    }
}

