package com.example.back_end.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PaymentGatewayService {

    private static final String DEFAULT_VNP_TMNCODE = "2QXUI4J4";
    private static final String DEFAULT_VNP_HASH_SECRET = "SECRETKEY123456789";
    private static final String DEFAULT_MOMO_PARTNER = "MOMO";
    private static final String DEFAULT_MOMO_ACCESS = "F8BBA842ECF85";
    private static final String DEFAULT_MOMO_SECRET = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    @Value("${payments.vnpay.tmn_code:}") private String vnpTmnCode;
    @Value("${payments.vnpay.hash_secret:}") private String vnpHashSecret;
    @Value("${payments.vnpay.pay_url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") private String vnpPayUrl;
    @Value("${payments.vnpay.return_url:http://localhost:3000/checkout/success}") private String vnpReturnUrl;
    @Value("${payments.vnpay.ipn_url:http://localhost:8081/api/payments/vnpay/ipn}") private String vnpIpnUrl;

    @Value("${payments.momo.partner_code:}") private String momoPartner;
    @Value("${payments.momo.access_key:}") private String momoAccess;
    @Value("${payments.momo.secret_key:}") private String momoSecret;
    @Value("${payments.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}") private String momoEndpoint;
    @Value("${payments.momo.redirect_url:http://localhost:3000/checkout/success}") private String momoRedirectUrl;
    @Value("${payments.momo.ipn_url:http://localhost:8081/api/payments/momo/ipn}") private String momoIpnUrl;

    private final ObjectMapper om = new ObjectMapper();

    public String createVnpayUrl(String orderId, long amountVnd, String orderInfo, String clientIp) {
        String tmnCode = resolvedVnpTmnCode();
        String hashSecret = resolvedVnpHashSecret();

        SortedMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amountVnd * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", clientIp != null ? clientIp : "127.0.0.1");
        params.put("vnp_CreateDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("vnp_Url", vnpPayUrl);
        params.put("vnp_IpnUrl", vnpIpnUrl);

        String query = buildQuery(params);
        String secureHash = hmacSHA512(hashSecret, query);
        return vnpPayUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public String createMomoUrl(String orderId, long amountVnd, String orderInfo) throws Exception {
        String partner = resolvedMomoPartner();
        String access = resolvedMomoAccess();
        String secret = resolvedMomoSecret();

        String requestId = String.valueOf(System.currentTimeMillis());
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("partnerCode", partner);
        payload.put("accessKey", access);
        payload.put("requestId", requestId);
        payload.put("amount", String.valueOf(amountVnd));
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoRedirectUrl);
        payload.put("ipnUrl", momoIpnUrl);
        payload.put("requestType", "captureWallet");
        payload.put("extraData", "");

        String raw = "accessKey=" + access
                + "&amount=" + amountVnd
                + "&extraData="
                + "&ipnUrl=" + momoIpnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partner
                + "&redirectUrl=" + momoRedirectUrl
                + "&requestId=" + requestId
                + "&requestType=captureWallet";
        String signature = hmacSHA256(secret, raw);
        payload.put("signature", signature);

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(momoEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(payload)))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode j = om.readTree(resp.body());
        if (j.has("payUrl")) return j.get("payUrl").asText();
        throw new IllegalStateException("MoMo create failed: " + resp.body());
    }

    private String resolvedVnpTmnCode() {
        return isBlank(vnpTmnCode) ? DEFAULT_VNP_TMNCODE : vnpTmnCode;
    }

    private String resolvedVnpHashSecret() {
        return isBlank(vnpHashSecret) ? DEFAULT_VNP_HASH_SECRET : vnpHashSecret;
    }

    private String resolvedMomoPartner() {
        return isBlank(momoPartner) ? DEFAULT_MOMO_PARTNER : momoPartner;
    }

    private String resolvedMomoAccess() {
        return isBlank(momoAccess) ? DEFAULT_MOMO_ACCESS : momoAccess;
    }

    private String resolvedMomoSecret() {
        return isBlank(momoSecret) ? DEFAULT_MOMO_SECRET : momoSecret;
    }

    private static String buildQuery(SortedMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String,String> e : params.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String hmacSHA512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String hmacSHA256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
}
