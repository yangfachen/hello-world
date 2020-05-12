package com.pansoft.nhlh.voucher.func;

import com.eai.frame.fcl.interfaces.appsvr.IEAIApplication;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.frame.runtime.TEAIEnv;
import com.eai.toolkit.functions.TEAIFunc;
import com.saf.sql.utils.TSqlUtils;
import org.nfunk.jep.ParseException;

import java.util.Stack;

/**
 * @author skylin
 * <p>CreateTime:2019-08-15 14:00:01</p>
 * <p>
 *     获取核算科目
 * </p>
 */
public class TGetHskmDF extends TEAIFunc {

    public TGetHskmDF() {
        T_NAME        = "GetHskmDF";
        T_RES         = "String";

        T_PTYPES      = new String[] { "String", "String"};
        T_PTYPE_NAMES = new String[] { "业务事项", "业务场景"};

        mName   = T_NAME;
        mNote   = "获取核算科目贷方";
        mSysObj = null;

        numberOfParameters = 2;

    }

    /**
     * 函数的执行体。
     * @param inStack Stack
     * @throws ParseException
     */
    public void run(Stack inStack) throws ParseException {
        checkStack(inStack);

        Object paramStr1 = inStack.pop();
        Object paramStr2 = inStack.pop();
//        Object paramStr3 = inStack.pop();
//        Object paramStr4 = inStack.pop();

//        String pKmlx    = (String)paramStr1;
//        String pYwcj  = (String)paramStr2;
        String pYwcj  = (String)paramStr1;
        String pYwsx    = (String)paramStr2;

        String pHskm = this.getHskm(pYwsx, pYwcj);
        inStack.push(pHskm);
    }

    public String getHskm(String pYwsx, String pYwcj) {
        IEAIApplication pRootApplication = TEAIEnv.getApplication("EAIManager");
        IDalConnection pLink = null;

        try {
            pLink = pRootApplication.openConnection("Default", null);
            String pSql = "SELECT * FROM NHLH_STO_YWSX_XJLL_HSKM WHERE F_YWSX=? AND F_HSYWCJ=? ";
            IDalResultSet pRS = TSqlUtils.QueryPreparedSql(pLink, pSql, pYwsx, pYwcj);
            if (pRS != null && pRS.First()) {
                return pRS.getStringValue("F_DFKMBH");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (pLink != null) {
                try {
                    pLink.RollBack();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (pLink != null) {
                pRootApplication.closeConnection(pLink);
            }
        }

        return "";
    }
}
