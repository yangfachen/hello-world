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
 * <p>出差申请 获取机票金额</p>
 * <p>CreateTime:2019-07-21 19:35:01</p>
 */
public class TBizQueryAirFareTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pDjbh= pParamJson.optString("F_DJBH");
        if (TWSUtil.isNullText(pDjbh)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("单据编号为空");

            return pReturnBean;
        }

        String pUserSql = "select SUM(F_SQJE) as F_AIFPARE from NHLH_CCBX_SQMX WHERE F_SSDJBH=?";
        IDalResultSet pQueryRS = TSqlUtils.QueryPreparedSql(pLink, pUserSql, pDjbh);
        if (pQueryRS != null && pQueryRS.First()) {
            String[] pColNames = pQueryRS.getColumnNames();
            JSONObject pAirFare = new JSONObject();
            for (String pColName : pColNames) {
                pAirFare.put(pColName, pQueryRS.getStringValue(pColName));
            }

            pReturnBean.setReturnObject(pAirFare);
        }

        return pReturnBean;
    }
}
