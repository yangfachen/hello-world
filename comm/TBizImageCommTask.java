package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author skylin
 * <p>CreateTime:2019-08-01 16:11:01</p>
 * <p>
 *     影像系统接口通用服务
 * </p>
 */
public class TBizImageCommTask extends TBizTaskParamJson {

    @Override
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pAppId = pParamJson.optString("appid");
        String pSecretKey = pParamJson.optString("appSecretKey");
        String pModuleId = pParamJson.optString("moduleId");
        String pTxCode = pParamJson.optString("txCode");
        String pTxSN = pParamJson.optString("txSN");
        String pReqData = pParamJson.optString("data");

        JSONObject pReturnObj = new JSONObject();
        pReturnObj.put("errcode", "0");
        pReturnObj.put("errmsg", "");
        pReturnBean.setReceiveObject(pReturnObj);

        return pReturnBean;
    }
}
