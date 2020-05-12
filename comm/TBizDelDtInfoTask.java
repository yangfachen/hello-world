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
 * 删除摊销子界面相关信息并更新bf_biz_info对应单据状态为作废（9）
 * @ClassName TBizDelDtInfoTask
 * @Description TODO
 * @Author YangFC
 * @Date 2020/6/15 9:33
 * @Version 1.0
 **/
public class TBizDelDtInfoTask extends TBizTask {


    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Throwable {
        //获取参数
        JSONObject pParamObj = TNhlhCommonTool.getObject(pTaskBean);//都可以用的

        String pDjbh = pParamObj.optString("F_DJBH");
        String pExistSql = "SELECT 1 FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?";//是否存在SQL
        String pDelZbSql = "DELETE FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?";//删除对应主表信息SQL
        String pdelMxSql = "DELETE FROM NHLH_ZZ_DTMX WHERE F_SSDJBH = (SELECT F_PKEY FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?)";//删除明细信息
        String pdelSjMxSql = "DELETE FROM NHLH_ZZ_DTYTMX WHERE F_SSDJBH = (SELECT F_PKEY FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?)";//删除三级明细信息
        String pUpdateSql = "UPDATE BF_BIZ_INFO SET F_DJZT = '9' WHERE F_DJBH = (SELECT F_PKEY FROM NHLH_ZZ_DTZB WHERE F_YDJBH = ?) ";//更新bf_biz_info 对应单据状态为作废
        IDalResultSet pQueryRs = TSqlUtils.QueryPreparedSql(pLink, pExistSql, new String[]{pDjbh});
        if(pQueryRs != null && pQueryRs.First()){//存在则删除
            TSqlUtils.UpdatePreparedSql(pLink,pDelZbSql,pDjbh);
            TSqlUtils.UpdatePreparedSql(pLink,pdelMxSql,pDjbh);
            TSqlUtils.UpdatePreparedSql(pLink,pdelSjMxSql,pDjbh);
            TSqlUtils.UpdatePreparedSql(pLink,pUpdateSql,pDjbh);

        }
        TNhlhCommonTool.dealReturnBean(pReturnBean, "0", "删除成功", true);
        return pReturnBean;
    }
}
