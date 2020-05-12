package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.pansoft.dms.entity.TArchivesConst;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.pansoft.nhlh.util.TNhlhUtils;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

import java.sql.PreparedStatement;

/**
 * @author:yhj
 * @title:记录微信小程序投单时间
 * @CreateTime:2019/11/19 11:13
 */
public class TBizRecordDeliveryTimeTask extends TBizReceiveBaseTask {

    protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
        String pDjbh = pParamJson.optString("F_DJBH");
        String pDeliveryTime = this.getDeliveryTime();
        //记录到档案池
        String pRecordSql = "UPDATE WD_DAC SET F_TD_TIME=?,F_TDLY='1' WHERE F_DJBH=?";
        int pUpdateNum = TSqlUtils.UpdatePreparedSql(pLink, pRecordSql, pDeliveryTime, pDjbh);
        if(pUpdateNum!=1){
            pRspObj.put("errcode", "-1");
            pRspObj.put("errmsg", "投递时间记录失败，单据尚未进入档案池");
        }else{
            pRspObj.put("errcode", "0");
            pRspObj.put("errmsg", "单据投递时间记录成功");

            /**
             * 记录档案日志
             */
            TNhlhUtils.insertArchLog(pLink, "", pDjbh, TArchivesConst.ARCH_STA_DELIVER, "", "", pDeliveryTime, "1");
        }
        return pReturnBean;
    }

    /**
     * 获取投单时间
     * @return
     */
    private String getDeliveryTime() {
        return TNhlhCommonTool.getCurrentTime("yyyyMMddHHmmss");
    }
}
