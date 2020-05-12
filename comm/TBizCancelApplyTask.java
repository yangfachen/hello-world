package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * AP单据取消申请
 * AP_CANCEL_APPLY
 */
public class TBizCancelApplyTask extends TBizTask {

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Throwable {
        this.classifyDeal(pLink, pLogLink, pTaskBean, pReturnBean);
        pReturnBean.setProcessOk(true);
        pReturnBean.setProcessCode("0");
        pReturnBean.setProcessMsg("取消成功");
        return pReturnBean;
    }

    /**
     * 根据单据类型处理
     * 通用对公支付             BZ0302
     * 预付款申请              BZ0301
     * 费用暂估                BZ0304
     * 禽批次运费结算单         BZ0308
     * 饲料采购运费结算单        BZ0309
     * 禽-冷藏销售运费结算单     BZ0310
     * 毛鸡鸭结算单             BZ0311
     * 寄养费结算单             BZ0312
     * 采购结算单              BZ0313
     *
     * @param pLink
     * @param pLogLink
     * @param pTaskBean
     * @param pReturnBean
     */
    private void classifyDeal(IDalConnection pLink, IDalConnection pLogLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pParamObject = (JSONObject) pTaskBean.getParamObject();
        String pDjlx = pParamObject.optString("F_DJLX");
        String pVoucherKey = pParamObject.optString("F_PKEY");
        if ("".equals(pVoucherKey)) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("所传参数F_PKEY为空");
            return;
        }
        if ("BZ0302".equals(pDjlx)) {
            this.callService(pLink, pLogLink, pVoucherKey, pReturnBean);
        } else if ("BZ0301".equals(pDjlx)) {
            this.callService2(pLink, pLogLink, pVoucherKey, pReturnBean);
        } else if ("BZ0304".equals(pDjlx)) {
            this.callService3(pLink, pLogLink, pVoucherKey, pReturnBean);
        } else if ("BZ0308".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_QPCYF", "9", pVoucherKey);
        } else if ("BZ0309".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_SLCGYF", "9", pVoucherKey);
        } else if ("BZ0310".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_QLCXSYF", "9", pVoucherKey);
        } else if ("BZ0311".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_MJYHS", "9", pVoucherKey);
        } else if ("BZ0312".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_JYF", "9", pVoucherKey);
        } else if ("BZ0313".equals(pDjlx)) {
            this.configVchrStatus(pLink, "NHLH_JS_CG", "9", pVoucherKey);
        } else {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg(String.format("所传参数F_DJLX【%s】无效", pDjlx));
            return;
        }
    }

    /**
     * 费用暂估
     *
     * @param pLink
     * @param pLogLink
     * @param pVoucherKey
     * @param pReturnBean
     */
    private void callService3(IDalConnection pLink, IDalConnection pLogLink, String pVoucherKey, TTaskReturnBean pReturnBean) throws Exception {
        String pUpdateSql = "UPDATE NHLH_FYZGSQ SET F_SFWXFQ  = ?  WHERE F_PKEY = ?";
        TSqlUtils.UpdatePreparedSql(pLink, pUpdateSql, new String[]{"1", pVoucherKey});
    }

    /**
     * 预付款申请
     *
     * @param pLink
     * @param pLogLink
     * @param pVoucherKey
     * @param pReturnBean
     */
    private void callService2(IDalConnection pLink, IDalConnection pLogLink, String pVoucherKey, TTaskReturnBean pReturnBean) throws Exception {
        this.configVchrStatus(pLink, "NHLH_YFKSQ", "9", pVoucherKey);
    }

    /**
     * 修改单据状态并且修改BF_BIZ_INFO中的单据状态
     */
    private void configVchrStatus(IDalConnection pLink, String pTableName, String pDjzt, String pVoucherKey) throws Exception {
        StringBuffer pUpdateSql = new StringBuffer();
        pUpdateSql.append("UPDATE ");
        pUpdateSql.append(pTableName);
        pUpdateSql.append(" SET F_DJZT = ? WHERE F_PKEY = ?");
        TSqlUtils.UpdatePreparedSql(pLink, pUpdateSql.toString(), pDjzt, pVoucherKey);
        String pSql = "UPDATE BF_BIZ_INFO SET F_SPZT = '已作废' WHERE F_DJBH = ?";
        TSqlUtils.UpdatePreparedSql(pLink, pSql, pVoucherKey);
    }

    /**
     * 通用支付申请
     *
     * @param pLink
     * @param pLogLink
     * @param pVoucherKey
     * @param pReturnBean
     * @throws Exception
     */
    private void callService(IDalConnection pLink, IDalConnection pLogLink, String pVoucherKey, TTaskReturnBean pReturnBean) throws Exception {
        if ("".equals(pVoucherKey)) {
            throw new Exception("获取不到参数F_PKEY");
        }
        try {
            String pUpdateDjztSql = "UPDATE NHLH_TYSQ SET F_DJZT = '9' WHERE F_PKEY = ?";
            TSqlUtils.UpdatePreparedSql(pLink, pUpdateDjztSql, pVoucherKey);
            String pSqdh = "";
            List<String> pSqdhList = new ArrayList<String>();
            String pQuerySqdhSql = "SELECT F_SQSQBH FROM NHLH_TYZFSQ_ZFSQMX WHERE F_SSDJBH = ?";
            IDalResultSet pSqdhRs = TSqlUtils.QueryPreparedSql(pLink, pQuerySqdhSql, pVoucherKey);
            if (pSqdhRs != null && pSqdhRs.First()) {
                int pRowCount = pSqdhRs.getRowCount();
                for (int iIndex = 0; iIndex < pRowCount; iIndex++) {
                    pSqdhRs.setRowIndex(iIndex);
                    pSqdh = pSqdhRs.getStringValue("F_SQSQBH");
                    pSqdhList.add(pSqdh);
                }
            }
            this.UpdateApplyVoucherStatus(pLink, pSqdhList);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 修改日常费用申请单中长期申请的F_IS_CLOSED字段为0
     *
     * @param pLink
     * @param pSqdhList
     */
    private void UpdateApplyVoucherStatus(IDalConnection pLink, List<String> pSqdhList) throws Exception {
        PreparedStatement pStatement = null;
        String pUpdateSql = "UPDATE NHLH_TYSQ SET F_IS_CLOSED = '0' WHERE F_SFCQXSQ = '1' AND F_PKEY = ?";
        try {
            pStatement = pLink.getJdbcConnection().prepareStatement(pUpdateSql);
            for (String pVoucherKey : pSqdhList) {
                pStatement.setString(1, pVoucherKey);
                pStatement.addBatch();
            }
            pStatement.executeUpdate();
        } catch (Exception e) {
            throw e;
        } finally {
            if (pStatement != null) {
                pStatement.close();
            }
        }
    }
}
