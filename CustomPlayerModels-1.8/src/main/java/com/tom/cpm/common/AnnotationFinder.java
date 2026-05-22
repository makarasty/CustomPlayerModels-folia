package com.tom.cpm.common;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;

public class AnnotationFinder {

	public static Set<String> getInstances(ASMDataTable adt, Class<?> annotationClass) {
		Set<ASMData> allClasses = adt.getAll(annotationClass.getName());
		Set<String> pluginClassNames = new LinkedHashSet<>();
		for (ASMData scanData : allClasses) {
			pluginClassNames.add(scanData.getClassName());
		}
		return pluginClassNames;
	}
}
