package org.example.springboot.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusLazyInitConfig {
    @Bean
   public static BeanFactoryPostProcessor milvusBeanFactoryPostProcessor() {
       return beanFactory ->{
           setLazy(beanFactory,"milvusClient");
           setLazy(beanFactory,"vectorStore");
       };
   }

   private static void setLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
       if(beanFactory.containsBeanDefinition(beanName)){
           BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
           beanDefinition.setLazyInit(true);

       }
   }
}
