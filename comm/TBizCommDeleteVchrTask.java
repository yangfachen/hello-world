package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * @author:yhj
 * @title:单据通用删除服务
 * @CreateTime:2020/6/10 15:38
 */
public class TBizCommDeleteVchrTask extends TBizReceiveBaseTask {

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pDjlx = pParamJson.optString("F_DJLX");
        String pDjbh = pParamJson.optString("F_DJBH");

        if(TWSUtil.isNullText(pDjlx)){
            throw new Exception(String.format("未传单据类型"));
        }
        if(TWSUtil.isNullText(pDjbh)){
            throw new Exception(String.format("未传单据编号"));
        }

        String pQueryConfSql = "SELECT * FROM NHLH_STO_DELVCHR_CONF WHERE F_DJLX=? AND F_DISABLE='0'";
        IDalResultSet pRs = TSqlUtils.QueryPreparedSql(pLink,pQueryConfSql,pDjlx);
        if(pRs!=null&&pRs.First()){
            String pSql = pRs.getStringValue("F_SQL");
            String[] pDeleteSqls = pSql.split(";");
            for(String pDeleteSql : pDeleteSqls){
                TSqlUtils.UpdatePreparedSql(pLink,pDeleteSql,pDjbh);
            }
            pRspObj.put("errcode","0");
            pRspObj.put("errmsg","单据删除成功");
        }else {
            pRspObj.put("errcode","-1");
            pRspObj.put("errmsg","未配置删除SQL");
        }

        return pReturnBean;
    }
}
