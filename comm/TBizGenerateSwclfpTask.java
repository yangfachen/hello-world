package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.bizlayer.bizmessage.vchrexchange.interfaces.IVchrExchange;
import com.eai.bizlayer.talk.services.interfaces.IBizTalkServiceHome;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.runtime.TEAIEnv;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import net.sf.json.JSONObject;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * @author skylin
 * <p>CreateTime:2019-08-17 10:15:01</p>
 * <p>
 *     生成事务处理发票
 *     入参
 *         1.${F_VCHR_ID}
 *         2.${F_VCHR_KEY}
 * </p>
 */
public class TBizGenerateSwclfpTask extends TBizReceiveBaseTask {
    private IBizTalkServiceHome mTalkService  = null;

    protected void onPrepare() {
        super.onPrepare();

        try {
            mTalkService = (IBizTalkServiceHome) TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizTalkService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        PreparedStatement pUpdateTalkLogSta = null;
        String pUpdateTalkLogSql = "UPDATE BIZ_TALK_LOG SET F_POOL_ID=?,F_IS_CANCEL=?,F_RED=? WHERE F_SRC_KEY=? AND F_RULE_ID=?";

        try {
            String pSrcVchrId = pParamJson.optString("F_VCHR_ID");
            String pSrcVchrKey = pParamJson.optString("F_VCHR_KEY");

            List<String[]> pRuleIdList = mTalkService.QueryTalkRuleInfoListByVchrId(pLink, pSrcVchrId);
            if(pRuleIdList.size() == 0){
                return pReturnBean;
            }

            pUpdateTalkLogSta = pLink.getJdbcConnection().prepareStatement(pUpdateTalkLogSql);

            String[] pRuleInfo = null;
            String pRuleId = null;
            String pRed = null;

            for(int iIndex = 0;iIndex < pRuleIdList.size(); iIndex++){
                pRuleInfo = pRuleIdList.get(iIndex);
                pRuleId = pRuleInfo[0];
                pRed = pRuleInfo[1];

                if(pRuleId != null && !"".equals(pRuleId)){

                    /**
                     * 生成凭证
                     */
                    IVchrExchange pTrgtVchr = mTalkService.ConvertVoucher(pLink, pSrcVchrId, pSrcVchrKey, pRuleId);

                    if(pTrgtVchr.getVoucherData(pLink, pTrgtVchr.getVoucherId()).getRowCount() == 0){
                        continue;
                    }

                    pUpdateTalkLogSta.setString(1, "0001");
                    pUpdateTalkLogSta.setString(2, "0");
                    pUpdateTalkLogSta.setString(3, pRed);
                    pUpdateTalkLogSta.setString(4, pSrcVchrKey);
                    pUpdateTalkLogSta.setString(5, pRuleId);
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
}
