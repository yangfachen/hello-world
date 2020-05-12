package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import net.sf.json.JSONObject;

/**
 * @author skylin
 * <p>CreateTime:2020-06-17 14:51:57</p>
 * <p>
 *     交互格式为json的接收服务基类
 * </p>
 */
public abstract class TBizJsonReceiveBaseTask extends TBizReceiveBaseTask {

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        JSONObject pReturnObj = new JSONObject();

        try {
            this.onServiceEntry(pLink, pTaskBean, pReturnBean, pParamJson, pReturnObj);
        } catch (Exception e) {
            String pErrMsg = e.getMessage();
            if (pErrMsg == null) {
                pErrMsg = e.toString();
            }

            pReturnObj.put("errcode", "-1");
            pReturnObj.put("errmsg", pErrMsg);
            pReturnBean.setProcessCode("-1");
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg(pErrMsg);
        } finally {
            pRspObj.put("data", pReturnObj);
        }

        return pReturnBean;
    }

    protected abstract TTaskReturnBean onServiceEntry(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Exception;
}
