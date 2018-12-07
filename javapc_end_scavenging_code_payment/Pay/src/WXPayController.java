package com.andata.controller;

import com.andata.pojo.ProductOrders;
import com.andata.pojo.Products;
import com.andata.service.ProOrdersService;
import com.andata.service.ProductsService;
import com.andata.util.GetIPAdder;
import com.andata.util.QRCodeUtil;
import com.andata.conf.WXpayConfig;
import com.andata.util.testPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayConstants.SignType;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.wxpay.sdk.WXPayUtil.*;

@Controller
public class WXPayController {
    @Resource
    private ProOrdersService proOrdersService;
    @Resource
    private ProductsService productsService;

    /**
     * 支付主接口
     * */
    @RequestMapping(value = "pay")
    public void createQRCode(HttpServletRequest request, HttpServletResponse response,
                             @Param("orderid") String orderid) {
        ServletOutputStream sos = null;
        try {
            String orderInfo = createOrderInfo(orderid);
            String code_url = httpOrder(orderInfo);//调用统一下单接口
            sos = response.getOutputStream();
            QRCodeUtil.encode(code_url, sos);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 生成统一下单格式的订单，XML格式字符串
     *
     * @param orderId
     * @return
     */
    private String createOrderInfo(String orderId) throws Exception {
        return createOrderInfo(orderId, 1);
    }

    private String createOrderInfo(String orderId, Integer productid) throws Exception {
        Products products = productsService.selectByPrimaryKey(Long.valueOf(productid));//商品对象
        ProductOrders productOrders = this.proOrdersService.selectByOrderId(orderId);//订单信息
        //生成订单对象
        Map<String, String> map = new HashMap<>();
        map.put("appid", WXpayConfig.APPID);//公众账号ID
        map.put("mch_id", WXpayConfig.WXPAYMENTACCOUNT);//商户号
        map.put("body", productOrders.getOrderDetails());//商品描述
        map.put("nonce_str", generateUUID());
        map.put("notify_url", WXpayConfig.notify_url);//通知地址
        map.put("out_trade_no", orderId);//订单号
        map.put("spbill_create_ip", GetIPAdder.getMyIP());//终端ip
        map.put("trade_type", "NATIVE");//交易类型
        map.put("total_fee", String.valueOf(productOrders.getTotalPrice()));//总金额
        String sign = createSign(map, WXpayConfig.APIKEY);
        map.put("sign", sign);//签名
        //将订单对象转为xml格式
        String s = null;
        try {
            return mapToXml(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(s.getBytes("UTF-8"));
    }

    /**
     * 调统一下单API
     *
     * @param orderInfo
     * @return
     */
    private String httpOrder(String orderInfo) {
        String url = WXpayConfig.basePath;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            //加入数据
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            BufferedOutputStream buffOutStr = new BufferedOutputStream(conn.getOutputStream());
            buffOutStr.write(orderInfo.getBytes("UTF-8"));
            buffOutStr.flush();
            buffOutStr.close();

            //获取输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Map<String, String> map = xmlToMap(sb.toString());
            String return_msg = map.get("return_msg");
//            System.out.println(return_msg);
            String return_code = map.get("return_code");
            String result_code = map.get("result_code");
            String code_url = map.get("code_url");
            //根据微信文档return_code 和result_code都为SUCCESS的时候才会返回code_url
            if (null != map && "SUCCESS".equals(return_code) && "SUCCESS".equals(result_code)) {
                return code_url;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 微信回调函数
     * 支付成功后微信服务器会调用此方法，修改数据库订单状态
     */
    @RequestMapping(value = "/wxPayCallBack.do")
    @ResponseBody
    public String wxPayCallBack(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("回调成功");
        try {
            InputStream inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            String result = new String(outSteam.toByteArray(), "utf-8");// 获取微信调用我们notify_url的返回信息
            Map<String, String> map = xmlToMap(result);
            if (map.get("result_code").equalsIgnoreCase("SUCCESS")) {
                //返回成功后修改订单状态
                String out_trade_no = map.get("out_trade_no");
                this.proOrdersService.updateByOrderId(out_trade_no);
            }
        } catch (Exception e) {

        }
        return "SUCCESS";
    }

    /**
     * 生成签名
     *
     * @param data 待签名数据
     * @param key  API密钥
     * @return 签名
     */
    public static String createSign(final Map<String, String> data, String key) throws Exception {
        return createSign(data, key, SignType.MD5);
    }

    /**
     * 生成签名. 注意，若含有sign_type字段，必须和signType参数保持一致。
     *
     * @param data     待签名数据
     * @param key      API密钥
     * @param signType 签名方式
     * @return 签名
     */
    private static String createSign(final Map<String, String> data, String key, SignType signType) throws Exception {
        //根据规则创建可排序的map集合
        Set<String> keySet = data.keySet();
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        Arrays.sort(keyArray);
        StringBuilder sb = new StringBuilder();
        for (String k : keyArray) {
            if (k.equals(WXPayConstants.FIELD_SIGN)) {
                continue;
            }
            if (data.get(k).trim().length() > 0) // 参数值为空，则不参与签名
                sb.append(k).append("=").append(data.get(k).trim()).append("&");
        }
        sb.append("key=").append(key);
        //转换UTF-8
        String str = new String(sb.toString().getBytes("UTF-8"));
        if (WXPayConstants.SignType.MD5.equals(signType)) {
            return MD5(sb.toString()).toUpperCase();
        } else if (WXPayConstants.SignType.HMACSHA256.equals(signType)) {
            return HMACSHA256(sb.toString(), key);
        } else {
            throw new Exception(String.format("Invalid sign_type: %s", signType));
        }
    }

}
