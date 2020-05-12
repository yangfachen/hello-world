package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TDOOperator;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.toolkit.io.FileTool;
import com.eai.toolkit.text.MD5Tool;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.util.TCommUtils;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * @author skylin
 * <p>发票识别</p>
 * <p>CreateTime:2019-07-12 19:02:01</p>
 */
public class TBizInvoiceOCRTask extends TBizTaskParamJson {
    /**
     * 业务处理
     * @param pLogLink
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @param pParamJson
     * @return
     * @throws Throwable
     */
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        pReturnBean.setSendObject(pParamJson);

        String pAppId = pTaskBean.getConfigString("appid");
        String pSecretKey = pTaskBean.getConfigString("secretkey");
        String pServiceUrl = pTaskBean.getConfigString("service.url");

        if (TWSUtil.isNullText(pAppId)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg("应用编号为空");

            return pReturnBean;
        }

        if (TWSUtil.isNullText(pSecretKey)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg("验证码为空");

            return pReturnBean;
        }

        if (TWSUtil.isNullText(pServiceUrl)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg("识别服务地址为空");

            return pReturnBean;
        }

        JSONArray pImages = pParamJson.optJSONArray("F_IMAGES");
        if (pImages == null || pImages.size() == 0) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg("未传入发票");

            return pReturnBean;
        }

        String pRspMsg = "";

        try {
            long pTimestamp = System.currentTimeMillis();
            String pAccessToken = this.createAccessToken(pAppId, pSecretKey, pTimestamp);
            String pMethodName = "POST";
            String pRspData = this.identifyInvoice(pServiceUrl, pMethodName, pAppId, pTimestamp, pAccessToken, pImages);
            pRspMsg = pRspData;

            JSONObject pRspObj = JSONObject.fromObject(pRspData);
            String pRetCode = pRspObj.optString("code", "-1");
            if (!"0".equals(pRetCode)) {
                throw new Exception(pRspObj.optString("msg"));
            }

            JSONObject pResultObj = pRspObj.optJSONObject("result");
            if (pResultObj == null || pResultObj.isEmpty()) {
                pReturnBean.setProcessOk(true);
                pReturnBean.setProcessMsg("未识别到发票");
                return pReturnBean;
            }

            pReturnBean.setReturnObject(pResultObj);
            pReturnBean.setReceiveObject(pRspData);

            /**
             * 记录发票台账
             */
//            this.insertInvoiceLedger(pLink, pTaskBean, pReturnBean, pResultObj);
        } catch (Exception e) {
            String pErrMsg = TWSUtil.getExceptionMessage(e);
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg(pErrMsg);
            pRspMsg = pErrMsg;
        } finally {
            pReturnBean.setSendObject(pParamJson);
            pReturnBean.setReceiveObject(pRspMsg);
        }

        return pReturnBean;
    }

    /**
     * 创建access_token
     * @param pAppId
     * @param pSecretKey
     * @param pTimestamp
     * @return
     */
    protected String createAccessToken(String pAppId, String pSecretKey, long pTimestamp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(pAppId).append("+");
        buffer.append(pTimestamp).append("+");
        buffer.append(pSecretKey);

        String md5Text = MD5Tool.toMD5(buffer.toString());

        return md5Text.toLowerCase();
    }

    /**
     * 请求发票识别服务
     * @param pUrl
     * @param pMethodName
     * @param pAppId
     * @param pTimestamp
     * @param pAccessToken
     * @param pImages
     * @return
     * @throws Exception
     */
    protected String identifyInvoice(String pUrl, String pMethodName, String pAppId, long pTimestamp, String pAccessToken, JSONArray pImages) throws Exception {
        CloseableHttpClient pHttpClient = HttpClients.createDefault();

        try {
            HttpPost pMethod = new HttpPost(pUrl);
            MultipartEntityBuilder pMultipartEntityBuilder = MultipartEntityBuilder.create();
            pMultipartEntityBuilder.addTextBody("key", pAppId);
            pMultipartEntityBuilder.addTextBody("times", String.valueOf(pTimestamp));
            pMultipartEntityBuilder.addTextBody("token", pAccessToken);

            /**
             * 设置影像，每个field名称即为影像号
             */
            int pSize = pImages.size();
            JSONObject pImage = null;
            String pImageId = "";
            String pBase64Text = "";
            byte[] pFileBytes = null;
            for (int iIndex = 0; iIndex < pSize; iIndex++) {
                pImage = pImages.getJSONObject(iIndex);
                pImageId = pImage.optString("F_IMAGE_ID");
                pBase64Text = pImage.optString("F_BASE64_TEXT");
                pFileBytes = StringTool.T2B(pBase64Text);
                pMultipartEntityBuilder.addBinaryBody(pImageId, pFileBytes, ContentType.DEFAULT_BINARY, pImageId + ".jpg");
            }

            HttpEntity pReqEntity = pMultipartEntityBuilder.build();
            pMethod.setEntity(pReqEntity);

            CloseableHttpResponse pResponse = pHttpClient.execute(pMethod);
            StatusLine pStatusLine = pResponse.getStatusLine();
            HttpEntity pRspEntity = pResponse.getEntity();
            String pRspMsg = EntityUtils.toString(pRspEntity, "utf-8");
            if (200 != pStatusLine.getStatusCode()) {
                throw new Exception(pRspMsg);
            } else {
                return pRspMsg;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (pHttpClient != null) {
                pHttpClient.close();
            }
        }
    }

    /**
     * 记录发票台账
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @param pInvoices
     * @return
     * @throws Exception
     */
    protected TTaskReturnBean insertInvoiceLedger(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pInvoices) throws Exception {
        TDOOperator pFptzOperator = new TDOOperator(pLink, "NHLH_FPTZ");
        Set<String> pImageIds = pInvoices.keySet();
        JSONArray pImageInvoices = null;
        String pImageId = "";
        for (Iterator<String> iterator = pImageIds.iterator(); iterator.hasNext();) {
            pImageId = iterator.next();
            pImageInvoices = pInvoices.optJSONArray(pImageId);
            if (pImageInvoices == null || pImageInvoices.size() == 0) {
                continue;
            }

            int pInvoiceCount = pImageInvoices.size();
            JSONObject pInvoiceObj = null;
            String pFphm = "";
            String pFpdm = "";
            String pFpnr = "";
            String pFprq = "";
            String pFplx = "";
            double pFpje = 0.0d;
            double pFpsl = 0.0d;
            double pFpse = 0.0d;
            double pFpshje = 0.0d;
            for (int iIndex = 0; iIndex < pInvoiceCount; iIndex++) {
                pInvoiceObj = pImageInvoices.getJSONObject(iIndex);
                pFphm = pInvoiceObj.optString("number");
                pFpdm = pInvoiceObj.optString("code");
                pFprq = pInvoiceObj.optString("date");
                pFplx = pInvoiceObj.optString("type");
                pFpje = pInvoiceObj.optDouble("total");

                pFptzOperator.AppendEmptyRow();
                pFptzOperator.setStringValue("F_PKEY", StringTool.UUIDCreate());
                pFptzOperator.setStringValue("F_FPBH", pFphm);
                pFptzOperator.setStringValue("F_FPDM", pFpdm);
                pFptzOperator.setStringValue("F_FPNR", pFpnr);
                pFptzOperator.setStringValue("F_FPRQ", pFprq);
                pFptzOperator.setStringValue("F_FPLX", pFplx);
                pFptzOperator.setDoubleValue("F_FPJE", pFpje);
                pFptzOperator.setDoubleValue("F_FPSL", pFpsl);
                pFptzOperator.setDoubleValue("F_FPSE", pFpse);
                pFptzOperator.setDoubleValue("F_FPSHJE", pFpshje);
                pFptzOperator.setStringValue("F_IMAGE_ID", pImageId);
            }
        }

        pFptzOperator.InsertRows(pLink);

        return pReturnBean;
    }
}
