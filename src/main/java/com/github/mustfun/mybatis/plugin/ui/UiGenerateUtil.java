package com.github.mustfun.mybatis.plugin.ui;

import com.github.mustfun.mybatis.plugin.listener.CheckMouseListener;
import com.github.mustfun.mybatis.plugin.model.DbSourcePo;
import com.github.mustfun.mybatis.plugin.model.LocalTable;
import com.github.mustfun.mybatis.plugin.model.Template;
import com.github.mustfun.mybatis.plugin.service.DbService;
import com.github.mustfun.mybatis.plugin.service.SqlLiteService;
import com.github.mustfun.mybatis.plugin.setting.ConnectDbSetting;
import com.github.mustfun.mybatis.plugin.ui.custom.DialogWrapperPanel;
import com.github.mustfun.mybatis.plugin.util.ConnectionHolder;
import com.github.mustfun.mybatis.plugin.util.JavaUtils;
import com.github.mustfun.mybatis.plugin.util.MybatisConstants;
import com.github.mustfun.mybatis.plugin.util.OrderedProperties;
import com.github.mustfun.mybatis.plugin.util.crypto.ConfigTools;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckBoxList;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author itar
 * @date 2018/6/12
 * @version 1.0
 * @since jdk1.8
 */
public final class UiGenerateUtil {

    private Project project;

    private FileEditorManager fileEditorManager;

    private ConnectDbSetting connectDbSetting;

    private UiGenerateUtil(Project project) {
        this.project = project;
        this.fileEditorManager = FileEditorManager.getInstance(project);
    }

    public static UiGenerateUtil getInstance(@NotNull Project project) {
        return new UiGenerateUtil(project);
    }

    public DialogWrapperPanel getCommonDialog(){
        UiComponentFacade uiComponentFacade = UiComponentFacade.getInstance(project);
        if (null == connectDbSetting) {
            this.connectDbSetting = new ConnectDbSetting();
        }
        connectDbSetting.getConnectButton().addActionListener(e -> {
            connectDbSetting.getTemplateCheckbox().clear();
            connectDbSetting.getTableCheckBox().clear();
            //监听点击
            String address = connectDbSetting.getAddress().getText();
            String port = connectDbSetting.getPort().getText();
            String dbName = connectDbSetting.getDbName().getText();
            String userName = connectDbSetting.getUserName().getText();
            String password = connectDbSetting.getPassword().getText();
            DbSourcePo dbSourcePo = new DbSourcePo();
            dbSourcePo.setDbAddress(address);
            dbSourcePo.setDbName(dbName);
            dbSourcePo.setPort(Integer.parseInt(port));
            dbSourcePo.setUserName(userName);
            dbSourcePo.setPassword(password);
            //连接数据库
            DbService dbService = DbService.getInstance(project);
            Connection connection = dbService.getConnection(dbSourcePo);
            ConnectionHolder.addConnection(MybatisConstants.MYSQL_DB_CONNECTION,connection);
            if (connection == null) {
                Messages.showMessageDialog("数据库连接失败", "连接数据库提示", Messages.getInformationIcon());
                return;
            }
            List<LocalTable> tables = dbService.getTables(connection);
            CheckBoxList<String> tableCheckBox = connectDbSetting.getTableCheckBox();
            for (LocalTable table : tables) {
                tableCheckBox.addItem(table.getTableName(),table.getTableName(),false);
            }
            Connection sqlLiteConnection = dbService.getSqlLiteConnection();
            ConnectionHolder.addConnection(MybatisConstants.SQL_LITE_CONNECTION,sqlLiteConnection);
            SqlLiteService sqlLiteService =  SqlLiteService.getInstance(sqlLiteConnection);
            List<Template> templates = sqlLiteService.queryTemplateList();
            CheckBoxList<Integer> templateCheckbox = connectDbSetting.getTemplateCheckbox();
            for (Template template : templates) {
                templateCheckbox.addItem(template.getId(),template.getTepName(),false);
            }
            templateCheckbox.addMouseListener(new CheckMouseListener(project,1,templates.get(2)));
        });


        //找出dao层所在目录
        JButton daoPanel = connectDbSetting.getDaoButton();
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile daoPath = JavaUtils.getFilePattenPath(baseDir, "/dao/","/dal/","Mapper.java","Dao.java");
        if (daoPath==null){
            daoPath = baseDir;
        }
        connectDbSetting.getDaoInput().setText(daoPath.getPath());
        VirtualFile finalDaoPath = daoPath;
        daoPanel.addActionListener(e -> {
            VirtualFile vf = uiComponentFacade.showSingleFolderSelectionDialog("请选择dao层存放目录", finalDaoPath, baseDir);
            //打印的就是选择的路径
            String path = vf.getPath();
            System.out.println("path = " + path);
            connectDbSetting.getDaoInput().setText(path);
        });

        //找出Mapper层所在目录
        JButton mapperButton = connectDbSetting.getMapperButton();
        VirtualFile mapperPath = JavaUtils.getFilePattenPath(baseDir, "/mapper/","Mapper.xml","Dao.xml");
        if (mapperPath==null){
            mapperPath = baseDir;
        }
        connectDbSetting.getMapperInput().setText(mapperPath.getPath());
        VirtualFile finalMapperPath = mapperPath;
        mapperButton.addActionListener(e->{
            VirtualFile vf = uiComponentFacade.showSingleFolderSelectionDialog("请选择Mapper层存放目录", finalMapperPath, baseDir);
            //打印的就是选择的路径
            String path = vf.getPath();
            System.out.println("path = " + path);
            connectDbSetting.getMapperInput().setText(path);
        });

        //找出model所在的目录
        VirtualFile modelPath = JavaUtils.getFilePattenPath(baseDir, "/model/");
        if (modelPath==null){
            modelPath = baseDir;
        }
        VirtualFile finalModelPath = modelPath;
        connectDbSetting.getPoInput().setText(modelPath.getPath());
        connectDbSetting.getPoButton().addActionListener(e->{
            VirtualFile vf = uiComponentFacade.showSingleFolderSelectionDialog("请选择实体层存放目录", finalModelPath, baseDir);
            //打印的就是选择的路径
            String path = vf.getPath();
            System.out.println("path = " + path);
            connectDbSetting.getPoInput().setText(path);
        });

        // 找出Service所在的目录
        VirtualFile servicePath = JavaUtils.getFilePattenPath(baseDir,"/service/","service.java");
        if (servicePath==null){
            servicePath = baseDir;
        }
        VirtualFile finalServicePath = servicePath;
        connectDbSetting.getServiceInput().setText(servicePath.getPath());
        connectDbSetting.getServiceButton().addActionListener(e->{
            VirtualFile vf = uiComponentFacade.showSingleFolderSelectionDialog("请选择Service层存放目录", finalServicePath, baseDir);
            //打印的就是选择的路径
            String path = vf.getPath();
            System.out.println("path = " + path);
            connectDbSetting.getServiceInput().setText(path);
        });

        // 找出controller所在的目录
        VirtualFile controllerPath = JavaUtils.getFilePattenPath(baseDir, "/facade/","/controller/","controller.java");
        if (controllerPath==null){
            controllerPath = baseDir;
        }
        VirtualFile finalControllerPath = controllerPath;
        connectDbSetting.getControllerInput().setText(controllerPath.getPath());
        connectDbSetting.getControllerButton().addActionListener(e->{
            VirtualFile vf = uiComponentFacade.showSingleFolderSelectionDialog("请选择Controller层存放目录", finalControllerPath, baseDir);
            //打印的就是选择的路径
            String path = vf.getPath();
            System.out.println("path = " + path);
            connectDbSetting.getControllerInput().setText(path);
        });

        //读取ymal或者property进行填充
        fillPanelText(connectDbSetting);


        return new DialogWrapperPanel(project,true,connectDbSetting);
    }


    private void fillPanelText(ConnectDbSetting connectDbSetting) {
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile file = JavaUtils.getFileByPattenName(baseDir, "application.properties","application-dev.properties","application.yml","application-dev.yml");
        if(file==null){
            return ;
        }
        //读取yml文件
        File ymlFile = new File(file.getPath());
        if (file.getPath().contains(".yml")||file.getPath().contains(".yaml")){
            insertPanelUseYaml(connectDbSetting, ymlFile);
        }else if(file.getPath().contains(".properties")){
            insertPanelUseProperties(connectDbSetting, ymlFile);
        }
    }

    private void insertPanelUseProperties(ConnectDbSetting connectDbSetting, File file) {
        //读取properties类型文件
        try {
            OrderedProperties properties = new OrderedProperties();
            properties.load(new FileInputStream(file));
            boolean findUserName=true,findPassword=true,findUrl=true;
            for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
                Object key = objectObjectEntry.getKey();
                boolean isMatchUserName = Pattern.matches(".*?druid(.*)(username$)", (String)key);
                if (isMatchUserName&&findUserName){
                    findUserName = false;
                    Object value = objectObjectEntry.getValue();
                    connectDbSetting.getUserName().setText((String)value);
                }
                boolean isMatchPassword = Pattern.matches(".*?druid(.*)(password$)", (String)key);
                if (isMatchPassword&&findPassword){
                    findPassword = false;
                    String password = (String) objectObjectEntry.getValue();
                    if (password!=null&&password.length()>=64){
                        password  = ConfigTools.decrypt(password);
                    }
                    connectDbSetting.getPassword().setText(password);
                }
                boolean isMatchUrl = Pattern.matches(".*?druid(.*)(url$)", (String)key);
                if (isMatchUrl&&findUrl){
                    findUrl = false;
                    Object value = objectObjectEntry.getValue();
                    String[] s = ((String)value).split("/");
                    String[] split = s[2].split(":");
                    connectDbSetting.getAddress().setText(split[0]);
                    connectDbSetting.getPort().setText(split[1]);
                    String s1 = s[3].split("\\?")[0];
                    connectDbSetting.getDbName().setText(s1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertPanelUseYaml(ConnectDbSetting connectDbSetting, File ymlFile) {
        try {
            Yaml yaml = new Yaml();
            //读入文件
            Iterable<Object> result = yaml.loadAll(new FileInputStream(ymlFile));
            while (result.iterator().hasNext()){
                Object next = result.iterator().next();
                String username = JavaUtils.findYamlValueByTag((LinkedHashMap)next ,"username");
                connectDbSetting.getUserName().setText(username==null?"root":username);
                String password = JavaUtils.findYamlValueByTag((LinkedHashMap)next,"password");
                if (password!=null&&password.length()>=64){
                    password  = ConfigTools.decrypt(password);
                }
                connectDbSetting.getPassword().setText(password);
                String url = JavaUtils.findYamlValueByTag((LinkedHashMap)next,"url");
                if (url!=null&&url.contains("jdbc:mysql")){
                    String[] s = url.split("/");
                    String[] split = s[2].split(":");
                    connectDbSetting.getAddress().setText(split[0]);
                    connectDbSetting.getPort().setText(split[1]);
                    String s1 = s[3].split("\\?")[0];
                    connectDbSetting.getDbName().setText(s1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public JBPopup getCommonPopUp(){
        if (null == connectDbSetting) {
            this.connectDbSetting = new ConnectDbSetting();
        }

        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(connectDbSetting.getMainPanel(), null)
                /*
                .setResizable(false)
                .setShowShadow(true)
                .setCancelKeyEnabled(true)
                .setShowBorder(true)*/
                .setTitle("生成MapperXml文件")
                .setShowBorder(true)
                .setCancelButton(new IconButton("关闭",AllIcons.Actions.Close))
                .setRequestFocus(true)
                .setFocusable(true)
                .setMovable(false)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnClickOutside(false)
                .setProject(this.project)
                .createPopup();
        return popup;
    }



    @NotNull
    private BalloonBuilder buildBalloon(JComponent component) {
        JBInsets BORDER_INSETS = JBUI.insets(20, 20, 20, 20);
        return JBPopupFactory.getInstance()
                .createDialogBalloonBuilder(component, null)
                .setHideOnClickOutside(true)
                .setShadow(true)
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(true)
                .setBorderInsets(BORDER_INSETS);
    }
}
