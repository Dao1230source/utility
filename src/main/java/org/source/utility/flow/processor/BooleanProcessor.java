package org.source.utility.flow.processor;

/**
 * @author zengfugen
 */
public interface BooleanProcessor extends Processor<Boolean> {

    Boolean DEFAULT_KEY = Boolean.FALSE;

    /**
     * 获取Boolean的key值，默认key=DEFAULT_KEY
     *
     * @return key值
     */
    @Override
    default Boolean getKey() {
        return DEFAULT_KEY;
    }
}
