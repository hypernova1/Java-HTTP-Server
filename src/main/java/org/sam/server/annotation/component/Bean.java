package org.sam.server.annotation.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컴포넌트 클래스 안의 메서드에 선언하여 해당 메서드의 반환 값을 빈으로 만든다.
 *
 * @author hypernova1
 * */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
}
