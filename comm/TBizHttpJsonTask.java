package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TBizHttpJsonTask extends TBizTaskParamJson {
    private static final String KEY_HTTP_REQUET_HEADERS = "saf.http.request.headers";
    private static final String KEY_HTTP_REQUET_BODY    = "saf.http.request.body";
    private static final String KEY_HTTP_REQUET_PARAMS  = "saf.http.request.params";
    private static final String KEY_HTTP_REQUET_URL     = "saf.http.request.url";
    private static final String KEY_HTTP_REQUET_METHOD  = "saf.http.request.method";
    private static final String KEY_HTTP_REQUET_STYLE   = "saf.http.request.style";

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        JSONObject pRspObj = new JSONObject();

        try {
            String pHttpUrl = this.getHttpUrl(pTaskBean);
            String pHttpMethod = this.getHttpMethod(pTaskBean);
            String pHttpStyle = this.getHttpStyle(pTaskBean);

            this.buildRequestParam(pLink, pTaskBean, pReturnBean, pParamJson);

            Map<String, String> pHeaders = this.getHttpHeaders(pTaskBean);

            String pRspText = "{}";

            //param
            if ("2".equals(pHttpStyle)) {
                Map<String, String> pParams = this.getHttpParams(pTaskBean);
                pRspText = this.invokeServiceByParam(pLink, pHttpUrl, pHttpMethod, pHeaders, pParams);
            }
            //body
            else {
                String pReqData = this.getRequestBody(pTaskBean);
                pRspText = this.invokeServiceByBody(pLink, pHttpUrl, pHttpMethod, pReqData, pHeaders);
            }

            if (TWSUtil.isNotJson(pRspText)) {
                throw new Exception(String.format("调用影像服务错误，返回值有误!%s", pRspText));
            }

            /**
             * 此服务只是作为中转，不做任何其他处理
             */
            JSONObject pCallReturnObj = JSONObject.fromObject(pRspText);
            String pCode = pCallReturnObj.optString("F_CODE");
            String pMsg = pCallReturnObj.optString("F_MESSAGE");
            pRspObj.put("errcode", pCode);
            pRspObj.put("errmsg", pMsg);
            pRspObj.put("data", pRspText);
        } catch (Exception e) {
            String pErrMsg = e.getMessage();
            if (pErrMsg == null) {
                pErrMsg = e.toString();
            }

            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("-1");
            pReturnBean.setProcessMsg(pErrMsg);

            pRspObj.put("errcode", "-1");
            pRspObj.put("errmsg", pErrMsg);
        } finally {
            pReturnBean.setReturnObject(pRspObj);
        }

        return pReturnBean;
    }

    protected abstract TTaskReturnBean buildRequestParam(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Exception;

    protected String invokeServiceByBody(IDalConnection pLink, String pUrl, String pReqMethod, String pReqData, Map<String, String> pHeaders) throws Exception {
        CloseableHttpClient pClient = HttpClients.createDefault();

        try {
            HttpRequestBase pHttpMethod = null;
            if ("POST".equals(pReqMethod)) {
                pHttpMethod = new HttpPost(pUrl);
            } else {
                pHttpMethod = new HttpGet(pUrl);
            }

            if (pHeaders != null && !pHeaders.isEmpty()) {
                for (Map.Entry<String, String> pEntry : pHeaders.entrySet()) {
                    pHttpMethod.addHeader(pEntry.getKey(), pEntry.getValue());
                }
            }

            pHttpMethod.addHeader("Content-Type", "application/json");

            if ("POST".equals(pReqMethod) && !TWSUtil.isNullText(pReqData)) {
                ContentType pContentType = ContentType.APPLICATION_JSON.withCharset("utf-8");
                HttpEntity pReqEntity = new StringEntity(pReqData.toString(), pContentType);
                ((HttpPost)pHttpMethod).setEntity(pReqEntity);
            }

            CloseableHttpResponse pResponse = pClient.execute(pHttpMethod);
            HttpEntity pRspEntity = pResponse.getEntity();
            if (200 != pResponse.getStatusLine().getStatusCode()) {
                throw new Exception(EntityUtils.toString(pRspEntity, "utf-8"));
            }

            return EntityUtils.toString(pRspEntity, "utf-8");
        } catch (Exception e) {
            throw e;
        } finally {
            if (pClient != null) {
                pClient.close();;
            }
        }
    }

    protected String invokeServiceByParam(IDalConnection pLink, String pUrl, String pReqMethod, Map<String, String> pHeaders, Map<String, String> pParams) throws Exception {
        CloseableHttpClient pClient = HttpClients.createDefault();

        try {
            HttpRequestBase pHttpMethod = null;
            if ("POST".equals(pReqMethod)) {
                pHttpMethod = new HttpPost(pUrl);
            } else {
                pHttpMethod = new HttpGet(pUrl);
            }

            if (pHeaders != null && !pHeaders.isEmpty()) {
                for (Map.Entry<String, String> pEntry : pHeaders.entrySet()) {
                    pHttpMethod.addHeader(pEntry.getKey(), pEntry.getValue());
                }
            }

            if ("POST".equals(pReqMethod) && pParams != null && pParams.size() > 0) {
                MultipartEntityBuilder pBuilder = MultipartEntityBuilder.create();
                for (Map.Entry<String, String> pEntry : pParams.entrySet()) {
                    pBuilder.addTextBody(pEntry.getKey(), pEntry.getValue(), ContentType.MULTIPART_FORM_DATA);
                }

                HttpEntity pReqEntity = pBuilder.build();
                ((HttpPost)pHttpMethod).setEntity(pReqEntity);
            }

            CloseableHttpResponse pResponse = pClient.execute(pHttpMethod);
            HttpEntity pRspEntity = pResponse.getEntity();
            if (200 != pResponse.getStatusLine().getStatusCode()) {
                throw new Exception(EntityUtils.toString(pRspEntity, "utf-8"));
            }

            return EntityUtils.toString(pRspEntity, "utf-8");
        } catch (Exception e) {
            throw e;
        } finally {
            if (pClient != null) {
                pClient.close();;
            }
        }
    }

    protected Map<String, String> setHttpHeaders(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        Map<String, String> pHeaders = new HashMap<String, String>();
        pHeaders.put("Content-Type", "application/json");
        this.setHttpHeaders(pTaskBean, pHeaders);
        return pHeaders;
    }

    protected Map<String, String> getHttpHeaders(TTaskBean pTaskBean) {
        return (Map<String, String>) pTaskBean.getAttribute(KEY_HTTP_REQUET_HEADERS);
    }

    protected void setHttpHeaders(TTaskBean pTaskBean, Map<String, String> pHttpHeaders) {
        pTaskBean.setAttribute(KEY_HTTP_REQUET_HEADERS, pHttpHeaders);
    }

    protected Map<String, String> getHttpParams(TTaskBean pTaskBean) {
        return (Map<String, String>) pTaskBean.getAttribute(KEY_HTTP_REQUET_PARAMS);
    }

    protected void setHttpParams(TTaskBean pTaskBean, Map<String, String> pHttpParams) {
        pTaskBean.setAttribute(KEY_HTTP_REQUET_PARAMS, pHttpParams);
    }

    protected String getRequestBody(TTaskBean pTaskBean) {
        return (String) pTaskBean.getAttribute(KEY_HTTP_REQUET_BODY);
    }

    protected void setRequestBody(TTaskBean pTaskBean, String pBody) {
        pTaskBean.setAttribute(KEY_HTTP_REQUET_BODY, pBody);
    }

    protected String getHttpUrl(TTaskBean pTaskBean) {
        JSONObject pParamJson = (JSONObject) pTaskBean.getParamObject();
        String pHttpUrl = pParamJson.optString("F_URL");
        if (TWSUtil.isNullText(pHttpUrl)) {
            pHttpUrl = pTaskBean.getConfigString(KEY_HTTP_REQUET_URL);
        }

        return pHttpUrl;
    }

    protected String getHttpMethod(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(KEY_HTTP_REQUET_METHOD);
    }

    protected String getHttpStyle(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(KEY_HTTP_REQUET_STYLE);
    }

    public static void main(String[] args)  throws Exception {
        TBizHttpJsonTask pTask = new TBizHttpJsonTask() {
            @Override
            protected TTaskReturnBean buildRequestParam(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Exception {
                return null;
            }
        };

        String pUrl = "http://fssc-imgtest.liuheco.com/WEBIMG/apis/fileTransferDownloadApplyAction.do";
        Map<String, String> pHeaders = null;
        Map<String, String> pParams = new HashMap<String, String>();
        pParams.put("jsondata", "{\"F_USER_ID\":\"\",\"F_ZIP\":\"1\",\"F_FILE_NAME\":\"\",\"F_APPLY_FILES\":[{\"F_OBJECT\":\"TYZF2019102100017\",\"F_TYPE\":\"0\",\"F_FILES\":[{\"F_STORE_KEY\":\"g1/2019/10/21/ED8C45573327DE89F73BACB8211DCF75.pdf\",\"F_FILE_NAME\":\"ED8C45753902ECB08189C347AA1A3E44.pdf\"},{\"F_STORE_KEY\":\"g1/2019/10/21/ED8C2A854E7ECEF4AE7E500F80E32F6E.docx\",\"F_FILE_NAME\":\"ED8C2AA12E8CCAA12E39B823ECF25981.docx\"},{\"F_STORE_KEY\":\"g1/2019/10/21/ED8C1999ADFC637B9A05B42A85D2D454.xlsx\",\"F_FILE_NAME\":\"ED8C19B663BA5A6B29BEB9521D6CE7C8.xlsx\"},{\"F_STORE_KEY\":\"g1/2019/10/21/ED8C39D2B07F45832903C0977128B647.jpg\",\"F_FILE_NAME\":\"ED8C39F076A105B34DF63FEB981D248F.jpg\"}]}]}");
        String pRspText = pTask.invokeServiceByParam(null, pUrl, "POST", pHeaders, pParams);
        System.out.println(pRspText);
    }
}
