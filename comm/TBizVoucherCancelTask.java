package com.pansoft.nhlh.biztask.comm;

import com.bizapps.ttf.bizbase.service.IBizService;
import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.frame.runtime.TEAIEnv;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.voucher.IBizVoucher;
import com.pansoft.reimb.cnst.TCommonConst;
import com.pansoft.reimb.tools.TVoucherTool;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * @author skylin
 * <p>CreateTime:2019-07-26 09:57:01</p>
 * <p>
 *     单据作废
 * </p>
 */
public class TBizVoucherCancelTask extends TBizTaskParamJson {
    private IBizService mBizService = null;

    protected void onPrepare() {
        super.onPrepare();
        try {
            mBizService = (IBizService) TEAIEnv.QueryServiceLocalInterface("TTFApplication", "BizService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pVchrId = pParamJson.optString("F_VCHR_ID");
        String pVchrPK = pParamJson.optString("F_VCHR_KEY");

        IBizVoucher pVoucher = (IBizVoucher) mBizService.createInstance(pLink, pVchrId, pVchrPK);
        String pVchrStatus = pVoucher.getDjzt();

        /**
         * 是否允许作废
         */
        if(!TVoucherTool.STU_VCHR_BACK.equals(pVchrStatus) && !TVoucherTool.STU_VCHR_ATTACH.equals(pVchrStatus) && TVoucherTool.STU_VCHR_SUPL.equals(pVchrStatus)){
            throw new Exception("当前单据状态不许作废");
        }

        /**
         * 作废流程
         */
        String pCancelTime = StringTool.getFormatDate(null, TCommonConst.DEFAULT_TIME_FMT);
        String pSql = "UPDATE BF_FLOW_RT SET FL_STU='6', FL_STOP_TIME=? WHERE FL_BASE_DRV_TYP = ? AND FL_BASE_DRV_OBJ = ?";
        TSqlUtils.UpdatePreparedSql(pLink, pSql, pCancelTime, pVchrId, pVchrPK);

        String pBizInfoSql = "UPDATE BF_BIZ_INFO SET F_DJZT='9' WHERE F_DJBH=?";
        TSqlUtils.UpdatePreparedSql(pLink, pBizInfoSql, pVchrPK);

        /**
         * OAMQMESSAGES表中的所有相关任务状态置为撤销
         */
        this.cancelOATask(pLink,pVchrPK);
        String pOASql = "UPDATE OAMQMESSAGES SET FLOWMESS=9,STATUS='1' WHERE F_VCHR_KEY =?";
        TSqlUtils.UpdatePreparedSql(pLink, pOASql, pVchrPK);

        /**
         * 单据类取消逻辑
         */
        pVoucher.cancel(pLink);

        pVoucher.setDjzt(TVoucherTool.STU_VCHR_CANCEL);
        pVoucher.updateMasterVoucher(pLink, null);

        pReturnBean.setProcessOk(true);
        pReturnBean.setProcessCode("0");
        pReturnBean.setProcessMsg("单据作废成功");
        pReturnBean.setReturnJsonObject();
        return pReturnBean;
    }

    /**
     * 取消OA待办
     * @param pLink
     * @param pVchrPK
     */
    protected void cancelOATask(IDalConnection pLink, String pVchrPK) throws Exception {
        String pQueryTaksInfoSql = "SELECT * FROM BF_TASK_USER WHERE F_TASK_SN IN(SELECT F_TASK_SN FROM BF_TASK WHERE F_FLOW_SN=(SELECT FL_INST_SN FROM BF_FLOW_RT WHERE FL_BASE_DRV_OBJ=?))";
        IDalResultSet pTaskInfoRS = TSqlUtils.QueryPreparedSql(pLink, pQueryTaksInfoSql, new String[]{pVchrPK});
        if (pTaskInfoRS != null && pTaskInfoRS.First()) {
            int pRowCount = pTaskInfoRS.getRowCount();
            for (int iIndex = 0; iIndex < pRowCount; iIndex++) {
                pTaskInfoRS.setRowIndex(iIndex);
                try {
                    JSONObject pTaskParam = new JSONObject();
                    JSONObject pTarObj = new JSONObject();
                    String pUserID = pTaskInfoRS.getStringValue("F_USER_ID");
                    String pSSOID = getSSOID(pLink, pUserID);
                    pTarObj.put("LoginName", pSSOID);
                    pTaskParam.put("modelId", pTaskInfoRS.getStringValue("F_TASK_SN"));
                    pTaskParam.put("targets", pTarObj);
                    pTaskParam.put("optType", "1");

                    String pServicId = "API_OA_TASK_REMOVE";
                    TBizTask.callBizTask(null, null, "FSSC", pServicId, pTaskParam);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取单点用户
     * @param pLink
     * @param pUserID
     * @return
     */
    private String getSSOID(IDalConnection pLink, String pUserID) throws Exception {
        String pSSOID = "";
        String pQuerySql = "SELECT USR_DESCRIPTION FROM SSF_USERS WHERE USR_USRID = ?";
        IDalResultSet pQueryRS = TSqlUtils.QueryPreparedSql(pLink, pQuerySql, new String[]{pUserID});
        if (pQueryRS != null && pQueryRS.First()) {
            pSSOID = pQueryRS.getStringValue("USR_DESCRIPTION");
        }
        return pSSOID;
    }
}
