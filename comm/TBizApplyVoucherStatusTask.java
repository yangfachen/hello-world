package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 修改申请单状态
 */
public class TBizApplyVoucherStatusTask extends TBizTaskParamJson {

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pDjbh = pParamJson.optString("F_PKEY");
        String pVchrType = pParamJson.optString("F_VCHR_TYPE");
        String pTableName="";
        StringBuffer pQueryBxTableSql=new StringBuffer();
        IDalResultSet pQueryBxTableRs=null;
        if("BZ0101".equals(pVchrType)){
            pTableName="NHLH_CCSQ";
            pQueryBxTableSql.append("SELECT 1 FROM NHLH_CCBX WHERE F_CCSQBH=? AND F_DJZT!='9'");
            pQueryBxTableRs=TSqlUtils.QueryPreparedSql(pLink,pQueryBxTableSql.toString(),pDjbh);
        }else if("BZ0104".equals(pVchrType)){
            pTableName="NHLH_TYSQ";
            pQueryBxTableSql.append("SELECT 1 FROM NHLH_TYBX_FYMX A LEFT JOIN NHLH_TYBX B ON A.F_SSDJBH=B.F_PKEY WHERE A.F_SSDJBH=? AND B.F_DJZT!='9' UNION ALL SELECT 1 FROM NHLH_TYZFSQ_ZFSQMX C LEFT JOIN NHLH_TYZFSQ D ON C.F_SSDJBH=D.F_PKEY WHERE C.F_SSDJBH=? AND D.F_DJZT!='9'");
            pQueryBxTableRs=TSqlUtils.QueryPreparedSql(pLink,pQueryBxTableSql.toString(),pDjbh,pDjbh);
        }else{
            throw new Exception(String.format("传入的单据类型参数【%s】错误", pVchrType));
        }
        if(pQueryBxTableRs==null){
            throw new Exception(String.format("查询错误"));
        }
        if(pQueryBxTableRs.getRowCount()>0){
            throw new Exception(String.format("申请单已被[%s]关联",pTableName));
        }
        StringBuffer pUpdateStatusSql = new StringBuffer("");
        pUpdateStatusSql.append("UPDATE ");
        pUpdateStatusSql.append(pTableName);
        pUpdateStatusSql.append(" SET F_IS_CLOSED = '2' WHERE F_PKEY = ?");
        int pUpdateRows = TSqlUtils.UpdatePreparedSql(pLink, pUpdateStatusSql.toString(), pDjbh);
        if (pUpdateRows == 1) {
            pReturnBean.setProcessOk(true);
            pReturnBean.setProcessCode("0");
            pReturnBean.setProcessMsg("无需报销成功");
            return pReturnBean;
        }
        pReturnBean.setProcessOk(false);
        pReturnBean.setProcessCode("1");
        pReturnBean.setProcessMsg("修改失败");
        return pReturnBean;
    }
}
