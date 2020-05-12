package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.bizlayer.bizmessage.vchrexchange.interfaces.IVchrExchange;
import com.eai.bizlayer.service.interfaces.IBizVoucherServiceHome;
import com.eai.bizlayer.talk.services.interfaces.IBizTalkServiceHome;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.frame.runtime.TEAIEnv;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skylin
 * <p>CreateTime:2019-08-17 10:15:01</p>
 * <p>
 *     同一个原始单据生成多张凭证
 *     入参
 *         1.${F_VCHR_ID}
 *         2.${F_VCHR_KEY}
 * </p>
 */
public class TBizGenerateVoucherMultiTask extends TBizReceiveBaseTask {
    private IBizTalkServiceHome mTalkService  = null;
    private IBizVoucherServiceHome mVoucherService = null;

    protected void onPrepare() {
        super.onPrepare();

        try {
            mTalkService = (IBizTalkServiceHome) TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizTalkService");
            this.mVoucherService = (IBizVoucherServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizVoucherService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        PreparedStatement pUpdateTalkLogSta = null;
        String pUpdateTalkLogSql = "UPDATE BIZ_TALK_LOG SET F_POOL_ID=?,F_IS_CANCEL=?,F_RED=?,F_YWLX=?,F_OP_UID=?,F_OP_UNAME=? WHERE F_SRC_KEY=? AND F_RULE_ID=?";

        try {
            String pSrcVchrId = pParamJson.optString("F_VCHR_ID");
            String pSrcVchrKey = pParamJson.optString("F_VCHR_KEY");

            if (TWSUtil.isNullText(pSrcVchrId)) {
                String pSrcDjlx = pParamJson.optString("F_DJLX");
                pSrcVchrId = this.getVchrIdOfDjlx(pLink, pSrcDjlx);
            }

            List<String[]> pRuleIdList = this.queryRuleInfo(pLink, pSrcVchrId);//mTalkService.QueryTalkRuleInfoListByVchrId(pLink, pSrcVchrId);
            if(pRuleIdList == null || pRuleIdList.size() == 0){
                return pReturnBean;
            }

            String pOptUser = pParamJson.optString("F_OPT_USER");
            String pOptUName = pParamJson.optString("F_OPT_UNAME");
            if (TWSUtil.isNullText(pOptUser)) {
                pOptUser = pLink.getRmtRunEnv().getUserID();
                pOptUName = pLink.getRmtRunEnv().getUserName();
            }

            pUpdateTalkLogSta = pLink.getJdbcConnection().prepareStatement(pUpdateTalkLogSql);

            String[] pRuleInfo = null;
            String pRuleId = null;
            String pRed = null;
            String pYwlx = null;
            IVchrExchange pSrcData = this.mVoucherService.QueryBizVoucherData(pLink, pSrcVchrId, pSrcVchrKey, false);
            IVchrExchange pTmpSrcData = null;
            for(int iIndex = 0;iIndex < pRuleIdList.size(); iIndex++){
                pRuleInfo = pRuleIdList.get(iIndex);
                pRuleId = pRuleInfo[0];
                pRed = pRuleInfo[1];
                pYwlx = pRuleInfo[2];

                if(pRuleId != null && !"".equals(pRuleId)){

                    /**
                     * 生成凭证
                     */
                    mTalkService.FlushTalkDefine(pLink, pRuleId);
                    IVchrExchange pTrgtVchr = mTalkService.ConvertVoucher(pLink, pTmpSrcData, pRuleId);

                    if(pTrgtVchr.getVoucherData(pLink, pTrgtVchr.getVoucherId()).getRowCount() == 0){
                        continue;
                    }

                    pUpdateTalkLogSta.setString(1, "0001");
                    pUpdateTalkLogSta.setString(2, "0");
                    pUpdateTalkLogSta.setString(3, pRed);
                    pUpdateTalkLogSta.setString(4, pYwlx);
                    pUpdateTalkLogSta.setString(5, pOptUser);        //操作人编码
                    pUpdateTalkLogSta.setString(6, pOptUName);       //操作人名称
                    pUpdateTalkLogSta.setString(7, pSrcVchrKey);
                    pUpdateTalkLogSta.setString(8, pRuleId);
                    pUpdateTalkLogSta.executeUpdate();
                }
            }

            pReturnBean.setProcessOk(true);
            pReturnBean.setProcessCode("0");
            pReturnBean.setProcessMsg("凭证生成成功");
        } catch (Exception e) {
            throw e;
        }
        finally{
            if (pUpdateTalkLogSta != null) {
                pUpdateTalkLogSta.close();
            }
        }

        return pReturnBean;
    }

    protected String getVchrIdOfDjlx(IDalConnection pLink, String pDjlx) throws Exception {
        String pSql = "SELECT F_BZ_ID AS F_VCHR_ID FROM IC_BX_DJLX WHERE F_BH=?";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pDjlx);
        if (pRS == null || !pRS.First()) {
            throw new Exception(String.format("单据类型[%s]不存在", pDjlx));
        }

        return pRS.getStringValue("F_VCHR_ID");
    }

    protected List<String[]> queryRuleInfo(IDalConnection pLink, String pSrcVchr) throws Exception {
        String pSql = "SELECT F_RULE_ID,F_RED,F_YWLX FROM BIZ_TALK_RULE WHERE F_SRC_VCHR=? AND F_DISABLE='0'";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pSrcVchr);
        if (pRS != null && pRS.First()) {
            List<String[]> pInfos = new ArrayList<String[]>();
            int pCount = pRS.getRowCount();
            String[] pInfo = null;
            for (int iRowIndex = 0; iRowIndex < pCount; iRowIndex++) {
                pRS.setRowIndex(iRowIndex);
                pInfo = new String[3];
                pInfo[0] = pRS.getStringValue("F_RULE_ID");
                pInfo[1] = pRS.getStringValue("F_RED");
                pInfo[2] = pRS.getStringValue("F_YWLX");
                pInfos.add(pInfo);
            }

            return pInfos;
        }

        return null;
    }
}
