package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author:yhj
 * @title:根据凭证编号查询业务单号
 * @CreateTime:2019/11/24 13:47
 */
public class TBizYwdhByPzbhTask extends TBizReceiveBaseTask {

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pKey = pParamJson.optString("F_PKEY");
        if(TWSUtil.isNullText(pKey)){
            throw new Exception(String.format("未传单据编号"));
        }
        String pPzbh = "";
        String pYwdh = "";
        String pQueryYwdhSql = "SELECT F_PZBH,F_DOC_NUM AS F_YWDH FROM NHLH_STO_PZBH_YWDH WHERE F_PZBH=?";
        IDalResultSet pPzbhRs = this.getPzbh(pLink,pKey);
        JSONObject pContrastObj = null;
        JSONArray pReturnArr = new JSONArray();
        int pRowCount = pPzbhRs.getRowCount();
        for(int iIndex=0;iIndex<pRowCount;iIndex++){
            pPzbhRs.setRowIndex(iIndex);
            pPzbh = pPzbhRs.getStringValue("F_HSPZH");
            IDalResultSet pContrastRs = TSqlUtils.QueryPreparedSql(pLink,pQueryYwdhSql,pPzbh);
            if(pContrastRs!=null&&pContrastRs.First()){
                int pContrastCount = pContrastRs.getRowCount();
                for(int iContrastIndex=0;iContrastIndex<pContrastCount;iContrastIndex++){
                    pContrastRs.setRowIndex(iContrastIndex);
                    pYwdh = pContrastRs.getStringValue("F_YWDH");
                    pContrastObj = new JSONObject();
                    pContrastObj.put("F_PZBH",pPzbh);
                    pContrastObj.put("F_YWDH",pYwdh);
                    pReturnArr.add(pContrastObj);
                }
            }
        }
        JSONObject pReturnObj = new JSONObject();
        pReturnObj.put("errcode","0");
        pReturnObj.put("errmsg","调用成功");
        pReturnObj.put("row_count",pReturnArr.size());
        pReturnObj.put("datas",pReturnArr);
        pReturnBean.setReturnObject(pReturnObj);
        return pReturnBean;
    }

    /**
     * 获取凭证编号结果集
     * @param pLink
     * @param pKey
     * @return
     */
    private IDalResultSet getPzbh(IDalConnection pLink, String pKey) throws Exception {
        String pSql = "SELECT F_HSPZH FROM NHLH_SSC_TYGDXX WHERE F_SSDJBH=?";
        IDalResultSet pRs = TSqlUtils.QueryPreparedSql(pLink,pSql,pKey);
        if(pRs==null||!pRs.First()){
            throw new Exception("未查到凭证编号");
        }
        return pRs;
    }
}
