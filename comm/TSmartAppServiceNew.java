package com.pansoft.nhlh.biztask.comm;

import com.eai.application.framework.tools.TProfiles;
import com.eai.application.ssfuser.interfaces.ISSFUserServiceHome;
import com.eai.bizframework.interfaces.BSOMethod;
import com.eai.bizframework.interfaces.IBSORpo;
import com.eai.bizframework.service.interfaces.IBizServiceBusHome;
import com.eai.bizlayer.bizmessage.vchrexchange.interfaces.IVchrExchange;
import com.eai.bizlayer.bizvoucher.interfaces.IBizVoucherDefine;
import com.eai.bizlayer.service.interfaces.IBizVoucherServiceHome;
import com.eai.business.sdkinterface.messages.IBizMessage;
import com.eai.dof.interfaces.IDOFElement;
import com.eai.dof.interfaces.IDOFEnumGroup;
import com.eai.dof.services.interfaces.IDofCommServiceHome;
import com.eai.dof.services.interfaces.IDofDctServiceHome;
import com.eai.dof.services.interfaces.IDofPhysicalObjectServiceHome;
import com.eai.dof.services.interfaces.IDofStoreServiceHome;
import com.eai.dof.storetable.interfaces.IStoreDefine;
import com.eai.dof.storetable.interfaces.IStoreFieldDefine;
import com.eai.exchange.data.interfaces.IDataManager;
import com.eai.exchange.data.interfaces.IStructure;
import com.eai.exchange.data.interfaces.ITable;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.eai.frame.fcl.interfaces.dal.IDalResultSet;
import com.eai.frame.fcl.interfaces.dal.IDalStatement;
import com.eai.frame.fcl.interfaces.env.IEAISession;
import com.eai.frame.runtime.TEAIEnv;
import com.pansoft.mobile.entity.interfaces.IBizVchrField;
import com.pansoft.mobile.entity.interfaces.IBizVchrGroup;
import com.pansoft.mobile.entity.interfaces.IBizVchrLayout;
import com.pansoft.mobile.generator.attach.TBizAttachGeneratorFactory;
import com.pansoft.mobile.generator.attach.interfaces.IBizAttachGenerator;
import com.pansoft.mobile.service.interfaces.ISmartMobileServiceHome;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
public class TSmartAppServiceNew {
	private SimpleDateFormat mDate10Format = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat mDate14Format = new SimpleDateFormat("yyyyMMddHHmmss");
    private SimpleDateFormat mDate19Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private IBSORpo mRpo = null;
    private IBizServiceBusHome mBizBusService = null;
    private IDofPhysicalObjectServiceHome mDBMSService = null;
    private IDofStoreServiceHome mStoreService = null;
    private IDofDctServiceHome mDctService = null;
    private ISSFUserServiceHome mSSFUserService = null;
    private ISmartMobileServiceHome mMobileService = null;
    private IBizVoucherServiceHome mBizVchrService = null;
    private IDofCommServiceHome mCommServiceHome = null;
    private IBizAttachGenerator mAttachUrlGenerator = null;

    public TSmartAppServiceNew() {
    }

    @BSOMethod(
        TRN = false,
        CONN = false,
        MInput = "BZ_VCHR_INPUT",
        MResult = "BZ_VCHR_RESULT",
        T = "查询凭据移动端布局"
    )
    public boolean queryVchrAppLayout(IEAISession pSession, IDalConnection pLink, IBizMessage pMsgInput, IBizMessage pMsgOutput) throws Exception {
        IDataManager pDataInput = pMsgInput.getBusinessParameters();
        IDataManager pDataOutput = pMsgOutput.getBusinessParameters();
        JSONObject pLayoutObj = new JSONObject();
        IDalStatement pStatement = null;
        String pRetCode = "0";
        String pRetMsg = "凭据自定义布局信息查询成功";

        try {
            label287: {
                String pDjlx = pDataInput.AsString("F_DJLX");
                if (pDjlx != null && !"".equals(pDjlx)) {
                    String pDjbh = pDataInput.AsString("F_DJBH");
                    if (pDjbh != null && !"".equals(pDjbh)) {
                        String pDevice = pDataInput.AsString("F_DEVICE");
                        if (pDevice == null || "".equals(pDevice)) {
                            pDevice = "1";
                        }

                        IBizVchrLayout pLayout = this.mMobileService.getDefaultVchrLayout(pLink, pDjlx, pDevice);
                        if (pLayout != null) {
                            String pVchrId = pLayout.getVchrId();
                            pLayoutObj.put("F_LAYOUT_ID", pLayout.getLayoutId());
                            pLayoutObj.put("F_DJLX", pLayout.getDjlx());
                            pLayoutObj.put("F_VCHR_ID", pVchrId);
                            pLayoutObj.put("F_SHOWTITLE", pLayout.isShowTitle() ? "1" : "0");
                            pLayoutObj.put("F_TITLE", pLayout.getTitle());
                            JSONArray pGroups = new JSONArray();
                            JSONObject pGroupObj = null;
                            List<IBizVchrGroup> pGroupList = pLayout.getGroupList();
                            IBizVchrGroup pGroup = null;
                            String pGrpType = "";
                            String pSubVchrId;
                            if (pGroupList != null && pGroupList.size() > 0) {
                                IBizVoucherDefine pVoucherDefine = this.mBizVchrService.QueryBizVoucherDefine(pLink, pVchrId);
                                IVchrExchange pVchrData = this.mBizVchrService.QueryBizVoucherData(pLink, pVchrId, pDjbh);
                                String pStoId = "";
                                IStoreDefine pStoreDefine = null;
                                IBizVoucherDefine pSubVchrDefine = null;
                                pSubVchrId = "";

                                for(int iIndex = 0; iIndex < pGroupList.size(); ++iIndex) {
                                    pGroupObj = new JSONObject();
                                    pGroup = (IBizVchrGroup)pGroupList.get(iIndex);
                                    pGrpType = pGroup.getGroupType();
                                    pSubVchrId = pGroup.getVchrId();
                                    pGroupObj.put("F_GROUP_ID", pGroup.getGroupId());
                                    pGroupObj.put("F_GROUP_TYPE", pGrpType);
                                    pGroupObj.put("F_LAYOUT", pGroup.getLayoutId());
                                    pGroupObj.put("F_SHOWTITLE", pGroup.isShowTitle() ? "1" : "0");
                                    pGroupObj.put("F_TITLE", pGroup.getTitle());
                                    pGroupObj.put("F_VCHR_ID", pSubVchrId);
                                    pGroupObj.put("F_VCHR_NAME", pGroup.getVchrName());
                                    pGroupObj.put("F_XH", pGroup.getXh());
                                    pSubVchrDefine = pVoucherDefine.getChildVoucher(pSubVchrId);
                                    pStoId = pSubVchrDefine.getStoreId();
                                    pStoreDefine = this.mStoreService.QueryStoreDefine(pLink, pStoId);
                                    IBizVchrField pVchrField = null;
                                    List<IBizVchrField> pFieldList = pGroup.getFieldList();
                                    if (pFieldList != null && pFieldList.size() > 0) {
                                        JSONArray pTitleArray = new JSONArray();
                                        JSONObject pTitleObj = null;

                                        for(int iColIndex = 0; iColIndex < pFieldList.size(); ++iColIndex) {
                                            pVchrField = (IBizVchrField)pFieldList.get(iColIndex);
                                            pTitleObj = new JSONObject();
                                            pTitleObj.put("key", pVchrField.getColId());
                                            pTitleObj.put("value", pVchrField.getColName());
                                            pTitleArray.add(pTitleObj);
                                        }

                                        pGroupObj.put("DataTitles", pTitleArray);
                                        IDalResultSet pSubVchrRS = pVchrData.getVoucherData(pSubVchrId);
                                        JSONArray pDataRows;
                                        JSONObject pDataRow;
                                        if ("kv".equals(pGrpType)) {
                                            pDataRows = new JSONArray();
                                            pDataRow = new JSONObject();
                                            pSubVchrRS.First();
                                            String pFieldValue = "";

                                            for(int iColIndex = 0; iColIndex < pFieldList.size(); ++iColIndex) {
                                                pVchrField = (IBizVchrField)pFieldList.get(iColIndex);
                                                pFieldValue = this.getVchrFieldValue(pLink, pSubVchrRS, pStoreDefine, pVchrField.getColId());
                                                pDataRow.put(pVchrField.getColId(), pFieldValue);
                                            }

                                            pDataRows.add(pDataRow);
                                            pGroupObj.put("DataRows", pDataRows);
                                        } else {
                                            if (!pSubVchrRS.First()) {
                                                continue;
                                            }

                                            pDataRows = new JSONArray();
                                            pDataRow = null;

                                            for(int iRow = 0; iRow < pSubVchrRS.getRowCount(); ++iRow) {
                                                pSubVchrRS.setRowIndex(iRow);
                                                pDataRow = new JSONObject();
                                                String pFieldValue = "";

                                                for(int iColIndex = 0; iColIndex < pFieldList.size(); ++iColIndex) {
                                                    pVchrField = (IBizVchrField)pFieldList.get(iColIndex);
                                                    pFieldValue = this.getVchrFieldValue(pLink, pSubVchrRS, pStoreDefine, pVchrField.getColId());
                                                    pDataRow.put(pVchrField.getColId(), pFieldValue);
                                                }

                                                pDataRows.add(pDataRow);
                                            }

                                            pGroupObj.put("DataRows", pDataRows);
                                        }
                                    }

                                    pGroups.add(pGroupObj);
                                }
                            }

                            pLayoutObj.put("Groups", pGroups);
                            pStatement = pLink.CreateDalStatement();
                            JSONArray pAttaches = this.getVchrAttaches(pStatement, pDjbh);
                            pDataOutput.setString("F_ATTACHES", pAttaches.toString());
                            IBizMessage pHisMsg = this.getApproveHis(pSession, pLink, pVchrId, pDjbh);
                            ITable pHisTable = pDataOutput.getTable("HIS_LIST");
                            ITable pSrcHisTable = pHisMsg.getBusinessParameters().getTable("HIS_LIST");
                            pDataOutput.AddData("HIS_LIST", pHisMsg.getBusinessParameters().getTable("HIS_LIST"));
                            Map<String, Integer> pNodeRowIndexMap = new HashMap();
                            pSubVchrId = null;
                            String pCrtMode = "";
                            String pNodeId = "";

                            for(int iRowIndex = 0; iRowIndex < pSrcHisTable.getRowCount(); ++iRowIndex) {
                                IStructure pCurHis = pSrcHisTable.getRow(iRowIndex);
                                pNodeId = pCurHis.AsString("F_NODE_ID");
                                pCrtMode = pCurHis.AsString("F_CRT_MODE");
                                if ("8".equals(pCrtMode)) {
                                    if (!pNodeRowIndexMap.containsKey(pNodeId)) {
                                        pHisTable.addRow(pCurHis);
                                        pNodeRowIndexMap.put(pNodeId, pHisTable.getRowCount() - 1);
                                    }

                                    int pIndex = (Integer)pNodeRowIndexMap.get(pNodeId);
                                    IStructure pSignNodeRow = pHisTable.getRow(pIndex);
                                    ITable pReceiverTable = pSignNodeRow.getDataManager().getTable("F_RECEIVE_USER");
                                    IStructure pNewReceiver = pReceiverTable.CreateEmptyRow();
                                    pNewReceiver.setString("F_USER_ID", pCurHis.AsString("F_OPT_USER"));
                                    pNewReceiver.setString("F_USER_NAME", pCurHis.AsString("F_OPT_USER_NAME"));
                                } else {
                                    pHisTable.addRow(pCurHis);
                                }
                            }
                        }
                        break label287;
                    }

                    throw new Exception("单据编号不能为空");
                }

                throw new Exception("单据类型不能为空");
            }
        } catch (Exception var41) {
            var41.printStackTrace();
            pRetCode = "-1";
            pRetMsg = var41.getMessage();
        } finally {
            if (pStatement != null) {
                pStatement.Close();
            }

        }

        pDataOutput.setString("RET_CODE", pRetCode);
        pDataOutput.setString("RET_MSG", pRetMsg);
        pDataOutput.setString("F_LAYOUT", pLayoutObj.toString());
        return true;
    }

    private JSONArray getVchrAttaches(IDalStatement pStatement, String pDjbh) throws Exception {
        JSONArray pAttachList = new JSONArray();
        JSONObject pAttach = null;
        String pFileName = "";
        String pFileSize = "0(KB)";
        String pFileId = "";
        String pAttType = "";
        String pActionUrl = TProfiles.getProperty("IMG_ATTACH_DOWNLOAD_URL", "http://fssc-imgtest.liuheco.com/Image/");
        if (pActionUrl == null || "".equals(pActionUrl)) {
            pActionUrl = TProfiles.getProperty("IMAGE_SERVER", "http://fssc-imgtest.liuheco.com/Image/");
        }

//        String pActionUrl = TProfiles.getProperty("file_preview", "preview.action", "http://fssc-bztest.liuheco.com/filePreview.do");
        String pImgSql = "SELECT F_PKEY,F_POOL_ID,F_ATT_TYPE,F_ATT_STO_KEY,F_ATT_TITLE,F_ATT_SIZE,F_IMG_CODE,F_FILE_TP FROM NHLH_IMG.IMG_STO_ATTACH WHERE F_BILL_ID='" + pDjbh + "' AND F_DISABLE = '0' ORDER BY F_ATT_TYPE,F_DSP_IDX";
        IDalResultSet pImgRS = pStatement.Query(pImgSql);
        if (pImgRS.First()) {
            double pSize = 0.0D;

            for(int i = 0; i < pImgRS.getRowCount(); ++i) {
                pImgRS.setRowIndex(i);
                pFileName = pImgRS.getStringValue("F_ATT_TITLE");
                pSize = (double)pImgRS.getIntegerValue("F_ATT_SIZE");
                pFileSize = (int)(pSize / 1024.0D) + "KB";
                pAttType = pImgRS.getStringValue("F_ATT_TYPE");
                pFileId = pImgRS.getStringValue("F_ATT_STO_KEY");
                StringBuffer pDownloadUrl = new StringBuffer();
                pDownloadUrl.append(pActionUrl).append("fileTransferDownloadAction.do?F_NEED_APPLY=1").append("&F_USER_ID=9999").append("&F_TYPE=").append(pAttType).append("&F_OBJECT=").append(pDjbh).append("&F_FILE_NAME=").append("&F_STORE_KEY=").append(pFileId);
                pAttach = new JSONObject();
                pAttach.put("fileName", pFileName);
                pAttach.put("fileSize", pFileSize);
                pAttach.put("fileType", pAttType);
                pAttach.put("fileId", "");
                pAttach.put("fileExt", pImgRS.getStringValue("F_FILE_TP"));
                pAttach.put("downloadUrl", pDownloadUrl.toString());
                pAttachList.add(pAttach);
            }
        }

        return pAttachList;
    }

    private JSONArray getApproveFlow(IEAISession pSession, IDalConnection pLink, String pVchrId, String pVchrKey) throws Exception {
        IBizMessage pAppMsg = this.getApproveHis(pSession, pLink, pVchrId, pVchrKey);
        Map<String, IStructure> pApproveMap = new HashMap();
        ITable pAppLogHis = pAppMsg.getBusinessParameters().getTable("HIS_LIST");
        String pAppOptType = "";
        IStructure pHisNode = null;
        String pCurNodeId = "";
        int pNodeCount = pAppLogHis.getRowCount();
        if (pNodeCount > 0) {
            pCurNodeId = pAppLogHis.getRow(pNodeCount - 1).AsString("F_NODE_ID");
        }

        for(int i = 0; i < pAppLogHis.getRowCount(); ++i) {
            pHisNode = pAppLogHis.getRow(i);
            pAppOptType = pHisNode.AsString("F_OPT_TYPE_ID");
            if (pAppOptType != null && !pAppOptType.equals("0") && !pAppOptType.equals("-1") && !pAppOptType.equals("-2")) {
                pApproveMap.put(pHisNode.AsString("F_NODE_ID"), pHisNode);
            }
        }

        JSONArray pFlowArray = new JSONArray();
        JSONObject pFlowObj = null;
        ITable pHisList = pAppMsg.getBusinessParameters().getTable("GROUP_HIS_LIST");
        IStructure pCurNode = null;

        for(int i = 0; i < pHisList.getRowCount(); ++i) {
            pCurNode = pHisList.getRow(i);
            pFlowObj = this.buildNodeInfo(pCurNode, pApproveMap, pCurNodeId);
            pFlowArray.add(pFlowObj);
        }

        return pFlowArray;
    }

    private IBizMessage getApproveHis(IEAISession pSession, IDalConnection pLink, String pVchrType, String pVchrKey) throws Exception {
        IBizMessage pHisInput = this.mBizBusService.CreateBizMessageEmpty("BF_TASK_HIS_INPUT");
        IBizMessage pHisOutput = this.mBizBusService.CreateBizMessageEmpty("BF_TASK_HIS_RESULT");

        try {
            pHisInput.getBusinessParameters().setString("F_BIZ_TYPE", pVchrType);
            pHisInput.getBusinessParameters().setString("F_VCHR_KEY", pVchrKey);
            pHisInput.getBusinessParameters().setString("F_IS_SIMULATION", "1");
            pHisInput.getBusinessParameters().setString("F_IS_GROUP", "0");
            this.mBizBusService.CallBSOMethod("BizFlowService", "queryApprovedTaskInfoNew", pSession, pLink, pHisInput, pHisOutput);
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return pHisOutput;
    }

    private JSONObject buildNodeInfo(IStructure pCurNode, Map<String, IStructure> pAppMap, String pCurNodeId) throws Exception {
        JSONObject pNodeObj = new JSONObject();
        JSONArray pNodeUsers = new JSONArray();
        JSONObject pNodeUser = null;
        String pNodeId = pCurNode.AsString("F_NODE_ID");
        String pCaption = pCurNode.AsString("F_NODE_CAPTION");
        pNodeObj.put("NodeName", pCaption);
        pNodeObj.put("IsCurNode", pCurNodeId.equals(pNodeId) ? "1" : "0");
        ITable pUserTable = pCurNode.getDataManager().getTable("F_NODE_INFO_LIST");
        IStructure pCurUser = null;
        IStructure pHisNode = null;
        String pIsCheck = "0";
        String pOptUser = "";
        String pIsApproved = "0";

        for(int i = 0; i < pUserTable.getRowCount(); ++i) {
            pCurUser = pUserTable.getRow(i);
            pNodeUser = new JSONObject();
            pOptUser = pCurUser.AsString("F_OPT_USER_NAME");
            pNodeUser.put("NodeUser", pOptUser);
            pNodeUser.put("NodeAction", "");
            pNodeUser.put("NodeNote", "");
            pNodeUser.put("NodeTime", "");
            pIsCheck = pCurUser.AsString("F_CHECK");
            if (pIsCheck != null && pIsCheck.equals("1") && pAppMap.containsKey(pCurNode.AsString("F_NODE_ID"))) {
                pHisNode = (IStructure)pAppMap.get(pNodeId);
                pIsApproved = "1";
                pNodeUser.put("NodeAction", pHisNode.AsString("F_OPT_TYPE"));
                pNodeUser.put("NodeNote", pHisNode.AsString("F_OPT_MSG"));
                pNodeUser.put("NodeTime", pHisNode.AsString("F_OPT_TIME"));
            } else {
                pIsApproved = "0";
            }

            pNodeUser.put("IsApproved", pIsApproved);
            pNodeUsers.add(pNodeUser);
        }

        pNodeObj.put("NodeUsers", pNodeUsers);
        return pNodeObj;
    }

    private JSONArray getApproveHis() throws Exception {
        return null;
    }

    private String getVchrFieldValue(IDalConnection pLink, IDalResultSet pMasterStoRS, IStoreDefine pStoreDefine, String pFieldName) throws Exception {
        IStoreFieldDefine pFieldDefine = pStoreDefine.getStoreFieldDefine(pFieldName);
        String pValue = "";
        if (!pFieldDefine.getLogicalType().equals("1") && !pFieldDefine.getLogicalType().equals("4") && !pFieldDefine.getLogicalType().equals("5")) {
            String pVal = pMasterStoRS.getStringValue(pFieldName);
            pValue = this.createFieldValue(pLink, pFieldDefine, pVal);
        } else {
            pValue = pMasterStoRS.getStringValue(pFieldName + "_FMC");
        }

        return pValue;
    }

    private String createFieldValue(IDalConnection pLink, IStoreFieldDefine pFieldDefine, String pVal) throws Exception {
        String pValue = "";
        if (pFieldDefine.isEnumable()) {
            pValue = this.getEnumShowValue(pLink, pFieldDefine, pVal);
        }

        String pShowFormat = "";
        if (pFieldDefine.isElementLike() && !"".equals(pFieldDefine.getElementLikeType())) {
            try {
                IDOFElement pElementRef = this.mCommServiceHome.QueryElementInstance(pLink, pFieldDefine.getElementLikeType());
                pShowFormat = pElementRef.getShowMask();
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }

        if ("3".equals(pFieldDefine.getEditStyle())) {
            pShowFormat = "".equals(pShowFormat) ? pFieldDefine.getShowFormat() : pShowFormat;
            pValue = this.getFloatShowValue(pShowFormat, pVal);
        } else if (!"5".equals(pFieldDefine.getEditStyle()) && !"15".equals(pFieldDefine.getEditStyle())) {
            if ("6".equals(pFieldDefine.getEditStyle())) {
                pShowFormat = pFieldDefine.getShowFormat();
                if (pShowFormat != null && !"".equals(pShowFormat)) {
                    pValue = this.getPopboxShowValue(pShowFormat, pVal);
                }
            } else if ("4".equals(pFieldDefine.getEditStyle())) {
                if (pVal == null) {
                    pVal = "";
                }

                pValue = String.valueOf(pVal).equals("1") ? "是" : "否";
            } else if (pValue.equals("")) {
                pValue = pVal;
            }
        } else {
            pValue = this.getDateShowValue(pFieldDefine, pVal);
        }

        return pValue;
    }

    private String getFloatShowValue(String pShowFormat, String pVal) {
        DecimalFormat pDF = new DecimalFormat(pShowFormat);
        if ("".equals(pVal)) {
            pVal = "0.0";
        }

        return pDF.format(Double.parseDouble(pVal));
    }

    private String getEnumShowValue(IDalConnection pLink, IStoreFieldDefine pFieldDefine, String pVal) {
        try {
            IDOFEnumGroup pGroup = this.mCommServiceHome.QueryEnumKeySet(pLink, pFieldDefine.getEnumKey());
            if (pGroup != null) {
                return pGroup.getDOFEnum(pVal).getCaption();
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        return pVal;
    }

    private String getPopboxShowValue(String pShowFormat, String pVal) {
        if (!"".equals(pShowFormat)) {
        	pShowFormat = pShowFormat.substring(13);
            String[] pItems = pShowFormat.split(",");

            for(int i = 0; i < pItems.length; ++i) {
                String[] pItem = pItems[i].split("\\|");
                if (pVal.equals(pItem[0])) {
                    return pItem[1];
                }
            }
        }

        return pVal;
    }

    private String getDateShowValue(IStoreFieldDefine pFieldDefine, String pVal) {
        if (pVal != null && !"".equals(pVal)) {
            try {
                String pDateFormat = pFieldDefine.getShowFormat();
                SimpleDateFormat mCustFormat = null;
                String pAppendStr;
                if ("".equals(pDateFormat)) {
                    if ("5".equals(pFieldDefine.getEditStyle())) {
                        mCustFormat = this.mDate10Format;
                    } else {
                        mCustFormat = this.mDate19Format;
                    }
                } else {
                    String[] pFormats = pDateFormat.split(";;");
                    pAppendStr = pFormats[0].split(":")[1];
                    mCustFormat = new SimpleDateFormat(pAppendStr);
                }

                int pSrcLen = pVal.length();
                if (pSrcLen < 14) {
                    pAppendStr = "00000101000000";
                    pVal = pVal + pAppendStr.substring(pSrcLen);
                }

                Date pDate = this.mDate14Format.parse(pVal.substring(0, 14));
                return mCustFormat.format(pDate);
            } catch (ParseException var7) {
                return "";
            }
        } else {
            return "";
        }
    }

    public void onStart() {
        try {
            this.mBizBusService = (IBizServiceBusHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizServiceBus");
            this.mDBMSService = (IDofPhysicalObjectServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "DOFPhysicalObjectService");
            this.mDctService = (IDofDctServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "DOFDictionaryService");
            this.mStoreService = (IDofStoreServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "DOFStoreService");
            this.mSSFUserService = (ISSFUserServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "SSFUserService");
            this.mMobileService = (ISmartMobileServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "SmartMobileService");
            this.mBizVchrService = (IBizVoucherServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "BizVoucherService");
            this.mCommServiceHome = (IDofCommServiceHome)TEAIEnv.QueryServiceLocalInterface("EAIManager", "DOFCommService");
            String pAttachUrlType = TProfiles.getProperty("ATTACH_URL_GENERATOR", "ImageSystem");
            this.mAttachUrlGenerator = TBizAttachGeneratorFactory.getInstance().getGenerator(pAttachUrlType);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        System.out.println("|TSmartAppServiceNew start.");
    }

    public void onStop() {
        System.out.println("|TSmartAppServiceNew stop.");
    }

    public void setRpo(IBSORpo pRpo) {
        this.mRpo = pRpo;
        System.out.println("|TSmartAppServiceNew rpo set.");
    }
}
