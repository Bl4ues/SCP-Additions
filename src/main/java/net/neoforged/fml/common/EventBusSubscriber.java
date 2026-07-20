package net.neoforged.fml.common;
import java.lang.annotation.*;
import net.neoforged.api.distmarker.Dist;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid() default "";
    Dist[] value() default {};
    Bus bus() default Bus.GAME;
    enum Bus { GAME, MOD }
}
