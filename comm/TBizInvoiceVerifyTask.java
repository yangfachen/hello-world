package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skylin
 * <p>CreateTime:2019-07-26 09:57:01</p>
 * <p>
 *     发票验真
 *     单个发票参数结构
 *     {
 *         "fpdm":"",    //发票代码
 *         "fphm":"",    //发票号码
 *         "kprq":"",    //开票日期yyyyMMdd
 *         "fpje":"",    //发票金额
 *         "jym":""      //校验码后六位，只取后六位
 *     }
 * </p>
 */
public class TBizInvoiceVerifyTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        JSONArray pFpList = pParamJson.optJSONArray("fplist");
        if (pFpList == null || pFpList.size() == 0) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("未传入发票");
            pReturnBean.setReturnJsonObject();
            return pReturnBean;
        }

        String pUrl = pTaskBean.getConfigString("http.url");
        String pClientId = pTaskBean.getConfigString("clientid");
        String pServiceId = pTaskBean.getConfigString("serviceid");
        String pUserId = pTaskBean.getConfigString("userid");
        String pAppId = pTaskBean.getConfigString("appid");
        String pAccessToken = pTaskBean.getConfigString("access_token");
        String pBWToken = pTaskBean.getConfigString("bw_token");

        CloseableHttpClient pClient = HttpClients.createDefault();
        try {
            HttpPost pMethod = new HttpPost(pUrl);
            List<NameValuePair> pParameters = new ArrayList<NameValuePair>();
            pParameters.add(new BasicNameValuePair("instruction", pServiceId));
            pParameters.add(new BasicNameValuePair("user_id", pUserId));
            pParameters.add(new BasicNameValuePair("app_id", pAppId));
            pParameters.add(new BasicNameValuePair("access_token", pAccessToken));
            pParameters.add(new BasicNameValuePair("bwToken", pBWToken));
            pParameters.add(new BasicNameValuePair("client_id", pClientId));
            pParameters.add(new BasicNameValuePair("fpList", pFpList.toString()));
            pParameters.add(new BasicNameValuePair("fpNum", String.valueOf(pFpList.size())));

            UrlEncodedFormEntity pReqEntity = new UrlEncodedFormEntity(pParameters, "utf-8");
            pMethod.setEntity(pReqEntity);

            CloseableHttpResponse pResponse = pClient.execute(pMethod);
            StatusLine pStatusLine = pResponse.getStatusLine();
            if (200 != pStatusLine.getStatusCode()) {
                pReturnBean.setProcessOk(false);
                pReturnBean.setProcessCode("300");
                pReturnBean.setProcessMsg("接口调用失败!");
                pReturnBean.setReturnJsonObject();
                return pReturnBean;
            }

            HttpEntity pRspEntity = pResponse.getEntity();
            String pRspData = EntityUtils.toString(pRspEntity);
            if (TWSUtil.isNotJson(pRspData)) {
                throw new Exception(String.format("返回结果错误！%s", pRspData));
            }

            //返回前台的对象
            JSONObject pReturnObj = new JSONObject();
            //返回前台的发票验真结果
            JSONArray pFpResults = new JSONArray();

            JSONObject pRspObj = JSONObject.fromObject(pRspData);
            String pResult = pRspObj.optString("code");
            String pMessage = pRspObj.optString("message");
            if (!"001".equals(pResult)) {
                pReturnObj.put("F_CODE", pResult);
            } else {
                pReturnObj.put("F_CODE", "0");
                JSONArray pTmpArray = pRspObj.optJSONArray("invoiceList");
                if (pTmpArray != null || pTmpArray.size() > 0) {
                    int pTmpCount = pTmpArray.size();
                    JSONObject pFpResult = null;
                    JSONObject pFpResultReturn = null;
                    for (int iIndex = 0; iIndex < pTmpCount; iIndex++) {
                        pFpResult = pTmpArray.getJSONObject(iIndex).getJSONObject("head");
                        pFpResultReturn = new JSONObject();
                        pFpResultReturn.put("yzjg", pFpResult.optString("F_CODE"));
                        pFpResultReturn.put("yzxx", pFpResult.optString("F_MESSAGE"));
                        pFpResultReturn.put("fpdm", pFpResult.optString("F_FPDM"));
                        pFpResultReturn.put("fphm", pFpResult.optString("F_FPHM"));

                        //2020-06-16 skylin将head信息全部返回
                        pFpResultReturn.putAll(pFpResult);

                        pFpResults.add(pFpResultReturn);
                    }
                }
            }

            pReturnObj.put("F_MESSAGE", pMessage);
            pReturnObj.put("fplist", pFpResults);
            pReturnBean.setReturnObject(pReturnObj);
        } catch (Exception e) {
            throw e;
        } finally {
            pClient.close();
        }

        return pReturnBean;
    }
}
