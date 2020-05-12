package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSBizTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * @author:yhj
 * @title:查询付款单信息,调用银行回单接口
 * @CreateTime:2019/11/19 10:20
 */
public class TBizQueryFkdInfoTask extends TBizReceiveBaseTask {

    private static final String ARCH_CMD_LOCK_SQL = "SELECT 1 FROM NHLH_ZJJS_FKD WHERE F_PKEY=? FOR UPDATE NOWAIT";
    private static final String ARCH_CMD_SQL = "SELECT F_PKEY,F_YHZH_FKF,F_YHJYLSH,F_JHFKHID,F_EVENT_ID,F_YWST,F_FKRQ FROM NHLH_ZJJS_FKD WHERE F_DJZT='1' AND F_SFPPHD='0' AND F_SFYPP='0'";
    private static final String ARCH_ISPP_UPDATE_SQL = "UPDATE NHLH_ZJJS_FKD SET F_SFYPP='1',F_SFPPHD='1' WHERE F_PKEY=?";
    private static final String UPDATE_ARCH_STATUS = "UPDATE NHLH_ARCH_CMD SET F_BIZ_STA ='0' WHERE F_DJBH = ?";

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        IDalResultSet pArchRS = TSqlUtils.QueryPreparedSql(pLink, ARCH_CMD_SQL);
        if (pArchRS == null || !pArchRS.First()) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("不存在待查找银行回单的付款单");
            return pReturnBean;
        }

        String pServiceId = pTaskBean.getConfigString(TWSBizTask.KEY_TASK_REF_SERVICE_ID);
        int pCmdCount = pArchRS.getRowCount();
        String pKey = "";
        String pYhjylsh = "";
        String pJhfkhId = "";
        String pEventId = "";
        String pHszt = "";
        String pKhbm = "";
        String pYhzh = "";
        String pFkrq = "";
        String pYear = "";
        String pMonth = "";
        StringBuffer pRealDir = new StringBuffer();
        for (int iIndex = 0; iIndex < pCmdCount; iIndex++) {
            pArchRS.setRowIndex(iIndex);
            pRealDir.setLength(0);
            pKey = pArchRS.getStringValue("F_PKEY");
            pYhjylsh = pArchRS.getStringValue("F_YHJYLSH");
            pJhfkhId = pArchRS.getStringValue("F_JHFKHID");
            if(TWSUtil.isNullText(pJhfkhId)){
                pJhfkhId = "0000";
            }
            pEventId = pArchRS.getStringValue("F_EVENT_ID");
            pHszt = pArchRS.getStringValue("F_YWST");
            pKhbm = TWSUtil.getFieldValue(pLink,"NHLH_DCT_HSZT","F_KHBM","F_BM",pHszt);
            pYhzh = pArchRS.getStringValue("F_YHZH_FKF");
            pFkrq = pArchRS.getStringValue("F_FKRQ");
            pYear = pFkrq.substring(0,4);
            pMonth = pFkrq.substring(0,6);
            pRealDir.append(pKhbm).append("/").append(pYhzh).append("/").append(pYear).append("/").append(pMonth).append("/").append(pFkrq);

            this.executeOneCmd(pLink, pServiceId, pKey, pYhjylsh, pJhfkhId, pEventId, pRealDir.toString());
        }
        return pReturnBean;
    }

    protected void executeOneCmd(IDalConnection pLink, String pServiceId, String pKey, String pYhjylsh, String pJhfkhId, String pEventId, String pRealDir) throws Exception {
        try {
            //锁住当前行
            TSqlUtils.UpdatePreparedSql(pLink, ARCH_CMD_LOCK_SQL, pKey);
            JSONObject pParamObj = new JSONObject();
            pParamObj.put("F_DJBH",pKey);
            pParamObj.put("F_DJLX","BZ0207");
            pParamObj.put("F_USER_ID","");
            pParamObj.put("F_USER_NAME","");
            pParamObj.put("F_YHJYLSH",pYhjylsh);
//            pParamObj.put("F_JHFKHID",pJhfkhId);
            pParamObj.put("F_EVENT_ID",pJhfkhId);//此处为了不大改代码,直接将计划付款行ID放入此字段
            pParamObj.put("F_WORK_DIR",pRealDir);
            pParamObj.put("F_BANK_RECEIPT_TYPE","1");
            TTaskReturnBean pReturnBean = null;
            try {
                pReturnBean = TBizTask.callBizTask(null, pLink, "FSSC", pServiceId, pParamObj);
                if (!pReturnBean.isProcessOk()) {
                    throw new Exception(pReturnBean.getProcessMsg());
                }
                TSqlUtils.UpdatePreparedSql(pLink, ARCH_ISPP_UPDATE_SQL, pKey);
                try {
                    TSqlUtils.UpdatePreparedSql(pLink, UPDATE_ARCH_STATUS, new String[]{pKey});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Throwable throwable) {
                throw new Exception(throwable);
            }
            pLink.Commit();
        } catch (Exception e) {
            e.printStackTrace();
            pLink.RollBack();
        }
    }

}
