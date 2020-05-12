package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSBizTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import net.sf.json.JSONObject;
import org.dom4j.Element;

/**
 * @author skylin
 * <p>CreateTime:2019-08-22 14:21:01</p>
 * <p>
 *     调用通用服务接口
 * </p>
 */
public class TBizCallCommServiceTask extends TWSBizTask {
    public static final String CFG_KEY_WS_AUTH_APPID  = "appid";
    public static final String CFG_KEY_WS_AUTH_APPKEY = "appSecretKey";
    public static final String CFG_KEY_WS_REQ_MODULE  = "moduleId";
    public static final String CFG_KEY_WS_REQ_SERVICE = "txCode";
    public static final String CFG_KEY_WS_REQ_SN      = "txSN";
    public static final String CFG_KEY_WS_REQ_DATA    = "data";

    protected TTaskReturnBean buildBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pParamJson = (JSONObject) pTaskBean.getParamObject();
        String pAppId = pTaskBean.getConfigString(CFG_KEY_WS_AUTH_APPID, "");
        String pAppkey = pTaskBean.getConfigString(CFG_KEY_WS_AUTH_APPKEY, "");
        String pModuleId = pParamJson.optString(CFG_KEY_WS_REQ_MODULE);
        String pSN = pParamJson.optString(CFG_KEY_WS_REQ_SN);
        String pService   = pParamJson.optString(CFG_KEY_WS_REQ_SERVICE);
        String pEncryptData = pParamJson.optString(CFG_KEY_WS_REQ_DATA);

        StringBuffer pReqData = new StringBuffer();
        pReqData.append("<appid>").append(pAppId).append("</appid>");
        pReqData.append("<appSecretKey>").append(pAppkey).append("</appSecretKey>");
        pReqData.append("<moduleId>").append(pModuleId).append("</moduleId>");
        pReqData.append("<txCode>").append(pService).append("</txCode>");
        pReqData.append("<txSN>").append(pSN).append("</txSN>");
        pReqData.append("<data>").append(pEncryptData).append("</data>");

        this.setRequestData(pTaskBean, pReqData.toString());
        return pReturnBean;
    }

    protected void processBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pRspObj = null;

        try {
            Element pRspEle = this.getReturnBody(pTaskBean);
            if (pRspEle == null) {
                throw new Exception("返回结果结果解析异常!");
            }

            String pEncryptData = pRspEle.elementTextTrim("return");
            if (TWSUtil.isNullText(pEncryptData)) {
                throw new Exception("返回结果结果为空!");
            }

            String pRealData = new String(StringTool.T2B(pEncryptData), "utf-8");
            if (TWSUtil.isNotJson(pRealData)) {
                throw new Exception("返回结果结果不是json!");
            }

            pRspObj = JSONObject.fromObject(pRealData);
        } catch (Exception e) {
            String pErrMsg = e.getMessage();
            if (pErrMsg == null) {
                pErrMsg = e.toString();
            }

            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("-1");
            pReturnBean.setProcessMsg(pErrMsg);
            pRspObj = new JSONObject();
            pRspObj.put("errcode", "-1");
            pRspObj.put("errmsg", pErrMsg);
        } finally {
            pReturnBean.setReturnObject(pRspObj);
        }
    }
}
