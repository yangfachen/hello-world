package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.tanxiao.TBizYsTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

public class TBizYfkhxTask extends TBizYsTask {

    protected void callService(IDalConnection pLink, IDalConnection pLogLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamObject, JSONObject pReturnObject) throws Exception {
        String pStatus = pParamObject.optString("F_STATUS");
        String pDjlx = pParamObject.optString("F_DJLX");
        String pVchrKey = pParamObject.optString("F_PKEY");

        if (!"1".equals(pStatus) && !"2".equals(pStatus) && !"3".equals(pStatus)&& !"4".equals(pStatus)) {
            throw new Exception(String.format("修改核销台账状态错误，pStatus为【%s】", pStatus));
        }
        String pQueryFkxxSql = this.getFkxxSql(pLink, pDjlx);
        IDalResultSet pYfkInfoRS = TSqlUtils.QueryPreparedSql(pLink, pQueryFkxxSql, new String[]{pVchrKey});
        if (pYfkInfoRS != null && pYfkInfoRS.First()) {
            int pRowCount = pYfkInfoRS.getRowCount();
            for (int iIndex = 0; iIndex < pRowCount; iIndex++) {
                pYfkInfoRS.setRowIndex(iIndex);
                String pYfkbh = pYfkInfoRS.getStringValue("F_FPBH");
                if (TWSUtil.isNullText(pYfkbh)) {
                    continue;
                }
                double pHxje = pYfkInfoRS.getDoubleValue("F_HXJE");//40
                String pHszt = pYfkInfoRS.getStringValue("F_HSZT");
                String pGys = pYfkInfoRS.getStringValue("F_GYS");
                if ("1".equals(pStatus)) {
                    if (isExit(pLink, pYfkbh)) {
                        updateRecord(pLink, pHxje, pYfkbh, 0);
                    } else {
                        createNewRecord(pLink, pHxje, pYfkbh, pHszt, pGys);
                    }
                } else if ("2".equals(pStatus)) {
                    pHxje = -this.getLastZyJe(pLink, pYfkbh, pVchrKey);
                    updateRecord(pLink, pHxje, pYfkbh, 0);
                } else if ("3".equals(pStatus)) {
                    double pSfje = -this.getLastZyJe(pLink, pYfkbh, pVchrKey);
                    updateRecord(pLink, pSfje, pYfkbh, pHxje);
                    pHxje = pSfje;
                }else if ("4".equals(pStatus)) {//modify by Yangfc
                    double pZyje = this.getLastZyJe(pLink, pYfkbh, pVchrKey);//占用金额
                    double pCe = pHxje - pZyje;//实际核销金额与占用金额的差额
                    if (isExit(pLink, pYfkbh)) {
                        updateRecord(pLink, pCe, pYfkbh, 0);
                    } else {
                        createNewRecord(pLink, pCe, pYfkbh, pHszt, pGys);
                    }
                    if (pZyje > 0){
                        createMxRecord(pLink, -pZyje, pYfkbh, pVchrKey);
                    }
                }
                createMxRecord(pLink, pHxje, pYfkbh, pVchrKey);
            }
        }


    }

    private String getFkxxSql(IDalConnection pLink, String pDjlx) throws Exception {
        IDalResultSet pConfInfo = this.getYfkColConfInfo(pLink, pDjlx);
        String pCllx = pConfInfo.getStringValue("F_CLLX");
        String pTxxTable = pConfInfo.getStringValue("F_TXX_TABLE");
        String pFkxxTable = pConfInfo.getStringValue("F_FKXX_TABLE");
        String pHsztField = pConfInfo.getStringValue("F_HSZT_FIELD");
        String pGysField = pConfInfo.getStringValue("F_GYS_FIELD");
        String pHxjeField = pConfInfo.getStringValue("F_HXJE_FIELD");
        String pYfkfpField = pConfInfo.getStringValue("F_YFKFP_FIELD");
        String pHeaderTable = pConfInfo.getStringValue("F_HEADER_TABLE");
        StringBuilder pQueryFkxxSql = new StringBuilder();
        if ("1".equals(pCllx)) {
            pQueryFkxxSql.append("SELECT A.F_HXJE,A.F_FPBH,B.").append(pHsztField).append(" AS F_HSZT,B.").append(pGysField).append(" AS F_GYS FROM (SELECT SUM(").append(pHxjeField).append(") AS F_HXJE,").append(pYfkfpField).append(" AS F_FPBH,F_SSDJBH FROM ").append(pFkxxTable).append(" WHERE F_SSDJBH =? AND F_FKFS = 'APPLY' GROUP BY ").append(pYfkfpField).append(",F_SSDJBH) A LEFT JOIN ").append(pTxxTable).append(" B ON A.F_SSDJBH = B.F_PKEY");
        } else if ("2".equals(pCllx)) {
            pQueryFkxxSql.append("SELECT A.F_HXJE,A.F_FPBH,A.F_GYS,B.").append(pHsztField).append(" AS F_HSZT FROM(SELECT SUM(").append(pHxjeField).append(") AS F_HXJE,").append(pYfkfpField).append(" AS F_FPBH,").append(pGysField).append(" AS F_GYS,F_SSDJBH FROM ").append(pFkxxTable).append(" WHERE F_SSDJBH =? AND F_FKFS = 'APPLY' GROUP BY ").append(pYfkfpField).append(",").append(pGysField).append(",F_SSDJBH) A LEFT JOIN ").append(pTxxTable).append(" B ON A.F_SSDJBH = B.F_PKEY");
        } else if ("3".equals(pCllx)) {
            pQueryFkxxSql.append("SELECT SUM(F_HXJE) AS F_HXJE,F_FPBH,F_GYS,F_HSZT FROM (SELECT A.").append(pYfkfpField).append(" AS F_FPBH,A.").append(pHxjeField).append(" AS F_HXJE,B.").append(pGysField).append(" AS F_GYS,C.").append(pHsztField).append(" AS F_HSZT FROM (SELECT * FROM ").append(pFkxxTable).append(" WHERE F_SSDJBH =? AND F_FKFF = 'APPLY') A LEFT JOIN ").append(pTxxTable).append(" B ON A.F_VCHRID = B.F_PKEY LEFT JOIN ").append(pHeaderTable).append(" C ON A.F_SSDJBH = C.F_PKEY) GROUP BY F_FPBH,F_HSZT,F_GYS");
        } else if ("4".equals(pCllx)) {
            pQueryFkxxSql.append("SELECT SUM(F_HXJE) AS F_HXJE,F_FPBH,F_GYS,F_HSZT FROM (SELECT A.").append(pYfkfpField).append(" AS F_FPBH,A.").append(pHxjeField).append(" AS F_HXJE,B.").append(pGysField).append(" AS F_GYS,C.").append(pHsztField).append(" AS F_HSZT FROM (SELECT * FROM ").append(pFkxxTable).append(" WHERE F_SSDJBH =? AND F_FKFS = 'APPLY') A LEFT JOIN ").append(pTxxTable).append(" B ON A.F_SSMXDJBH = B.F_PKEY LEFT JOIN ").append(pHeaderTable).append(" C ON A.F_SSDJBH = C.F_PKEY) GROUP BY F_FPBH,F_HSZT,F_GYS");
        } else if ("5".equals(pCllx)) {
            pQueryFkxxSql.append("SELECT SUM(F_HXJE) AS F_HXJE,F_FPBH,F_GYS,F_HSZT FROM (SELECT A.").append(pYfkfpField).append(" AS F_FPBH,A.").append(pHxjeField).append(" AS F_HXJE,B.").append(pGysField).append(" AS F_GYS,C.").append(pHsztField).append(" AS F_HSZT FROM (SELECT * FROM ").append(pFkxxTable).append(" WHERE F_SSDJBH =? AND F_FKFF = 'APPLY') A LEFT JOIN ").append(pTxxTable).append(" B ON A.F_SSMXDJBH = B.F_PKEY LEFT JOIN ").append(pHeaderTable).append(" C ON A.F_SSDJBH = C.F_PKEY) GROUP BY F_FPBH,F_HSZT,F_GYS");
        } else {
            return "";
        }
        return pQueryFkxxSql.toString();
    }

    /**
     * 获取预付款列配置信息
     * @param pLink
     * @param pDjlx
     * @return
     */
    private IDalResultSet getYfkColConfInfo(IDalConnection pLink, String pDjlx) throws Exception {
        String pQuerySql = "SELECT * FROM BZ_YFK_COL_CONF WHERE F_DJLX = ?";
        IDalResultSet pQueryRS = TSqlUtils.QueryPreparedSql(pLink, pQuerySql, new String[]{pDjlx});
        if (pQueryRS != null && pQueryRS.First()) {
            return pQueryRS;
        }
        return null;
    }

    /**
     * 是否存在该台账
     *
     * @param pLink
     * @param pYfkbh
     * @return
     */
    private boolean isExit(IDalConnection pLink, String pYfkbh) throws Exception {
        String pQuerySql = "SELECT 1 FROM NHLH_STO_YFKHXTZ WHERE F_FPBH = ?";
        IDalResultSet pQueryRs = TSqlUtils.QueryPreparedSql(pLink, pQuerySql, pYfkbh);
        if (pQueryRs != null && pQueryRs.First()) {
            return true;
        }
        return false;
    }

    /**
     * 存在则修改台账记录
     *
     * @param pLink
     * @param pHxje
     * @param pYfkbh
     */
    private void updateRecord(IDalConnection pLink, double pHxje, String pYfkbh, double pYhxje) throws Exception {
        String pUpdateSql = "UPDATE NHLH_STO_YFKHXTZ SET F_ZTHXJE = F_ZTHXJE + " + pHxje + ",F_YHXJE = F_YHXJE + " + pYhxje + " WHERE F_FPBH = ?";
        TSqlUtils.UpdatePreparedSql(pLink, pUpdateSql, new String[]{pYfkbh});
    }

    /**
     * 不存在则新增台账记录
     *
     * @param pLink
     * @param pHxje
     * @param pYfkbh
     */
    private void createNewRecord(IDalConnection pLink, double pHxje, String pYfkbh,String pHszt,String pGys) throws Exception {
        String pInsertSql = "INSERT INTO NHLH_STO_YFKHXTZ (F_PKEY,F_FPBH,F_ZTHXJE,F_HSZT,F_GYSBM) VALUES (SYS_GUID(),?,?,?,?)";
        TSqlUtils.UpdatePreparedSql(pLink, pInsertSql, new String[]{pYfkbh, String.valueOf(pHxje), pHszt, pGys});
    }

    /**
     * 获取最近一次占用金额
     *
     * @param pLink
     * @param pYfkbh
     * @param pVchrKey
     * @return
     */
    private double getLastZyJe(IDalConnection pLink, String pYfkbh, String pVchrKey) throws Exception {
        String pQuerySql = "SELECT F_HXJE FROM NHLH_STO_YFKHXMX WHERE F_FPBH = ? AND F_DJBH = ? AND F_LASTZY = ?";
        String pLastZyStatus = "1";
        IDalResultSet pQueryRs = TSqlUtils.QueryPreparedSql(pLink, pQuerySql, new String[]{pYfkbh, pVchrKey, pLastZyStatus});
        if (pQueryRs != null && pQueryRs.First()) {
            return pQueryRs.getDoubleValue("F_HXJE");
        }
        return 0.0d;
    }

    /**
     * 新增明细记录
     *  @param pLink
     * @param pHxje
     * @param pYfkbh
     * @param pDjbh
     */
    private void createMxRecord(IDalConnection pLink, double pHxje, String pYfkbh, String pDjbh) throws Exception {
        String pKey = StringTool.UUIDCreate();
        String pXgsj = TNhlhCommonTool.getCurrentTime("yyyyMMddHHmmSS");
        String pCzrbm = pLink.getRmtRunEnv().getUserID();
        String pCzrmc = this.getUserMcById(pLink, pCzrbm);
        String pLastZyStatus = pHxje > 0 ? "1" : "0";
        String pInsertSql = "INSERT INTO NHLH_STO_YFKHXMX (F_PKEY,F_FPBH,F_DJBH,F_XGSJ,F_HXJE,F_CZRBM,F_CZRMC,F_LASTZY) VALUES (?,?,?,?,?,?,?,?)";
        TSqlUtils.UpdatePreparedSql(pLink, pInsertSql, new String[]{pKey, pYfkbh, pDjbh, pXgsj, String.valueOf(pHxje), pCzrbm, pCzrmc, pLastZyStatus});
        if ("0".equals(pLastZyStatus)) return;
        String pUpdateSql = "UPDATE NHLH_STO_YFKHXMX SET F_LASTZY = ? WHERE F_PKEY != ? AND F_DJBH = ? AND F_FPBH = ?";
        TSqlUtils.UpdatePreparedSql(pLink, pUpdateSql, new String[]{"0", pKey, pDjbh, pYfkbh});
    }

    /**
     * 通过用户ID获取名称
     *
     * @param pLink
     * @param pUserId
     * @return
     */
    private String getUserMcById(IDalConnection pLink, String pUserId) throws Exception {
        String pQuerySql = "SELECT USR_CAPTION FROM SSF_USERS WHERE USR_USRID = ?";
        IDalResultSet pQueryRs = TSqlUtils.QueryPreparedSql(pLink, pQuerySql, pUserId);
        if (pQueryRs != null && pQueryRs.First()) {
            return pQueryRs.getStringValue("USR_CAPTION");
        }
        return "";
    }
}
