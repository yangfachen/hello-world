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
 * @author skylin
 * <p>更新报账系统流程状态</p>
 * <p>CreateTime:2019-07-20 14:30:01</p>
 */
public class TBizUpdateBZFlowStatusTask extends TBizReceiveBaseTask {
    private static final String UPDATE_SC_BACK_INFO = "UPDATE BF_BIZ_INFO SET F_REJECT_REASON=?,F_GO_BACK=?,F_SFCXSM=? WHERE F_DJBH=?";
    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pOptUser = "SC.VIRT.USER";
        String pOptName = "共享审核岗";
        this.fillSessionParam(null, pLink, pOptUser, pOptName);

        String pDjlx = pParamJson.optString("F_DJLX");
        String pDjbh = pParamJson.optString("F_DJBH");
        String pYwxt = pParamJson.optString("F_YWXT");
        //1审批通过2审批拒绝
        String pOpType = pParamJson.optString("F_OP_TYPE");
        String pOptMsg = pParamJson.optString("F_OP_MSG");
        String pReason = pParamJson.optString("F_REASON");

        /**
         * 处理当F_OPT_TYPE为空时的逻辑
         * 判断F_REASION是否为空
         * 1.为空，认为是同意
         * 2.不为空，认为是退回
         */
        if (TWSUtil.isNullText(pOpType)) {
            pOpType = TWSUtil.isNullText(pReason) ? "1" : "2";
        }

        String pFlowSql = "SELECT RT.FL_INST_SN AS F_FLOW_SN, RT.FL_OWNER_FLOW AS F_FLOW_ID, TK.F_TASK_SN, RT.FL_BASE_DRV_OBJ AS F_DJBH, RT.FL_BASE_DRV_TYP AS F_VCHR_ID, RT.FL_BASE_PUR_TYP AS F_DJLX, DJLX.F_STO_ID FROM BF_FLOW_RT RT LEFT JOIN IC_BX_DJLX DJLX ON RT.FL_BASE_PUR_TYP = DJLX.F_BH LEFT JOIN BF_TASK TK ON RT.FL_INST_SN = TK.F_FLOW_SN WHERE RT.FL_BASE_DRV_OBJ = ? ";
        IDalResultSet pFlowRS = TSqlUtils.QueryPreparedSql(pLink, pFlowSql, pDjbh);
        if (pFlowRS == null || !pFlowRS.First()) {
            String pErrMsg = String.format("单据编号[%s]未发起流程", pDjbh);
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("2");
            pReturnBean.setProcessMsg(pErrMsg);

            pRspObj.put("errcode", "2");
            pRspObj.put("errmsg", pErrMsg);
            return pReturnBean;
        }

        String pTaskSn = pFlowRS.getStringValue("F_TASK_SN");
        String pFlowId = pFlowRS.getStringValue("F_FLOW_ID");
        String pFlowSn = pFlowRS.getStringValue("F_FLOW_SN");
        String pVchrId = pFlowRS.getStringValue("F_VCHR_ID");

        System.out.println(String.format("更新报账系统系统流程状态:F_DJLX=%s;F_DJBH=%s;F_YWXT=%s;F_OP_TYPE=%s", pDjlx, pDjbh, pYwxt, pOpType));

        /**
         * 记录共享退回原因
         */
        this.updateSCBackInfo(pLink, pParamJson);

        /**
         * 退回到制单人
         */
        String[] pResult = mBizCommService.approveTask(pLink, pTaskSn, pFlowId, pFlowSn, pVchrId, pDjbh, pOptMsg, pOpType);
        //审核成功
        if (!"0".equals(pResult[0])) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode(pResult[0]);
            pReturnBean.setProcessMsg(pResult[1]);

            pRspObj.put("errcode", pResult[0]);
            pRspObj.put("errmsg", pResult[1]);
            return pReturnBean;
        }

        pRspObj.put("errcode", "0");
        pRspObj.put("errmsg", String.format("报账单据编号[%s]流程状态更新成功", pDjbh));

        return pReturnBean;
    }

    protected void updateSCBackInfo(IDalConnection pLink, JSONObject pParamJson) throws Exception {
        String pDjbh = pParamJson.optString("F_DJBH");
        String pReason = pParamJson.optString("F_REASON");
        String isGoBack = pParamJson.optString("isGoBack", "0");
        String pSfcxsm = pParamJson.optString("F_SFCXSM", "0");
        TSqlUtils.UpdatePreparedSql(pLink, UPDATE_SC_BACK_INFO, pReason, isGoBack, pSfcxsm, pDjbh);
    }
}
