package com.jea.achieve.test;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.yea.achieve.generator.dto.GeneratorConfig;
import com.yea.achieve.generator.facade.GeneratorFacade;


@RunWith(SpringJUnit4ClassRunner.class)  
@ContextConfiguration(locations="/application-bean.xml")
public class GeneratorTest {
	@Autowired 
    private GeneratorFacade generatorFacade;
	
    @Test
    public void start() throws Exception {
    	String path = new File(this.getClass().getResource("/").getPath()).getParentFile().getParentFile().toString();
    	GeneratorConfig config = new GeneratorConfig();
    	config.setBuildPath(path);
    	config.setTablePrefixOverrides("t_");
    	config.setTableName("t_role%");
    	config.setBasePackagePath("com.team.demo");
    	config.setModuleName("user");
    	config.setDaoGenerateable(false);
    	
    	Object[] arg0 = new Object[]{config};
    	generatorFacade.facade(arg0);
    	Thread.sleep(90 * 60 * 1000);        
    }
    
}
