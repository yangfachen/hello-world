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
 * <p>日常费用事前申请中获取已报销金额 </p>
 * <p>CreateTime:2019-07-21 19:05:01</p>
 */
public class TBizQueryYbxjeTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pDjbh= pParamJson.optString("F_DJBH");
        if (TWSUtil.isNullText(pDjbh)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("单据编号为空");

            return pReturnBean;
        }

        String pUserSql = "SELECT F_YBXJE FROM NHLH_TYSQ WHERE F_PKEY=?";
        IDalResultSet pQueryRS = TSqlUtils.QueryPreparedSql(pLink, pUserSql, pDjbh);
        if (pQueryRS != null && pQueryRS.First()) {
            JSONObject pYbxje = new JSONObject();
            pYbxje.put("F_YBXJE", pQueryRS.getStringValue("F_YBXJE"));
            pReturnBean.setReturnObject(pYbxje);
        }

        return pReturnBean;
    }
}
