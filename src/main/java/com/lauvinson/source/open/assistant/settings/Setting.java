package com.lauvinson.source.open.assistant.settings;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.table.JBTable;
import com.lauvinson.source.open.assistant.Constant;
import com.lauvinson.source.open.assistant.Group;
import com.lauvinson.source.open.assistant.configuration.Config;
import com.lauvinson.source.open.assistant.configuration.ConfigService;
import com.lauvinson.source.open.assistant.utils.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Setting implements Configurable {

    private final ConfigService configService = ServiceManager.getService(ConfigService.class);
    private final Config config = configService.getState();
    private static final Object[][] EMPT_TTWO_DIMENSION_ARRAY = new Object[0][0];
    private final LinkedHashMap<String, LinkedHashMap<String, String>> group = new LinkedHashMap<>();
    private LinkedHashMap<String, String> attribute = new LinkedHashMap<>();

    private static final String DISPLAY_NAME = "Interactive Assistant";

    private JPanel root;
    private JButton groupAddButton;
    private JButton groupRemoveButton;
    private JScrollPane groupPanel;
    private JLabel abilityMapLabel;
    private JLabel groupLabel;
    private JList<Object> groupList;
    private JPanel top;
    private JTable attributeTable;
    private JTabbedPane tabs;
    private JPanel bottom;
    private JButton attributeAddButton;
    private JButton attributeRemoveButton;


    @Nls
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        CollectionUtils.Companion.mapCopy(config.getGroup(), this.group);
        this.loadSettings();
        this.tabs.setSelectedIndex(0);
        return this.root;
    }

    @Override
    public boolean isModified() {
        boolean modify = config.getGroup().equals(group);
        if (!modify) {
            Group.Companion.modify();
        }
        return !modify;
    }

    @Override
    public void apply() throws ConfigurationException {
        //set config
        LinkedHashMap<String, LinkedHashMap<String, String>> temp = new LinkedHashMap<>(this.group.size());
        CollectionUtils.Companion.mapCopy(this.group, temp);
        config.setGroup(temp);
    }

    private void loadSettings() {
        //load xml file to the panel
        this.updateGroupUi();
        this.initListener();
    }

    private void initListener() {
        groupAddButton.addActionListener(e -> {
            LinkedHashMap<String, String> value = new LinkedHashMap<String, String>(1) {{
                put(Constant.AbilityType, Constant.AbilityType_API);
            }};
            group.put(UUID.randomUUID().toString(), value);
            updateGroupUi();
        });

        groupRemoveButton.addActionListener(e -> {
            Object select = groupList.getSelectedValue();
            group.remove(select.toString());
            updateAbilityUi(EMPT_TTWO_DIMENSION_ARRAY);
            updateGroupUi();
            groupRemoveButton.setEnabled(false);
            attributeRemoveButton.setEnabled(!MapUtils.isEmpty(attribute));
            attributeAddButton.setEnabled(true);
        });

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(groupList.getSelectedIndex() != -1) {
                    Object oldValue = groupList.getSelectedValue();
                    if (1 == e.getClickCount()) {
                        groupRemoveButton.setEnabled(true);
                        // show map
                        attribute = group.get(oldValue.toString());
                        Object[][] ability = CollectionUtils.Companion.getMapKeyValue(attribute);
                        updateAbilityUi(ability);
                        attributeAddButton.setEnabled(true);
                    }
                    if(e.getClickCount() == 2) {
                        String newValue = doubleClick(oldValue);
                        if (StringUtils.isNotBlank(newValue)) {
                            group.put(newValue, group.remove(oldValue.toString()));
                            updateGroupUi();
                        }
                    }
                }
            }
        });

        attributeAddButton.addActionListener(e -> {
            Object select = groupList.getSelectedValue();
            Map<String, String> attributes = group.get(select.toString());
            attributes.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            Object[][] ability = CollectionUtils.Companion.getMapKeyValue(attributes);
            updateAbilityUi(ability);
        });

        attributeRemoveButton.addActionListener(e -> {
            int row = attributeTable.getSelectedRow();
            attribute.remove(attributeTable.getValueAt(row, 0).toString());
            Object[][] ability = CollectionUtils.Companion.getMapKeyValue(attribute);
            attributeRemoveButton.setEnabled(!MapUtils.isEmpty(attribute));
            updateAbilityUi(ability);
        });

        attributeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(attributeTable.getSelectedRow() != -1) {
                    int row = attributeTable.getSelectedRow();
                    if (Constant.AbilityType.equals(attributeTable.getValueAt(row, 0).toString())) {
                        return;
                    }
                    attributeRemoveButton.setEnabled(true);
                }
            }
        });
    }

    private void updateGroupUi() {
        Object[] groups = group.keySet().toArray();
        this.groupList.setListData(groups);
        this.groupList.updateUI();
    }

    private void updateAbilityUi(Object[][] data) {
        ((AbilityTableModel) this.attributeTable.getModel()).setData(data);
        this.groupList.updateUI();
        this.attributeTable.updateUI();
    }

    private void changeAbilityKey(String oldk, String newk) {
        String select = groupList.getSelectedValue().toString();
        String oldv = group.get(select).remove(oldk);
        group.get(select).put(newk, oldv);
    }

    private void changeAbilityValue(String key, String value) {
        Object select = groupList.getSelectedValue().toString();
        group.get(select).put(key, value);
    }

    private String doubleClick(Object value) {
        return JOptionPane.showInputDialog(
                root,
                "Modify the group name",
                value
        );
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        attributeTable = new JBTable(new AbilityTableModel());
    }


    class AbilityTableModel extends AbstractTableModel {
        // 表格中第一行所要显示的内容存放在字符串数组columnNames中
        String[] abilityColumnNames = { "Name", "Pass"};
        // 表格中各行的内容保存在二维数组data中
        Object[][] data;

        // 下述方法是重写AbstractTableModel中的方法，其主要用途是被JTable对象调用，以便在表格中正确的显示出来。程序员必须根据采用的数据类型加以恰当实现。


        AbilityTableModel() {
        }

        public void setData(Object[][] data) {
            this.data = data;
        }

        AbilityTableModel(Object[][] data) {
            this.data = data;
        }

        // 获得列的数目
        @Override
        public int getColumnCount() {
            return abilityColumnNames.length;
        }

        // 获得行的数目
        @Override
        public int getRowCount() {
            return null != data ? data.length : 0;
        }

        // 获得某列的名字，而目前各列的名字保存在字符串数组columnNames中
        @Override
        public String getColumnName(int col) {
            return abilityColumnNames[col];
        }

        // 获得某行某列的数据，而数据保存在对象数组data中
        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        // 判断每个单元格的类型
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        // 将表格声明为可编辑的
        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        // 改变某个数据的值
        @Override
        public void setValueAt(Object value, int row, int col) {
            if (data[0][col] instanceof Integer && !(value instanceof Integer)) {
                try {
                    data[row][col] = Integer.valueOf(value.toString());
                    fireTableCellUpdated(row, col);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(root, "The \""
                            + getColumnName(col)
                            + "\" column accepts only integer values.");
                }
            } else {
                switch (col) {
                    case 0:
                        changeAbilityKey(data[row][0].toString(), value.toString());
                        break;
                    case 1:
                        changeAbilityValue(data[row][0].toString(), value.toString());
                        break;
                    default:
                        break;
                }
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }

        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i = 0; i < numRows; i++) {
                System.out.print(" row " + i + ":");
                for (int j = 0; j < numCols; j++) {
                    System.out.print(" " + data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }


}
