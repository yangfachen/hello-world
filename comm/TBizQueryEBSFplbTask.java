package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.google.gson.JsonObject;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class TBizQueryEBSFplbTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pOU = pParamJson.optString("F_OU");
        String pGYS = pParamJson.optString("F_GYS");
        if (TWSUtil.isNullText(pOU)) {
            return isNull("F_OU",pReturnBean);
        }
        if (TWSUtil.isNullText(pGYS)) {
            return isNull("F_GYS", pReturnBean);
        }
        JSONObject pFpxx = new JSONObject();
        pFpxx.put("F_FPBH","123456");
        pFpxx.put("F_FPRQ", "20190101");
        pFpxx.put("F_FPJE", 100.0d);
        pFpxx.put("F_WFJE",20.0d);
        pFpxx.put("F_YFJE", 80.0d);
        JSONArray pArray = new JSONArray();
        pArray.add(pFpxx);
        JSONObject pJson = new JSONObject();
        pJson.put("row_count", pFpxx.size());
        pJson.put("datas", pArray);
        pReturnBean.setReturnObject(pJson);
        return pReturnBean;
    }
    private TTaskReturnBean isNull(String pQueryData, TTaskReturnBean pReturnBean) {
        pReturnBean.setProcessOk(false);
        pReturnBean.setProcessCode("1");
        pReturnBean.setProcessMsg("未查询到"+pQueryData);
        pReturnBean.setReturnJsonObject();
        return pReturnBean;
    }
}
