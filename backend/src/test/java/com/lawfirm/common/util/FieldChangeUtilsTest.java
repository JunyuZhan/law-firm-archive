package com.lawfirm.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FieldChangeUtils 单元测试
 *
 * 测试字段变更记录功能
 */
@DisplayName("FieldChangeUtils 字段变更工具测试")
class FieldChangeUtilsTest {

    // ========== 测试模型类 ==========

    static class TestModel {
        private String name;
        private Integer age;
        private Boolean active;
        private String address;

        public TestModel() {}

        public TestModel(String name, Integer age, Boolean active, String address) {
            this.name = name;
            this.age = age;
            this.active = active;
            this.address = address;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    static class ParentModel {
        private String parentField;
        public ParentModel() {}
        public ParentModel(String parentField) { this.parentField = parentField; }
        public String getParentField() { return parentField; }
        public void setParentField(String parentField) { this.parentField = parentField; }
    }

    static class ChildModel extends ParentModel {
        private String childField;
        public ChildModel() {}
        public ChildModel(String parentField, String childField) {
            super(parentField);
            this.childField = childField;
        }
        public String getChildField() { return childField; }
        public void setChildField(String childField) { this.childField = childField; }
    }

    // ========== compare 测试 ==========

    @Test
    @DisplayName("两个 null 对象应该返回空列表")
    void compare_shouldReturnEmptyForBothNull() {
        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(null, null);
        assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("新对象非空旧对象为空应该返回新增变更")
    void compare_shouldReturnAdditionsWhenOldIsNull() {
        TestModel newObj = new TestModel("张三", 30, true, "北京");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(null, newObj);

        assertThat(changes).hasSize(4);
        assertThat(changes).anyMatch(c -> c.getFieldName().equals("name") && c.getOldValue() == null);
    }

    @Test
    @DisplayName("新对象为空旧对象非空应该返回删除变更")
    void compare_shouldReturnDeletionsWhenNewIsNull() {
        TestModel oldObj = new TestModel("张三", 30, true, "北京");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, null);

        assertThat(changes).hasSize(4);
        assertThat(changes).anyMatch(c -> c.getFieldName().equals("name") && c.getNewValue() == null);
    }

    @Test
    @DisplayName("不同类型的对象应该返回空列表")
    void compare_shouldReturnEmptyForDifferentTypes() {
        TestModel obj1 = new TestModel("张三", 30, true, "北京");
        String obj2 = "not a model";

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(obj1, obj2);

        assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("应该正确检测字段变更")
    void compare_shouldDetectFieldChanges() {
        TestModel oldObj = new TestModel("张三", 30, true, "北京");
        TestModel newObj = new TestModel("李四", 35, true, "上海");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj);

        assertThat(changes).hasSize(2);
        assertThat(changes).anyMatch(c ->
            c.getFieldName().equals("name") &&
            c.getOldValue().equals("张三") &&
            c.getNewValue().equals("李四")
        );
        assertThat(changes).anyMatch(c ->
            c.getFieldName().equals("age") &&
            c.getOldValue().equals("30") &&
            c.getNewValue().equals("35")
        );
    }

    @Test
    @DisplayName("相同字段值不应该产生变更记录")
    void compare_shouldNotDetectUnchangedFields() {
        TestModel oldObj = new TestModel("张三", 30, true, "北京");
        TestModel newObj = new TestModel("张三", 30, true, "北京");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj);

        assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("应该只比较指定的字段")
    void compare_shouldOnlyCompareSpecifiedFields() {
        TestModel oldObj = new TestModel("张三", 30, true, "北京");
        TestModel newObj = new TestModel("李四", 35, false, "上海");

        Set<String> fieldNames = Set.of("name", "age");
        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj, fieldNames);

        assertThat(changes).hasSize(2);
        assertThat(changes).allMatch(c -> fieldNames.contains(c.getFieldName()));
    }

    @Test
    @DisplayName("应该包含父类字段的变更检测")
    void compare_shouldIncludeParentFields() {
        ChildModel oldObj = new ChildModel("父字段值1", "子字段值1");
        ChildModel newObj = new ChildModel("父字段值2", "子字段值2");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj);

        assertThat(changes).hasSize(2);
        assertThat(changes).anyMatch(c -> c.getFieldName().equals("parentField"));
        assertThat(changes).anyMatch(c -> c.getFieldName().equals("childField"));
    }

    @Test
    @DisplayName("应该正确处理 null 值变更")
    void compare_shouldHandleNullValueChanges() {
        TestModel oldObj = new TestModel(null, null, null, null);
        TestModel newObj = new TestModel("张三", 30, true, "北京");

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj);

        assertThat(changes).hasSize(4);
    }

    @Test
    @DisplayName("应该正确处理值变为 null")
    void compare_shouldHandleValueToNull() {
        TestModel oldObj = new TestModel("张三", 30, true, "北京");
        TestModel newObj = new TestModel(null, null, null, null);

        List<FieldChangeUtils.FieldChange> changes = FieldChangeUtils.compare(oldObj, newObj);

        assertThat(changes).hasSize(4);
        assertThat(changes).allMatch(c -> c.getNewValue() == null);
    }

    // ========== formatChanges 测试 ==========

    @Test
    @DisplayName("formatChanges: 空列表应该返回默认文本")
    void formatChanges_shouldReturnDefaultForEmpty() {
        String result = FieldChangeUtils.formatChanges(List.of());
        assertThat(result).isEqualTo("无变更");
    }

    @Test
    @DisplayName("formatChanges: null 应该返回默认文本")
    void formatChanges_shouldReturnDefaultForNull() {
        String result = FieldChangeUtils.formatChanges(null);
        assertThat(result).isEqualTo("无变更");
    }

    @Test
    @DisplayName("formatChanges: 应该正确格式化变更记录")
    void formatChanges_shouldFormatChanges() {
        List<FieldChangeUtils.FieldChange> changes = List.of(
            new FieldChangeUtils.FieldChange("name", "张三", "李四"),
            new FieldChangeUtils.FieldChange("age", "30", "35")
        );

        String result = FieldChangeUtils.formatChanges(changes);

        assertThat(result).contains("name: [张三] -> [李四]")
                         .contains("age: [30] -> [35]")
                         .contains("; ");
    }

    @Test
    @DisplayName("formatChanges: 单条变更不应该有分隔符")
    void formatChanges_shouldNotHaveDelimiterForSingleChange() {
        List<FieldChangeUtils.FieldChange> changes = List.of(
            new FieldChangeUtils.FieldChange("name", "张三", "李四")
        );

        String result = FieldChangeUtils.formatChanges(changes);

        assertThat(result).isEqualTo("name: [张三] -> [李四]");
    }

    // ========== FieldChange 测试 ==========

    @Test
    @DisplayName("FieldChange: 应该正确存储字段信息")
    void FieldChange_shouldStoreFieldInfo() {
        FieldChangeUtils.FieldChange change = new FieldChangeUtils.FieldChange("name", "old", "new");

        assertThat(change.getFieldName()).isEqualTo("name");
        assertThat(change.getOldValue()).isEqualTo("old");
        assertThat(change.getNewValue()).isEqualTo("new");
    }
}
