package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TBizTaskParamJson;
import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import net.sf.json.JSONObject;

/**
 * @author YangYang
 * <p>获取借款人的历史借款总额</p>
 * <p>CreateTime:2019-7-12 16:48:01</p>
 */
public class TBizQueryHistoryTAOBTask extends TBizTaskParamJson {
    @Override
    protected TTaskReturnBean onProcess(IDalConnection pLogLink, IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson) throws Throwable {

        return pReturnBean;
    }
}
