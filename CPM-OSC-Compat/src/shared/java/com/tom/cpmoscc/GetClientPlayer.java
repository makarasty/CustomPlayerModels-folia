package com.tom.cpmoscc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class GetClientPlayer {
	private static final String[] MC_CLASS = new String[] {"net.minecraft.client.Minecraft", "net.minecraft.class_310"};
	private static final String[] PLAYER = new String[] {"player", "field_71439_g", "f_91074_", "field_1724"};
	private static Supplier<Object> getPlayer;

	public static void init() {
		Class<?> mcClass = null;
		for (int i = 0; i < MC_CLASS.length; i++) {
			String string = MC_CLASS[i];
			try {
				mcClass = Class.forName(string);
			} catch (Throwable e) {
				continue;
			}
		}
		if (mcClass == null)throw new RuntimeException("Can't find Minecraft");
		Object inst = null;
		try {
			for(Method method : mcClass.getDeclaredMethods()) {
				if(Modifier.isStatic(method.getModifiers()) && method.getReturnType() == mcClass) {
					inst = method.invoke(null);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		if(inst == null)throw new NoSuchMethodError("Failed to find Minecraft.getInstance()");

		try {
			Field playerF = null;
			for (String m : PLAYER) {
				try {
					playerF = mcClass.getDeclaredField(m);
				} catch (Throwable e) {
					continue;
				}
			}
			if(playerF == null)throw new RuntimeException("Failed to find Minecraft.player");

			final Field pf = playerF;
			final Object i = inst;
			getPlayer = () -> {
				try {
					return pf.get(i);
				} catch (IllegalAccessException | IllegalArgumentException e) {
					return false;
				}
			};
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Object get() {
		return getPlayer.get();
	}
}
