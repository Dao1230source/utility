package org.source.utility.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public
class ExceptionConstructor<E extends BaseException> {
    private Constructor<E> base;
    private Constructor<E> baseAndExtra;
    private Constructor<E> baseAndEx;
    private Constructor<E> baseAndExAndExtra;
}
