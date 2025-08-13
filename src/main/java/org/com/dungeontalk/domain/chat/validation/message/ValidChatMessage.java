package org.com.dungeontalk.domain.chat.validation.message;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ChatMessageValidator.class)
public @interface ValidChatMessage {
    String message() default "Invalid chat message payload";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
