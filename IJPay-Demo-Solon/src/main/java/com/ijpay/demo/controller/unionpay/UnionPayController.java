package com.ijpay.demo.controller.unionpay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ijpay.core.enums.SignType;
import com.ijpay.core.kit.HttpKit;
import com.ijpay.core.kit.IpKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.demo.entity.UnionPayBean;
import com.ijpay.demo.vo.AjaxResult;
import com.ijpay.unionpay.UnionPayApi;
import com.ijpay.unionpay.enums.ServiceEnum;
import com.ijpay.unionpay.model.*;
import com.jfinal.kit.StrKit;
import org.noear.solon.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@Mapping("/unionPay")
public class UnionPayController {
	private static final Logger logger = LoggerFactory.getLogger(UnionPayController.class);

	@Inject
	UnionPayBean unionPayBean;

	@Mapping("")
	public String index() {
		logger.info("欢迎使用 IJPay 中的云闪付	- by Javen");
		return "欢迎使用 IJPay 中的云闪付	- by Javen";
	}

	@Mapping("test")
	public UnionPayBean test() {
		return unionPayBean;
	}

	/**
	 * 刷卡支付
	 */

	@Get
	@Post
	@Mapping("/microPay")
	public AjaxResult microPay(HttpServletRequest request, HttpServletResponse response) {
		try {
			String authCode = request.getParameter("authCode");
			String totalFee = request.getParameter("totalFee");

			String ip = IpKit.getRealIp(request);
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = MicroPayModel.builder()
				.service(ServiceEnum.MICRO_PAY.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付测试")
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.auth_code(authCode)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			logger.info("请求参数:" + JSONUtil.toJsonStr(params));

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			String returnCode = result.get("status");
			String resultCode = result.get("result_code");
			String errMsg = result.get("err_msg");
			String errCode = result.get("err_code");

			if (!"0".equals(returnCode) || !"0".equals(resultCode)) {
				return new AjaxResult().addError("errCode:" + errCode + " errMsg:" + errMsg);
			}
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/queryOrder")
	public AjaxResult query(@Param(value = "transactionId", required = false) String transactionId,
							@Param(value = "outTradeNo", required = false) String outTradeNo) {
		try {

			if (StrUtil.isEmpty(transactionId) && StrUtil.isEmpty(outTradeNo)) {
				return new AjaxResult().addError("out_trade_no transaction_id 不能同时为空");
			}

			Map<String, String> params = OrderQueryModel.builder()
				.service(ServiceEnum.QUERY.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(outTradeNo)
				.transaction_id(transactionId)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			if (!WxPayKit.verifyNotify(result, unionPayBean.getKey(), SignType.MD5)) {
				return new AjaxResult().addError("签名异常");
			}
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/microPayReverse")
	public AjaxResult microPayReverse(@Param("outTradeNo") String outTradeNo) {
		try {

			Map<String, String> params = OrderQueryModel.builder()
				.service(ServiceEnum.MICRO_PAY_REVERSE.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(outTradeNo)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/refund")
	public AjaxResult refund(@Param(value = "transactionId", required = false) String transactionId,
							 @Param(value = "outTradeNo", required = false) String outTradeNo,
							 @Param(value = "totalFee") String totalFee,
							 @Param(value = "refundFee") String refundFee) {
		try {

			if (StrUtil.isEmpty(transactionId) && StrUtil.isEmpty(outTradeNo)) {
				return new AjaxResult().addError("out_trade_no transaction_id 不能同时为空");
			}

			Map<String, String> params = RefundModel.builder()
				.service(ServiceEnum.REFUND.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(outTradeNo)
				.transaction_id(transactionId)
				.out_refund_no(transactionId)
				.total_fee(totalFee)
				.refund_fee(refundFee)
				.op_user_id(unionPayBean.getMachId())
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/queryRefund")
	public AjaxResult queryRefund(@Param(value = "transactionId", required = false) String transactionId,
								  @Param(value = "outTradeNo", required = false) String outTradeNo,
								  @Param(value = "outRefundNo", required = false) String outRefundNo,
								  @Param(value = "refundId", required = false) String refundId) {
		try {

			if (StrUtil.isEmpty(transactionId) && StrUtil.isEmpty(outTradeNo)) {
				return new AjaxResult().addError("out_trade_no transaction_id 不能同时为空");
			}

			if (StrUtil.isEmpty(outRefundNo) && StrUtil.isEmpty(refundId)) {
				return new AjaxResult().addError("out_refund_no refund_id 不能同时为空");
			}

			Map<String, String> params = RefundQueryModel.builder()
				.service(ServiceEnum.REFUND_QUERY.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(outTradeNo)
				.transaction_id(transactionId)
				.out_refund_no(outRefundNo)
				.refund_id(refundId)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/authCodeToOpenId")
	public AjaxResult authCodeToOpenId(@Param(value = "code") String code) {
		try {

			Map<String, String> params = AuthCodeToOpenIdModel.builder()
				.service(ServiceEnum.AUTH_CODE_TO_OPENID.toString())
				.mch_id(unionPayBean.getMachId())
				.auth_code(code)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/native")
	public AjaxResult nativePay(HttpServletRequest request,
								@Param(value = "totalFee", defaultValue = "1") String totalFee) {
		try {
			String notifyUrl = unionPayBean.getDomain().concat("/unionPay/payNotify");

			String ip = IpKit.getRealIp(request);
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = UnifiedOrderModel.builder()
				.service(ServiceEnum.NATIVE.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付-扫码支付")
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.notify_url(notifyUrl)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/wxJsPay")
	public AjaxResult wxJsPay(HttpServletRequest request,
							  @Param(value = "totalFee", defaultValue = "1") String totalFee,
							  @Param(value = "openId") String openId) {
		try {

			String notifyUrl = unionPayBean.getDomain().concat("/unionPay/payNotify");

			String ip = IpKit.getRealIp(request);
			if (ip.contains(",")) {
				ip = ip.split(",")[0];
			}
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = UnifiedOrderModel.builder()
				.service(ServiceEnum.WEI_XIN_JS_PAY.toString())
				.mch_id(unionPayBean.getMachId())
				.is_raw("1")
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付-微信公众号/小程序支付")
				.sub_openid(openId)
//                    .sub_appid("appId")
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.notify_url(notifyUrl)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			System.out.println(params);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/wxApp")
	public AjaxResult wxApp(HttpServletRequest request,
							@Param(value = "totalFee", defaultValue = "1") String totalFee,
							@Param(value = "appId") String appId) {
		try {

			String notifyUrl = unionPayBean.getDomain().concat("/unionPay/payNotify");

			String ip = IpKit.getRealIp(request);
			if (ip.contains(",")) {
				ip = ip.split(",")[0];
			}
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = UnifiedOrderModel.builder()
				.service(ServiceEnum.WEI_XIN_APP_PAY.toString())
				.mch_id(unionPayBean.getMachId())
				.appid(appId)
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付-微信 App 支付")
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.notify_url(notifyUrl)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			System.out.println(params);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			if (!WxPayKit.verifyNotify(result, unionPayBean.getKey(), SignType.MD5)) {
				return new AjaxResult().addError("签名异常");
			}
			String status = result.get("status");
			String resultCode = result.get("result_code");
			if (!"0".equals(status) && !"0".equals(resultCode)) {
				return new AjaxResult().addError(result.get("err_msg"));
			}

			return new AjaxResult().success(result.get("pay_info"));
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/aliJsPay")
	public AjaxResult aliJsPay(HttpServletRequest request,
							   @Param(value = "totalFee", defaultValue = "1") String totalFee,
							   @Param(value = "buyerLogonId", required = false) String buyerLogonId,
							   @Param(value = "buyerId", required = false) String buyerId) {
		try {
			if (StrUtil.isEmpty(buyerLogonId) && StrUtil.isEmpty(buyerId)) {
				return new AjaxResult().addError("buyer_logon_id buyer_id 不能同时为空");
			}

			String notifyUrl = unionPayBean.getDomain().concat("/unionPay/payNotify");

			String ip = IpKit.getRealIp(request);
			if (ip.contains(",")) {
				ip = ip.split(",")[0];
			}
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = UnifiedOrderModel.builder()
				.service(ServiceEnum.ALI_PAY_JS_PAY.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付-支付宝服务窗口")
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.notify_url(notifyUrl)
				.nonce_str(WxPayKit.generateStr())
				.buyer_id(buyerId)
				.buyer_logon_id(buyerLogonId)
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			System.out.println(params);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}



	@Get
	@Post
	@Mapping("/unionPayUserAuth")
	public void unionPayUserAuth(HttpServletResponse response) throws IOException {
		String notifyUrl = unionPayBean.getDomain().concat("/unionPay/callBack");
		String authUrl = UnionPayApi.buildAuthUrl(notifyUrl);
		logger.info("authUrl:" + authUrl);
		response.sendRedirect(authUrl);
	}


	@Get
	@Post
	@Mapping("/callBack")
	public Map<String, String> unionPayCall(HttpServletRequest request) {
		Map<String, String> params = new HashMap<>();
		Map<String, String[]> requestParams = request.getParameterMap();
		for (String name : requestParams.keySet()) {
			String[] values = requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
			}
			params.put(name, valueStr);
		}
		return params;
	}


	@Get
	@Post
	@Mapping("/unionJsPay")
	public AjaxResult unionJsPay(HttpServletRequest request,
								 @Param(value = "totalFee", defaultValue = "1") String totalFee,
								 @Param(value = "userId", required = false) String userId) {
		try {

			String notifyUrl = unionPayBean.getDomain().concat("/unionPay/payNotify");

			String ip = IpKit.getRealIp(request);
			if (ip.contains(",")) {
				ip = ip.split(",")[0];
			}
			if (StrKit.isBlank(ip)) {
				ip = "127.0.0.1";
			}

			Map<String, String> params = UnifiedOrderModel.builder()
				.service(ServiceEnum.UNION_JS_PAY.toString())
				.mch_id(unionPayBean.getMachId())
				.out_trade_no(WxPayKit.generateStr())
				.body("IJPay 云闪付-银联JS支付")
				.user_id(userId)
				.customer_ip(ip)
				.attach("聚合支付 SDK")
				.total_fee(totalFee)
				.mch_create_ip(ip)
				.notify_url(notifyUrl)
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			System.out.println(params);

			String xmlResult = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("xmlResult:" + xmlResult);
			Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}


	@Get
	@Post
	@Mapping("/billDownload")
	public AjaxResult billDownload(@Param(value = "billDate", defaultValue = "20200325") String billDate,
								   @Param(value = "billType", defaultValue = "ALL") String billType) {
		try {

			Map<String, String> params = BillDownloadModel.builder()
				.service(ServiceEnum.BILL_MERCHANT.toString())
				.bill_date(billDate)
				.bill_type(billType)
				.mch_id(unionPayBean.getMachId())
				.nonce_str(WxPayKit.generateStr())
				.build()
				.createSign(unionPayBean.getKey(), SignType.MD5);

			System.out.println(params);

			String result = UnionPayApi.execution(unionPayBean.getServerUrl(), params);
			logger.info("result:" + result);
			return new AjaxResult().success(result);
		} catch (Exception e) {
			e.printStackTrace();
			return new AjaxResult().addError(e.getMessage());
		}
	}

	/**
	 * 异步通知
	 */
	@Get
	@Post
	@Mapping("/payNotify")
	public String payNotify(HttpServletRequest request) {
		String xmlMsg = HttpKit.readData(request);
		logger.info("支付通知=" + xmlMsg);
		Map<String, String> params = WxPayKit.xmlToMap(xmlMsg);

		String status = params.get("status");
		String returnCode = params.get("result_code");

		logger.info(status + " " + returnCode);

		if ("0".equals(status) && "0".equals(returnCode)) {
			// 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
			// 注意此处签名方式需与统一下单的签名类型一致
			if (WxPayKit.verifyNotify(params, unionPayBean.getKey(), SignType.MD5)) {
				logger.info("支付成功....");
				// 更新订单信息
				// 发送通知等
				return "success";
			}
		}
		return "fail";
	}
}
