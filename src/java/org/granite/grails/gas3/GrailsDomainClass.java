package org.granite.grails.gas3;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.Constraint;
import org.granite.generator.as3.As3Type;
import org.granite.generator.as3.reflect.JavaEntityBean;
import org.granite.generator.as3.reflect.JavaProperty;
import org.granite.generator.as3.reflect.JavaTypeFactory;
import org.granite.grails.integration.GrailsExternalizer;

import groovy.lang.GroovyObject;


public class GrailsDomainClass extends JavaEntityBean {
	
	private Map<String, As3Type> hasMany = null;
	private Map<String, Map<String, String>> constraints = null;
	private org.codehaus.groovy.grails.commons.GrailsDomainClass domainClass = null;


    @SuppressWarnings("unchecked")
    public GrailsDomainClass(JavaTypeFactory provider, Class<?> type, URL url) {
        super(provider, type, url);
        
        domainClass = new DefaultGrailsDomainClass(type);
        
        Map<String, Class<?>> hasMany = domainClass.getAssociationMap();
    	if (hasMany != null && !hasMany.isEmpty()) {
    		this.hasMany = new HashMap<String, As3Type>();
        	for (Map.Entry<String, Class<?>> me : hasMany.entrySet()) {
        		this.hasMany.put(me.getKey(), getProvider().getAs3Type(me.getValue()));
        	}
    	}

        Map<String, ConstrainedProperty> constraints = domainClass.getConstrainedProperties();
        
    	if (constraints != null && !constraints.isEmpty()) {
    		List<GrailsDomainClassProperty> properties = new ArrayList<GrailsDomainClassProperty>();
    		for (String propertyName : constraints.keySet())
    			properties.add(domainClass.getPropertyByName(propertyName));
    		Collections.sort(properties, new DomainClassPropertyComparator(domainClass));
    		
    		this.constraints = new LinkedHashMap<String, Map<String, String>>();
    		for (GrailsDomainClassProperty property : properties) {
    			if ("uid".equals(property.getName()) || "version".equals(property.getName()) || "id".equals(property.getName()))
    				continue;
    			
    			Collection<Constraint> appliedConstraints = constraints.get(property.getName()).getAppliedConstraints();
    	        
    			Map<String, String> c = new HashMap<String, String>();
    			for (Constraint constraint : appliedConstraints) {
    				if ("org.codehaus.groovy.grails.validation.NullableConstraint".equals(constraint.getClass().getName()))
    					continue;
    				String value = constraint.toString();
    				c.put(constraint.getName(), "\"" + value.substring(value.indexOf('[')+1, value.lastIndexOf(']')) + "\"");
    			}
    			
				this.constraints.put(property.getName(), c);
    		}
    	}
    }
    
    @Override
	protected SortedMap<String, JavaProperty> initProperties() {
    	SortedMap<String, JavaProperty> properties = super.initProperties();
    	// domainClass not ready at this time
        if (!type.getSuperclass().equals(GroovyObject.class) &&
            !type.getSuperclass().equals(Object.class) &&
            !Modifier.isAbstract(type.getSuperclass().getModifiers())) {
    		properties.remove("id");
    		properties.remove("version");
    	}
        
        for (String event : GrailsExternalizer.EVENTS)
        	properties.remove(event);
        
    	return properties;
	}

	@Override
    public boolean hasIdentifiers() {
    	if (!domainClass.isRoot())
    		return false;
        return super.hasIdentifiers();
    }
    
    public Map<String, As3Type> getHasMany() {
    	return hasMany;
    }

    public Map<String, Map<String, String>> getConstraints() {
    	return constraints;
    }
}