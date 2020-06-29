package com.pansoft.nhlh.biztask.auto;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * @author:yhj
 * @title:自动上传影像服务
 * @CreateTime:2020/6/28 18:37
 */
public class TBizAutoUploadImgTask extends TBizReceiveBaseTask {

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pSql = "SELECT * FROM SYS_ATTACH_POOL_MASTER WHERE F_STATUS='0'";
        IDalResultSet pRs = TSqlUtils.QueryPreparedSql(pLink,pSql);
        if(pRs!=null&&pRs.First()){
            String pDjbh = "";
            String pServiceId = pTaskBean.getConfigString("F_SERVICE_ID");
            JSONObject pReqJson = null;
            int pRowCount = pRs.getRowCount();
            for(int iIndex = 0;iIndex<pRowCount;iIndex++){
                pRs.setRowIndex(iIndex);
                pDjbh = pRs.getStringValue("F_DJBH");
                pReqJson = new JSONObject();
                pReqJson.put("F_DJBH",pDjbh);
                try {
                    TBizTask.callBizTask(null,pLink,"BZ",pServiceId,pReqJson);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

        return pReturnBean;
    }
}
