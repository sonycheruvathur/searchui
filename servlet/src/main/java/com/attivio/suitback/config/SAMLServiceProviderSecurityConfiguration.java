package com.attivio.suitback.config;

import static com.github.ulisesbocchio.spring.boot.security.saml.util.FunctionalUtils.unchecked;

import java.util.Collections;
import java.util.List;

import org.assertj.core.util.Lists;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.log.SAMLLogger;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.SAMLProcessor;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileHoKImpl;

import com.github.ulisesbocchio.spring.boot.security.saml.bean.SAMLConfigurerBean;
import com.github.ulisesbocchio.spring.boot.security.saml.bean.override.LocalExtendedMetadata;
import com.github.ulisesbocchio.spring.boot.security.saml.configurer.ServiceProviderBuilder;
import com.github.ulisesbocchio.spring.boot.security.saml.configurer.ServiceProviderConfigurer;
import com.github.ulisesbocchio.spring.boot.security.saml.configurer.ServiceProviderEndpoints;
import com.github.ulisesbocchio.spring.boot.security.saml.properties.SAMLSSOProperties;

/**
 * This is basically the same code as
 * <code>com.github.ulisesbocchio.spring.boot.security.saml.configuration.SAMLServiceProviderSecurityConfiguration</code>
 * with the beans all marked as only occurring if we're using the SAML profile.
 * 
 * If this isn't done like this, then when the configuration is included but we're
 * not configured for SAML, the servlet won't start due to a non-existing idp-metadata.xml
 * file.
 */
@Configuration
@EnableConfigurationProperties(SAMLSSOProperties.class)
@Profile("saml")
public class SAMLServiceProviderSecurityConfiguration implements InitializingBean {

    @Autowired
    private SAMLSSOProperties sAMLSsoProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SAMLLogger samlLogger;

    @Autowired(required = false)
    private ExtendedMetadata extendedMetadata;

    @Autowired(required = false)
    private LocalExtendedMetadata localExtendedMetadata;

    @Autowired(required = false)
    private SAMLContextProvider samlContextProvider;

    @Autowired(required = false)
    private KeyManager keyManager;

    @Autowired(required = false)
    private MetadataManager metadataManager;

    @Autowired(required = false)
    private MetadataGenerator metadataGenerator;

    @Autowired(required = false)
    private SAMLProcessor samlProcessor;

    @Autowired(required = false)
    private WebSSOProfileConsumer webSSOProfileConsumer;

    @Autowired(required = false)
    private WebSSOProfileConsumerHoKImpl hokWebSSOProfileConsumer;

    @Autowired(required = false)
    private WebSSOProfile webSSOProfile;

    @Autowired(required = false)
    private WebSSOProfileECPImpl ecpProfile;

    @Autowired(required = false)
    private WebSSOProfileHoKImpl hokWebSSOProfile;

    @Autowired(required = false)
    private SingleLogoutProfile sloProfile;

    @Autowired(required = false)
    private SAMLAuthenticationProvider samlAuthenticationProvider;

    @Autowired(required = false)
    List<ServiceProviderConfigurer> serviceProviderConfigurers = Lists.newArrayList();

    @Autowired(required = false)
    SAMLConfigurerBean samlConfigurerBean;

    @Autowired
    ServiceProviderBuilder serviceProviderBuilder;

    @Bean
    @ConditionalOnMissingBean
    @Profile("saml")
    public static SAMLBootstrap sAMLBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("saml")
    public ParserPoolHolder parserPoolHolder() {
        return new ParserPoolHolder();
    }

    @Bean(initMethod = "initialize")
    @ConditionalOnMissingBean
    @Profile("saml")
    public ParserPool parserPool() {
        return new StaticBasicParserPool();
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("saml")
    public SAMLLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    @Bean
    @Profile("saml")
    public static ServiceProviderBuilder serviceProviderBuilder() {
        return new ServiceProviderBuilder();
    }

    /**
     * Default Web Security Configurer that delegates configuration of the service provider to {@link
     * ServiceProviderConfigurer}
     */
    @ConditionalOnMissingBean(SAMLConfigurerBean.class)
    @Configuration
    @Profile("saml")
    static class SAMLWebSecurityConfigurer extends WebSecurityConfigurerAdapter implements Ordered {

        @Autowired(required = false)
        private List<ServiceProviderConfigurer> serviceProviderConfigurers = Collections.emptyList();

        @Override
        public void configure(WebSecurity web) throws Exception {
            serviceProviderConfigurers.forEach(unchecked(c -> c.configure(web)));
        }

        @Bean
        SAMLConfigurerBean saml() {
            return new SAMLConfigurerBean();
        }

        @Override
        @ConditionalOnMissingBean
        @Bean
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            auth.parentAuthenticationManager(null);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.httpBasic()
                .disable()
                .csrf()
                .disable()
                .anonymous()
            .and()
                .apply(saml())
                .serviceProvider(serviceProviderConfigurers)
            .http()
                .authorizeRequests()
                .requestMatchers(saml().endpointsMatcher()).permitAll();

            serviceProviderConfigurers.forEach(unchecked(spc -> spc.configure(http)));

            http
                .authorizeRequests()
                .anyRequest()
                .authenticated();
            // @formatter:on
        }

        @Override
        public int getOrder() {
            return -17;
        }
    }

    @Override
    public void afterPropertiesSet() {
        //All existing beans are thrown as shared objects to the ServiceProviderSecurityBuilder, which will wire all
        //beans/objects related to spring security SAML.
        serviceProviderBuilder.setSharedObject(ParserPool.class, ParserPoolHolder.getPool());
        serviceProviderBuilder.setSharedObject(WebSSOProfileConsumerImpl.class, (WebSSOProfileConsumerImpl) webSSOProfileConsumer);
        serviceProviderBuilder.setSharedObject(WebSSOProfileConsumerHoKImpl.class, hokWebSSOProfileConsumer);
        serviceProviderBuilder.setSharedObject(ServiceProviderEndpoints.class, new ServiceProviderEndpoints());
        serviceProviderBuilder.setSharedObject(ResourceLoader.class, resourceLoader);
        serviceProviderBuilder.setSharedObject(SAMLSSOProperties.class, sAMLSsoProperties);
        serviceProviderBuilder.setSharedObject(ExtendedMetadata.class, extendedMetadata);
        serviceProviderBuilder.setSharedObject(LocalExtendedMetadata.class, localExtendedMetadata);
        serviceProviderBuilder.setSharedObject(SAMLAuthenticationProvider.class, samlAuthenticationProvider);
        serviceProviderBuilder.setSharedObject(SAMLContextProvider.class, samlContextProvider);
        serviceProviderBuilder.setSharedObject(KeyManager.class, keyManager);
        serviceProviderBuilder.setSharedObject(MetadataManager.class, metadataManager);
        serviceProviderBuilder.setSharedObject(MetadataGenerator.class, metadataGenerator);
        serviceProviderBuilder.setSharedObject(SAMLProcessor.class, samlProcessor);
        serviceProviderBuilder.setSharedObject(WebSSOProfile.class, webSSOProfile);
        serviceProviderBuilder.setSharedObject(WebSSOProfileECPImpl.class, ecpProfile);
        serviceProviderBuilder.setSharedObject(WebSSOProfileHoKImpl.class, hokWebSSOProfile);
        serviceProviderBuilder.setSharedObject(SingleLogoutProfile.class, sloProfile);
        serviceProviderBuilder.setSharedObject(WebSSOProfileConsumer.class, webSSOProfileConsumer);
        serviceProviderBuilder.setSharedObject(WebSSOProfileConsumerHoKImpl.class, hokWebSSOProfileConsumer);
        serviceProviderBuilder.setSharedObject(SAMLLogger.class, samlLogger);
    }

}
