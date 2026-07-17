package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.AbstractBackpackItem;
import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PortableContainerPolicyTest {
    @Test
    void everyBackpackTypeUsesTheSharedContainerSafetyBase() {
        assertEquals(AbstractBackpackItem.class, TieredBackpackItem.class.getSuperclass());
        assertEquals(AbstractBackpackItem.class, DeathBackpackItem.class.getSuperclass());
    }

    @Test
    void sharedBackpackBaseOverridesTheVanillaContainerHook() throws ReflectiveOperationException {
        Method method = AbstractBackpackItem.class.getDeclaredMethod("canFitInsideContainerItems");

        assertNotNull(method);
        assertEquals(boolean.class, method.getReturnType());
        assertFalse(method.isSynthetic());
    }

    @Test
    void addonTagUsesTheStableDeadRecallIdentifier() {
        assertEquals("deadrecall:portable_containers", PortableContainerPolicy.PORTABLE_CONTAINERS.location().toString());
    }
}
