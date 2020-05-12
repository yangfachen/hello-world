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
 * @author skylin
 * <p>获取用户信息</p>
 * <p>CreateTime:2019-07-11 21:05:01</p>
 */
public class TBizQueryUserInfoTask extends TBizTaskParamJson {
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pUserId = pParamJson.optString("F_USER_ID");
        if (TWSUtil.isNullText(pUserId)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("用户编码为空");

            return pReturnBean;
        }

        String pUserSql = "SELECT A.USR_USRID AS F_USER_ID, A.USR_CAPTION AS F_USER_NAME, A.USR_DESCRIPTION AS F_SSO_ID, A.USR_ORGID AS F_ORG_ID, B.F_CHR3 AS F_SSDW, A.USR_PHONE1 AS F_PHONE, A.USR_T04 AS F_YGZJ, C.F_YSZZ AS F_YSBM, '' AS F_YWST, A.USR_T18 AS F_YT, A.F_CBZX AS F_CBZX, A.USR_N04 AS F_HSZT, d.f_bwb as F_BWB FROM SSF_USERS A LEFT JOIN BF_ORG B ON A.USR_ORGID = B.F_ID LEFT JOIN NHLH_GLZZ_YSZZ C ON A.USR_ORGID = C.F_GLZZ left join NHLH_DCT_HSZT D ON a.usr_n04 = d.f_bm  WHERE A.USR_USRID = ? AND A.USR_TYPE = '1'";
        IDalResultSet pUserRS = TSqlUtils.QueryPreparedSql(pLink, pUserSql, pUserId);
        if (pUserRS != null && pUserRS.First()) {
            String[] pColNames = pUserRS.getColumnNames();
            JSONObject pUserInfo = new JSONObject();
            for (String pColName : pColNames) {
                pUserInfo.put(pColName, pUserRS.getStringValue(pColName));
            }

            pReturnBean.setReturnObject(pUserInfo);
        }else{
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessMsg("未查询到数据");
            pReturnBean.setReturnJsonObject();
        }

        return pReturnBean;
    }
}
