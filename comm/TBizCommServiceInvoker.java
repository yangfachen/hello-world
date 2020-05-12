package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSBizTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.entity.IBizVoucherField;
import com.pansoft.nhlh.entity.TBizVoucherField;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skylin
 * <p>CreateTime:2019-09-07 12:16:01</p>
 * <p>
 *     通用服务调用器
 * </p>
 */
public abstract class TBizCommServiceInvoker extends TWSBizTask {
    /**
     * 构造业务数据体
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @return
     * @throws Exception
     */
    protected abstract String buildDataBody(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception;

    protected TTaskReturnBean buildBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        String pAppId = pTaskBean.getConfigString("F_APP_ID");
        String pAppKey = pTaskBean.getConfigString("F_SECRET_KEY");
        String pModuleId = pTaskBean.getConfigString("F_MODULE_ID");
        String pTxCode = pTaskBean.getConfigString("F_TX_CODE");
        String pTxSN = StringTool.UUIDCreate();

        StringBuilder pBizData = new StringBuilder();
        pBizData.append("<appid>").append(pAppId).append("</appid>");
        pBizData.append("<appSecretKey>").append(pAppKey).append("</appSecretKey>");
        pBizData.append("<moduleId>").append(pModuleId).append("</moduleId>");
        pBizData.append("<txCode>").append(pTxCode).append("</txCode>");
        pBizData.append("<txSN>").append(pTxSN).append("</txSN>");

        String pDataBody = this.buildDataBody(pLink, pTaskBean, pReturnBean);
        if (!TWSUtil.isNullText(pDataBody)) {
            pDataBody = StringTool.B2T(pDataBody.getBytes("utf-8"));
        }

        pBizData.append("<data>").append(pDataBody).append("</data>");
        this.setRequestData(pTaskBean, pBizData.toString());
        return pReturnBean;
    }

    protected void processBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pInvokeReturn = new JSONObject();

        try {
            Element pRspEle = this.getReturnBody(pTaskBean);
            if (pRspEle == null) {
                throw new Exception("返回结果异常");
            }

            String pReturnData = pRspEle.elementTextTrim("return");
            String pRealData = this.decodeBase64(pReturnData);
            if (TWSUtil.isNotJson(pRealData)) {
                throw new Exception("返回结果不是json格式");
            }

            JSONObject pReturnObj = JSONObject.fromObject(pRealData);
            this.processReturnData(pLink, pTaskBean, pReturnBean, pReturnObj, pInvokeReturn);
        } catch (Exception e) {
            String pErrMsg = this.getThrowableText(e);
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("-1");
            pReturnBean.setProcessMsg(pErrMsg);

            pInvokeReturn.put("errcode", "-1");
            pInvokeReturn.put("errmsg", pErrMsg);
        } finally {
            pReturnBean.setReturnObject(pInvokeReturn);
        }
    }

    /**
     * 对于返回业务数据的处理
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @param pReturnData
     * @param pInvokeReturn
     * @throws Exception
     */
    protected abstract void processReturnData(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pReturnData, JSONObject pInvokeReturn) throws Exception;

    /**
     * 获取字段映射
     * @param pLink
     * @param pBizKey
     * @param pBizSubKey
     * @return
     * @throws Exception
     */
    protected List<IBizVoucherField> queryBizVoucherFields(IDalConnection pLink, String pBizKey, String pBizSubKey) throws Exception {
        List<IBizVoucherField> pVchrFields = new ArrayList<IBizVoucherField>();
        String pSql = "SELECT * FROM BIZ_VCHR_CONF WHERE F_BIZ_KEY=? AND F_BIZ_SUBKEY=? ORDER BY F_INDEX";
        IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pBizKey, pBizSubKey);
        if (pRS != null && pRS.First()) {
            int pRowCount = pRS.getRowCount();
            IBizVoucherField pVchrField = null;
            for (int iIndex = 0; iIndex < pRowCount; iIndex++) {
                pRS.setRowIndex(iIndex);
                pVchrField = new TBizVoucherField();
                pVchrField.setPkey(pRS.getStringValue("F_PKEY"));
                pVchrField.setIndex(pRS.getIntegerValue("F_INDEX"));
                pVchrField.setBizKey(pBizKey);
                pVchrField.setBizSubKey(pBizSubKey);
                pVchrField.setTcolId(pRS.getStringValue("F_TCOL_ID"));
                pVchrField.setTcolName(pRS.getStringValue("F_TCOL_NAME"));
                pVchrField.setTColType(pRS.getStringValue("F_TCOL_TYPE"));
                pVchrField.setScolId(pRS.getStringValue("F_SCOL_ID"));
                pVchrField.setScolName(pRS.getStringValue("F_SCOL_NAME"));
                pVchrField.setScolType(pRS.getStringValue("F_SCOL_TYPE"));
                pVchrFields.add(pVchrField);
            }
        }

        return pVchrFields;
    }

    protected void transformRowColValue2Json(IDalConnection pLink, IDalResultSet pSrcRS, JSONObject pTrgtRS, List<IBizVoucherField> pVchrFields) throws Exception {
        String pTColValue = "";
        for (IBizVoucherField pVchrField : pVchrFields) {
            pTColValue = this.getStringValue(pSrcRS, pVchrField.getScolId(), pVchrField.getScolType());
            pTrgtRS.put(pVchrField.getTcolId(), pTColValue);
        }
    }

    /**
     * 获取文本值
     * @param pDataRS
     * @param pSrcCol
     * @param pSrcType
     * @return
     * @throws Exception
     */
    protected String getStringValue(IDalResultSet pDataRS, String pSrcCol, String pSrcType) throws Exception {
        String pValue = "";
        if ("Integer".equals(pSrcType)) {
            pValue = String.valueOf(pDataRS.getIntegerValue(pSrcCol));
        } else if ("Double".equals(pSrcType)) {
            pValue = String.valueOf(pDataRS.getDoubleValue(pSrcCol));
        } else {
            pValue = pDataRS.getStringValue(pSrcCol);
        }

        return pValue;
    }

}
