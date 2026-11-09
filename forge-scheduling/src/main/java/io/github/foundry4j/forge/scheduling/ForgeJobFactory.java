package io.github.foundry4j.forge.scheduling;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/// Creates Quartz job instances through the Spring bean factory.
///
/// Going through `createBean` instead of plain reflection means jobs support constructor
/// injection, method injection and every `BeanPostProcessor`, which the default Quartz
/// factory does not offer.
public class ForgeJobFactory extends SpringBeanJobFactory {

    private final AutowireCapableBeanFactory beanFactory;

    /// Creates the factory.
    public ForgeJobFactory(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /// {@inheritDoc}
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) {
        return beanFactory.createBean(bundle.getJobDetail().getJobClass());
    }
}
