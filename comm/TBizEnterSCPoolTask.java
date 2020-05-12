package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.common.biz.biztask.utils.TBizTaskUtils;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.tools.lang.TRCI;
import com.pansoft.nhlh.biztask.TBizTaskUtil;
import com.pansoft.nhlh.biztask.ws.TWSBizTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.sc.biztask.enter.IBizEnterSCPoolExtendProcessor;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skylin
 * <p>报账进入共享池</p>
 * <p>CreateTime:2019-07-20 14:30:01</p>
 */
public class TBizEnterSCPoolTask extends TWSBizTask {
    protected boolean isCanInvokeService(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pParamJson = (JSONObject) pTaskBean.getParamObject();
        if (pParamJson == null || pParamJson.isEmpty()) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("入池参数为空");
            return false;
        }

        String pDjbh = pParamJson.optString("F_DJBH");
        return TBizTaskUtil.isTaskCanExecute(pLink, pTaskBean.getTaskId(), pDjbh);
    }

    protected TTaskReturnBean buildBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        JSONObject pParamJson = (JSONObject) pTaskBean.getParamObject();
        if (pParamJson == null || pParamJson.isEmpty()) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("1");
            pReturnBean.setProcessMsg("入池参数为空");
            return pReturnBean;
        }

        String pDjbh = pParamJson.optString("F_DJBH");
        String pFlowSn = pParamJson.optString("F_FLOW_SN");
        String pFlowSql = "select rt.fl_base_drv_obj as f_djbh,rt.fl_base_drv_typ as f_vchr_id,rt.fl_base_pur_typ as f_djlx,djlx.f_sto_id from bf_flow_rt rt left join ic_bx_djlx djlx on rt.fl_base_pur_typ=djlx.f_bh where rt.fl_inst_sn=?";
        IDalResultSet pFlowRS = TSqlUtils.QueryPreparedSql(pLink, pFlowSql, pFlowSn);
        if (pFlowRS == null || !pFlowRS.First()) {
            pReturnBean.setProcessOk(false);
            pReturnBean.setProcessCode("2");
            pReturnBean.setProcessMsg(String.format("单据编号[%s]未发起流程", pDjbh));
            return pReturnBean;
        }

        JSONObject pTaskInfo = pParamJson.optJSONObject(TBizTaskUtils.KEY_TASK_INFO);
        String pVchrId = pTaskInfo.optString("F_DJMX");
        String pDjlx = pTaskInfo.optString("F_DJLX");
        String pStoId = pFlowRS.getStringValue("F_STO_ID");

        /**
         * 构造请求参数
         */
        String pAuthToken = pTaskBean.getConfigString("sc.pool.entry.token");
        String pServiceId = pTaskBean.getConfigString("sc.pool.entry.service", "API_BZ_FSSC_ENTER_SC_SVR");
        String pCallSys = "BZ";
        String pUserName = pTaskBean.getConfigString("sc.pool.entry.user");
        String pUserPass = pTaskBean.getConfigString("sc.pool.entry.pass");
        String pIsBase64 = pTaskBean.getConfigString("sc.pool.entry.base64");
        String pExtData = "";
        JSONObject pVchrInfo = this.buildVchrInfo(pLink, pDjlx, pVchrId, pDjbh, pStoId);
        this.fillExtendFields(pLink, pTaskBean, pReturnBean, pVchrInfo);
        String pReqData = pVchrInfo.toString();

        StringBuffer data = new StringBuffer();
        data.append("<callApiInput>");
        data.append("   <F_AUTH_TOKEN>").append(pAuthToken).append("</F_AUTH_TOKEN>");
        data.append("   <F_SERVICE_ID>").append(pServiceId).append("</F_SERVICE_ID>");
        data.append("   <F_SYSTEM_ID>").append(pCallSys).append("</F_SYSTEM_ID>");
        data.append("   <F_SYSTEM_USER>").append(pUserName).append("</F_SYSTEM_USER>");
        data.append("   <F_SYSTEM_USER_PASS>").append(pUserPass).append("</F_SYSTEM_USER_PASS>");
        data.append("   <F_IS_BASE64>").append(pIsBase64).append("</F_IS_BASE64>");
        data.append("   <F_BODY>").append(pReqData).append("</F_BODY>");
        data.append("   <F_EXC01>").append(pExtData).append("</F_EXC01>");
        data.append("   <F_EXC02>").append(pExtData).append("</F_EXC02>");
        data.append("   <F_EXC03>").append(pExtData).append("</F_EXC03>");
        data.append("   <F_EXC04>").append(pExtData).append("</F_EXC04>");
        data.append("   <F_EXC05>").append(pExtData).append("</F_EXC05>");
        data.append("</callApiInput>");

        this.setRequestData(pTaskBean, data.toString());
        System.out.println(String.format("单据[%s]进入共享参数组装成功，F_VCHR_ID=%s;F_DJLX=%s;F_STO_ID=%s", pDjbh, pVchrId, pDjlx, pStoId));

        return pReturnBean;
    }

    protected void processBizData(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        Element pRspBody = this.getReturnBody(pTaskBean);
        Element pReturnBody = pRspBody.element("callApiReturn");
        String pCallCode = pReturnBody.elementTextTrim("F_CODE");
        if (!"0".equals(pCallCode)) {
            throw new Exception(String.format("进入共享池失败！原因[%s]", pReturnBody.elementText("F_MSG")));
        }

        String pRspData = pReturnBody.elementText("F_BODY");
        if (TWSUtil.isNotJson(pRspData)) {
            throw new Exception("返回参数有误");
        }

        JSONObject pRspObj = JSONObject.fromObject(pRspData);
        String pRetCode = pRspObj.optString("F_CODE");
        if (!"0".equals(pRetCode)) {
            throw new Exception(pRspObj.optString("F_MSG"));
        }

        /**
         * 入池成功，后续逻辑处理
         */
    }

    /**
     * 构造入池参数
     * @param pLink
     * @param pDJlx
     * @param pVchrId
     * @param pDjbh
     * @param pStoId
     * @return
     * @throws Exception
     */
    private JSONObject buildVchrInfo(IDalConnection pLink, String pDJlx, String pVchrId, String pDjbh, String pStoId) throws Exception {
        String pSql = "SELECT ZB.F_DJBH, ZB.F_DJLX, ZB.F_DJLX_MC, ZB.F_DJMX, DJLX.F_DJFZ, RT.FL_INST_SN AS F_FLOW_SN, ZB.F_JE, F_ZDR, F_ZDR_MC, ZB.F_ZDSJ, ZB.F_YTBH, YT.F_YTMC, ZB.F_ZRZX, ZRZX.F_MC AS F_ZRZX_MC, ZB.F_INFO, ZB.F_IS_ZZFJ, ZB.F_SF_JJZF, ZB.F_SFCXSM FROM (SELECT * FROM BF_BIZ_INFO WHERE F_DJBH = ?) ZB LEFT JOIN IC_BX_DJLX DJLX ON ZB.F_DJLX = DJLX.F_BH LEFT JOIN BF_FLOW_RT RT ON ZB.F_DJBH = RT.FL_BASE_DRV_OBJ LEFT JOIN PUB_DCT_YTZD YT ON ZB.F_YTBH = YT.F_YTBH LEFT JOIN NHLH_DCT_HSZT ZRZX ON ZB.F_ZRZX = ZRZX.F_BM";
        IDalResultSet pVchrRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pDjbh);
        if (pVchrRS == null || !pVchrRS.First()) {
            throw new Exception(String.format("单据[%s]不存在", pDjbh));
        }

        String pXtbh = "BZ";
        String pDjlxMc = pVchrRS.getStringValue("F_DJLX_MC");
        String pVirDjlx = "";
        String pDjmx = pVchrId;
        String pDjfz = pVchrRS.getStringValue("F_DJFZ");
        //入池类型，应当根据流程退回状态判断，目前先写死
        String pEnterType = "1";
        String pFlowSn = pVchrRS.getStringValue("F_FLOW_SN");
        String pGoBack = "0";
        String pDgds = "1";
        String pBxje = pVchrRS.getStringValue("F_JE");
        String pHsxt = "EBS";
        String pHTUrl = "";
        String pJbr = pVchrRS.getStringValue("F_ZDR");
        String pJbrMc = pVchrRS.getStringValue("F_ZDR_MC");
        String pJjzfzt = "";
        String pJjzfztMc = "";
        String pModelName = "BZ";
        String pReadUrl = "";
        String pEditUrl = "";
        String pVchrUrl = "";
        String pTbsj = "";
        String pZdsj = pVchrRS.getStringValue("F_ZDSJ");
        String pYtbh = "NLH";    //pVchrRS.getStringValue("F_YTBH");
        String pYtmc = "新希望六和";    //pVchrRS.getStringValue("F_YTMC");
        String pZrzx = pVchrRS.getStringValue("F_ZRZX");
        String pZrzxMc = pVchrRS.getStringValue("F_ZRZX_MC");
        String pZy = pVchrRS.getStringValue("F_INFO");
        String pSfzzfj = pVchrRS.getStringValue("F_IS_ZZFJ");
        String pSfjjzf = pVchrRS.getStringValue("F_SF_JJZF");
        String pSfcxsm = pVchrRS.getStringValue("F_SFCXSM");

        JSONArray pLsArray = new JSONArray();

        if (TWSUtil.isNullText(pYtbh)) {
            pYtbh = "NLH";
        }

        if (TWSUtil.isNullText(pSfzzfj)) {
            pSfzzfj = "0";
        }

        if (TWSUtil.isNullText(pSfjjzf)) {
            pSfjjzf = "0";
        }

        if (TWSUtil.isNullText(pSfcxsm)) {
            pSfcxsm = "1";
        }

        JSONObject vchrInfo = new JSONObject();
        vchrInfo.put("F_XTBH", pXtbh);
        vchrInfo.put("F_DJSN", pDjbh);
        vchrInfo.put("F_DJBH", pDjbh);
        vchrInfo.put("F_DJLX", pDJlx);
        vchrInfo.put("F_DJLXMC", pDjlxMc);
        vchrInfo.put("F_VIRTUAL_DJLX", pVirDjlx);
        vchrInfo.put("F_DJMX", pDjmx);
        vchrInfo.put("F_DJFZ", pDjfz);
        vchrInfo.put("F_ENTER_TYPE", pEnterType);
        vchrInfo.put("F_FLOW_SN", pFlowSn);
        vchrInfo.put("F_GO_BACK", pGoBack);
        vchrInfo.put("F_DGDS", pDgds);
        vchrInfo.put("F_BXJE", pBxje);
        vchrInfo.put("F_HSXT", pHsxt);
        vchrInfo.put("F_HT_URL", pHTUrl);
        vchrInfo.put("F_JBR_OABH", pJbr);
        vchrInfo.put("F_JBR_OAMC", pJbrMc);
        vchrInfo.put("F_JJZTBH", pJjzfzt);
        vchrInfo.put("F_JJZTMC", pJjzfztMc);
        vchrInfo.put("F_LS_LIST", pLsArray);
        vchrInfo.put("F_MODEL_NAME", pModelName);
        vchrInfo.put("F_READ_ONLY_URL", pReadUrl);
        vchrInfo.put("F_SE_EDIT_URL", pEditUrl);
        vchrInfo.put("F_YWDJ_URL", pVchrUrl);
        vchrInfo.put("F_TBSJ", pTbsj);
        vchrInfo.put("F_YTBH", pYtbh);
        vchrInfo.put("F_YTMC", pYtmc);
        vchrInfo.put("F_ZDSJ", pZdsj);
        vchrInfo.put("F_ZRZX", pZrzx);
        vchrInfo.put("F_ZRZXMC", pZrzxMc);
        vchrInfo.put("F_ZY", pZy);
        vchrInfo.put("F_SF_ZZFJ", pSfzzfj);
        vchrInfo.put("F_IS_ZZFJ", pSfzzfj);
        vchrInfo.put("F_SF_JJZF", pSfjjzf);
        vchrInfo.put("F_SFCXSM", pSfcxsm);
        vchrInfo.put("F_IS_ZCXZ", "0");

        return vchrInfo;
    }

    /**
     * 入池参数扩展字段处理
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @param pVchrInfo
     * @throws Exception
     */
    protected void fillExtendFields(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pVchrInfo) throws Exception {
        JSONObject pParamJson = (JSONObject) pTaskBean.getParamObject();
        JSONObject pTaskInfo = pParamJson.optJSONObject(TBizTaskUtils.KEY_TASK_INFO);
        String pDjlx = pTaskInfo.optString("F_DJLX");
        String pDjbh = pTaskInfo.optString("F_DJBH");
        List<IBizEnterSCPoolExtendProcessor> pPoolExtendProcessorList = this.getExtendProcessor(pLink, pDjlx);

        if (pPoolExtendProcessorList != null && pPoolExtendProcessorList.size() > 0) {
            IBizEnterSCPoolExtendProcessor pPoolExtendProcessor = null;
            int pSize = pPoolExtendProcessorList.size();
            for(int iIndex = 0; iIndex < pSize; iIndex++){
                pPoolExtendProcessor = pPoolExtendProcessorList.get(iIndex);
                if (pPoolExtendProcessor == null) {
                    continue;
                }

                pPoolExtendProcessor.fillExtendFiled(pLink, pDjlx, pDjbh, pVchrInfo);
            }
        }
    }

    protected List<IBizEnterSCPoolExtendProcessor> getExtendProcessor(IDalConnection pLink, String pDjlx) throws Exception {
        List<IBizEnterSCPoolExtendProcessor> pPoolExtendProcessorList = null;
        IBizEnterSCPoolExtendProcessor pPoolExtendProcessor = null;

        try {
            String pSql = "SELECT F_CLASS FROM NHLH_POOL_EXTEND WHERE F_DJLX=?";
            IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pDjlx);
            if (pRS != null && pRS.First()) {
                int pRowCount = pRS.getRowCount();
                pPoolExtendProcessorList = new ArrayList<IBizEnterSCPoolExtendProcessor>();
                for(int iIndex = 0;iIndex < pRowCount;iIndex++){
                    pRS.setRowIndex(iIndex);
                    String pClassName = pRS.getStringValue("F_CLASS");
                    pPoolExtendProcessor = (IBizEnterSCPoolExtendProcessor) TRCI.MakeObjectInstance(pClassName);
                    pPoolExtendProcessorList.add(pPoolExtendProcessor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pPoolExtendProcessorList;
    }
}
