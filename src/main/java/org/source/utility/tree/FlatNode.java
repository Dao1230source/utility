package org.source.utility.tree;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.function.SFunction;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Element;
import org.source.utility.utils.Lambdas;
import org.source.utility.utils.Streams;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
* 扁平节点类
* <p>
* 支持将元素的属性扁平化为 JSON 的节点实现。
* 通过属性提取函数，将元素的多个属性直接作为节点的属性输出。
* </p>
* <p>
* 特性：
* <ul>
*   <li>属性扁平化：将元素属性直接作为节点属性</li>
*   <li>动态属性：支持任意数量的属性</li>
*   <li>JSON 友好：通过 @JsonAnyGetter 实现属性映射</li>
* </ul>
* </p>
* <p>
* 使用场景：
* <ul>
*   <li>需要扁平化输出的树形结构</li>
*   <li>RESTful API 的响应</li>
*   <li>数据转换和聚合</li>
* </ul>
* </p>
*
* @param <I> ID 类型
* @param <E> 元素类型
* @author utility
* @since 1.0
*/
@Slf4j
@JsonIgnoreProperties({"element", "properties"})
@EqualsAndHashCode(callSuper = true)
@Data
public class FlatNode<I extends Comparable<I>, E extends Element<I>> extends AbstractNode<I, E, FlatNode<I, E>> {
    /**
     * 属性提取函数列表
     * 用于从元素中提取属性，生成动态的键值对
     */
    @JsonIgnore
    private final List<SFunction<E, Object>> propertyGetters;

    /**
     * 动态属性映射
     * 存储从元素提取的所有属性，作为节点的额外属性输出
     */
    @JsonAnyGetter
    private LinkedHashMap<String, Object> properties;

    /**
     * 构造函数
     *
     * @param propertyGetters 属性提取函数列表，不能为空
     * @throws BaseException 如果 propertyGetters 为空
     */
    public FlatNode(List<SFunction<E, Object>> propertyGetters) {
        BaseExceptionEnum.NOT_EMPTY.notEmpty(propertyGetters, "propertyGetters 集合不能为空");
        this.propertyGetters = propertyGetters;
        this.properties = LinkedHashMap.newLinkedHashMap(propertyGetters.size());
    }

    /**
     * 创建空节点
     *
     * @return 新的 FlatNode 实例，使用相同的属性提取函数列表
     */
    @Override
    public FlatNode<I, E> emptyNode() {
        return new FlatNode<>(this.propertyGetters);
    }

    /**
     * 节点处理钩子
     * <p>
     * 在节点添加到树后，执行属性提取逻辑。
     * 将元素的属性通过提取函数提取出来，存储到 properties 中。
     * </p>
     */
    @Override
    public void nodeHandler() {
        if (Objects.isNull(this.getElement())) {
            return;
        }
        Streams.of(this.propertyGetters).forEach(k -> {
            String fieldName = Lambdas.getFieldName(k);
            BaseExceptionEnum.LAMBDA_FIELD_NAME_NOT_FOUND.notEmpty(fieldName);
            Object value = k.apply(this.getElement());
            if (Objects.nonNull(value)) {
                this.getProperties().put(fieldName, value);
            }
        });
    }
}