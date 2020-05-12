package com.pansoft.nhlh.biztask.comm;

import com.bis.wsdl.impl.core.objects.TWsdlContext;
import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * @author yangyang
 * <p>获取报销标准</p>
 * <p>CreateTime:2019-08-06 15:58:01</p>
 */
public class TBizQueryUserBxbzTask extends TBizTaskParamJson {


    /**
     * 此功能用于查询住宿补贴（F_FYLX='1'）和市内交通费（F_FYLX='2'）
     * @param pLogLink
     * @param pLink
     * @param pTaskBean
     * @param pReturnBean
     * @param pParamJson
     * @return
     * @throws Throwable
     */
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {
        String pFylx = pParamJson.optString("F_FYLX");
        String pUserId = "";
        String pCityId = "";
        String pJdlx = "";
        String pYwsx = "";
        if (!TWSUtil.isNullText(pFylx)) {
            if ("1".equals(pFylx)) {
                pUserId = pParamJson.optString("F_USER_ID");
                pCityId = pParamJson.optString("F_CITY_ID");
                pJdlx = pParamJson.optString("F_JDLX");
                if (TWSUtil.isNullText(pUserId)) {
                    return this.isNull("F_USER_ID",pReturnBean);
                }
                if (TWSUtil.isNullText(pCityId)) {
                    return this.isNull("F_CITY_ID",pReturnBean);
                }
                if (TWSUtil.isNullText(pJdlx)) {
                    return this.isNull("F_JDLX",pReturnBean);
                }
                /**
                 * 非协议酒店返回消费标准
                 */
                if ("1".equals(pJdlx)) {
                    String pFxyjdbzSql = "SELECT F_FXYJDBZ AS F_ZSBZ FROM NHLH_STO_ZSBZ WHERE F_YGZJ = (SELECT MAX(USR_T04) AS F_YGZJ FROM SSF_USERS WHERE USR_USRID = ?) AND F_CITY_GRD = (SELECT F_CITY_GRD FROM PUB_DCT_CITY WHERE F_ID = ?)";
                    IDalResultSet pFxyjdbzRs = TSqlUtils.QueryPreparedSql(pLink, pFxyjdbzSql, pUserId, pCityId);
                    if (pFxyjdbzRs != null && pFxyjdbzRs.First()) {
                        JSONObject pZsbzInfo = new JSONObject();
                        pZsbzInfo.put("F_ZSBZ", pFxyjdbzRs.getDoubleValue("F_ZSBZ"));
                        pReturnBean.setReturnObject(pZsbzInfo);
                    } else {
                        return this.isNull("数据," + pFxyjdbzSql, pReturnBean);
                    }
                }
                /**
                 * 协议酒店返回酒店等级上下限
                 */
                 else if ("2".equals(pJdlx) || "3".equals(pJdlx) || "4".equals(pJdlx)) {
                        String pXyjdsxxSql = "SELECT F_JDSX,F_JDXX FROM NHLH_STO_YGZJ_XYJD WHERE F_YGZJ = (SELECT MAX(USR_T04) AS F_YGZJ FROM SSF_USERS WHERE USR_USRID = ?)";
                        IDalResultSet pXyjdsxxRs = TSqlUtils.QueryPreparedSql(pLink, pXyjdsxxSql, pUserId);
                        if (pXyjdsxxRs != null && pXyjdsxxRs.First()) {
                            JSONObject pZsbzInfo = new JSONObject();
                            pZsbzInfo.put("F_JDSX", pXyjdsxxRs.getStringValue("F_JDSX"));
                            pZsbzInfo.put("F_JDXX", pXyjdsxxRs.getStringValue("F_JDXX"));
                            pReturnBean.setReturnObject(pZsbzInfo);
                        } else {
                            return this.isNull("数据，" + pXyjdsxxSql, pReturnBean);
                        }
                    }else{
                    return this.isInvalidParam("F_JDLX", pReturnBean);
                }
            }
            else if ("2".equals(pFylx)) {
                pCityId = pParamJson.optString("F_CITY_ID");
                pYwsx = pParamJson.optString("F_YWSX");
                if (TWSUtil.isNullText(pCityId)) {
                    return this.isNull("城市ID",pReturnBean);
                }
                if (TWSUtil.isNullText(pYwsx)) {
                    return this.isNull("业务事项", pReturnBean);
                }
                /**
                 * 非自驾车
                 */
                if ("1".equals(pYwsx) || "2".equals(pYwsx)) {
                    String pSnjtfBzSql = "SELECT F_SNJTF FROM NHLH_STO_CSDJ_SNJTF WHERE F_CITY_GRD = (SELECT F_CITY_GRD FROM PUB_DCT_CITY WHERE F_ID = ?)";
                    IDalResultSet pSnjtfBzRs = TSqlUtils.QueryPreparedSql(pLink, pSnjtfBzSql, pCityId);
                    if (pSnjtfBzRs != null && pSnjtfBzRs.First()) {
                        JSONObject pSnjtfInfo = new JSONObject();
                        pSnjtfInfo.put("F_SNJTF", pSnjtfBzRs.getDoubleValue("F_SNJTF"));
                        pReturnBean.setReturnObject(pSnjtfInfo);
                    }else{
                        return this.isNull("数据，" + pSnjtfBzSql, pReturnBean);
                    }
                }
                /**
                 * 自驾车
                 */
                else if ("3".equals(pYwsx)) {
                    JSONObject pSnjtfInfo = new JSONObject();
                    pSnjtfInfo.put("F_SNJTF", 0.0d);
                    pReturnBean.setReturnObject(pSnjtfInfo);
                }else {
                    return this.isInvalidParam("F_YWSX", pReturnBean);
                }
            }else{
                return this.isInvalidParam("F_FYLX", pReturnBean);
            }
        } else {
            return this.isNull("F_FYLX",pReturnBean);
        }
        return pReturnBean;
    }

    private TTaskReturnBean isInvalidParam(String param, TTaskReturnBean pReturnBean) {
        pReturnBean.setProcessOk(false);
        pReturnBean.setProcessCode("1");
        pReturnBean.setProcessMsg("参数"+param+"为无效参数");
        pReturnBean.setReturnJsonObject();
        return pReturnBean;
    }


    private TTaskReturnBean isNull(String pQueryData, TTaskReturnBean pReturnBean) {
        pReturnBean.setProcessOk(false);
        pReturnBean.setProcessCode("1");
        pReturnBean.setProcessMsg("未查询到"+pQueryData);
        pReturnBean.setReturnJsonObject();
        return pReturnBean;
    }



}
