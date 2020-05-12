package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * 更新待摊子界面单据状态
 * @ClassName TBizUpdateDtzjmDjztTask
 * @Description TODO
 * @Author YangFC
 * @Date 2020/6/24 14:00
 * @Version 1.0
 **/
public class TBizUpdateDtzjmDjztTask extends TBizTask{

    @Override
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Throwable {

        if (!TNhlhCommonTool.allow2do(pLink, "updateDtzjmDjzt")) {
            return pReturnBean;
        }
        //获取参数
        JSONObject pParamObj = TNhlhCommonTool.getObject(pTaskBean);//都可以用的
        String pDjbh = pParamObj.optString("F_DJBH");
        String pDjzt = pParamObj.optString("F_DJZT");
        if ("".equals(pDjbh)){
            throw  new Exception("单据编号为空！");
        }
        if ("".equals(pDjzt)){
            throw  new Exception("单据状态为空！");
        }
        String pQuerySql = "SELECT 1 FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?";
        IDalResultSet pRs = TSqlUtils.QueryPreparedSql(pLink,pQuerySql,pDjbh);
        if (pRs != null &&pRs.First()){
            String pUpdateSql = "UPDATE NHLH_ZZ_DTZB SET F_DJZT = ? WHERE F_YDJBH = ?";
            String pUpdateInfoSql = "UPDATE BF_BIZ_INFO SET F_DJZT = ? WHERE F_DJBH = (SELECT F_PKEY FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?)";
            TSqlUtils.UpdatePreparedSql(pLink, pUpdateSql, new String[]{pDjzt,pDjbh});
            TSqlUtils.UpdatePreparedSql(pLink, pUpdateInfoSql, new String[]{pDjzt,pDjbh});
        }
        return pReturnBean;
    }
}
