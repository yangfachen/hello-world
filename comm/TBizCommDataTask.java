package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author skylin
 * <p>CreateTime:2019-07-27 14:43:01</p>
 * <p>
 *     通用数据查询(测试用)
 * </p>
 */
public class TBizCommDataTask extends TBizTaskParamJson {

    @Override
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pTableName = pParamJson.optString("dataset_name");
        String pFilterSql = pParamJson.optString("dataset_filter");
        StringBuffer pSql = new StringBuffer();
        pSql.append("select * from ").append(pTableName);
        pSql.append(" where 1=1");
        if (!TWSUtil.isNullText(pFilterSql)) {
            pSql.append(" and ").append(pFilterSql);
        }

        IDalResultSet pRS = TSqlUtils.QuerySql(pLink, pSql.toString());
        JSONObject pReturnObj = new JSONObject();
        pReturnObj.put("row_count", 0);
        JSONArray pDatas = new JSONArray();

        if (pRS != null && pRS.First()) {
            JSONObject pData = null;
            int pRowCount = pRS.getRowCount();
            pReturnObj.put("row_count", pRowCount);
            String[] pColNames = pRS.getColumnNames();
            for (int iRowIndex = 0; iRowIndex < pRowCount; iRowIndex++) {
                pRS.setRowIndex(iRowIndex);
                pData = new JSONObject();

                for (String pColName : pColNames) {
                    pData.put(pColName, pRS.getObjectValue(pColName));
                }

                pDatas.add(pData);
            }
        }

        pReturnObj.put("datas", pDatas);
        pReturnBean.setReturnObject(pReturnObj);

        return pReturnBean;
    }
}
