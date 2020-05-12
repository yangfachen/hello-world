package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.toolkit.io.FileTool;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TBizHttpMultiFormTask extends TBizTaskParamJson {
    private static final String KEY_HTTP_REQUET_HEADERS = "saf.http.request.headers";
    private static final String KEY_HTTP_REQUET_BODY    = "saf.http.request.body";
    private static final String KEY_HTTP_REQUET_PARAMS  = "saf.http.request.params";
    private static final String KEY_HTTP_REQUET_URL     = "saf.http.request.url";
    private static final String KEY_HTTP_REQUET_METHOD  = "saf.http.request.method";
    private static final String KEY_HTTP_REQUET_STYLE   = "saf.http.request.style";
    protected static final String KEY_HTTP_REQUET_FORM_FIELD    = "saf.http.request.form.field";
    protected static final String KEY_HTTP_REQUET_FORM_BINARY   = "saf.http.request.form.binary";

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        JSONObject pRspObj = new JSONObject();

        try {
            String pHttpUrl = this.getHttpUrl(pTaskBean);
            String pHttpMethod = this.getHttpMethod(pTaskBean);
            String pHttpStyle = this.getHttpStyle(pTaskBean);

            this.buildRequestParam(pLink, pTaskBean, pReturnBean, pParamJson);

            Map<String, String> pHeaders = this.getHttpHeaders(pTaskBean);

            String pRspText = "{}";

            Map<String, List<Map<String,String>>> pParams = this.getHttpParams(pTaskBean);
            pRspText = this.invokeServiceByBody(pLink, pHttpUrl, pHttpMethod, pParams, pHeaders);

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

    protected String invokeServiceByBody(IDalConnection pLink, String pUrl, String pReqMethod, Map<String, List<Map<String,String>>> pReqData, Map<String, String> pHeaders) throws Exception {
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

            if ("POST".equals(pReqMethod) && pReqData != null) {
                MultipartEntityBuilder pBuilder = MultipartEntityBuilder.create();
                List<Map<String, String>> pFileds = pReqData.get(KEY_HTTP_REQUET_FORM_FIELD);
                if (pFileds != null && pFileds.size() > 0) {
                    for (Map<String, String> pField : pFileds) {
                        for (Map.Entry<String, String> pEntry : pField.entrySet()) {
                            pBuilder.addTextBody(pEntry.getKey(), pEntry.getValue());
                        }
                    }
                }

                List<Map<String, String>> pFiles = pReqData.get(KEY_HTTP_REQUET_FORM_BINARY);
                if (pFiles != null && pFiles.size() > 0) {
                    for (Map<String, String> pFile : pFiles) {
                        for (Map.Entry<String, String> pEntry : pFile.entrySet()) {
                            byte[] pFileBytes = StringTool.T2B(pEntry.getKey());
                            pBuilder.addBinaryBody("file", pFileBytes, ContentType.DEFAULT_BINARY, "file.jpg");
                        }
                    }
                }

                HttpEntity pReqEntity = pBuilder.build();
                ((HttpPost) pHttpMethod).setEntity(pReqEntity);
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

    protected Map<String, List<Map<String,String>>> getHttpParams(TTaskBean pTaskBean) {
        return (Map<String, List<Map<String,String>>>) pTaskBean.getAttribute(KEY_HTTP_REQUET_PARAMS);
    }

    protected void setHttpParams(TTaskBean pTaskBean, Map<String, List<Map<String,String>>> pHttpParams) {
        pTaskBean.setAttribute(KEY_HTTP_REQUET_PARAMS, pHttpParams);
    }

    protected String getRequestBody(TTaskBean pTaskBean) {
        return (String) pTaskBean.getAttribute(KEY_HTTP_REQUET_BODY);
    }

    protected void setRequestBody(TTaskBean pTaskBean, String pBody) {
        pTaskBean.setAttribute(KEY_HTTP_REQUET_BODY, pBody);
    }

    protected String getHttpUrl(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(KEY_HTTP_REQUET_URL);
    }

    protected String getHttpMethod(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(KEY_HTTP_REQUET_METHOD);
    }

    protected String getHttpStyle(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(KEY_HTTP_REQUET_STYLE);
    }

    public static void main(String[] args)  throws Exception {
        final TBizHttpMultiFormTask pTask = new TBizHttpMultiFormTask() {
            @Override
            protected TTaskReturnBean buildRequestParam(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Exception {
                return pReturnBean;
            }
        };

        String pUrl = "http://fssc-imgtest.liuheco.com/WEBIMG/apis/fileTransferUploadActionImg.do";
        Map<String, String> pHeaders = null;

        Map<String, List<Map<String,String>>> pParams = buildParams();
        String pRspText = pTask.invokeServiceByBody(null, pUrl, "POST", pParams, pHeaders);
        System.out.println(pRspText);
    }

    public static Map<String, List<Map<String, String>>> buildParams() throws Exception {
        Map<String, List<Map<String,String>>> pParams = new HashMap<>();
        List<Map<String, String>> pFields = new ArrayList<>();
        List<Map<String, String>> pFiles = new ArrayList<>();

        String pAppId = "LHBZ";
        String pUserId = "NH071016";
        String pUserName = "方庆";
        String pBillId = "TYZF2019111500013";
        String pBillType = "LHBZ_BZ0302";
        String pImageType = "COMMOMATT";
        long pFileSize = 0L;
        String pFileName = "微信图片_20190723144706.jpg";

        String pJsonData = "";//buildUploadData(pAppId, pUserId, pUserName, pBillId, pBillType, pImageType, pFileName, pFileSize);
        Map<String, String> pJsonMap = new HashMap<>();
        pJsonMap.put("jsondata", pJsonData);

        byte[] pFielBytes = FileTool.File2MemoryBytes("D:/upload.jpg");
        String pFileContent = StringTool.B2T(pFielBytes);
        Map<String, String> pFilesMap = new HashMap<>();
        pFilesMap.put("file", pFileContent);
        pFiles.add(pFilesMap);

        pFields.add(pJsonMap);
        pParams.put("saf.http.request.form.field", pFields);
        pParams.put("saf.http.request.form.binary", pFiles);

        return pParams;
    }
}
