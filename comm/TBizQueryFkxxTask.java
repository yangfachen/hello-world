package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * @author yangyang
 * <p>获取付款信息</p>
 * <p>CreateTime:2019-07-22 10:53:01</p>
 */
public class TBizQueryFkxxTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pSsdjbh= pParamJson.optString("F_SSDJBH");
        if (TWSUtil.isNullText(pSsdjbh)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("单据编号为空");

            return pReturnBean;
        }

        String pUserSql = "select * from nhlh_fkxx where f_ssdjbh=?";
        IDalResultSet pQueryRS = TSqlUtils.QueryPreparedSql(pLink, pUserSql, pSsdjbh);
        if (pQueryRS != null && pQueryRS.First()) {
            String[] pColNames = pQueryRS.getColumnNames();
            JSONObject pFkxx = new JSONObject();
            for (String pColName : pColNames) {
                pFkxx.put(pColName, pQueryRS.getStringValue(pColName));
            }

            pReturnBean.setReturnObject(pFkxx);
        }

        return pReturnBean;
    }
}
