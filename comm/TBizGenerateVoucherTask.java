package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TDOOperator;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.bizlayer.bizmessage.vchrexchange.interfaces.IVchrExchange;
import com.eai.bizlayer.service.interfaces.IBizVoucherServiceHome;
import com.eai.bizlayer.talk.services.interfaces.IBizTalkServiceHome;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.frame.runtime.TEAIEnv;
import com.eai.toolkit.text.StringTool;
import com.eai.tools.lang.TRCI;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.pansoft.talk.IBizMultiTalkExecutor;
import com.saf.sql.utils.TSqlUtils;
import com.sun.star.deployment.UpdateInformationProvider;
import net.sf.json.JSONObject;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skylin
 * <p>CreateTime:2019-08-17 10:15:01</p>
 * <p>
 *     生成凭证
 *     入参
 *         1.${F_VCHR_ID}
 *         2.${F_VCHR_KEY}
 * </p>
 */
public class TBizGenerateVoucherTask extends TBizReceiveBaseTask {
    private static final String BIZ_TALK_PLUGIN_KEY = "BIZ_TALK_TRANSFER";
    private static final String BIZ_TALK_PLUGIN_SQL = "SELECT F_PLUGIN_NAME,F_PLUGIN_CLASS FROM SYS_PLUGIN_CONF WHERE F_BIZ_ID=? AND F_PLUGIN_ID=?";
    private static final String MULTI_TALK_PICE_INSERT_SQL = "INSERT BIZ_TALK_MULTI_PICE(F_PKEY,F_RULE_ID,F_SRC_KEY,F_BIZ_KEY,F_INDEX) VALUES(?,?,?,?,?)";
    private static final String DELETE_NOT_MATCH_RULE_VOUCHER_SQL = "DELETE FROM BIZ_TALK_POOL WHERE F_RULE_ID=? AND F_SRC_KEY=?";
    private IBizTalkServiceHome mTalkService  = null;
    private IBizVoucherServiceHome mVoucherService = null;

    protected void onPrepare() {
        super.onPrepare();

        try {
            mTalkService = (IBizTalkServiceHome) TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizTalkService");
            mVoucherService = (IBizVoucherServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizVoucherService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pDjbh = pParamJson.optString("F_VCHR_KEY");
        String pDjlx = pParamJson.optString("F_DJLX");
        if ("BZ0102".equals(pDjlx) || "BZ0105".equals(pDjlx) || "BZ0103".equals(pDjlx) || "BZ0301".equals(pDjlx) || "BZ0302".equals(pDjlx) || "BZ0314".equals(pDjlx)) {
            if (TNhlhCommonTool.allow2do(pLink, "splitPlanBeforeGenPz")) {
                splitPlanBeforeGenPz(pLink, pDjbh);
            }
        }
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
            String pRuleFilterExp = null;
            IVchrExchange pSrcData = this.mVoucherService.QueryBizVoucherData(pLink, pSrcVchrId, pSrcVchrKey, false);
            IVchrExchange pTmpSrcData = null;

            for(int iIndex = 0;iIndex < pRuleIdList.size(); iIndex++){
                pRuleInfo = pRuleIdList.get(iIndex);
                pRuleId = pRuleInfo[0];
                pRed = pRuleInfo[1];
                pYwlx = pRuleInfo[2];
                pRuleFilterExp = pRuleInfo[3];

                if(pRuleId != null && !"".equals(pRuleId)){
                    /**
                     * 生成凭证
                     */
                    mTalkService.FlushTalkDefine(pLink, pRuleId);

                    pTmpSrcData = pSrcData.clone();
                    if (!this.isMultiTalk(pLink, pRuleId, pYwlx)) {
                        this.transferOneRow(pLink, pUpdateTalkLogSta, pTmpSrcData, pRuleId, pSrcVchrKey, pSrcVchrKey, pYwlx, pRed, pOptUser, pOptUName, pRuleFilterExp);
                    } else {
                        this.transferMultiRows(pLink, pUpdateTalkLogSta, pTmpSrcData, pRuleId, pSrcVchrKey, pYwlx, pRed, pOptUser, pOptUName, pRuleFilterExp);
                    }

//                    IVchrExchange pTrgtVchr = mTalkService.ConvertVoucher(pLink, pSrcVchrId, pSrcVchrKey, pRuleId);

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

    private void splitPlanBeforeGenPz(IDalConnection pLink, String pDjbh) {
        try {
            JSONObject pParamObj = new JSONObject();
            pParamObj.put("F_PKEY", pDjbh);
            TBizTask.callBizTask(null, pLink, "FSSC", "BZ_PLANTOPAY_SPLIT", pParamObj);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    protected void isCanGenerate(IDalConnection pLink, String pSrcKey, String pRuleId, String pBizKey) throws Exception {
        String pSql = "SELECT 1 FROM BIZ_TALK_POOL WHERE F_SRC_KEY=? AND F_RULE_ID=? AND F_BIZ_KEY=? AND F_TRS_OK='1'";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pSrcKey, pRuleId, pBizKey);
        if (pRS != null && pRS.First()) {
            throw new Exception(String.format("原单据[%s]转换规则[%s]业务主键[%s]已推送EBS，不允许重新生成！", pSrcKey, pRuleId, pBizKey));
        }
    }

    /**
     * 单个业务主表转换
     * @param pLink
     * @param pUpdateTalkLogSta
     * @param pTmpSrcData
     * @param pRuleId
     * @param pSrcVchrKey
     * @param pYwlx
     * @param pRed
     * @param pOptUser
     * @param pOptUName
     * @throws Exception
     */
    protected IVchrExchange transferOneRow(IDalConnection pLink, PreparedStatement pUpdateTalkLogSta, IVchrExchange pTmpSrcData, String pRuleId, String pBizKey, String pSrcVchrKey, String pYwlx, String pRed, String pOptUser, String pOptUName, String pRuleFilterExp) throws Exception {
        //不满足转换条件，删除此规则之前生成的凭证
        pTmpSrcData.initExpDriver();
        String pCompileText = pTmpSrcData.compileExpression(pRuleFilterExp);
        if (!TWSUtil.isNullText(pCompileText) && !pTmpSrcData.calcExpressionTextAsBoolean(pCompileText)) {
//            this.deleteOldVoucher(pLink, pRuleId, pSrcVchrKey);
            return null;
        }

        //是否允许生成
        this.isCanGenerate(pLink, pSrcVchrKey, pRuleId, pBizKey);

        IVchrExchange pTrgtVchr = mTalkService.ConvertVoucher(pLink, pTmpSrcData, pRuleId);

        if(pTrgtVchr.getVoucherData(pLink, pTrgtVchr.getVoucherId()).getRowCount() == 0){
            return pTrgtVchr;
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

        /**
         * 记录新转换日志
         */
        this.cloneTalkLog(pLink, "0001", pRed, pRuleId, pBizKey, pSrcVchrKey, pYwlx);

        /**
         * 更新BIZ_TALK_POOL的核算主体
         */
        this.updateZrzx(pLink, pSrcVchrKey, pRuleId, pBizKey, pTrgtVchr);

        return pTrgtVchr;
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
        String pSql = "SELECT F_RULE_ID,F_RED,F_YWLX,F_FILTER_EXP FROM BIZ_TALK_RULE WHERE F_SRC_VCHR=? AND F_DISABLE='0'";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pSrcVchr);
        if (pRS != null && pRS.First()) {
            List<String[]> pInfos = new ArrayList<String[]>();
            int pCount = pRS.getRowCount();
            String[] pInfo = null;
            for (int iRowIndex = 0; iRowIndex < pCount; iRowIndex++) {
                pRS.setRowIndex(iRowIndex);
                pInfo = new String[4];
                pInfo[0] = pRS.getStringValue("F_RULE_ID");
                pInfo[1] = pRS.getStringValue("F_RED");
                pInfo[2] = pRS.getStringValue("F_YWLX");
                pInfo[3] = pRS.getStringValue("F_FILTER_EXP");
                pInfos.add(pInfo);
            }

            return pInfos;
        }

        return null;
    }

    /**
     * 是否一个单据生成多张凭证
     * @param pLink
     * @param pRuleId
     * @param pYwlx
     * @return
     * @throws Exception
     */
    protected boolean isMultiTalk(IDalConnection pLink, String pRuleId, String pYwlx) throws Exception {
        String pSql = "SELECT F_SFCF FROM BIZ_TALK_MULTI_CONF WHERE F_RULE_ID=?";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pRuleId);
        if (pRS == null || !pRS.First()) {
            return false;
        }

        return "1".equals(pRS.getStringValue("F_SFCF"));
    }

    protected String[] queryFilterConf(IDalConnection pLink, String pRuleId) throws Exception {
        String pSql = "SELECT F_VCHR_ID,F_FILTER FROM BIZ_TALK_MULTI_FILTER WHERE F_RULE_ID=?";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pRuleId);
        if (pRS != null && pRS.First()) {
            String[] pConf = new String[2];
            pConf[0] = pRS.getStringValue("F_VCHR_ID");
            pConf[1] = pRS.getStringValue("F_FILTER");
            return pConf;
        }

        return null;
    }

    /**
     * 找出符合条件的转换数据
     * @param pLink
     * @param pRuleId
     * @param pSrcVchrKey
     * @param pVchrData
     * @return
     * @throws Exception
     */
    protected List<String[]> insertTalkPice(IDalConnection pLink, String pRuleId, String pSrcVchrKey, IVchrExchange pVchrData, String[] pFilterConf) throws Exception {
        List<String[]> pPickKeys = new ArrayList<String[]>();
        try {
            if (pFilterConf == null) {
                throw new Exception(String.format("凭证转换规则启用了拆分，但未配置拆分子凭据ID", pRuleId));
            }

            String pVchrId = pFilterConf[0];
            String pFilterExp = pFilterConf[1];
            IDalResultSet pSrcRS = pVchrData.getVoucherData(pVchrId);

            int pSrcCount = pSrcRS.getRowCount();
            for (int iIndex = 0; iIndex < pSrcCount; iIndex++) {
                pSrcRS.setRowIndex(iIndex);

                if (TWSUtil.isNullText(pFilterExp)) {
                    pPickKeys.add(new String[] { pSrcRS.getStringValue("F_PKEY"), String.valueOf(iIndex)});
                    continue;
                }

                String pCompileText = pVchrData.compileExpression(pFilterExp);
                if (pVchrData.calcExpressionTextAsBoolean(pCompileText)) {
                    pPickKeys.add(new String[] { pSrcRS.getStringValue("F_PKEY"), String.valueOf(iIndex)});
                }
            }
        } catch (Exception e) {
            throw e;
        }

        return pPickKeys;
    }

    protected void transferMultiRows(IDalConnection pLink, PreparedStatement pUpdateTalkLogSta, IVchrExchange pSrcData, String pRuleId, String pSrcVchrKey, String pYwlx, String pRed, String pOptUser, String pOptUName, String pRuleFilterExp) throws Exception {
        IBizMultiTalkExecutor pMultiTalkExecutor = this.getMultiTalkExecutor(pLink, pRuleId, pYwlx);
        pMultiTalkExecutor.onTalkExecute(pLink, pUpdateTalkLogSta, pSrcData, pRuleId, pSrcVchrKey, pYwlx, pRed, pOptUser, pOptUName, pRuleFilterExp);
    }

    protected void cloneTalkLog(IDalConnection pLink, String pPoolId, String pRed, String pRuleId, String pBizKey, String pSrcKey, String pYwlx) throws Exception {
        String pSql = "SELECT * FROM BIZ_TALK_LOG WHERE F_RULE_ID=? AND F_SRC_KEY=?";
        IDalResultSet pTalkLog = TSqlUtils.QueryPreparedSql(pLink, pSql, pRuleId, pSrcKey);
        pTalkLog.First();

        String pExistSql = "SELECT 1 FROM BIZ_TALK_POOL WHERE F_RULE_ID=? AND F_BIZ_KEY=? AND F_SRC_KEY=?";
        IDalResultSet pExistRS = TSqlUtils.QueryPreparedSql(pLink, pExistSql, pRuleId, pBizKey, pSrcKey);
        if (pExistRS != null && pExistRS.First()) {
            String pCvtOK = pTalkLog.getStringValue("F_CVT_OK");
            String pCvtTime = pTalkLog.getStringValue("F_CVT_TIME");
            String pCvtKey = pTalkLog.getStringValue("F_CVT_KEY");

            String pOptUid = pLink.getRmtRunEnv().getUserID();
            String pOptUname = pLink.getRmtRunEnv().getUserName();
            String pUpdateTalkLogSql = "UPDATE BIZ_TALK_POOL SET F_CVT_OK=?,F_CVT_TIME=?,F_CVT_KEY=?,F_RED=?,F_YWLX=?,F_OP_UID=?,F_OP_UNAME=? WHERE F_SRC_KEY=? AND F_RULE_ID=? AND F_BIZ_KEY=?";
            TSqlUtils.UpdatePreparedSql(pLink, pUpdateTalkLogSql, pCvtOK, pCvtTime, pCvtKey, pRed, pYwlx, pOptUid, pOptUname, pSrcKey, pRuleId, pBizKey);

            return;
        }

        TDOOperator pTalkPoolOP = new TDOOperator(pLink, "BIZ_TALK_POOL");
        pTalkPoolOP.AppendEmptyRow();
        pTalkPoolOP.setStringValue("F_PKEY", StringTool.UUIDCreate());
        pTalkPoolOP.setStringValue("F_RULE_ID", pRuleId);
        pTalkPoolOP.setStringValue("F_BIZ_KEY", pBizKey);
        pTalkPoolOP.setStringValue("F_SRC_VCHR", pTalkLog.getStringValue("F_SRC_VCHR"));
        pTalkPoolOP.setStringValue("F_SRC_KEY", pSrcKey);
        pTalkPoolOP.setStringValue("F_TRGT_VCHR", pTalkLog.getStringValue("F_TRGT_VCHR"));
        pTalkPoolOP.setStringValue("F_CVT_OK", pTalkLog.getStringValue("F_CVT_OK"));
        pTalkPoolOP.setStringValue("F_CVT_KEY", pTalkLog.getStringValue("F_CVT_KEY"));
        pTalkPoolOP.setStringValue("F_CVT_TIME", pTalkLog.getStringValue("F_CVT_TIME"));
        pTalkPoolOP.setStringValue("F_CRT_TIME", pTalkLog.getStringValue("F_CRT_TIME"));
        pTalkPoolOP.setStringValue("F_POOL_ID", pPoolId);
        pTalkPoolOP.setStringValue("F_IS_CANCEL", "0");
        pTalkPoolOP.setStringValue("F_RED", pRed);
        pTalkPoolOP.setStringValue("F_YWLX", pYwlx);
        pTalkPoolOP.InsertRows(pLink);
    }

    /**
     * 更新核算主体
     * @param pLink
     * @param pSrcVchrKey
     * @param pRuleId
     * @param pBizKey
     * @param pTrgtVchr
     * @throws Exception
     */
    protected void updateZrzx(IDalConnection pLink, String pSrcVchrKey, String pRuleId, String pBizKey, IVchrExchange pTrgtVchr) throws Exception {
        try {
            String pVchrId = pTrgtVchr.getVoucherId();
            IDalResultSet pMasterRS = pTrgtVchr.getVoucherData(pVchrId);
            pMasterRS.First();
            String pZrzxCol = "F_GSBH";
            if ("BIZ_PZ".equals(pVchrId)) {
                pZrzxCol = "F_RSP_CNTR";
            }
            String pZrzx = pMasterRS.getStringValue(pZrzxCol);

            String pSql = "UPDATE BIZ_TALK_POOL SET F_ZRZX=? WHERE F_SRC_KEY=? AND F_RULE_ID=? AND F_BIZ_KEY=?";
            TSqlUtils.UpdatePreparedSql(pLink, pSql, pZrzx, pSrcVchrKey, pRuleId, pBizKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected IBizMultiTalkExecutor getMultiTalkExecutor(IDalConnection pLink, String pRuleId, String pYwlx) throws Exception {
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, BIZ_TALK_PLUGIN_SQL, BIZ_TALK_PLUGIN_KEY, pRuleId);
        if (pRS == null || !pRS.First()) {
            pRS = TSqlUtils.QueryPreparedSql(pLink, BIZ_TALK_PLUGIN_SQL, BIZ_TALK_PLUGIN_KEY, "$$");
            if (pRS == null || !pRS.First()) {
                throw new Exception(String.format("转换规则[%s]启用了多凭证转化，但未配置转换器！", pRuleId));
            }

            String pPluginClass = pRS.getStringValue("F_PLUGIN_CLASS");
            return (IBizMultiTalkExecutor) TRCI.MakeObjectInstance(pPluginClass);
        }

        String pPluginClass = pRS.getStringValue("F_PLUGIN_CLASS");
        return (IBizMultiTalkExecutor) TRCI.MakeObjectInstance(pPluginClass);
    }

    public void initEnv() {
        this.onPrepare();
    }

    /**
     * 删除之前生成的凭证，单目前不再符合
     * @param pLink
     * @param pRuleId
     * @param pSrcKey
     * @return
     * @throws Exception
     */
    protected int deleteOldVoucher(IDalConnection pLink, String pRuleId, String pSrcKey) throws Exception {
        return TSqlUtils.UpdatePreparedSql(pLink, DELETE_NOT_MATCH_RULE_VOUCHER_SQL, pRuleId, pSrcKey);
    }
}
