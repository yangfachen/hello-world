package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.toolkit.text.StringTool;
import com.pansoft.nhlh.biztask.ws.TWSBizTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author skylin
 * <p>CreateTime:2019-07-27 14:43:01</p>
 * <p>
 *     通用数据帮助
 *     1.接收${F_SERVICE_ID}参数，用来获取目标数据
 *     2.接收${F_INTERFACE_ID}参数，用来查询定义的数据结构，定义了要返回前台的字段以及要显示的字段
 * </p>
 */
public class TBizCommHelpTask extends TBizTaskParamJson {

    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        JSONObject pColMetaObj = new JSONObject();
        JSONArray pColMetas = new JSONArray();
        JSONObject pDataObj = new JSONObject();

        try {
            /**
             * 字典列元数据
             */
            JSONObject pColMeta = null;
            String pInterfaceId = pParamJson.optString("F_INTERFACE_ID");
            String pColMetaSql = "SELECT F_TCOL_ID,F_TCOL_MC,F_TCOL_TYPE,F_SCOL_ID,F_SCOL_MC,F_SCOL_TYPE,F_VISIBLE,F_IS_CX FROM MD_SYN_CONF WHERE F_TASK_ID=? ORDER BY F_INDEX";
            IDalResultSet pColMetaRS = TSqlUtils.QueryPreparedSql(pLink, pColMetaSql, pInterfaceId);
            if (pColMetaRS != null && pColMetaRS.First()) {
                int pRowCount = pColMetaRS.getRowCount();
                String[] pColNames = pColMetaRS.getColumnNames();
                for (int iRowIndex = 0; iRowIndex < pRowCount; iRowIndex++) {
                    pColMetaRS.setRowIndex(iRowIndex);
                    pColMeta = new JSONObject();

                    for (String pColName : pColNames) {
                        pColMeta.put(pColName, pColMetaRS.getStringValue(pColName));
                    }

                    pColMetas.add(pColMeta);
                }
            }

            /**
             * 字典数据
             */
            boolean isLoadData = "1".equals(pParamJson.optString("IS_LOAD_DATA", "1"));
            if (isLoadData) {
                String pServiceId = pParamJson.optString("F_SERVICE_ID");
                JSONObject pServiceParam = pParamJson.optJSONObject("F_SERVICE_PARAM");
                String pCallSystemId = "FSSC";

                TTaskReturnBean pServiceReturnBean = TBizTask.callBizTask(pLogLink, pLink, pCallSystemId, pServiceId, pServiceParam, null);
                if (pServiceReturnBean != null && pServiceReturnBean.isProcessOk()) {
                    pDataObj = (JSONObject) pServiceReturnBean.getReturnObject();
                } else {
                    pDataObj.put("row_count", 0);
                    pDataObj.put("datas", new JSONArray());
                }
            } else {
                pDataObj.put("row_count", 0);
                pDataObj.put("datas", new JSONArray());
            }

            pColMetaObj.put("F_CODE", "0");
            pColMetaObj.put("F_MSG", "帮助数据查询成功");
        } catch (Exception e) {
            e.printStackTrace();

            String pErrMsg = e.getMessage();
            if (pErrMsg == null) {
                pErrMsg = e.toString();
            }

            pColMetaObj.put("F_CODE", "-1");
            pColMetaObj.put("F_MSG", pErrMsg);
        } finally {
            pColMetaObj.put("colmetas", pColMetas);
            pColMetaObj.put("datas", pDataObj);
            pReturnBean.setReturnObject(pColMetaObj);
        }

        return pReturnBean;
    }
}
