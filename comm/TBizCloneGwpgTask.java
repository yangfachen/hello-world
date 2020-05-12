package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTask;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.pansoft.nhlh.util.TNhlhCommonTool;
import com.saf.sql.utils.TSqlUtils;
import net.sf.json.JSONObject;

/**
 * 岗位派工克隆服务
 * @ClassName TBizCloneGwpgTask
 * @Description TODO
 * @Author yfc
 * @Date 2020/4/23 18:53
 * @Version 1.0
 **/
public class TBizCloneGwpgTask extends TBizTask{


    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Throwable {
        //获取参数
        JSONObject pParamObj = TNhlhCommonTool.getObject(pTaskBean);//都可以用的


        String pOldZrzx = pParamObj.optString("F_OLD_ZRZX");
        String pNewZrzx = pParamObj.optString("F_NEW_ZRZX");

        if (TWSUtil.isNullText(pOldZrzx)){
            throw new Exception("参数传递错误,原责任中心为空！");
        }
        if (TWSUtil.isNullText(pNewZrzx)){
            throw new Exception("参数传递错误,新责任中心为空！");
        }


        StringBuffer sbSql = new StringBuffer();//克隆SQL
        sbSql.append("INSERT INTO NHLH_SC.SC_TASK_DISP VALUE (F_PKEY, F_SERVICE_CONTENT, F_SERVICE_TASK, F_DJLX, F_ZRZX, F_ROLE_GROUP, F_ROLE_ID, F_DISP_COND, F_DISP_RULE, F_DISABLE)" );
        sbSql.append("(SELECT SYS_GUID(), F_SERVICE_CONTENT, F_SERVICE_TASK, F_DJLX, '" );
        sbSql.append(pNewZrzx);
        sbSql.append("' AS F_ZRZX, F_ROLE_GROUP, F_ROLE_ID, F_DISP_COND, F_DISP_RULE, F_DISABLE");
        sbSql.append(" FROM NHLH_SC.SC_TASK_DISP WHERE F_ZRZX = ? AND F_SERVICE_CONTENT NOT IN ('TR04', 'TR03')) ");

        System.out.println(sbSql.toString());
        TSqlUtils.UpdatePreparedSql(pLink,sbSql.toString(),pOldZrzx);
        TNhlhCommonTool.dealReturnBean(pReturnBean, "0", "克隆成功", true);

        return pReturnBean;
    }

}
