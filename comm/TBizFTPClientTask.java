package com.pansoft.nhlh.biztask.comm;

import com.common.biz.biztask.TTaskBean;
import com.common.biz.biztask.TTaskReturnBean;
import com.eai.frame.fcl.interfaces.dal.IDalConnection;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import com.jcraft.jsch.*;
import com.pansoft.nhlh.biztask.TBizReceiveBaseTask;
import com.pansoft.nhlh.biztask.ws.TWSUtil;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import net.sf.json.JSONObject;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author skylin
 * <p>CreateTime:2019-09-22 11:36:01</p>
 * <p>
 *     FTP操作基类
 * </p>
 */
public abstract class TBizFTPClientTask extends TBizReceiveBaseTask {
    private static final String FTP_SERVER_URL = "ftp.server.url";
    private static final String FTP_SERVER_PORT = "ftp.server.port";
    private static final String FTP_SERVER_USER = "ftp.server.user";
    private static final String FTP_SERVER_PASS = "ftp.server.pass";
    private static final String FTP_SERVER_WORKDIR = "ftp.server.workdir";

    protected String getServerUrl(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(FTP_SERVER_URL);
    }

    protected String getServerUser(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(FTP_SERVER_USER);
    }

    protected int getServerPort(TTaskBean pTaskBean) {
        return Integer.parseInt(pTaskBean.getConfigString(FTP_SERVER_PORT, "22"));
    }

    protected void setServerPort(TTaskBean pTaskBean, String port) {
        pTaskBean.setAttribute(FTP_SERVER_PORT, port);
    }

    protected String getServerPass(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(FTP_SERVER_PASS);
    }

    protected String getServerWorkdir(TTaskBean pTaskBean) {
        return pTaskBean.getConfigString(FTP_SERVER_WORKDIR);
    }

    public FTPClient initFtpClient(String hostname, int port, String username, String password) throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding("utf-8");

        try {
            ftpClient.connect(hostname, port); //连接ftp服务器
            ftpClient.login(username, password); //登录ftp服务器
            int replyCode = ftpClient.getReplyCode(); //是否成功登录服务器
            if(!FTPReply.isPositiveCompletion(replyCode)){
                throw new Exception(String.format("FTP服务器连接失败!url=[%s],port=[%d]", hostname, port));
            }
        }catch (Exception e) {
            throw e;
        }

        return ftpClient;
    }

    public byte[] downloadFileFTP(String hostname, int port, String username, String password, String pathname, String filename) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FTPClient ftpClient = null;

        try {
            System.out.println("开始下载文件");
            ftpClient = initFtpClient(hostname, port, username, password);

            //切换FTP目录
            boolean isSuccess = ftpClient.changeWorkingDirectory(pathname);
            if (!isSuccess) {
                throw new Exception(String.format("FTP工作目录[%s]切换失败!", pathname));
            }

            FTPFile[] ftpFiles = ftpClient.listFiles();
            for(FTPFile file : ftpFiles){
                if(filename.equals(file.getName())){
                    ftpClient.retrieveFile(file.getName(), os);
                }
            }

            byte[] fileBytes = os.toByteArray();

            return fileBytes;
        } catch (Exception e) {
            throw e;
        } finally{
            if (ftpClient != null) {
                try {
                    ftpClient.logout();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(ftpClient.isConnected()){
                try{
                    ftpClient.disconnect();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            if(null != os){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] downloadFileFTP(FTPClient ftpClient, FTPFile ftpFile) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            ftpClient.retrieveFile(ftpFile.getName(), os);
            os.flush();
            return os.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally{
            if(null != os){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 1.银行回单：根据银行交易流水号和计划付款行ID匹配
     * 2.POS回单：根据银行交易流水号匹配
     * @param pYhjyls
     * @param pEventId
     * @param pFileName
     * @return
     * @throws Exception
     */
    protected boolean isFileNameMatch(String pYhjyls, String pEventId, String pBankReceiptType, String pFileName) throws Exception {
        if (TWSUtil.isNullText(pFileName) || ".".equals(pFileName) || "..".equals(pFileName)) {
            return false;
        }

        int pDotIndex = pFileName.lastIndexOf(".");
        String pSimpleFileName = pFileName.substring(0, pDotIndex);
        String[] pSplitNames = pSimpleFileName.split("-");

        if("1".equals(pBankReceiptType)){
            /**
             * [1]ID
             * [2]事业部名称
             * [3]客户编号
             * [4]账号
             * [5]日期
             * [6]业务流水号
             * [7]电子回单号
             * [8]计划付款行ID
             */
            if (pYhjyls.equals(pSplitNames[5]) && pEventId.equals(pSplitNames[7])) {
                return true;
            }
        }else if("2".equals(pBankReceiptType)){
            /**
             * [1]运单号
             * [2]商户OU代码
             * [3]收款到账账户
             * [4]日期
             * [5]对应EBS收款单的虚拟订单编号(银行流水号)
             * [6]金额
             */
            if (pYhjyls.equals(pSplitNames[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据银行交易流水号和付款唯一标志匹配银行回单文件名
     * @param pYhjyls
     * @param pEventId
     * @param pFileName
     * @return
     * @throws Exception
     */
    protected boolean isFileNameMatch(String pYhjyls, String pEventId, String pFileName) throws Exception {
        if (TWSUtil.isNullText(pFileName) || ".".equals(pFileName) || "..".equals(pFileName)) {
            return false;
        }

        int pDotIndex = pFileName.lastIndexOf(".");
        String pSimpleFileName = pFileName.substring(0, pDotIndex);
        /**
         * [1]ID
         * [2]事业部名称
         * [3]客户编号
         * [4]账号
         * [5]日期
         * [6]业务流水号
         * [7]电子回单号
         * [8]计划付款行ID
         */
        String[] pSplitNames = pSimpleFileName.split("-");
        if (pYhjyls.equals(pSplitNames[5]) && pEventId.equals(pSplitNames[7])) {
            return true;
        }

        return false;
    }

    protected FTPFile[] listDirFiles(FTPClient ftpClient, String dir) throws Exception {
        boolean isChanged = ftpClient.changeWorkingDirectory(dir);
        if (!isChanged) {
            throw new Exception(String.format("FTP工作目录[%s]切换失败!", dir));
        }

        return ftpClient.listFiles();
    }

    protected FTPFile getMatchedFile(FTPClient ftpClient, FTPFile[] dirFiles, String pYhjylsh, String pEventId) throws Exception {
        if (dirFiles == null || dirFiles.length == 0) {
            return null;
        }

        boolean isMatched = false;
        for (FTPFile pFtpFile : dirFiles) {
            if (pFtpFile.isDirectory()) {
                continue;
            }

            isMatched = isFileNameMatch(pYhjylsh, pEventId, pFtpFile.getName());
            if (isMatched) {
                return pFtpFile;
            }
        }

        return null;
    }

    protected byte[] getMatchedFileBytes(FTPClient ftpClient, FTPFile pFtpFile) throws Exception {
        if (pFtpFile == null || !pFtpFile.isFile()) {
            return new byte[0];
        }

        return this.downloadFileFTP(ftpClient, pFtpFile);
    }

    protected List<byte[]> transformPdf2Image(byte[] fileByets, int pImgDPI, String pTrgtImgType) throws Exception {
        List<byte[]> pPdfImages = new ArrayList<byte[]>();

        try {
            PDDocument pPdocument = PDDocument.load(fileByets);
            PDFRenderer pRender = new PDFRenderer(pPdocument);
            int pPageCount = pPdocument.getNumberOfPages();
            BufferedImage pNewImage = null;

            ByteArrayOutputStream out = null;
            byte[] pImageBytes = null;
            for (int iIndex = 0; iIndex < pPageCount; iIndex++) {
                pNewImage = pRender.renderImageWithDPI(iIndex, pImgDPI, ImageType.RGB);
                out = new ByteArrayOutputStream();
                ImageIO.write(pNewImage, pTrgtImgType, out);
                pImageBytes = out.toByteArray();
                pPdfImages.add(pImageBytes);
                pImageBytes = null;
                out = null;
            }
        } catch (Exception e) {
            throw e;
        }

        return pPdfImages;
    }

    /**
     * 获取匹配银行回单
     * @param ftpClient
     * @param pWorkDir
     * @param pYhjylsh
     * @param pEventId
     * @return
     * @throws Exception
     */
    protected byte[] getYhhdFile(FTPClient ftpClient, String pWorkDir, String pYhjylsh, String pEventId) throws Exception {
        FTPFile[] pFtpFiles = this.listDirFiles(ftpClient, pWorkDir);
        FTPFile pMatchedFile = this.getMatchedFile(ftpClient, pFtpFiles, pYhjylsh, pEventId);
        return this.getMatchedFileBytes(ftpClient, pMatchedFile);
    }

    protected List<byte[]> getYhhdFile(ChannelSftp ftpClient, String pWorkDir, String pYhjylsh, String pEventId, String pBankReceiptType) throws Exception {
        Vector<?> pFtpFiles = this.listDirs(ftpClient, pWorkDir);
        return getMatchedFile(ftpClient, pFtpFiles, pWorkDir, pYhjylsh, pEventId, pBankReceiptType);
    }

    protected List<byte[]> getMatchedFile(ChannelSftp ftpClient, Vector<?> dirFiles, String pWorkDir, String pYhjylsh, String pEventId, String pBankReceiptType) throws Exception {
        if (dirFiles == null || dirFiles.size() == 0) {
            return null;
        }

        List<byte[]> pMatchedFiles = new ArrayList<byte[]>();
        boolean isMatched = false;
        ChannelSftp.LsEntry pEntry = null;
        for (Iterator<?> iterator = dirFiles.iterator(); iterator.hasNext();) {
            pEntry = (ChannelSftp.LsEntry) iterator.next();

            if (pEntry.getAttrs().isDir()) {
                continue;
            }

            isMatched = isFileNameMatch(pYhjylsh, pEventId, pBankReceiptType, pEntry.getFilename());
            if (isMatched) {
                ftpClient.cd(pWorkDir);
                byte[] pFileBytes = getSftpFileByte(ftpClient, pEntry.getFilename());
                pMatchedFiles.add(pFileBytes);
            }
        }

        return pMatchedFiles;
    }

    protected byte[] getSftpFileByte(ChannelSftp pFtpClient, String pFileEntry) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            pFtpClient.get(pFileEntry, out);
            out.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public Map<ChannelSftp, Session> getSftpChannel(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean) throws Exception {
        String pFtpHost = pTaskBean.getConfigString(FTP_SERVER_URL, "127.0.0.1");
        int pFtpPort = Integer.parseInt(pTaskBean.getConfigString(FTP_SERVER_PORT, "22"));
        String pFtpUser = pTaskBean.getConfigString(FTP_SERVER_USER);
        String pFtpPass = pTaskBean.getConfigString(FTP_SERVER_PASS);
        String pFtpProxyHost = pTaskBean.getConfigString("ftp.config.proxy.host");
        int pFtpProxyPort = Integer.parseInt(pTaskBean.getConfigString("ftp.config.proxy.port", "0"));
        String pSftpPrivateKey = pTaskBean.getConfigString("sftp.ssh.privatekey");
        String pSftpPrivatePass = pTaskBean.getConfigString("sftp.ssh.privatekey.pass");

        return connect(pFtpHost, pFtpPort, pFtpUser, pFtpPass, pSftpPrivateKey, pSftpPrivatePass, pFtpProxyHost, pFtpProxyPort);
    }

    /**
     * SFTP
     * @param host
     * @param port
     * @param username
     * @param password
     * @param properties
     * @return
     * @throws Exception
     */
    public Map<ChannelSftp, Session> getSFTPChannel(String host, int port, String username, String password, Properties properties) throws Exception {
        String pPrivateKey = properties.getProperty("sftp.ssh.privatekey", "");
        String pPrivatePass = properties.getProperty("sftp.ssh.privatekey.pass", "");
        String pProxyHost = properties.getProperty("sftp.proxy.host", "127.0.0.1");
        int pProxyPort = Integer.parseInt(properties.getProperty("sftp.proxy.port", "1080"));
        return connect(host, port, username, password, pPrivateKey, pPrivatePass, pProxyHost, pProxyPort);
    }

    public Map<ChannelSftp,Session> connect(String host, int port, String username, String password, String pPrivateKey, String pPrivatePass, String pProxyHost, int pProxyPort) throws Exception {
        Map<ChannelSftp,Session> pSessionMap = new HashMap<ChannelSftp, Session>();
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            if (!TWSUtil.isNullText(pPrivateKey)) {
                if (!TWSUtil.isNullText(pPrivatePass)) {
                    jsch.addIdentity(pPrivateKey, pPrivatePass.getBytes("utf-8"));
                } else {
                    jsch.addIdentity(pPrivateKey);
                }
            }

            Session sshSession = jsch.getSession(username, host, port);
            if (!TWSUtil.isNullText(pProxyHost)) {
                ProxySOCKS5 pProxySocks5 = new ProxySOCKS5(pProxyHost, pProxyPort);
                sshSession.setProxy(pProxySocks5);
            }

            System.out.println("Session created.");

            if (!TWSUtil.isNullText(password)) {
                sshSession.setPassword(password);
            }

            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);

            sshSession.connect();

            System.out.println("Session connected.");
            System.out.println("Opening Channel.");

            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            System.out.println("Connected to " + host + ".");

            pSessionMap.put(sftp, sshSession);
        } catch (Exception e) {
            throw e;
        }

        return pSessionMap;
    }

    public Vector<?> listDirs(ChannelSftp pSftpChannel, String pWorkDir) throws Exception {
        pSftpChannel.cd(pWorkDir);
        return pSftpChannel.ls(pWorkDir);
    }

    public static void main(String[] args) throws Exception {
        TBizFTPClientTask pTask = new TBizFTPClientTask() {
            @Override
            protected TTaskReturnBean service(IDalConnection pLink, TTaskBean pTaskBean, TTaskReturnBean pReturnBean, JSONObject pParamJson, JSONObject pRspObj) throws Throwable {
                return pReturnBean;
            }
        };

        String pHost = "10.10.110.52";
        int port = 22;
        String pUserId = "newhope";
        String pPass = "";
        String pPrivateKey = "C:\\Users\\skyli\\.ssh\\dc.ppk";
        String pProxyHost = "127.0.0.1";
        String pProxyPort = "1081";
        Properties properties = new Properties();
        properties.setProperty("sftp.ssh.privatekey", pPrivateKey);
        properties.setProperty("sftp.ssh.privatekey.pass", "123456");
        properties.setProperty("sftp.proxy.host", pProxyHost);
        properties.setProperty("sftp.proxy.port", pProxyPort);
        Map<ChannelSftp, Session> pChannel = pTask.getSFTPChannel(pHost, port, pUserId, pPass, properties);
        ChannelSftp pSftp = pChannel.keySet().iterator().next();
        Session pSession = pChannel.get(pSftp);
        String pFilePath = "/home/newhope/20191127-aaa-bbb.pdf";
        OutputStream out = new FileOutputStream("D:/20191127-aaa-bbb.pdf");
        pSftp.get(pFilePath, out);
        Vector<?> pDirs = pTask.listDirs(pSftp, "/home/newhope/");
        ChannelSftp.LsEntry pEntry = null;
        for (Iterator<?> iterator = pDirs.iterator(); iterator.hasNext();) {
            pEntry = (ChannelSftp.LsEntry) iterator.next();
            System.out.println("fielname=" + pEntry.getFilename());
        }
        out.close();
        pSftp.disconnect();
        pSession.disconnect();
    }
}
