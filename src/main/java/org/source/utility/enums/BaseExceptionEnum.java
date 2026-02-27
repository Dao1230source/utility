package org.source.utility.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.utility.exceptions.BaseException;
import org.source.utility.exceptions.EnumProcessor;

/**
 *
 */
@Getter
@AllArgsConstructor
public enum BaseExceptionEnum implements EnumProcessor<BaseException> {

    /**
     * runtime
     */
    RUNTIME_EXCEPTION("服务异常"),
    SYSTEM_EXCEPTION("系统异常"),
    CIRCULAR_REFERENCE_EXCEPTION("循环引用"),
    /**
     * utility
     */
    RESOLVE_S_FUNCTION_EXCEPTION("解析SFunction异常"),
    REFLECT_EXCEPTION("反射方法异常"),
    LAMBDA_FIELD_NAME_NOT_FOUND("lambdas 获取fieldName失败"),

    /**
     * Assert
     */
    VALIDATE_ERROR("参数检验错误"),
    NOT_NULL("不能为null"),
    NOT_EMPTY("不能为空"),
    NOT_EXISTS("不存在"),
    ENUM_EXCEPTION("枚举值校验异常"),
    SIZE_MIN("大小必须大于"),
    MUST_BE_ENUM_TYPE("必须是枚举类型"),
    FIELD_NAME_INVALID("字段名称不正确"),
    RECORD_NOT_FOUND("数据未找到"),
    RECORD_HAS_EXISTS("数据已存在"),
    BATCHSIZE_MUST_BE_POSITIVE("批次数必须为正数"),

    /**
     * http request
     */
    INVALID_RPC_REQUEST("无效的RPC请求"),
    INVALID_SECRET_KEY("无效的secret key"),
    /**
     * request
     */
    REQUEST_EXECUTE_EXCEPTION("request_execute_exception"),
    REQUEST_DO_CALL_EXCEPTION("request_do_call_exception"),
    /**
     * json
     */
    JSON_STRING_2_OBJECT_EXCEPTION("String转换为对象异常"),
    JSON_OBJECT_2_STRING_EXCEPTION("对象转换为String异常"),
    JSON_OBJECT_2_BYTES_EXCEPTION("对象转换为bytes异常"),
    /**
     * spring utility
     */
    GET_IP_PORT_FAIL("获取IP或Port失败"),
    /**
     * assign exception
     */
    ASSIGN_ACQUIRE_RUN_EXCEPTION("assign acquire exception"),
    ASSIGN_PARALLEL_EXECUTE_EXCEPTION("assign parallel execute exception"),
    /**
     * thread
     */
    THREAD_INTERRUPTED("thread interrupted"),
    /**
     * tree
     */
    TREE_MERGE_EXCEPTION("merge_exception"),
    TREE_CAN_NOT_UPDATE_ID("can not update id"),
    ;

    private final String message;

}
